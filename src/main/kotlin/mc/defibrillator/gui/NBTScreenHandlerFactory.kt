/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui

import mc.defibrillator.DefibState
import mc.defibrillator.Defibrillator
import mc.defibrillator.exception.InvalidArgument
import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.gui.data.RightClickMode
import mc.defibrillator.gui.util.getTextEntry
import mc.defibrillator.gui.util.toGuiEntry
import mc.defibrillator.util.*
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.item.Items
import net.minecraft.nbt.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import org.github.p03w.quecee.api.gui.QueCeeHandlerFactory
import org.github.p03w.quecee.api.gui.inventory.ItemActionMap
import org.github.p03w.quecee.api.util.guiStack
import kotlin.time.ExperimentalTime

class NBTScreenHandlerFactory(
    private val player: ServerPlayerEntity,
    private val title: Text,
    menuState: MenuState,
    private val allowEditing: Boolean,
    onClose: (MenuState) -> Unit
) : QueCeeHandlerFactory<MenuState>(
    title,
    6,
    menuState,
    onClose
) {
    init {
        menuState.factory = this
    }

    override fun getDisplayName(): Text {
        return title
    }

    @ExperimentalTime
    fun makeNBTViewer(
        state: MenuState
    ): ItemActionMap<MenuState> {
        val actionMap = ItemActionMap<MenuState> {
            // Last page if not on first page
            if (state.page > 0) {
                addEntry(
                    0,
                    Items.PLAYER_HEAD.guiStack("Last Page").applySkull(BACK_TEXTURE, BACK_ID)
                ) { _, _ ->
                    state.page -= 1
                    rebuild()
                }
            }

            // Delete option
            when (state.clickMode) {
                RightClickMode.PASS -> addEntry(2, Items.GLASS.guiStack("Right Click: None")) { _, _ ->
                    state.clickMode = RightClickMode.COPY
                    rebuild()
                }
                RightClickMode.COPY -> addEntry(2, Items.EMERALD.guiStack("Right Click: Copy")) { _, _ ->
                    state.clickMode = RightClickMode.DELETE
                    rebuild()
                }
                RightClickMode.DELETE -> addEntry(
                    2,
                    Items.TNT.guiStack("Right Click: Delete", Formatting.RED).withGlint()
                ) { _, _ ->
                    state.clickMode = RightClickMode.PASS
                    rebuild()
                }
            }

            // Up / parent
            addEntry(3, Items.PLAYER_HEAD.guiStack("Up/Parent").applySkull(OUT_TEXTURE, OUT_ID)) { _, _ ->
                state.keyStack.removeLastOrNull()
                state.page = 0
                rebuild()
            }

            // Info
            val info = (listOf(
                "Current page: ${state.page}",
                "Entries: ${state.getAvailableKeys().size}",
                "Path:",
                "  <ROOT>"
            ) union (state.keyStack.map { "  $it" })).toList()
            addEntry(
                4, Items.PLAYER_HEAD
                    .guiStack("Info", Formatting.GOLD)
                    .applySkull(INFO_TEXTURE, INFO_ID)
                    .withLore(info)
            ) { _, _ -> }

            // Cancel button
            addEntry(5, Items.BARRIER.guiStack("Cancel changes (Right Click)")) { data, _ ->
                if (data == 1) {
                    state.suppressOnClose.set(true)
                    player.closeHandledScreen()
                    player.sendSystemMessage(LiteralText("Discarded changes"), Util.NIL_UUID)
                    DefibState.activeSessions.remove(state.playerUUID)
                    state.suppressOnClose.set(false)
                }
            }

            // Add entry
            if (allowEditing) {
                addEntry(
                    6,
                    Items.PLAYER_HEAD.guiStack("Add Tag/Entry").applySkull(PLUS_TEXTURE, PLUS_ID)
                ) { _, _ ->
                    rebuild()
                }
            }

            // Next page if more pages to go to
            if (state.getAvailableKeys().size - ((state.page + 1) * MenuState.PER_PAGE) > 0) {
                addEntry(
                    8,
                    Items.PLAYER_HEAD.guiStack("Next Page").applySkull(NEXT_TEXTURE, NEXT_ID)
                ) { _, _ ->
                    state.page += 1
                    rebuild()
                }
            }

            // Add all the tags
            var index = 9
            try {
                for (entry in (state.page * MenuState.PER_PAGE) until ((state.page + 1) * MenuState.PER_PAGE)) {
                    val possible = state.getAvailableKeys()[entry]
                    try {
                        val converted = state.getActiveTag().retrieve(possible)!!.toGuiEntry(possible)
                        addEntry(index++, converted.first, converted.second)
                    } catch (ignored: NotImplementedError) {
                        println(
                            "Not implemented: $possible ${
                                state.getActiveTag().retrieve(possible)!!::class.java
                            }"
                        )
                    }
                }
            } catch (ignored: IndexOutOfBoundsException) {
            }
        }

        state.isInAddMenu = false
        return actionMap
    }

    @ExperimentalTime
    private fun makeNBTAdder(
        state: MenuState
    ): ItemActionMap<MenuState> {
        val actionMap = ItemActionMap<MenuState> {
            // Cancel
            addEntry(0, Items.PLAYER_HEAD.guiStack("Cancel").applySkull(OUT_TEXTURE, OUT_ID)) { _, _ ->
                state.page = 0
                rebuild()
            }

            fun punchInAndExit(tag: Tag, name: String, state: MenuState) {
                when (val active = state.getActiveTag()) {
                    is CompoundTag -> active.put(name, tag)
                    is AbstractListTag<*> -> {
                        if (active.elementType == tag.type || active.elementType == 0.toByte()) {
                            try {
                                val index = name.toInt()
                                active.setTag(index, tag)
                            } catch (ignored: Throwable) {
                                active.addTag(active.size, tag)
                            }
                        }
                    }
                }
                state.page = 0
                rebuild()
                player.openHandledScreen(this@NBTScreenHandlerFactory)
            }

            var index = 9

            val active = state.getActiveTag()

            fun canAdd(nbtType: Int): Boolean {
                return if (active is AbstractListTag<*>) {
                    active.wouldAccept(nbtType)
                } else {
                    true
                }
            }

            if (canAdd(NbtType.COMPOUND))
                addEntry(index++, Items.SHULKER_BOX.guiStack("Compound Tag")) { _, composite ->
                    getTextEntry(state, "compound name") {
                        punchInAndExit(CompoundTag(), it ?: "PLACEHOLDER", composite)
                    }
                }

            if (canAdd(NbtType.LIST))
                addEntry(index++, Items.WRITABLE_BOOK.guiStack("List Tag")) { _, composite ->
                    getTextEntry(composite, "list name") {
                        punchInAndExit(ListTag(), it ?: "PLACEHOLDER", composite)
                    }
                }

            if (canAdd(NbtType.INT_ARRAY))
                addEntry(index++, Items.WRITABLE_BOOK.guiStack("Int Array Tag")) { _, composite ->
                    getTextEntry(composite, "int array name") {
                        punchInAndExit(IntArrayTag(listOf()), it ?: "PLACEHOLDER", composite)
                    }
                }

            if (canAdd(NbtType.BYTE_ARRAY))
                addEntry(index++, Items.WRITABLE_BOOK.guiStack("Byte Array Tag")) { _, composite ->
                    getTextEntry(composite, "byte array name") {
                        punchInAndExit(ByteArrayTag(listOf()), it ?: "PLACEHOLDER", composite)
                    }
                }

            if (canAdd(NbtType.LONG_ARRAY))
                addEntry(index++, Items.WRITABLE_BOOK.guiStack("Long Array Tag")) { _, composite ->
                    getTextEntry(composite, "long array name") {
                        punchInAndExit(LongArrayTag(listOf()), it ?: "PLACEHOLDER", composite)
                    }
                }

            // Give a generic number option if multiple would be supported
            // And enabled in config
            if (canAdd(NbtType.END) && Defibrillator.config.collapseNumberOptions) {
                addEntry(index++, Items.PLAYER_HEAD.guiStack("Number").asHashtag()) { _, composite ->
                    getDoubleTextEntry(composite, "number value") { name, value ->
                        punchInAndExit(convertEntryToNumberTag(value), name, composite)
                    }
                }
            } else {
                if (canAdd(NbtType.BYTE))
                    addEntry(index++, Items.PLAYER_HEAD.guiStack("Byte").asHashtag()) { _, composite ->
                        getDoubleTextEntry(composite, "byte value") { name, value ->
                            punchInAndExit(ByteTag.of(value.toInt().toByte()), name, composite)
                        }
                    }

                if (canAdd(NbtType.FLOAT))
                    addEntry(index++, Items.PLAYER_HEAD.guiStack("Float").asHashtag()) { _, composite ->
                        getDoubleTextEntry(composite, "float value") { name, value ->
                            punchInAndExit(FloatTag.of(value.toFloat()), name, composite)
                        }
                    }

                if (canAdd(NbtType.DOUBLE))
                    addEntry(index++, Items.PLAYER_HEAD.guiStack("Double").asHashtag()) { _, composite ->
                        getDoubleTextEntry(composite, "double value") { name, value ->
                            punchInAndExit(DoubleTag.of(value.toDouble()), name, composite)
                        }
                    }

                if (canAdd(NbtType.INT))
                    addEntry(index++, Items.PLAYER_HEAD.guiStack("Int").asHashtag()) { _, composite ->
                        getDoubleTextEntry(composite, "integer value") { name, value ->
                            punchInAndExit(IntTag.of(value.toInt()), name, composite)
                        }
                    }

                if (canAdd(NbtType.LONG))
                    addEntry(index++, Items.PLAYER_HEAD.guiStack("Long").asHashtag()) { _, composite ->
                        getDoubleTextEntry(composite, "long value") { name, value ->
                            punchInAndExit(LongTag.of(value.toLong()), name, composite)
                        }
                    }

                if (canAdd(NbtType.SHORT))
                    addEntry(index++, Items.PLAYER_HEAD.guiStack("Short").asHashtag()) { _, composite ->
                        getDoubleTextEntry(composite, "short value") { name, value ->
                            punchInAndExit(ShortTag.of(value.toShort()), name, composite)
                        }
                    }
            }

            if (canAdd(NbtType.STRING))
                addEntry(index, Items.PAPER.guiStack("String")) { _, composite ->
                    getDoubleTextEntry(composite, "string value") { name, value ->
                        punchInAndExit(StringTag.of(value), name, composite)
                    }
                }
        }
        state.isInAddMenu = true
        return actionMap
    }


    @ExperimentalTime
    private fun getDoubleTextEntry(
        state: MenuState,
        value2For: String,
        onSuccess: (String, String) -> Unit
    ) {
        getTextEntry(state, "tag name/index") { value1 ->
            getTextEntry(state, value2For) { value2 ->
                try {
                    onSuccess(value1 ?: "PLACEHOLDER", value2 ?: "0")
                } catch (err: Throwable) {
                    player.sendMessage(
                        LiteralText("Failed to parse and/or handle!").formatted(Formatting.RED),
                        false
                    )
                    err.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val OUT_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFkNmM4MWY4OTlhNzg1ZWNmMjZiZTFkYzQ4ZWFlMmJjZmU3NzdhODYyMzkwZjU3ODVlOTViZDgzYmQxNGQifX19"
        private val OUT_ID = listOf(894754875, -1741863695, -1947963016, 1537745656)

        private const val PLUS_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjBiNTVmNzQ2ODFjNjgyODNhMWMxY2U1MWYxYzgzYjUyZTI5NzFjOTFlZTM0ZWZjYjU5OGRmMzk5MGE3ZTcifX19"
        private val PLUS_ID = listOf(-2043523718, -777107838, -1082670654, 1929131299)

        private const val NEXT_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzg2MTg1YjFkNTE5YWRlNTg1ZjE4NGMzNGYzZjNlMjBiYjY0MWRlYjg3OWU4MTM3OGU0ZWFmMjA5Mjg3In19fQ=="
        private val NEXT_ID = listOf(-1924610023, 724324220, -1668086297, 1774964076)

        private const val BACK_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ3M2NmNjZkMzFiODNjZDhiODY0NGMxNTk1OGMxYjczYzhkOTczMjNiODAxMTcwYzFkODg2NGJiNmE4NDZkIn19fQ=="
        private val BACK_ID = listOf(-1851153710, -1126085201, -2100427987, 895449682)

        private const val INFO_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjBiZTIwNWQ2MDg5ZTQ5ODIyNWU1MTFkYmMzYzFkM2JmZDA3MzkwMzlkYTRkMmUyMzFhZWEyYmIxZjc2ZjMxYSJ9fX0="
        private val INFO_ID = listOf(-1255748294, -2041099761, -1825112504, 2136088914)

        private fun convertEntryToNumberTag(input: String): AbstractNumberTag {
            try {
                val value = input.cleanNumber()

                // Decimal, default to double if not specified
                if (input.contains('.')) {
                    if (input.endsWith('F', true)) {
                        return FloatTag.of(value.toFloat())
                    }
                    return DoubleTag.of(value.toDouble())
                }

                // Fixed number, default to int
                if (input.endsWith("L", true)) {
                    return LongTag.of(value.toLong())
                }

                if (input.endsWith("B", true)) {
                    return ByteTag.of(value.toByte())
                }

                if (input.endsWith("S", true)) {
                    return ShortTag.of(value.toShort())
                }

                return IntTag.of(value.toInt())
            } catch (err: Throwable) {
                err.printStackTrace()
                throw InvalidArgument()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun generateActionMap(state: MenuState): ItemActionMap<MenuState> {
        return if (state.isInAddMenu) {
            makeNBTAdder(state)
        } else {
            makeNBTViewer(state)
        }
    }
}
