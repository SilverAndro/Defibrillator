/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import com.github.p03w.aegis.*
import com.mojang.brigadier.CommandDispatcher
import mc.defibrillator.command.OfflinePlayerSuggester
import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.gui.util.openNBTGui
import mc.defibrillator.util.copyableText
import me.basiqueevangelist.nevseti.OfflineDataCache
import me.basiqueevangelist.nevseti.OfflineNameCache
import me.basiqueevangelist.nevseti.nbt.CompoundTagView
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.LiteralText
import net.minecraft.text.MutableText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Util
import java.util.*
import kotlin.time.ExperimentalTime

object EventHandlers {
    fun onServerStarted(server: MinecraftServer) {
        DefibState.serverInstance = server
    }

    fun onWorldEndTick(world: ServerWorld) {
        world.players.forEach {
            it.inventory.remove(
                { stack -> stack.tag?.contains("defib-DELETE") ?: false },
                Int.MAX_VALUE,
                it.playerScreenHandler.method_29281()
            )
        }
    }

    @ExperimentalTime
    fun onOfflineDataChanged(uuid: UUID, data: CompoundTagView) {
        try {
            val state = DefibState.activeSessions.getB(uuid)
            state.rootTag = data.copy()
            if (!state.isInAddMenu) {
                state.factory?.rebuild()
            }
        } catch (ignored: NullPointerException) {
            // Don't have a session
        }
    }

