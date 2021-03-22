/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import mc.defibrillator.DefibState
import mc.defibrillator.exception.InvalidArgument
import mc.defibrillator.OfflinePlayerCache
import net.minecraft.command.CommandSource
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import net.minecraft.util.WorldSavePath
import java.io.File
import java.util.concurrent.CompletableFuture

class OfflinePlayerSuggester : SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if (context.source is CommandSource) {
            OfflinePlayerCache.filterByOnline((context.source as CommandSource).playerNames)
            return CommandSource.suggestMatching(
                OfflinePlayerCache.getOfflinePlayerNames((context.source as CommandSource).playerNames),
                builder,
            )
        }
        return Suggestions.empty()
    }

    companion object {
        fun getPlayerData(context: CommandContext<ServerCommandSource>, name: String): CompoundTag {
            val providedName = context.getArgument(name, String::class.java)
            val uuid = OfflinePlayerCache.getByName(providedName)

            val file = DefibState.serverInstance.getSavePath(WorldSavePath.PLAYERDATA).toFile()
                .resolve(OfflinePlayerCache.getByName(providedName).toString() + ".dat")

            if (!file.exists()) {
                generateError(context, "No .dat file associated with that uuid! ($uuid)")
            }

            context.source.sendFeedback(LiteralText("Opening player data manager for \"$providedName\" ($uuid)"), true)
            return NbtIo.readCompressed(file)
        }

        private fun generateError(context: CommandContext<ServerCommandSource>, error: String, exception: Exception = InvalidArgument()) {
            context.source.sendError(LiteralText(error).formatted(Formatting.RED))
            throw exception
        }
    }
}
