package cafe.yamds.detection;

import java.util.List;
import java.util.Optional;

/**
 * 维护小游戏 UI 的最新状态：最近一帧位置、估算速度、是否仍然活跃。
 *
 * <p>所有时间单位都是毫秒，由调用方传入，便于测试。
 */
public final class FishingUiDetector {

    /** 超过这个时间没有新的 UI 帧，就认为小游戏已经结束。 */
    public static final long STALE_MS = 1_000;

    private final PointerTracker tracker = new PointerTracker();

    private boolean active;
    private long lastUpdateAt;
    private int barType = -1;
    private int position = -1;
    private double speedMs;

    /** Title 中出现 "progress" 时提前标记小游戏开始。 */
    public void onTitle(List<TextPart> parts, long now) {
        for (TextPart part : parts) {
            if (UiDecoder.hintsGame(part.text())) {
                active = true;
                lastUpdateAt = now;
                return;
            }
        }
    }

    /**
     * 处理一帧 Subtitle / Overlay 数据。
     *
     * @return 成功解码时返回当前画面快照
     */
    public Optional<GameView> onUi(List<TextPart> parts, long now) {
        Optional<UiFrame> frame = UiDecoder.decodeFrame(parts);
        if (frame.isEmpty()) {
            return Optional.empty();
        }

        UiFrame decoded = frame.get();
        active = true;
        lastUpdateAt = now;
        barType = decoded.barType();
        position = decoded.position();

        tracker.record(position, now);
        speedMs = tracker.averageSpeedMsPerSection();

        return Optional.of(view(now));
    }

    /**
     * @return 当前画面；小游戏已结束或从未开始则返回 {@link GameView#INACTIVE}
     */
    public GameView view(long now) {
        if (!active || now - lastUpdateAt > STALE_MS) {
            return GameView.INACTIVE;
        }

        BarTable.BarInfo bar = BarTable.byType(barType);
        return new GameView(
                true,
                barType,
                position,
                bar.maxPosition(),
                speedMs,
                tracker.sampleCount(),
                bar.successRate(position)
        );
    }

    public void reset() {
        active = false;
        lastUpdateAt = 0;
        barType = -1;
        position = -1;
        speedMs = 0.0;
        tracker.reset();
    }
}
