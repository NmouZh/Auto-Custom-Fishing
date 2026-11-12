package cafe.yamds.fishing;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近若干次点击的记录，供配置界面调参使用。
 */
public final class FishingLog {

    public static final int MAX_ENTRIES = 20;

    private final List<LogEntry> entries = new ArrayList<>();

    public void add(int barType, double averageSpeedMs, int clickPosition, int targetPosition) {
        entries.add(0, new LogEntry(
                barType,
                averageSpeedMs,
                clickPosition,
                targetPosition,
                System.currentTimeMillis()
        ));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }

    public List<LogEntry> entries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }

    public record LogEntry(
            int barType,
            double averageSpeedMs,
            int clickPosition,
            int targetPosition,
            long timestamp
    ) {
    }
}
