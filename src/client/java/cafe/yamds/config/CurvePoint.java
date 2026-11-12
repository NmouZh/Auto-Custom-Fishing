package cafe.yamds.config;

/**
 * 提前量曲线上的一个控制点。
 *
 * @param speedMs       速度（毫秒/格）
 * @param advanceAmount 提前量（格），正数表示更早点击
 */
public record CurvePoint(double speedMs, double advanceAmount) {
}
