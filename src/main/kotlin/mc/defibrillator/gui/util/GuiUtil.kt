/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.util

import kotlinx.coroutines.*
import mc.defibrillator.DefibState
import mc.defibrillator.Defibrillator
import mc.defibrillator.exception.SafeCoroutineExit
import mc.defibrillator.gui.NBTScreenHandlerFactory
import mc.defibrillator.gui.data.GuiStateComposite
import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.gui.data.RightClickMode
import mc.defibrillator.util.asHashtag
import mc.defibrillator.util.copyableText
import mc.defibrillator.util.delete
import mc.defibrillator.util.retrieve
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

typealias GuiAction = (Int, GuiStateComposite) -> Unit

/**
 * Opens a NBT editing/viewing gui
 *
 * @param player The player that will have the screen opened
 * @param title Title of the screen
 * @param state A MenuState that will be passed around
 * @param onClose A method that will be called when the screen is closed (if not suppressed)
 */
fun openNBTGui(
    player: ServerPlayerEntity,
    title: Text,
    state: MenuState,
    allowEditing: Boolean = true,
    onClose: (MenuState) -> Unit
): MenuState {
    val factory = NBTScreenHandlerFactory(title, state, allowEditing, onClose)
    player.openHandledScreen(factory)
    return state
}

/**
 * Prompts the player for a text entry in chat with a 30s timeout
 */
@ExperimentalTime
fun getTextEntry(composite: GuiStateComposite, forMessage: String, onComplete: (String?) -> Unit) {
    composite.state.suppressOnClose.set(true)

    val player = composite.player

    GlobalScope.launch(Defibrillator.crashHandler) {
        try {
            player.closeHandledScreen()
            player.sendMessage(LiteralText("Type in chat for $forMessage"), false)

            val topRoutine = this

            launch(Defibrillator.crashHandler) {
                delay(30.toDuration(DurationUnit.SECONDS))
                player.sendMessage(LiteralText("Timed out!").formatted(Formatting.RED), false)
                DefibState.awaitingInput.remove(player)
                topRoutine.cancel()
            }

            DefibState.awaitingInput[player] = {
                DefibState.readInput.computeIfAbsent(player) { mutableListOf() }
                DefibState.readInput[player]!!.add(it)
                player.sendMessage(LiteralText("${forMessage.capitalize()} set"), false)
                topRoutine.cancel(SafeCoroutineExit())
            }

            while (isActive) {
                delay(10L)
            }
        } catch (err: Throwable) {
            if (err is SafeCoroutineExit) {
                onComplete(DefibState.readInput[composite.player]?.removeFirstOrNull())
            }
            composite.state.suppressOnClose.set(false)
        }
    }
}

/**
 * Converts an ItemConvertible into an ItemStack with a custom name and a tag that makes it get cleaned up from creative
 */
fun ItemConvertible.guiStack(name: String = "", nameColor: Formatting = Formatting.WHITE): ItemStack {
    return ItemStack(this)
        .setCustomName(
            LiteralText(name)
                .setStyle(
                    Style.EMPTY
                        .withItalic(false)
                        .withFormatting(nameColor)
                )
        )
        .apply { orCreateTag.putBoolean("defib-DELETE", true) }
}

/**
 * Generates an ItemStack and GuiAction for the tag with the given name
 */
@ExperimentalTime
fun Tag.toGuiEntry(name: String): Pair<ItemStack, GuiAction> {
    return when (this) {
        is ByteArrayTag -> Pair(Items.WRITABLE_BOOK.guiStack("$name (Byte)")) { data, composite ->
            modeOrOpen(data, name, composite)
        }

        is ByteTag -> Pair(Items.PLAYER_HEAD.guiStack("$name: $this (Byte)").asHashtag()) { data, composite ->
            modeOrDo(data, name, composite) { _, _ -> }
        }

        is CompoundTag -> Pair(Items.SHULKER_BOX.guiStack(name)) { data, composite ->
            modeOrOpen(data, name, composite)
        }

        is DoubleTag -> Pair(Items.PLAYER_HEAD.guiStack("$name: $this (Double)").asHashtag()) { data, composite ->
            modeOrDo(data, name, composite) { _, _ -> }
        }

        is FloatTag -> Pair(Items.PLAYER_HEAD.guiStack("$name: $this (Float)").asHashtag()) { data, composite ->
            modeOrDo(data, name, composite) { _, _ -> }
        }

        is IntArrayTag -> Pair(Items.WRITABLE_BOOK.guiStack("$name (Int)")) { data, composite ->
            modeOrOpen(data, name, composite)
        }

        is IntTag -> Pair(Items.PLAYER_HEAD.guiStack("$name: $this (Int)").asHashtag()) { data, composite ->
            modeOrDo(data, name, composite) { _, _ -> }
        }

        is ListTag -> Pair(Items.WRITABLE_BOOK.guiStack(name)) { data, composite ->
            modeOrOpen(data, name, composite)
        }

        is LongArrayTag -> Pair(Items.WRITABLE_BOOK.guiStack("$name (Long)")) { data, composite ->
            modeOrOpen(data, name, composite)
        }

        is LongTag -> Pair(Items.PLAYER_HEAD.guiStack("$name: $this (Long)").asHashtag()) { data, composite ->
            modeOrDo(data, name, composite) { _, _ -> }
        }

        is ShortTag -> Pair(Items.PLAYER_HEAD.guiStack("$name: $this (Short)").asHashtag()) { data, composite ->
            modeOrDo(data, name, composite) { _, _ -> }
        }

        is StringTag -> Pair(Items.PAPER.guiStack("$name: $this")) { data, composite ->
            modeOrDo(data, name, composite) { _, _ -> }
        }

        else -> TODO("Handle unknown tags (if they somehow exist)")
    }
}

/**
 * Executes the mode action if right click, executes the default action and refreshes
 */
@ExperimentalTime
fun modeOrDo(data: Int, name: String, composite: GuiStateComposite, action: GuiAction) {
    if (data == 1) {
        when (composite.state.clickMode) {
            RightClickMode.PASS -> action(data, composite)
            RightClickMode.DELETE -> composite.state.getActiveTag().delete(name)
            RightClickMode.COPY -> copy(composite, name)
        }
    } else {
        action(data, composite)
    }
    composite.state.factory?.makeAndUpdateNBTViewer(composite.defaultedInventory, composite.state)
}

/**
 * Executes the mode action if right click, otherwise adds the tag to the keyStack and refreshes
 */
@ExperimentalTime
fun modeOrOpen(data: Int, name: String, composite: GuiStateComposite) {
    if (data == 1) {
        when (composite.state.clickMode) {
            RightClickMode.PASS -> composite.state.keyStack.add(name)
            RightClickMode.DELETE -> composite.state.getActiveTag().delete(name)
            RightClickMode.COPY -> copy(composite, name)
        }
    } else {
        composite.state.keyStack.add(name)
    }
    composite.state.factory?.makeAndUpdateNBTViewer(composite.defaultedInventory, composite.state)
}

/**
 * Sends a message to the player with text that can be clicked to copy the string representation of the tag
 */
private fun copy(composite: GuiStateComposite, name: String) {
    val tag = composite.state.getActiveTag().retrieve(name)
    composite.player.sendMessage(
        copyableText(
            tag.toString(),
            "Click to copy ${composite.state.keyStack.joinToString("/")}/$name"
        ), false
    )
}
