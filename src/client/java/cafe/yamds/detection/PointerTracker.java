package cafe.yamds.detection;

/**
 * 记录指针位置随时间的采样，并估算平均移动速度（毫秒/格）。
 *
 * <p>纯计算类，不依赖 Minecraft。所有时间单位都是毫秒。
 */
public final class PointerTracker {

    private static final int CAPACITY = 50;
    /** 只统计相邻变化（1-3 格），过滤指针回弹、瞬移等异常样本。 */
    private static final int MAX_STEP = 3;

    private final long[] timestamps = new long[CAPACITY];
    private final int[] positions = new int[CAPACITY];

    private int writeIndex;
    private int count;

    public void record(int position, long timeMs) {
        timestamps[writeIndex] = timeMs;
        positions[writeIndex] = position;
        writeIndex = (writeIndex + 1) % CAPACITY;
        if (count < CAPACITY) {
            count++;
        }
    }

    public void reset() {
        writeIndex = 0;
        count = 0;
    }

    public int sampleCount() {
        return count;
    }

    /**
     * @return 平均速度（毫秒/格）；样本不足时返回 0
     */
    public double averageSpeedMsPerSection() {
        if (count < 2) {
            return 0.0;
        }

        double totalTime = 0.0;
        int totalChange = 0;
        int previous = (writeIndex - count + CAPACITY) % CAPACITY;

        for (int i = 1; i < count; i++) {
            int current = (previous + 1) % CAPACITY;
            int change = Math.abs(positions[current] - positions[previous]);
            long timeDiff = timestamps[current] - timestamps[previous];

            if (change > 0 && change <= MAX_STEP && timeDiff > 0) {
                totalTime += timeDiff;
                totalChange += change;
            }
            previous = current;
        }

        return totalChange > 0 ? totalTime / totalChange : 0.0;
    }
}
