/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator

import java.util.*

object OfflinePlayerCache {
    var currentlyOffline: HashMap<UUID, String> = hashMapOf()
    var all: HashMap<UUID, String> = hashMapOf()

    init {
        findNotInCache()
    }

    fun getByName(name: String): UUID {
        return currentlyOffline.entries.first { it.value.equals(name, true) }.key
    }

    fun getOfflinePlayerNames(playerNames: MutableCollection<String>): List<String> {
        filterByOnline(playerNames)
        return currentlyOffline.values.toList()
    }

    fun getOfflinePlayerUUIDS(playerNames: MutableCollection<String>): List<UUID> {
        filterByOnline(playerNames)
        return currentlyOffline.keys.toList()
    }

    fun filterByOnline(playerNames: MutableCollection<String>) {
        playerNames.forEach { name ->
            currentlyOffline.filterNot { it.value.equals(name, true) }
        }
    }

    fun findNotInCache() {
        val lines = DefibState.cache.readLines()
        lines.forEach {
            if (it.isNotEmpty()) {
                try {
                    val split = it.split("*")
                    val name = split[1]
                    val uuid = UUID.fromString(split[0])

                    all[uuid] = name
                } catch (err: Throwable) {
                    println("Failed to read line of defibrillator cache!")
                    println(it)
                    err.printStackTrace()
                }
            }
        }
        all.forEach { (t, u) -> currentlyOffline[t] = u }

        DefibState.cache.printWriter().use {
            all.forEach { (name, uuid) ->
                it.println("$name*$uuid")
            }
        }
    }
}
