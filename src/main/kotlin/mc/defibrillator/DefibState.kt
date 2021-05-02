/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import mc.defibrillator.gui.data.AdvancementMenuState
import mc.defibrillator.gui.data.NBTMenuState
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.github.p03w.quecee.util.DualHashMap
import java.util.*

object DefibState {
    @JvmField
    val awaitingInput: HashMap<ServerPlayerEntity, (String) -> Unit> = hashMapOf()

    @JvmField
    val readInput: HashMap<ServerPlayerEntity, MutableList<String>> = hashMapOf()

    @JvmField
    val activeNBTSessions: DualHashMap<UUID, ServerPlayerEntity, NBTMenuState> = DualHashMap()
    val activeAdvancementSessions: DualHashMap<UUID, ServerPlayerEntity, AdvancementMenuState> = DualHashMap()

    @JvmStatic
    internal lateinit var serverInstance: MinecraftServer
}
