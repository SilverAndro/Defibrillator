/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.mixin;

import com.mojang.authlib.GameProfile;
import mc.defibrillator.OfflinePlayerCache;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerManager.class)
public class PlayerJoinDataGrabber {
    @Inject(
        method = "onPlayerConnect",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/UserCache;add(Lcom/mojang/authlib/GameProfile;)V"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void grabUserNameUUIDOnAuthenticate(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci, GameProfile gameProfile) {
        OfflinePlayerCache.INSTANCE.getAll().put(gameProfile.getId(), gameProfile.getName());
    }
}
