package cafe.yamds.fishing;

import cafe.yamds.config.AdvanceCurve;
import cafe.yamds.config.AutoFishConfig;
import cafe.yamds.detection.TestUiFrames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingControllerTest {

    private static final long T0 = 1_000;

    private FakeEnvironment env;
    private AutoFishConfig config;
    private FishingLog log;
    private FishingController controller;

    @BeforeEach
    void setUp() {
        env = new FakeEnvironment();
        config = new AutoFishConfig();
        config.autoRecastMinutes = 0.0;
        config.castIntervalSeconds = 1.0;
        config.castIntervalRandomSeconds = 0.0;
        log = new FishingLog();
        controller = new FishingController(
                env,
                config,
                bar -> AdvanceCurve.of(AutoFishConfig.defaultCurveFor(bar)),
                log,
                new Random(0)
        );
    }

    @Test
    void happyPathCastsBitesClicksAndRecasts() {
        controller.tick(T0);
        assertEquals(1, env.casts);
        assertEquals(FishingState.CASTING, controller.state());

        controller.tick(T0 + 50);
        assertEquals(FishingState.WAITING_BITE, controller.state());

        controller.onBite(T0 + 100);
        assertEquals(1, env.reels);
        assertEquals(FishingState.WAITING_GAME, controller.state());

        controller.onGameText(TestUiFrames.frame(1, 6), T0 + 150);
        assertEquals(2, env.reels);
        assertEquals(FishingState.REELING, controller.state());
        assertEquals(1, log.entries().size());

        controller.tick(T0 + 200);
        assertEquals(FishingState.RECAST_DELAY, controller.state());

        controller.tick(T0 + 1_200);
        assertEquals(FishingState.IDLE, controller.state());

        controller.tick(T0 + 1_250);
        assertEquals(2, env.casts);
    }

    @Test
    void ordinaryCatchRecastsAfterGameStartTimeout() {
        castAndWaitForBite();
        controller.onBite(T0 + 100);

        controller.tick(T0 + 500);
        assertEquals(FishingState.WAITING_GAME, controller.state());

        controller.tick(T0 + 600);
        assertEquals(FishingState.RECAST_DELAY, controller.state());
        assertEquals(1, env.reels);
    }

    @Test
    void missedMinigameRecastsWhenUiGoesStale() {
        castAndWaitForBite();
        controller.onBite(T0 + 100);

        // 位置 1 不是完美位置 → 不点击
        controller.onGameText(TestUiFrames.frame(1, 1), T0 + 150);
        assertEquals(FishingState.PLAYING, controller.state());
        assertEquals(1, env.reels);

        controller.tick(T0 + 1_300);
        assertEquals(FishingState.RECAST_DELAY, controller.state());
    }

    @Test
    void forcedRecastWhenBiteNeverComes() {
        config.autoRecastMinutes = 0.01; // 600ms

        controller.tick(T0);
        controller.tick(T0 + 50);
        assertEquals(FishingState.WAITING_BITE, controller.state());

        controller.tick(T0 + 600);
        assertEquals(1, env.reels);
        assertEquals(FishingState.RECAST_DELAY, controller.state());
        assertTrue(env.messages.stream().anyMatch(message -> message.contains("超时")));
    }

    @Test
    void watchdogForcesRecastWhenHookStaysGone() {
        castAndWaitForBite();
        env.hook = false;

        controller.tick(T0 + 100);
        controller.tick(T0 + 100 + FishingController.NO_HOOK_WATCHDOG_MS);
        assertEquals(FishingState.RECAST_DELAY, controller.state());
        assertEquals(0, env.reels);
    }

    @Test
    void resetWhenRodIsSwitchedAway() {
        controller.tick(T0);
        env.rod = false;

        controller.tick(T0 + 50);
        assertEquals(FishingState.IDLE, controller.state());
    }

    @Test
    void existingHookIsPickedUpWithoutCasting() {
        env.hook = true;
        controller.tick(T0);
        assertEquals(0, env.casts);
        assertEquals(FishingState.WAITING_BITE, controller.state());
    }

    @Test
    void disabledConfigIgnoresFrames() {
        config.modEnabled = false;
        controller.tick(T0);
        controller.tick(T0 + 50);
        controller.onBite(T0 + 100);
        controller.onGameText(TestUiFrames.frame(1, 6), T0 + 150);

        assertEquals(0, env.reels);
        assertEquals(0, log.entries().size());
    }

    private void castAndWaitForBite() {
        controller.tick(T0);
        controller.tick(T0 + 50);
        assertEquals(FishingState.WAITING_BITE, controller.state());
    }

    private static final class FakeEnvironment implements FishingEnvironment {
        boolean rod = true;
        boolean hook;
        int casts;
        int reels;
        final List<String> messages = new ArrayList<>();

        @Override
        public boolean holdingRod() {
            return rod;
        }

        @Override
        public boolean hasHook() {
            return hook;
        }

        @Override
        public void castRod() {
            casts++;
            hook = true;
        }

        @Override
        public void reelIn() {
            reels++;
            hook = false;
        }

        @Override
        public void actionBar(String message) {
            messages.add(message);
        }
    }
}