    fun registerMainCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(aegisCommand("defib") {
            requires {
                it.hasPermissionLevel(Defibrillator.config.commands.minimumRequiredLevel)
            }
            if (Defibrillator.config.commands.enableDebugCommands) {
                attachDebugTree()
            }
            literal("view") {
                requires {
                    it.hasPermissionLevel(Defibrillator.config.commands.viewRequiredLevel)
                }
                literal("item") {
                    executes {
                        openNBTGui(
                            it.source.player,
                            LiteralText("Held Item (VIEW)"),
                            MenuState(
                                it.source.player.mainHandStack.toTag(CompoundTag()),
                                Util.NIL_UUID,
                                it.source.player
                            ),
                            false
                        ) { }
                    }
                }
                literal("block") {
                    blockPos("blockPos") {
                        executes(debug = true) {
                            val pos = it.getBlockPos("blockPos")
                            val world = it.source.world
                            val entity = world.getBlockEntity(pos)
                            if (entity != null) {
                                val tag = entity.toTag(CompoundTag())
                                openNBTGui(
                                    it.source.player,
                                    TranslatableText(world.getBlockState(pos).block.translationKey)
                                        .append(LiteralText("[${pos.x}, ${pos.y}, ${pos.z}] (VIEW)")),
                                    MenuState(
                                        tag,
                                        Util.NIL_UUID,
                                        it.source.player
                                    ),
                                    false
                                ) { }
                            } else {
                                it.source.sendError(
                                    LiteralText(
                                        "No block entity at $pos"
                                    ).formatted(Formatting.RED)
                                )
                            }
                        }
                    }
                }
                literal("player") {
                    literal("data") {
                        string("playerData") {
                            suggests(OfflinePlayerSuggester()::getSuggestions)
                            executes(debug = true) {
                                try {
                                    val uuid = OfflineNameCache.INSTANCE.getUUIDFromName(
                                        it.getString("playerData")
                                    )
                                    openNBTGui(
                                        it.source.player,
                                        LiteralText(it.getString("playerData"))
                                            .append(LiteralText(" (VIEW)")),
                                        MenuState(
                                            OfflineDataCache.INSTANCE.get(uuid).copy(),
                                            uuid,
                                            it.source.player
                                        ),
                                        false
                                    ) { }
                                } catch (npe: NullPointerException) {
                                    it.source.sendError(
                                        LiteralText(
                                            "Could not load data for that user!"
                                        ).formatted(Formatting.RED)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            literal("modify") {
                requires {
                    it.hasPermissionLevel(Defibrillator.config.commands.editRequiredLevel)
                }
                literal("item") {
                    executes {
                        openNBTGui(
                            it.source.player,
                            LiteralText("Held Item"),
                            MenuState(
                                it.source.player.mainHandStack.toTag(CompoundTag()),
                                Util.NIL_UUID,
                                it.source.player
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
                literal("block") {
                    blockPos("blockPos") {
                        executes(debug = true) {
                            val pos = it.getBlockPos("blockPos")
                            val world = it.source.world
                            val entity = world.getBlockEntity(pos)
                            if (entity != null) {
                                val tag = entity.toTag(CompoundTag())
                                openNBTGui(
                                    it.source.player,
                                    TranslatableText(world.getBlockState(pos).block.translationKey)
                                        .append(LiteralText("[${pos.x}, ${pos.y}, ${pos.z}]")),
                                    MenuState(
                                        tag,
                                        Util.NIL_UUID,
                                        it.source.player
                                    )
                                ) { state ->
                                    entity.fromTag(world.getBlockState(pos), state.rootTag)
                                    it.source.sendFeedback(LiteralText("Saved block data"), false)
                                }
                            } else {
                                it.source.sendError(
                                    LiteralText(
                                        "No block entity at $pos"
                                    ).formatted(Formatting.RED)
                                )
                            }
                        }
                    }
                }
                literal("player") {
                    string("playerData") {
                        suggests(OfflinePlayerSuggester()::getSuggestions)
                        executes(debug = true) {
                            try {
                                val uuid = OfflineNameCache.INSTANCE.getUUIDFromName(
                                    it.getString("playerData")
                                )
                                if (!DefibState.activeSessions.contains(uuid)) {
                                    val state = openNBTGui(
                                        it.source.player,
                                        LiteralText(it.getString("playerData")),
                                        MenuState(
                                            OfflineDataCache.INSTANCE.get(uuid).copy(),
                                            uuid,
                                            it.source.player
                                        )
                                    ) { state ->
                                        try {
                                            OfflineDataCache.INSTANCE.save(uuid, state.rootTag)
                                            it.source.sendFeedback(LiteralText("Saved user data"), true)
                                        } catch (ex: Exception) {
                                            it.source.sendError(
                                                LiteralText("Failed to save user data").formatted(
                                                    Formatting.RED
                                                )
                                            )
                                            ex.printStackTrace()
                                        } finally {
                                            DefibState.activeSessions.remove(uuid)
                                        }
                                    }

                                    DefibState.activeSessions.set(
                                        uuid,
                                        it.source.player,
                                        state
                                    )
                                } else {
                                    it.source.sendError(
                                        LiteralText(
                                            "${DefibState.activeSessions[uuid].first.entityName} already has a session open for that uuid!"
                                        ).formatted(Formatting.RED)
                                    )
                                }
                            } catch (npe: NullPointerException) {
                                it.source.sendError(
                                    LiteralText(
                                        "Could not load data for that user!"
                                    ).formatted(Formatting.RED)
                                )
                            }
                        }
                    }
                }
            }
        })
    }

    private fun AegisCommandBuilder.attachDebugTree() {
        literal("debug") {
            requires {
                it.hasPermissionLevel(2)
            }
            literal("input") {
                executes {
                    if (DefibState.awaitingInput.isEmpty()) {
                        it.source.sendFeedback(LiteralText("Not awaiting any input"), false)
                    } else {
                        DefibState.awaitingInput.forEach { entry ->
                            it.source.sendFeedback(
                                (entry.key.displayName as MutableText),
                                false
                            )
                        }
                    }
                }
            }
            literal("sessions") {
                literal("clear") {
                    uuid("uuid") {
                        executes {
                            DefibState.activeSessions.remove(it.getUUID("uuid"))
                            it.source.sendFeedback(LiteralText("Removed session (if present)"), true)
                        }
                    }
                }
                literal("clearAll") {
                    executes {
                        DefibState.activeSessions.clear()
                        it.source.sendFeedback(LiteralText("Removed all sessions (if present)"), true)
                    }
                }
                literal("list") {
                    executes {
                        if (DefibState.activeSessions.isEmpty()) {
                            it.source.sendFeedback(LiteralText("No active sessions"), false)
                        } else {
                            DefibState.activeSessions.forEach { uuid, playerEntity, _ ->
                                it.source.sendFeedback(
                                    (playerEntity.displayName as MutableText)
                                        .append(" -> ")
                                        .append(
                                            copyableText(
                                                uuid.toString(),
                                                OfflineNameCache.INSTANCE.getNameFromUUID(uuid)
                                            )
                                        ),
                                    false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
