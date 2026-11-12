package cafe.yamds.fishing;

import cafe.yamds.AutoFishingMod;
import cafe.yamds.config.AutoFishConfig;
import cafe.yamds.config.CurveSource;
import cafe.yamds.detection.FishingUiDetector;
import cafe.yamds.detection.GameView;
import cafe.yamds.detection.TextPart;

import java.util.List;
import java.util.Random;

/**
 * 自动钓鱼状态机。
 *
 * <p>设计约定：
 * <ul>
 *     <li>所有方法只在客户端线程调用，时间为毫秒时间戳，由调用方传入。</li>
 *     <li>点击决策发生在 {@link #onGameText}（即收到 UI 包的那一刻），
 *         不再等下一个 tick，去掉最多 50ms 的额外延迟。</li>
 *     <li>本类不依赖任何 Minecraft 类型，可直接用假环境做单元测试。</li>
 * </ul>
 */
public final class FishingController {

    /** 两次主动操作之间的最小间隔，避免发包过快。 */
    static final long CAST_COOLDOWN_MS = 500;
    /** 收杆后等待小游戏 UI 出现的超时；超时视为钓到普通物品。 */
    static final long GAME_START_TIMEOUT_MS = 500;
    /** 收杆后等待鱼钩消失的超时。 */
    static final long REELING_TIMEOUT_MS = 1_000;
    /** 鱼钩消失过久的兜底保护。 */
    static final long NO_HOOK_WATCHDOG_MS = 60_000;
    /** 重抛间隔下限。 */
    private static final long MIN_RECAST_DELAY_MS = 200;

    private final FishingEnvironment env;
    private final AutoFishConfig config;
    private final CurveSource curves;
    private final FishingLog log;
    private final Random random;
    private final FishingUiDetector detector = new FishingUiDetector();
    private final ClickPredictor predictor = new ClickPredictor();

    private FishingState state = FishingState.IDLE;
    private long stateEnteredAt;
    private long lastActionAt;
    private long castAt;
    private long recastDelayMs;
    private long hookAbsentSince;

    public FishingController(
            FishingEnvironment env,
            AutoFishConfig config,
            CurveSource curves,
            FishingLog log,
            Random random
    ) {
        this.env = env;
        this.config = config;
        this.curves = curves;
        this.log = log;
        this.random = random;
    }

    public FishingState state() {
        return state;
    }

    /** 驱动状态机的周期逻辑；建议每个客户端 tick 调用一次。 */
    public void tick(long now) {
        if (!config.modEnabled) {
            return;
        }
        if (!env.holdingRod()) {
            if (state != FishingState.IDLE) {
                resetSession(now);
            }
            return;
        }
        if (handleTimeouts(now)) {
            return;
        }

        switch (state) {
            case IDLE -> {
                if (env.hasHook()) {
                    enter(FishingState.WAITING_BITE, now);
                } else if (now - lastActionAt >= CAST_COOLDOWN_MS) {
                    cast(now);
                }
            }
            case RECAST_DELAY -> {
                if (now - stateEnteredAt >= recastDelayMs) {
                    enter(FishingState.IDLE, now);
                }
            }
            case CASTING -> {
                if (env.hasHook()) {
                    enter(FishingState.WAITING_BITE, now);
                }
            }
            case WAITING_BITE -> {
                // UI 已经出现（例如手动触发了小游戏）：直接进入游戏。
                if (detector.view(now).active()) {
                    enter(FishingState.PLAYING, now);
                }
            }
            case WAITING_GAME -> {
                if (detector.view(now).active()) {
                    enter(FishingState.PLAYING, now);
                } else if (now - stateEnteredAt >= GAME_START_TIMEOUT_MS) {
                    env.actionBar("§7未检测到小游戏，重新抛竿...");
                    startRecastDelay(now);
                }
            }
            case PLAYING -> {
                // 1 秒没有新的 UI 帧 = 小游戏结束（可能漏点）。
                if (!detector.view(now).active()) {
                    startRecastDelay(now);
                }
            }
            case REELING -> {
                if (!env.hasHook() || now - stateEnteredAt >= REELING_TIMEOUT_MS) {
                    startRecastDelay(now);
                }
            }
        }
    }

    /**
     * 鱼咬钩回调（由鱼钩 Mixin 在 biting 变化时调用）。
     * 立即收杆进入小游戏，不再等下一个 tick。
     */
    public void onBite(long now) {
        if (!config.modEnabled || state != FishingState.WAITING_BITE) {
            return;
        }
        reel(now);
        enter(FishingState.WAITING_GAME, now);
        env.actionBar("§e⚡上钩!");
    }

