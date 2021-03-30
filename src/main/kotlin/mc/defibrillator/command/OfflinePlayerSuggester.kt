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
import me.basiqueevangelist.nevseti.OfflineNameCache
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

/**
 * Suggests player names from the OfflineNameCache
 */
class OfflinePlayerSuggester : SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if (context.source is CommandSource) {
            return CommandSource.suggestMatching(
                OfflineNameCache.INSTANCE.names.values.filter { context.source.playerNames.contains(it).not() },
                builder
            )
        }
        return Suggestions.empty()
    }
}
