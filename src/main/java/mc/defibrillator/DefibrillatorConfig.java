/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator;

import mc.microconfig.Comment;
import mc.microconfig.ConfigData;

public class DefibrillatorConfig implements ConfigData {
    @Comment("Delay in minutes between the cache being recached")
    public double recacheDelay = 3.0;
}
