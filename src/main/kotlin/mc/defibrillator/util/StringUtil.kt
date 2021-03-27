/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util

/**
 * Removes the postfix from numbers if it exists
 */
fun String.cleanNumber(): String {
    val regex = Regex("^([\\-+]?[\\d\\.]+)\\w?")
    return regex.find(this)?.groupValues?.get(1) ?: "0"
}
