package cafe.yamds.config;

/**
 * 按 bar 类型提供提前量曲线。运行时由 {@link ConfigStore} 提供缓存实现。
 */
@FunctionalInterface
public interface CurveSource {

    AdvanceCurve curveFor(int barType);
}
