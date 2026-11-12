package cafe.yamds.mixin;

import cafe.yamds.AutoFishingClient;
import cafe.yamds.AutoFishingMod;
import cafe.yamds.ModRuntime;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截标题文字，把 UI 组件原样交给运行时解码（1.21.11 的 HUD 类名为 {@link Gui}）。
 *
 * <p>只做转发，不做解析；异常在这里吞掉，避免影响渲染。
 */
@Mixin(Gui.class)
public class HudMixin {

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void autofishing$onSetTitle(Component title, CallbackInfo ci) {
        ModRuntime runtime = AutoFishingClient.runtime();
        if (runtime == null || title == null) {
            return;
        }
        try {
            runtime.onTitle(title);
        } catch (Exception e) {
            AutoFishingMod.LOGGER.debug("Title handling failed", e);
        }
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void autofishing$onSetSubtitle(Component subtitle, CallbackInfo ci) {
        forwardSubtitle(subtitle);
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void autofishing$onSetOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
        forwardSubtitle(message);
    }

    private static void forwardSubtitle(Component component) {
        ModRuntime runtime = AutoFishingClient.runtime();
        if (runtime == null || component == null) {
            return;
        }
        try {
            runtime.onSubtitle(component);
        } catch (Exception e) {
            AutoFishingMod.LOGGER.debug("Subtitle handling failed", e);
        }
    }
}
