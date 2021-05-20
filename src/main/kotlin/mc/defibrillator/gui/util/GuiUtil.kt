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
import mc.defibrillator.gui.AdvancementScreenHandlerFactory
import mc.defibrillator.gui.NBTScreenHandlerFactory
import mc.defibrillator.gui.data.AdvancementMenuState
import mc.defibrillator.gui.data.NBTMenuState
import mc.defibrillator.gui.data.RightClickMode
import mc.defibrillator.util.asHashtag
import mc.defibrillator.util.copyableText
import mc.defibrillator.util.delete
import mc.defibrillator.util.retrieve
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.github.p03w.quecee.api.util.guiStack
import org.github.p03w.quecee.util.GuiAction
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/**
 * Opens a NBT editing/viewing gui
 *
 * @param player The player that will have the screen opened
 * @param title Title of the screen
 * @param state A NBTMenuState that will be passed around
 * @param allowEditing if editing data should be allowed
 * @param onClose A method that will be called when the screen is closed (if not suppressed)
 */
fun openNBTGui(
    player: ServerPlayerEntity,
    title: Text,
    state: NBTMenuState,
    allowEditing: Boolean = true,
    onClose: (NBTMenuState) -> Unit
): NBTMenuState {
    val factory = NBTScreenHandlerFactory(player, title, state, allowEditing, onClose)
    player.openHandledScreen(factory)
    return state
}

/**
 * Opens an Advancement editing/viewing gui
 *
 * @param player The player that will have the screen opened
 * @param title Title of the screen
 * @param state A NBTMenuState that will be passed around
 * @param allowEditing if editing data should be allowed
 * @param onClose A method that will be called when the screen is closed (if not suppressed)
 */
fun openAdvancementGui(
    player: ServerPlayerEntity,
    title: Text,
    state: AdvancementMenuState,
    allowEditing: Boolean = true,
    onClose: (AdvancementMenuState) -> Unit
): AdvancementMenuState {
    val factory = AdvancementScreenHandlerFactory(player, title, state, allowEditing, onClose)
    player.openHandledScreen(factory)
    return state
}


/**
 * Prompts the player for a text entry in chat with a 30s timeout
 */
@ExperimentalTime
fun getTextEntry(state: NBTMenuState, forMessage: String, onComplete: (String?) -> Unit) {
    state.suppressOnClose.set(true)

    DefibState.coroutineScope.launch(Defibrillator.crashHandler) {
        try {
            state.player.closeHandledScreen()
            state.player.sendMessage(LiteralText("Type in chat for $forMessage"), false)

            val topRoutine = this

            launch(Defibrillator.crashHandler) {
                delay(30.toDuration(DurationUnit.SECONDS))
                state.player.sendMessage(LiteralText("Timed out!").formatted(Formatting.RED), false)
                DefibState.awaitingInput.remove(state.player)
                topRoutine.cancel()
            }

            DefibState.awaitingInput[state.player] = {
                DefibState.readInput.computeIfAbsent(state.player) { mutableListOf() }
                DefibState.readInput[state.player]!!.add(it)
                state.player.sendMessage(LiteralText("${forMessage.capitalize()} set"), false)
                topRoutine.cancel(SafeCoroutineExit())
            }

            while (isActive) {
                delay(10L)
            }
        } catch (err: Throwable) {
            if (err is SafeCoroutineExit) {
                onComplete(DefibState.readInput[state.player]?.removeFirstOrNull())
            }
            state.suppressOnClose.set(false)
        }
    }
}

/**
 * Generates an ItemStack and GuiAction for the tag with the given name
 */
@ExperimentalTime
fun Tag.toGuiEntry(name: String): Pair<ItemStack, GuiAction<NBTMenuState>> {
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
fun modeOrDo(data: Int, name: String, state: NBTMenuState, action: GuiAction<NBTMenuState>) {
    if (data == 1) {
        when (state.clickMode) {
            RightClickMode.PASS -> action(data, state)
            RightClickMode.DELETE -> state.getActiveTag().delete(name)
            RightClickMode.COPY -> copy(state, name)
        }
    } else {
        action(data, state)
    }
    state.factory?.rebuild()
}

/**
 * Executes the mode action if right click, otherwise adds the tag to the keyStack and refreshes
 */
@ExperimentalTime
fun modeOrOpen(data: Int, name: String, state: NBTMenuState) {
    if (data == 1) {
        when (state.clickMode) {
            RightClickMode.PASS -> state.keyStack.add(name)
            RightClickMode.DELETE -> state.getActiveTag().delete(name)
            RightClickMode.COPY -> copy(state, name)
        }
    } else {
        state.keyStack.add(name)
    }
    state.factory?.rebuild()
}

/**
 * Sends a message to the player with text that can be clicked to copy the string representation of the tag
 */
private fun copy(state: NBTMenuState, name: String) {
    val tag = state.getActiveTag().retrieve(name)
    state.player.sendMessage(
        copyableText(
            tag.toString(),
            "Click to copy ${state.keyStack.joinToString("/")}/$name"
        ), false
    )
}
