package cafe.yamds;

import cafe.yamds.config.AutoFishConfig;
import cafe.yamds.config.ConfigStore;
import cafe.yamds.detection.ComponentScanner;
import cafe.yamds.fishing.FishingController;
import cafe.yamds.fishing.FishingLog;
import cafe.yamds.fishing.MinecraftFishingEnvironment;
import cafe.yamds.ui.AutoFishConfigScreen;
import cafe.yamds.ui.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * 客户端运行时：持有配置、状态机，并把游戏事件翻译成状态机信号。
 *
 * <p>1.21.11 的屏幕切换入口是 {@link Minecraft#setScreen} / {@link Minecraft#screen}。
 */
public final class ModRuntime {

    private final ConfigStore configStore;
    private final FishingLog log;
    private final MinecraftFishingEnvironment environment;
    private final FishingController controller;

    private boolean wasEnabled = true;

    public ModRuntime(Minecraft minecraft) {
        this.configStore = ConfigStore.withDefaultPath();
        this.configStore.load();
        this.log = new FishingLog();
        this.environment = new MinecraftFishingEnvironment(minecraft);
        this.controller = new FishingController(
                environment,
                configStore.config(),
                configStore,
                log,
                new Random()
        );
    }

    public ConfigStore configStore() {
        return configStore;
    }

    public FishingLog log() {
        return log;
    }

    public boolean isEnabled() {
        return configStore.config().modEnabled;
    }

    /** 每个客户端 tick 调用一次。 */
    public void tick(Minecraft client) {
        handleInput(client);

        long now = System.currentTimeMillis();
        if (!isEnabled()) {
            if (wasEnabled) {
                controller.reset(now);
            }
            wasEnabled = false;
            return;
        }
        wasEnabled = true;

        // 离开世界 / 未进入世界时丢弃会话，避免旧状态带到下一局。
        if (client.player == null || client.level == null) {
            controller.reset(now);
            return;
        }
        controller.tick(now);
    }

    // ========== Mixin 信号入口（客户端线程） ==========

    public void onTitle(Component component) {
        if (!isEnabled()) {
            return;
        }
        controller.onTitleText(ComponentScanner.scan(component), System.currentTimeMillis());
    }

    public void onSubtitle(Component component) {
        onUiComponent(component);
    }

    /** 鱼钩 biting 由 false 变 true。 */
    public void onFishBite() {
        if (!isEnabled()) {
            return;
        }
        controller.onBite(System.currentTimeMillis());
    }

    private void onUiComponent(Component component) {
        if (!isEnabled()) {
            return;
        }
        controller.onGameText(ComponentScanner.scan(component), System.currentTimeMillis());
    }

    // ========== 输入 ==========

    private void handleInput(Minecraft client) {
        while (KeyBindings.toggleAutoFishing.consumeClick()) {
            toggleEnabled(client);
        }
        while (KeyBindings.openConfig.consumeClick()) {
            if (client.player != null) {
                client.setScreen(AutoFishConfigScreen.createConfigScreen(client.screen));
            }
        }
    }

    private void toggleEnabled(Minecraft client) {
        AutoFishConfig config = configStore.config();
        config.modEnabled = !config.modEnabled;
        configStore.save();

        if (!config.modEnabled) {
            controller.reset(System.currentTimeMillis());
        }

        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(
                    config.modEnabled ? "§a[Auto Fishing] Enabled" : "§c[Auto Fishing] Disabled"
            ), false);
        }
        AutoFishingMod.LOGGER.info("Auto Fishing: {}", config.modEnabled ? "ON" : "OFF");
    }
}
