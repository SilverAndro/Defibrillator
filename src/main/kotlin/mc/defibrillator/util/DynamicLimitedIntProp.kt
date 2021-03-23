/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util

import kotlin.reflect.KProperty

class DynamicLimitedIntProp(private val minMaker: () -> Int, private val maxMaker: () -> Int) {
    private var backing = 0

    operator fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return backing
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: Any?) {
        if (value is Int) {
            val min = minMaker()
            val max = maxMaker()

            if (value in min..max) {
                backing = value
            }

            if (value < min) {
                backing = min
            }

            if (backing > max) {
                backing = max
            }
        }
    }
}
