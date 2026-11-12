package cafe.yamds.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 不可变的提前量曲线：速度（ms/格）→ 提前量（格）。
 *
 * <p>构造时排序并夹取到合法范围，运行时查询就是一次线性插值。
 * 配置界面通过返回新实例的方式编辑，避免共享可变状态。
 */
public final class AdvanceCurve {

    public static final double MIN_SPEED = 0.0;
    public static final double MAX_SPEED = 35.0;
    public static final double MIN_ADVANCE = -5.0;
    public static final double MAX_ADVANCE = 5.0;

    private static final List<CurvePoint> FALLBACK_POINTS = List.of(
            new CurvePoint(0.0, 1.0),
            new CurvePoint(35.0, 0.0)
    );
    private static final AdvanceCurve FALLBACK = new AdvanceCurve(FALLBACK_POINTS);

    private final List<CurvePoint> points;

    private AdvanceCurve(List<CurvePoint> sortedPoints) {
        this.points = List.copyOf(sortedPoints);
    }

    /**
     * 规范化并创建曲线：过滤 null、夹取数值、按速度排序。
     * 少于 2 个点时返回默认曲线（与旧版行为一致）。
     */
    public static AdvanceCurve of(List<CurvePoint> rawPoints) {
        if (rawPoints == null || rawPoints.size() < 2) {
            return FALLBACK;
        }

        List<CurvePoint> normalized = new ArrayList<>(rawPoints.size());
        for (CurvePoint point : rawPoints) {
            if (point != null) {
                normalized.add(clamp(point.speedMs(), point.advanceAmount()));
            }
        }
        if (normalized.size() < 2) {
            return FALLBACK;
        }
        normalized.sort(Comparator.comparingDouble(CurvePoint::speedMs));
        return new AdvanceCurve(normalized);
    }

    public static AdvanceCurve fallback() {
        return FALLBACK;
    }

    public List<CurvePoint> points() {
        return points;
    }

    /** 线性插值；超出两端时取端点值。 */
    public double advanceAt(double speedMs) {
        if (speedMs <= points.get(0).speedMs()) {
            return points.get(0).advanceAmount();
        }
        CurvePoint last = points.get(points.size() - 1);
        if (speedMs >= last.speedMs()) {
            return last.advanceAmount();
        }

        for (int i = 0; i < points.size() - 1; i++) {
            CurvePoint left = points.get(i);
            CurvePoint right = points.get(i + 1);
            if (speedMs >= left.speedMs() && speedMs <= right.speedMs()) {
                double ratio = (speedMs - left.speedMs()) / (right.speedMs() - left.speedMs());
                return left.advanceAmount() + ratio * (right.advanceAmount() - left.advanceAmount());
            }
        }
        return 0.0;
    }

    /** 添加一个控制点并返回新曲线。 */
    public AdvanceCurve withPoint(double speedMs, double advanceAmount) {
        List<CurvePoint> copy = new ArrayList<>(points);
        copy.add(clamp(speedMs, advanceAmount));
        return of(copy);
    }

    /** 删除控制点；至少保留 2 个点。 */
    public AdvanceCurve withoutPoint(CurvePoint point) {
        if (points.size() <= 2) {
            return this;
        }
        List<CurvePoint> copy = new ArrayList<>(points);
        copy.remove(point);
        return of(copy);
    }

    /**
     * 查找距离给定坐标最近的控制点。
     *
     * @param maxDistance 超过该距离返回 null
     */
    public CurvePoint nearest(double speedMs, double advanceAmount, double maxDistance) {
        CurvePoint nearest = null;
        double best = maxDistance;

        for (CurvePoint point : points) {
            double dx = point.speedMs() - speedMs;
            double dy = point.advanceAmount() - advanceAmount;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < best) {
                best = distance;
                nearest = point;
            }
        }
        return nearest;
    }

    private static CurvePoint clamp(double speedMs, double advanceAmount) {
        return new CurvePoint(
                clamp(speedMs, MIN_SPEED, MAX_SPEED),
                clamp(advanceAmount, MIN_ADVANCE, MAX_ADVANCE)
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
