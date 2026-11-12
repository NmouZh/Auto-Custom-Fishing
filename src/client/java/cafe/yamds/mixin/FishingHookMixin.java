package cafe.yamds.mixin;

import cafe.yamds.AutoFishingClient;
import cafe.yamds.AutoFishingMod;
import cafe.yamds.ModRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 监听本地玩家鱼钩的 biting 状态：false → true 即鱼咬钩。
 *
 * <p>咬钩信号会立刻触发收杆（在客户端线程内直接 useItem），
 * 不再等下一个 tick。
 */
@Mixin(FishingHook.class)
public class FishingHookMixin {

    @Shadow
    private boolean biting;

    private boolean lastBitingState;

    @Inject(method = "tick", at = @At("TAIL"))
    private void autofishing$onTick(CallbackInfo ci) {
        ModRuntime runtime = AutoFishingClient.runtime();
        if (runtime == null || !runtime.isEnabled()) {
            return;
        }

        FishingHook hook = (FishingHook) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || hook.getPlayerOwner() != minecraft.player) {
            return;
        }

        if (biting && !lastBitingState) {
            try {
                runtime.onFishBite();
            } catch (Exception e) {
                AutoFishingMod.LOGGER.debug("Bite handling failed", e);
            }
        }
        lastBitingState = biting;
    }
}
