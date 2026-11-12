package cafe.yamds.config;

import cafe.yamds.AutoFishingMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置的加载、保存与运行时曲线缓存。
 *
 * <p>配置文件路径默认是 {@code config/autofishing.json}（相对游戏目录），
 * 格式与旧版本完全兼容。
 */
public final class ConfigStore implements CurveSource {

    public static final String DEFAULT_PATH = "config/autofishing.json";

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<Integer, AdvanceCurve> compiledCurves = new HashMap<>();

    private AutoFishConfig config = new AutoFishConfig();

    public ConfigStore(Path file) {
        this.file = file;
        compileCurves();
    }

    public static ConfigStore withDefaultPath() {
        return new ConfigStore(Path.of(DEFAULT_PATH));
    }

    public AutoFishConfig config() {
        return config;
    }

    public Path file() {
        return file;
    }

    /** 从磁盘加载；文件不存在时创建默认配置。失败时回退到内存默认值。 */
    public void load() {
        try {
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    AutoFishConfig loaded = gson.fromJson(reader, AutoFishConfig.class);
                    if (loaded != null) {
                        config = loaded;
                    }
                }
                AutoFishingMod.LOGGER.info("Config loaded from {}", file);
            } else {
                save();
                AutoFishingMod.LOGGER.info("Created default config at {}", file);
            }
        } catch (Exception e) {
            AutoFishingMod.LOGGER.error("Failed to load config from {}, using defaults", file, e);
            config = new AutoFishConfig();
        }

        config.validate();
        compileCurves();
    }

    /** 校验、写盘，并刷新运行时曲线缓存。 */
    public void save() {
        config.validate();
        compileCurves();

        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(file)) {
                gson.toJson(config, writer);
            }
            AutoFishingMod.LOGGER.info("Config saved to {}", file);
        } catch (IOException e) {
            AutoFishingMod.LOGGER.error("Failed to save config to {}", file, e);
        }
    }

    @Override
    public AdvanceCurve curveFor(int barType) {
        return compiledCurves.getOrDefault(barType, AdvanceCurve.fallback());
    }

    private void compileCurves() {
        compiledCurves.clear();
        config.barCurves.forEach((bar, points) -> compiledCurves.put(bar, AdvanceCurve.of(points)));
    }
}
