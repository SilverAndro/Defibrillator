/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util

import net.minecraft.text.*
import net.minecraft.util.Formatting

fun copyableText(text: String, display: String = text): MutableText {
    return LiteralText(display).setStyle(
        Style.EMPTY
            .withFormatting(Formatting.UNDERLINE)
            .withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, LiteralText("Copy to clipboard")))
    )
}
