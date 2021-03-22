/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui

import mc.defibrillator.gui.data.GuiStateComposite
import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.gui.data.RightClickMode
import mc.defibrillator.gui.inventory.ItemActionMap
import mc.defibrillator.gui.inventory.SimpleDefaultedInventory
import mc.defibrillator.gui.util.*
import mc.defibrillator.util.retrieve
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Items
import net.minecraft.nbt.*
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.ExperimentalTime

class ScreenHandlerFactory(
    private val title: String,
    private val state: MenuState,
    private val onClose: (MenuState) -> Unit
) : NamedScreenHandlerFactory {

    init {
        state.factory = this
    }

    @ExperimentalTime
    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler {
        val defaultedInv = newDefaulted()
        val actionMap = makeAndUpdateNBTViewer(defaultedInv, state)
        return MainScreenHandler(syncId, inv, defaultedInv, actionMap, state, onClose)
    }

    override fun getDisplayName(): Text {
        return LiteralText(title)
    }

    @ExperimentalTime
    fun makeAndUpdateNBTViewer(
        defaultedInventory: SimpleDefaultedInventory, state: MenuState
    ): ItemActionMap {
        val actionMap = ItemActionMap {
            // Last page if not on first page
            if (state.page > 0) {
                addEntry(
                    0,
                    Items.PLAYER_HEAD.guiStack("Last Page").applySkull(BACK_TEXTURE, BACK_ID)
                ) { _, composite ->
                    composite.state.page -= 1
                    makeAndUpdateNBTViewer(defaultedInventory, composite.state)
                }
            }

            // Delete option
            val deleteOn = state.clickMode == RightClickMode.DELETE
            addEntry(
                2, Items.TNT
                    .guiStack(
                        "Delete (${if (deleteOn) "DELETES ON RIGHT-CLICK" else "OFF"})",
                        if (deleteOn) Formatting.RED else Formatting.WHITE
                    )
                    .withGlint(deleteOn)
            ) { _, composite ->
                if (deleteOn) {
                    composite.state.clickMode = RightClickMode.PASS
                } else {
                    composite.state.clickMode = RightClickMode.DELETE
                }
                makeAndUpdateNBTViewer(defaultedInventory, composite.state)
            }

            // Up / parent
            addEntry(3, Items.PLAYER_HEAD.guiStack("Up/Parent").applySkull(OUT_TEXTURE, OUT_ID)) { _, composite ->
                composite.state.keyStack.removeLastOrNull()
                composite.state.page = 0
                makeAndUpdateNBTViewer(defaultedInventory, composite.state)
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

            // Copy option
            val copyOn = state.clickMode == RightClickMode.COPY
            addEntry(
                5,
                Items.EMERALD
                    .guiStack("Copy to Clipboard (${if (copyOn) "copy on right-click" else "off"})")
                    .withGlint(copyOn)
            ) { _, composite ->
                if (copyOn) {
                    composite.state.clickMode = RightClickMode.PASS
                } else {
                    composite.state.clickMode = RightClickMode.COPY
                }
                makeAndUpdateNBTViewer(defaultedInventory, composite.state)
            }

            // Add entry
            addEntry(6, Items.PLAYER_HEAD.guiStack("Add Tag/Entry").applySkull(PLUS_TEXTURE, PLUS_ID)) { _, composite ->
                makeAndUpdateNBTTagAdder(defaultedInventory, composite.state)
            }

            // Next page if more pages to go to
            if (state.getAvailableKeys().size - ((state.page + 1) * MenuState.PER_PAGE) > 0) {
                addEntry(
                    8,
                    Items.PLAYER_HEAD.guiStack("Next Page").applySkull(NEXT_TEXTURE, NEXT_ID)
                ) { _, composite ->
                    composite.state.page += 1
                    makeAndUpdateNBTViewer(defaultedInventory, composite.state)
                }
            }

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

        val clean = newDefaulted()
        actionMap.copyIntoInventory(clean)
        for (slot in 0 until clean.size()) {
            defaultedInventory.setStack(slot, clean.getStack(slot))
        }
        state.handler?.actions = actionMap
        return actionMap
    }

    @ExperimentalTime
    private fun makeAndUpdateNBTTagAdder(
        defaultedInventory: SimpleDefaultedInventory, state: MenuState
    ): ItemActionMap {
        val actionMap = ItemActionMap {
            // Cancel
            addEntry(0, Items.PLAYER_HEAD.guiStack("Cancel").applySkull(OUT_TEXTURE, OUT_ID)) { _, composite ->
                composite.state.page = 0
                makeAndUpdateNBTViewer(defaultedInventory, composite.state)
            }

            fun punchInAndExit(tag: Tag, name: String, composite: GuiStateComposite) {
                when (val active = composite.state.getActiveTag()) {
                    is CompoundTag -> active.put(name, tag)
                    is ListTag -> {
                        if (active.elementType == tag.type || active.elementType == 0.toByte()) {
                            try {
                                val index = name.toInt()
                                active[index] = tag
                            } catch (ignored: Throwable) {
                                active.add(tag)
                            }
                        }
                    }
                }
                composite.state.page = 0
                makeAndUpdateNBTViewer(defaultedInventory, composite.state)
                composite.player.openHandledScreen(this@ScreenHandlerFactory)
            }

            var index = 9
            addEntry(index++, Items.SHULKER_BOX.guiStack("Compound Tag")) { _, composite ->
                getTextEntry(composite, "compound name") {
                    punchInAndExit(CompoundTag(), it ?: "PLACEHOLDER", composite)
                }
            }
            addEntry(index++, Items.WRITABLE_BOOK.guiStack("List Tag")) { _, composite ->
                getTextEntry(composite, "list name") {
                    punchInAndExit(ListTag(), it ?: "PLACEHOLDER", composite)
                }
            }
            addEntry(index++, Items.PLAYER_HEAD.guiStack("Byte").asHashtag()) { _, composite ->
                getDoubleTextEntry(composite, "byte value") { name, value ->
                    punchInAndExit(ByteTag.of(value.toInt().toByte()), name, composite)
                }
            }
            addEntry(index++, Items.PLAYER_HEAD.guiStack("Float").asHashtag()) { _, composite ->
                getDoubleTextEntry(composite, "float value") { name, value ->
                    punchInAndExit(FloatTag.of(value.toFloat()), name, composite)
                }
            }
            addEntry(index++, Items.PLAYER_HEAD.guiStack("Double").asHashtag()) { _, composite ->
                getDoubleTextEntry(composite, "double value") { name, value ->
                    punchInAndExit(DoubleTag.of(value.toDouble()), name, composite)
                }
            }
            addEntry(index++, Items.PLAYER_HEAD.guiStack("Int").asHashtag()) { _, composite ->
                getDoubleTextEntry(composite, "integer value") { name, value ->
                    punchInAndExit(IntTag.of(value.toInt()), name, composite)
                }
            }
            addEntry(index++, Items.PLAYER_HEAD.guiStack("Long").asHashtag()) { _, composite ->
                getDoubleTextEntry(composite, "long value") { name, value ->
                    punchInAndExit(LongTag.of(value.toLong()), name, composite)
                }
            }
            addEntry(index++, Items.PLAYER_HEAD.guiStack("Short").asHashtag()) { _, composite ->
                getDoubleTextEntry(composite, "short value") { name, value ->
                    punchInAndExit(ShortTag.of(value.toShort()), name, composite)
                }
            }
            addEntry(index, Items.PAPER.guiStack("String")) { _, composite ->
                getDoubleTextEntry(composite, "string value") { name, value ->
                    punchInAndExit(StringTag.of(value), name, composite)
                }
            }
        }

        val clean = newDefaulted()
        actionMap.copyIntoInventory(clean)
        for (slot in 0 until clean.size()) {
            defaultedInventory.setStack(slot, clean.getStack(slot))
        }
        state.handler?.actions = actionMap
        return actionMap
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

        private fun newDefaulted(): SimpleDefaultedInventory {
            return SimpleDefaultedInventory(
                9 * 6,
                Items.LIGHT_GRAY_STAINED_GLASS_PANE.guiStack()
            )
        }

        @ExperimentalTime
        private fun getDoubleTextEntry(
            composite: GuiStateComposite,
            value2For: String,
            onSuccess: (String, String) -> Unit
        ) {
            getTextEntry(composite, "tag name/index") { value1 ->
                getTextEntry(composite, value2For) { value2 ->
                    try {
                        onSuccess(value1 ?: "PLACEHOLDER", value2 ?: "0")
                    } catch (err: Throwable) {
                        composite.player.sendMessage(
                            LiteralText("Failed to parse and/or handle!").formatted(Formatting.RED),
                            false
                        )
                        err.printStackTrace()
                    }
                }
            }
        }
    }
}
