/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import com.github.p03w.aegis.aegisCommand
import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import mc.defibrillator.command.OfflinePlayerSuggester
import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.gui.util.openNBTGui
import mc.defibrillator.util.copyableText
import mc.microconfig.MicroConfig
import me.basiqueevangelist.nevseti.OfflineDataCache
import me.basiqueevangelist.nevseti.OfflineDataChanged
import me.basiqueevangelist.nevseti.OfflineNameCache
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.UuidArgumentType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.text.MutableText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Util
import kotlin.time.ExperimentalTime

class Defibrillator : ModInitializer {
    @ExperimentalTime
    @ExperimentalStdlibApi
    override fun onInitialize() {
        println("----")
        println("DEFIBRILLATOR IS IN BETA")
        println("PLEASE REPORT ANY AND ALL ERRORS")
        println("----")

        // Remove GUI items from players
        ServerTickEvents.END_WORLD_TICK.register(EventHandlers::onWorldEndTick)

        // Refresh sessions when data changes
        OfflineDataChanged.EVENT.register(EventHandlers::onOfflineDataChanged)

        // Main command
        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<ServerCommandSource>, _ ->
            EventHandlers.registerMainCommand(dispatcher)
        }
    }

    companion object {
        @JvmStatic
        val config: DefibrillatorConfig = MicroConfig.getOrCreate("defib", DefibrillatorConfig())

        val crashHandler = CoroutineExceptionHandler { context, exception ->
            println("DEFIBRILLATOR ASYNC EXCEPTION")
            println("CONTEXT: $context")
            println("EXCEPTION MESSAGE: ${exception.message}")
            throw exception
        }
    }
}
