package cafe.yamds.ui;

import cafe.yamds.detection.BarTable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 用彩色方块可视化某个 bar 的成功率分布。
 */
public final class BarVisualizer {

    private static final String SYMBOL = "█";

    private BarVisualizer() {
    }

    public static Component generateBarVisualization(int barNum) {
        return generateBarVisualization(barNum, -1);
    }

    /**
     * @param highlightPosition 高亮位置（1-based，-1 表示不高亮）
     */
    public static Component generateBarVisualization(int barNum, int highlightPosition) {
        if (barNum < 1 || barNum > BarTable.count()) {
            return Component.literal("Invalid bar number");
        }

        BarTable.BarInfo bar = BarTable.byType(barNum);
        MutableComponent result = Component.literal("");

        for (int position = 1; position <= bar.maxPosition(); position++) {
            ChatFormatting color = position == highlightPosition
                    ? ChatFormatting.WHITE
                    : colorForRate(bar.successRate(position));
            result.append(Component.literal(SYMBOL).withStyle(color));
        }
        return result;
    }

    private static ChatFormatting colorForRate(double rate) {
        if (rate >= 1.0) {
            return ChatFormatting.GREEN;
        }
        if (rate >= 0.6) {
            return ChatFormatting.YELLOW;
        }
        if (rate >= 0.2) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.RED;
    }
}
