/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util.classes

/**
 * A data structure that keeps 2 `HashMap`s updated in parallel
 */
class DualHashMap<KEY, TYPE_A, TYPE_B> {
    private val backingA: HashMap<KEY, TYPE_A> = hashMapOf()
    private val backingB: HashMap<KEY, TYPE_B> = hashMapOf()

    fun set(key: KEY, value1: TYPE_A, value2: TYPE_B) {
        backingA[key] = value1
        backingB[key] = value2
    }

    fun remove(key: KEY) {
        backingA.remove(key)
        backingB.remove(key)
    }

    fun contains(key: KEY): Boolean {
        return backingA.containsKey(key)
    }

    fun getA(key: KEY): TYPE_A = backingA[key]!!
    fun getB(key: KEY): TYPE_B = backingB[key]!!
    operator fun get(key: KEY): Pair<TYPE_A, TYPE_B> = Pair(getA(key), getB(key))

    fun forEach(receiver: (KEY, TYPE_A, TYPE_B) -> Unit) {
        for (key in backingA.keys) {
            val entry = get(key)
            receiver(key, entry.first, entry.second)
        }
    }
}
