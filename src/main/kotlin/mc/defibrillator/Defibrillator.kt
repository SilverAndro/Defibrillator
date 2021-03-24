/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.p03w.aegis.aegisCommand
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.*
import mc.defibrillator.command.OfflinePlayerSuggester
import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.gui.util.openNBTGui
import mc.defibrillator.util.copyableText
import mc.microconfig.MicroConfig
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class Defibrillator : ModInitializer {
    @ExperimentalTime
    @ExperimentalStdlibApi
    override fun onInitialize() {
        // Remove GUI items from players
        ServerTickEvents.END_WORLD_TICK.register { world ->
            world.players.forEach {
                it.inventory.remove(
                    { stack -> stack.orCreateTag.contains("defib-DELETE") },
                    Int.MAX_VALUE,
                    it.playerScreenHandler.method_29281()
                )
            }
        }

        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<ServerCommandSource>, _: Boolean ->
            dispatcher.register(aegisCommand("defib") {
                requires {
                    it.hasPermissionLevel(2)
                }
                literal("modify") {
                    literal("playerdata") {
                        string("playerData") {
                            suggests(OfflinePlayerSuggester()::getSuggestions)
                            executes(debug = true) {
                                val uuid = OfflinePlayerCache.getByName(
                                    it.getArgument("playerData", String::class.java)
                                )
                                if (!DefibState.activeSessions.contains(uuid)) {
                                    DefibState.activeSessions[uuid] = it.source.player
                                    openNBTGui(
                                        it.source.player,
                                        it.getArgument("playerData", String::class.java),
                                        MenuState(
                                            OfflinePlayerSuggester.getPlayerData(it, "playerData")
                                        )
                                    ) { state ->
                                        try {
                                            val dir =
                                                DefibState.serverInstance.getSavePath(WorldSavePath.PLAYERDATA).toFile()

                                            val compoundTag = state.rootTag
                                            val file = File.createTempFile(
                                                "$uuid-",
                                                ".dat",
                                                dir
                                            )
                                            NbtIo.writeCompressed(compoundTag, file)
                                            val file2 = File(dir, "$uuid.dat")
                                            val file3 = File(dir, "$uuid.dat_old")
                                            Util.backupAndReplace(file2, file, file3)
                                            it.source.sendFeedback(LiteralText("Saved user data"), true)
                                        } catch (ex: Exception) {
                                            it.source.sendError(LiteralText("Failed to save user data").formatted(Formatting.RED))
                                            ex.printStackTrace()
                                        } finally {
                                            DefibState.activeSessions.remove(uuid)
                                        }
                                    }
                                } else {
                                    it.source.sendError(
                                        LiteralText(
                                            "${DefibState.activeSessions[uuid]?.entityName} already has a session open for that uuid!"
                                        ).formatted(Formatting.RED)
                                    )
                                }
                            }
                        }
                    }
                    literal("item") {
                        executes {
                            openNBTGui(
                                it.source.player,
                                "Held Item",
                                MenuState(
                                    it.source.player.mainHandStack.toTag(CompoundTag())
                                )
                            ) { state ->
                                it.source.player.setStackInHand(
                                    Hand.MAIN_HAND,
                                    ItemStack.fromTag(state.rootTag)
                                )
                                it.source.sendFeedback(LiteralText("Saved item data"), false)
                            }
                        }
                    }
                }
                literal("recache") {
                    executes(debug = true) {
                        OfflinePlayerCache.recache()
                        it.source.sendFeedback(
                            LiteralText("Defibrillator offline player cache successfully re-cached"),
                            true
                        )
                    }
                    literal("import") {
                        executes(debug = true) {
                            it.source.sendFeedback(
                                LiteralText("Beginning import of players from .dat files"),
                                true
                            )

                            GlobalScope.launch {
                                val possible =
                                    DefibState.serverInstance.getSavePath(WorldSavePath.PLAYERDATA).toFile().list()
                                var uuids = OfflinePlayerCache.all.keys.map { uuid -> uuid.toString() }

                                if (possible != null) {
                                    var needed = possible
                                        .map { filename -> filename.replace(".dat_old", "").replace(".dat", "") }
                                        .distinct()
                                        .filter { cleaned -> !uuids.contains(cleaned) }

                                    if (needed.isEmpty()) {
                                        it.source.sendFeedback(
                                            LiteralText("No unknown users, canceling"),
                                            true
                                        )
                                        cancel()
                                        return@launch
                                    }

                                    val before = needed.size
                                    needed.forEach { uuid ->
                                        val fromString = UUID.fromString(uuid)
                                        val possibleProfile = DefibState.serverInstance.userCache.getByUuid(fromString)
                                        if (possibleProfile != null) {
                                            OfflinePlayerCache.all[fromString] = possibleProfile.name
                                            OfflinePlayerCache.currentlyOffline[fromString] = possibleProfile.name
                                        }
                                    }
                                    uuids = OfflinePlayerCache.all.keys.map { uuid -> uuid.toString() }
                                    needed = needed.filter { cleaned -> !uuids.contains(cleaned) }

                                    if (needed.isEmpty()) {
                                        it.source.sendFeedback(
                                            LiteralText("${before - needed.size} users imported from usercache. leaving none unknown, saving"),
                                            true
                                        )
                                        cancel()
                                        return@launch
                                    }

                                    it.source.sendFeedback(
                                        LiteralText(
                                            "Found ${needed.size} unknown users (${before - needed.size} imported from usercache), beginning import " +
                                                    "(Estimated time until completion: ${
                                                        (2.1).toDuration(DurationUnit.SECONDS).times(needed.size)
                                                    })"
                                        ),
                                        true
                                    )

                                    val startTime = Date().time
                                    for (uuid in needed) {
                                        getAndSave(uuid, it)
                                        // Delay 2 seconds
                                        // Technically the limit is 600 per 10min (1 per sec) but we don't
                                        //     want to risk getting rate limited
                                        delay(2.toDuration(DurationUnit.SECONDS))
                                    }

                                    OfflinePlayerCache.recache()
                                    OfflinePlayerCache.filterByOnline(it.source.playerNames)
                                    it.source.sendFeedback(
                                        LiteralText(
                                            "Done importing (took ${
                                                (Date().time - startTime).toDuration(
                                                    DurationUnit.MILLISECONDS
                                                )
                                            })"
                                        ),
                                        true
                                    )

                                    try {
                                        it.source.player.playSound(
                                            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                            SoundCategory.MASTER,
                                            5.0f,
                                            0.7f
                                        )
                                    } catch (ignored: Throwable) {
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }

        GlobalScope.launch {
            while (isActive) {
                // Re-cache every 3 minutes
                delay(config.recacheDelay.toDuration(DurationUnit.MINUTES))
                OfflinePlayerCache.recache()
            }
        }
    }

    private fun getAndSave(uuid: String, context: CommandContext<ServerCommandSource>) {
        val (_, _, result) = "https://api.mojang.com/user/profiles/${uuid.replace("-", "")}/names"
            .httpGet()
            .responseString()

        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                context.source.sendError(
                    LiteralText("Failed to get username for ")
                        .append(copyableText(uuid))
                        .append(LiteralText("API status code: ${ex.response.statusCode})"))
                )
                println(ex)
            }
            is Result.Success -> {
                val data = result.get()

                // Quit early with error
                if (data.isEmpty()) {
                    context.source.sendError(
                        LiteralText("Failed to get username for ")
                            .append(copyableText(uuid))
                    )
                    return
                }

                val gson = Gson().fromJson(data, ArrayList::class.java)

                var currentName = ""
                var greatestChangedAt = -1.0
                for (entry in gson) {
                    val casted = entry as LinkedTreeMap<*, *>
                    val changedTime = (casted["changedToAt"] as? Double) ?: 0.0
                    if (changedTime > greatestChangedAt) {
                        greatestChangedAt = changedTime
                        currentName = casted["name"] as String
                    }
                }

                val uuidFromString = UUID.fromString(uuid)
                OfflinePlayerCache.all[uuidFromString] = currentName
                OfflinePlayerCache.currentlyOffline[uuidFromString] = currentName
            }
        }
    }

    companion object {
        val config: DefibrillatorConfig = MicroConfig.getOrCreate("defibrillator", DefibrillatorConfig())
    }
}
