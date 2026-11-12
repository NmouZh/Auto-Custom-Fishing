package cafe.yamds.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动钓鱼配置数据。
 *
 * <p>只负责“数据 + 规范化”，读写由 {@link ConfigStore} 负责。
 * Gson 反序列化会调用无参构造器，因此旧配置缺失字段时会自动保留这里的默认值，
 * 不再需要手工逐字段兜底。
 */
public final class AutoFishConfig {

    public static final int BAR_COUNT = 9;

    // ========== 主设置 ==========
    /** Mod 总开关 */
    public boolean modEnabled = true;
    /** 全局提前量（格），叠加在所有 bar 曲线上 */
    public double globalAdvance = 0.0;
    /** 抛竿后多久仍未收杆就强制收杆重抛（分钟），0 关闭 */
    public double autoRecastMinutes = 2.0;
    /** 收杆后到重新抛竿的基础等待时间（秒） */
    public double castIntervalSeconds = 1.0;
    /** 抛竿间隔随机浮动（秒），实际间隔在基础值 ± 范围内随机 */
    public double castIntervalRandomSeconds = 0.5;

    // ========== 曲线设置 ==========
    /** 每个 bar 的提前量曲线：速度(ms/格) → 提前量(格) */
    public Map<Integer, List<CurvePoint>> barCurves = defaultBarCurves();

    /** 全部 bar 的默认曲线（默认参数来自游戏内调校）。 */
    public static Map<Integer, List<CurvePoint>> defaultBarCurves() {
        Map<Integer, List<CurvePoint>> curves = new LinkedHashMap<>();
        for (int bar = 1; bar <= BAR_COUNT; bar++) {
            curves.put(bar, defaultCurveFor(bar));
        }
        return curves;
    }

    /**
     * 单个 bar 的默认曲线。
     * bar 1-3：简单；4-6：中等；7-9：高难度、更积极的提前量。
     */
    public static List<CurvePoint> defaultCurveFor(int barNum) {
        if (barNum <= 3) {
            return List.of(
                    new CurvePoint(0.0, 0.2),
                    new CurvePoint(35.0, -0.2)
            );
        }
        if (barNum <= 6) {
            return List.of(
                    new CurvePoint(0.0, 1.5),
                    new CurvePoint(16.8, 1.2),
                    new CurvePoint(20.0, 0.9),
                    new CurvePoint(35.0, 0.7)
            );
        }
        return List.of(
                new CurvePoint(0.0, 2.4),
                new CurvePoint(31.8, 1.7),
                new CurvePoint(35.0, 1.5)
        );
    }

    /**
     * 校验并补全配置。所有非常值、越界值、缺失曲线都在这里被修正。
     */
    public void validate() {
        if (!Double.isFinite(globalAdvance)) {
            globalAdvance = 0.0;
        }
        globalAdvance = clamp(globalAdvance, -1.0, 3.0);

        if (!Double.isFinite(autoRecastMinutes) || autoRecastMinutes < 0.0) {
            autoRecastMinutes = 2.0;
        }
        if (!Double.isFinite(castIntervalSeconds) || castIntervalSeconds <= 0.0) {
            castIntervalSeconds = 1.0;
        }
        if (!Double.isFinite(castIntervalRandomSeconds) || castIntervalRandomSeconds < 0.0) {
            castIntervalRandomSeconds = 0.5;
        }

        Map<Integer, List<CurvePoint>> normalized = new LinkedHashMap<>();
        for (int bar = 1; bar <= BAR_COUNT; bar++) {
            List<CurvePoint> points = barCurves == null ? null : barCurves.get(bar);
            normalized.put(bar, normalizeCurve(points, defaultCurveFor(bar)));
        }
        barCurves = normalized;
    }

    private static List<CurvePoint> normalizeCurve(List<CurvePoint> points, List<CurvePoint> fallback) {
        if (points == null || points.size() < 2) {
            return fallback;
        }
        List<CurvePoint> result = new ArrayList<>(points.size());
        for (CurvePoint point : points) {
            if (point == null) {
                continue;
            }
            result.add(new CurvePoint(
                    clamp(point.speedMs(), AdvanceCurve.MIN_SPEED, AdvanceCurve.MAX_SPEED),
                    clamp(point.advanceAmount(), AdvanceCurve.MIN_ADVANCE, AdvanceCurve.MAX_ADVANCE)
            ));
        }
        if (result.size() < 2) {
            return fallback;
        }
        result.sort(java.util.Comparator.comparingDouble(CurvePoint::speedMs));
        return result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
