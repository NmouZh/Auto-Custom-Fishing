package cafe.yamds.detection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointerTrackerTest {

    @Test
    void needsAtLeastTwoSamples() {
        PointerTracker tracker = new PointerTracker();
        tracker.record(1, 0);
        assertEquals(0.0, tracker.averageSpeedMsPerSection());
    }

    @Test
    void measuresSteadyMovement() {
        PointerTracker tracker = new PointerTracker();
        for (int i = 0; i < 4; i++) {
            tracker.record(1 + i, 1_000 + i * 50L);
        }
        // 每 50ms 移动 1 格
        assertEquals(50.0, tracker.averageSpeedMsPerSection(), 1e-9);
        assertEquals(4, tracker.sampleCount());
    }

    @Test
    void ignoresLargeJumps() {
        PointerTracker tracker = new PointerTracker();
        tracker.record(1, 1_000);
        tracker.record(2, 1_050);
        tracker.record(10, 1_100);  // 跳跃 8 格，忽略
        tracker.record(11, 1_150);

        // 有效样本：1→2 (50ms) 与 10→11 (50ms)
        assertEquals(50.0, tracker.averageSpeedMsPerSection(), 1e-9);
    }

    @Test
    void averagesMultiSectionSteps() {
        PointerTracker tracker = new PointerTracker();
        tracker.record(1, 1_000);
        tracker.record(3, 1_100); // 2 格 / 100ms
        assertEquals(50.0, tracker.averageSpeedMsPerSection(), 1e-9);
    }

    @Test
    void resetClearsSamples() {
        PointerTracker tracker = new PointerTracker();
        tracker.record(1, 1_000);
        tracker.record(2, 1_050);
        tracker.reset();

        assertEquals(0, tracker.sampleCount());
        assertEquals(0.0, tracker.averageSpeedMsPerSection());
    }
}
