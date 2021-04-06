/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import mc.microconfig.MicroConfig
import me.basiqueevangelist.nevseti.OfflineDataChanged
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.command.ServerCommandSource
import kotlin.time.ExperimentalTime

class Defibrillator : ModInitializer {
    @ExperimentalTime
    @ExperimentalStdlibApi
    override fun onInitialize() {
        // Grab the server on start
        ServerLifecycleEvents.SERVER_STARTED.register(EventHandlers::onServerStarted)

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
            if (config.rethrowAsyncErrors) {
                throw exception
            }
        }
    }
}
