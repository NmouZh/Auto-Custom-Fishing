package cafe.yamds.fishing;

import cafe.yamds.config.AdvanceCurve;
import cafe.yamds.detection.BarTable;
import cafe.yamds.detection.GameView;

/**
 * 根据当前指针位置、速度与提前量曲线，判断是否应该立刻收杆。
 *
 * <p>纯计算，无 Minecraft 依赖。
 */
public final class ClickPredictor {

    /** 向后查找完美位置的最大格数。 */
    private static final int MAX_LOOKAHEAD = 5;

    public Decision decide(GameView view, AdvanceCurve curve, double globalAdvance) {
        BarTable.BarInfo bar = BarTable.byType(view.barType());
        double currentRate = bar.successRate(view.position());
        boolean click = false;
        int target = view.position();

        // 22/44 格的 bar 速度较快，用速度预测提前点击。
        if (view.maxPosition() == 22 || view.maxPosition() == 44) {
            if (view.position() >= 3 && view.samples() >= 3 && view.speedMs() > 0.0) {
                int nextPerfect = bar.nextPerfectPosition(view.position(), MAX_LOOKAHEAD);
                if (nextPerfect > 0) {
                    int positionsToGo = nextPerfect - view.position();
                    double timeToTarget = positionsToGo * view.speedMs();
                    double advance = curve.advanceAt(view.speedMs()) + globalAdvance;
                    double advanceTime = advance * view.speedMs();

                    if (positionsToGo == 0 || timeToTarget <= advanceTime) {
                        click = true;
                        target = nextPerfect;
                    }
                }
            }
            // 兜底：预测不满足时，只要当前就在完美位置就点。
            if (!click && currentRate >= 1.0) {
                click = true;
            }
        } else if (currentRate >= 1.0) {
            click = true;
        }

        return new Decision(click, target);
    }

    /**
     * @param click          是否立刻收杆
     * @param targetPosition 预测的完美位置（用于日志/调参）
     */
    public record Decision(boolean click, int targetPosition) {
    }
}
