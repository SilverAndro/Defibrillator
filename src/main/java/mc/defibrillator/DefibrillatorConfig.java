/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator;

import mc.microconfig.Comment;
import mc.microconfig.ConfigData;

public class DefibrillatorConfig implements ConfigData {
    @Comment("If the \"Number\" option should be shown instead of the specific tag options")
    public boolean collapseNumberOptions = true;
}
