package cafe.yamds.detection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingUiDetectorTest {

    private static final long T0 = 10_000;

    @Test
    void tracksPositionSpeedAndSuccessRate() {
        FishingUiDetector detector = new FishingUiDetector();

        for (int i = 0; i < 4; i++) {
            int position = 5 + i;
            detector.onUi(TestUiFrames.frame(1, position), T0 + i * 50L);
        }

        GameView view = detector.view(T0 + 150);
        assertTrue(view.active());
        assertEquals(1, view.barType());
        assertEquals(8, view.position());
        assertEquals(11, view.maxPosition());
        assertEquals(50.0, view.speedMs(), 1e-9);
        assertEquals(4, view.samples());
        assertEquals(0.2, view.successRate());
    }

    @Test
    void becomesInactiveWhenUiStops() {
        FishingUiDetector detector = new FishingUiDetector();
        detector.onUi(TestUiFrames.frame(1, 6), T0);

        assertTrue(detector.view(T0 + FishingUiDetector.STALE_MS).active());
        assertFalse(detector.view(T0 + FishingUiDetector.STALE_MS + 1).active());
    }

    @Test
    void ignoresNonCustomFishingComponents() {
        FishingUiDetector detector = new FishingUiDetector();
        assertTrue(detector.onUi(List.of(new TextPart("hello", "minecraft:default")), T0).isEmpty());
        assertFalse(detector.view(T0).active());
    }

    @Test
    void titleHintMarksGameActive() {
        FishingUiDetector detector = new FishingUiDetector();
        detector.onTitle(List.of(new TextPart("Fishing progress", "minecraft:default")), T0);
        assertTrue(detector.view(T0).active());
    }

    @Test
    void resetClearsEverything() {
        FishingUiDetector detector = new FishingUiDetector();
        detector.onUi(TestUiFrames.frame(4, 11), T0);
        detector.reset();
        assertFalse(detector.view(T0).active());
    }
}
