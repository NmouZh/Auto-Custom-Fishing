package cafe.yamds.ui;

import cafe.yamds.AutoFishingClient;
import cafe.yamds.ModRuntime;
import cafe.yamds.config.AutoFishConfig;
import cafe.yamds.config.ConfigStore;
import cafe.yamds.fishing.FishingLog;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/**
 * 自动钓鱼配置界面（Cloth Config）。
 */
public final class AutoFishConfigScreen {

    private AutoFishConfigScreen() {
    }

    public static Screen createConfigScreen(Screen parent) {
        ModRuntime runtime = Objects.requireNonNull(
                AutoFishingClient.runtime(), "Auto Fishing runtime not initialized");
        ConfigStore store = runtime.configStore();
        AutoFishConfig config = store.config();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.autofishing.title"))
                .setSavingRunnable(store::save);

        ConfigEntryBuilder entries = builder.entryBuilder();

        // ========== 主设置 ==========
        ConfigCategory main = builder.getOrCreateCategory(
                Component.translatable("config.autofishing.category.main"));

        main.addEntry(entries
                .startBooleanToggle(
                        Component.translatable("config.autofishing.mod_enabled"),
                        config.modEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.autofishing.mod_enabled.tooltip"))
                .setSaveConsumer(value -> config.modEnabled = value)
                .build());

        main.addEntry(entries
                .startDoubleField(
                        Component.translatable("config.autofishing.global_advance"),
                        config.globalAdvance)
                .setDefaultValue(0.0)
                .setMin(-1.0)
                .setMax(3.0)
                .setTooltip(Component.translatable("config.autofishing.global_advance.tooltip"))
                .setSaveConsumer(value -> config.globalAdvance = value)
                .build());

        main.addEntry(entries
                .startDoubleField(
                        Component.translatable("config.autofishing.auto_recast_minutes"),
                        config.autoRecastMinutes)
                .setDefaultValue(2.0)
                .setMin(0.0)
                .setTooltip(Component.translatable("config.autofishing.auto_recast_minutes.tooltip"))
                .setSaveConsumer(value -> config.autoRecastMinutes = value)
                .build());

        main.addEntry(entries
                .startDoubleField(
                        Component.translatable("config.autofishing.cast_interval"),
                        config.castIntervalSeconds)
                .setDefaultValue(1.0)
                .setMin(0.1)
                .setMax(10.0)
                .setTooltip(Component.translatable("config.autofishing.cast_interval.tooltip"))
                .setSaveConsumer(value -> config.castIntervalSeconds = value)
                .build());

        main.addEntry(entries
                .startDoubleField(
                        Component.translatable("config.autofishing.cast_interval_random"),
                        config.castIntervalRandomSeconds)
                .setDefaultValue(0.5)
                .setMin(0.0)
                .setMax(5.0)
                .setTooltip(Component.translatable("config.autofishing.cast_interval_random.tooltip"))
                .setSaveConsumer(value -> config.castIntervalRandomSeconds = value)
                .build());

        main.addEntry(entries
                .startTextDescription(Component.translatable("config.autofishing.keybind.info"))
                .build());

        // ========== 钓鱼日志 ==========
        ConfigCategory logs = builder.getOrCreateCategory(
                Component.translatable("config.autofishing.category.logs"));

        logs.addEntry(entries
                .startTextDescription(Component.translatable("config.autofishing.logs.title"))
                .build());

        List<FishingLog.LogEntry> entriesList = runtime.log().entries();
        if (entriesList.isEmpty()) {
            logs.addEntry(entries
                    .startTextDescription(Component.translatable("config.autofishing.logs.empty"))
                    .build());
        } else {
            for (FishingLog.LogEntry entry : entriesList) {
                logs.addEntry(entries
                        .startTextDescription(Component.literal(String.format(
                                "Bar %d | %.1fms | 点击 %d → 目标 %d",
                                entry.barType(), entry.averageSpeedMs(),
                                entry.clickPosition(), entry.targetPosition())))
                        .build());
                logs.addEntry(entries
                        .startTextDescription(
                                BarVisualizer.generateBarVisualization(entry.barType(), entry.clickPosition()))
                        .build());
            }
        }

        // ========== Bar 1-9 预判设置 ==========
        ConfigCategory prediction = builder.getOrCreateCategory(
                Component.translatable("config.autofishing.category.prediction"));

        for (int barNum = 1; barNum <= AutoFishConfig.BAR_COUNT; barNum++) {
            addBarSection(prediction, entries, barNum);
        }

        return builder.build();
    }

    private static void addBarSection(ConfigCategory category, ConfigEntryBuilder entries, int barNum) {
        category.addEntry(entries
                .startTextDescription(Component.literal("§l§nBar " + barNum))
                .build());

        category.addEntry(entries
                .startTextDescription(BarVisualizer.generateBarVisualization(barNum))
                .build());

        category.addEntry(new OpenCurveEditorEntry(barNum));

        category.addEntry(entries
                .startTextDescription(Component.literal("§7§m" + "─".repeat(50)))
                .build());
    }
}