    /**
     * 收到 Subtitle / ActionBar 组件时调用。若本帧处于完美位置，立即收杆。
     */
    public void onGameText(List<TextPart> parts, long now) {
        if (!config.modEnabled) {
            return;
        }

        detector.onUi(parts, now).ifPresent(view -> {
            env.actionBar(view.hudLine());

            if (state == FishingState.WAITING_BITE || state == FishingState.WAITING_GAME) {
                enter(FishingState.PLAYING, now);
            }
            if (state != FishingState.PLAYING) {
                return;
            }

            ClickPredictor.Decision decision = predictor.decide(
                    view,
                    curves.curveFor(view.barType()),
                    config.globalAdvance
            );
            if (decision.click()) {
                AutoFishingMod.LOGGER.debug(
                        "Click: bar={} pos={} speed={}ms target={}",
                        view.barType(), view.position(), view.speedMs(), decision.targetPosition());
                reel(now);
                log.add(view.barType(), view.speedMs(), view.position(), decision.targetPosition());
                enter(FishingState.REELING, now);
            }
        });
    }

    /** 收到 Title 组件时调用（用于提前识别小游戏开始）。 */
    public void onTitleText(List<TextPart> parts, long now) {
        if (!config.modEnabled) {
            return;
        }
        detector.onTitle(parts, now);
    }

    /** 丢弃当前会话（关闭 Mod、切走鱼竿等）。 */
    public void reset(long now) {
        resetSession(now);
    }

    private boolean handleTimeouts(long now) {
        // 抛竿后长时间没有咬钩：强制收杆重抛。
        if (config.autoRecastMinutes > 0
                && castAt > 0
                && (state == FishingState.CASTING || state == FishingState.WAITING_BITE)
                && now - castAt >= (long) (config.autoRecastMinutes * 60_000.0)) {
            env.actionBar("§7超时未收杆，重新抛竿...");
            reel(now);
            startRecastDelay(now);
            return true;
        }

        // 鱼钩消失过久（状态机卡死兜底）。
        if (env.hasHook()) {
            hookAbsentSince = 0;
            return false;
        }
        if (state == FishingState.IDLE || state == FishingState.RECAST_DELAY) {
            return false;
        }
        if (hookAbsentSince == 0) {
            hookAbsentSince = now;
            return false;
        }
        if (now - hookAbsentSince < NO_HOOK_WATCHDOG_MS) {
            return false;
        }

        env.actionBar("§c⚠检测到1分钟未抛竿，强制抛竿...");
        detector.reset();
        hookAbsentSince = 0;
        startRecastDelay(now);
        return true;
    }

    private void cast(long now) {
        env.castRod();
        lastActionAt = now;
        castAt = now;
        enter(FishingState.CASTING, now);
        env.actionBar("§b🎣等待咬钩...");
    }

    private void reel(long now) {
        env.reelIn();
        lastActionAt = now;
        castAt = 0;
    }

    private void resetSession(long now) {
        detector.reset();
        state = FishingState.IDLE;
        stateEnteredAt = now;
        castAt = 0;
        hookAbsentSince = 0;
    }

    private void enter(FishingState next, long now) {
        if (state != next) {
            AutoFishingMod.LOGGER.debug("Fishing state: {} -> {}", state, next);
        }
        state = next;
        stateEnteredAt = now;

        // 进入新回合（等待游戏/空闲）时清空上一局的 UI 数据；
        // 进入 PLAYING 时绝不能清空，否则会把刚收到的这一帧丢掉。
        if (next == FishingState.IDLE || next == FishingState.WAITING_GAME) {
            detector.reset();
            if (next == FishingState.IDLE) {
                castAt = 0;
            }
        }
    }

    private void startRecastDelay(long now) {
        recastDelayMs = randomRecastDelayMs();
        state = FishingState.RECAST_DELAY;
        stateEnteredAt = now;
        castAt = 0;
        detector.reset();
    }

    private long randomRecastDelayMs() {
        double base = Math.max(0.1, config.castIntervalSeconds);
        double range = Math.max(0.0, config.castIntervalRandomSeconds);
        double delayMs = (base - range + random.nextDouble() * range * 2.0) * 1000.0;
        return Math.max(MIN_RECAST_DELAY_MS, (long) delayMs);
    }
}
