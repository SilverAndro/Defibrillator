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
import me.basiqueevangelist.nevseti.OfflineDataCache
import me.basiqueevangelist.nevseti.OfflineNameCache
import net.minecraft.command.CommandSource
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import net.minecraft.util.WorldSavePath
import java.util.concurrent.CompletableFuture

class OfflinePlayerSuggester : SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if (context.source is CommandSource) {
            return Suggestions.empty()
            /*OfflinePlayerCache.filterByOnline((context.source as CommandSource).playerNames)
            return CommandSource.suggestMatching(
                OfflinePlayerCache.getOfflinePlayerNames((context.source as CommandSource).playerNames),
                builder,
            )*/
        }
        return Suggestions.empty()
    }
}
