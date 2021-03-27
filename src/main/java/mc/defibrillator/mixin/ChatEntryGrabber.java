/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.mixin;

import mc.defibrillator.DefibState;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayNetworkHandler.class)
public class ChatEntryGrabber {
    @Inject(
        method = "onGameMessage",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z"),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    // Grabs the message sent by players that we are awaiting input from, and cancels further processing
    public void captureChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci, String string) {
        ServerPlayerEntity player = ((ServerPlayNetworkHandler)(Object)this).player;
        if (DefibState.awaitingInput.containsKey(player)) {
            DefibState.awaitingInput.get(player).invoke(string);
            DefibState.awaitingInput.remove(player);
            ci.cancel();
        }
    }
}
