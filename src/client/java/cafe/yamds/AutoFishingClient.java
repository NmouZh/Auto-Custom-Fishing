package cafe.yamds;

import cafe.yamds.ui.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * 客户端入口：只负责创建运行时、注册快捷键与 tick 事件。
 */
public class AutoFishingClient implements ClientModInitializer {

    private static ModRuntime runtime;

    /** Mixin 与 UI 访问运行时实例的入口；初始化完成前为 null。 */
    public static ModRuntime runtime() {
        return runtime;
    }

    @Override
    public void onInitializeClient() {
        AutoFishingMod.LOGGER.info("Auto Fishing Client initialized!");

        KeyBindings.register();
        runtime = new ModRuntime(Minecraft.getInstance());

        ClientTickEvents.END_CLIENT_TICK.register(runtime::tick);
    }
}
