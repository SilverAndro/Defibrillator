/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.mixin;

import com.mojang.authlib.GameProfile;
import mc.defibrillator.DefibState;
import mc.defibrillator.Defibrillator;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public class PreventPlayerJoin {
    @Inject(
        method = "checkCanJoin",
        at = @At("HEAD"),
        cancellable = true
    )
    public void preventPlayersFromJoiningWhileDataEdited(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (DefibState.activeSessions.contains(profile.getId())) {
            cir.setReturnValue(
                new LiteralText(
                    Defibrillator
                        .getConfig()
                        .failedConnectMessage
                        .replace("%editor%", DefibState.activeSessions.get(profile.getId()).component1().getEntityName())
                )
            );
        }
    }
}
