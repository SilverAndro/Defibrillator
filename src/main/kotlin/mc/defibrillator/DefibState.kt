/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.util.classes.DualHashMap
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

object DefibState {
    @JvmField
    val awaitingInput: HashMap<ServerPlayerEntity, (String) -> Unit> = hashMapOf()

    @JvmField
    val readInput: HashMap<ServerPlayerEntity, MutableList<String>> = hashMapOf()

    @JvmField
    val activeSessions: DualHashMap<UUID, ServerPlayerEntity, MenuState> = DualHashMap()

    @JvmStatic
    internal lateinit var serverInstance: MinecraftServer
}
