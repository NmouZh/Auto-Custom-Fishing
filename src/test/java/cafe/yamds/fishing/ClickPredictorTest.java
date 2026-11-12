package cafe.yamds.fishing;

import cafe.yamds.config.AdvanceCurve;
import cafe.yamds.config.CurvePoint;
import cafe.yamds.detection.GameView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickPredictorTest {

    private final ClickPredictor predictor = new ClickPredictor();

    @Test
    void clicksOnPerfectPositionForShortBars() {
        assertTrue(decide(view(1, 6, 11, 50, 4), curve(0.0), 0.0).click());
        assertFalse(decide(view(1, 5, 11, 50, 4), curve(0.0), 0.0).click());
    }

    @Test
    void predictsClickForFastBars() {
        // bar 4：下一个完美位置 11，距离 1 格；速度 50ms，提前 1 格 → 现在点
        ClickPredictor.Decision decision = decide(view(4, 10, 22, 50, 4), flatCurve(1.0), 0.0);
        assertTrue(decision.click());
    }

    @Test
    void waitsWhenAdvanceIsTooSmall() {
        // 距离 2 格（9 → 11），需要 100ms；没有提前量 → 不点
        assertFalse(decide(view(4, 9, 22, 50, 4), flatCurve(0.0), 0.0).click());
    }

    @Test
    void globalAdvanceIsAddedToCurve() {
        // 曲线 0 提前量 + 全局 1 格 → 现在点
        assertTrue(decide(view(4, 10, 22, 50, 4), flatCurve(0.0), 1.0).click());
    }

    @Test
    void currentPerfectPositionIsAlwaysClicked() {
        // 即使没有速度样本，当前就在完美位置也要点
        assertTrue(decide(view(4, 11, 22, 0, 0), flatCurve(0.0), 0.0).click());
    }

    @Test
    void requiresEnoughSamplesForPrediction() {
        // 样本不足时不做预测；当前不在完美位置 → 不点
        assertFalse(decide(view(4, 10, 22, 50, 2), flatCurve(5.0), 0.0).click());
    }

    @Test
    void handles44SectionBars() {
        // bar 7：38 → 39（完美），距离 1 格，速度 20ms，提前 1 格 → 点
        assertTrue(decide(view(7, 38, 44, 20, 4), flatCurve(1.0), 0.0).click());
    }

    private static GameView view(int barType, int position, int maxPosition, double speedMs, int samples) {
        return new GameView(true, barType, position, maxPosition, speedMs, samples, 0.0);
    }

    private ClickPredictor.Decision decide(GameView view, AdvanceCurve curve, double globalAdvance) {
        return predictor.decide(view, curve, globalAdvance);
    }

    private static AdvanceCurve flatCurve(double advance) {
        return AdvanceCurve.of(List.of(
                new CurvePoint(0.0, advance),
                new CurvePoint(35.0, advance)
        ));
    }

    private static AdvanceCurve curve(double advance) {
        return flatCurve(advance);
    }
}
