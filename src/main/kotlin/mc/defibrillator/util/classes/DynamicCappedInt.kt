/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util.classes

import kotlin.reflect.KProperty

/**
 * An Int property that recalculates the limits of what it can be set to every time the value is attempted to be set
 * @param min the minimum value allowed
 * @param maxMaker A lambda that returns the max value for each set
 */
class DynamicCappedInt(private val min: Int, private inline val maxMaker: () -> Int) {
    private var backing = 0

    operator fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return backing
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: Any?) {
        if (value is Int) {
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
