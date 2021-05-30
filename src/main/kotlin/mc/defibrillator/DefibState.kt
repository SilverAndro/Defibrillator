/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import mc.defibrillator.gui.data.AdvancementMenuState
import mc.defibrillator.gui.data.NBTMenuState
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.github.p03w.quecee.util.DualHashMap
import java.util.*
import kotlin.coroutines.CoroutineContext

object DefibState {
    @JvmField
    val awaitingInput: HashMap<ServerPlayerEntity, (String) -> Unit> = hashMapOf()

    @JvmField
    val readInput: HashMap<ServerPlayerEntity, MutableList<String>> = hashMapOf()

    @JvmField
    val activeNBTSessions: DualHashMap<UUID, ServerPlayerEntity, NBTMenuState> = DualHashMap()
    val activeAdvancementSessions: DualHashMap<UUID, ServerPlayerEntity, AdvancementMenuState> = DualHashMap()

    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    // TODO: Replace Boolean with a chunk reference
    val activeChunkSessions: DualHashMap<UUID, Boolean, MutableList<UUID>> = DualHashMap()

    internal lateinit var serverInstance: MinecraftServer
}
