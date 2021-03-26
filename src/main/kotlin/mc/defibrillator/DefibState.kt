/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.io.File
import java.util.*
import kotlin.collections.HashMap

object DefibState {
    @JvmField
    val awaitingInput: HashMap<ServerPlayerEntity, (String) -> Unit> = hashMapOf()
    @JvmField
    val readInput: HashMap<ServerPlayerEntity, MutableList<String>> = hashMapOf()

    val activeSessions: HashMap<UUID, ServerPlayerEntity> = hashMapOf()

    @JvmStatic
    internal lateinit var serverInstance: MinecraftServer
}
