package cafe.yamds.detection;

import java.util.List;

/**
 * 构造 Custom-Fishing 协议帧的测试辅助类。
 */
public final class TestUiFrames {

    public static final String FONT_BAR = "customfishing:default";
    public static final String FONT_OFFSET = "customfishing:offset_chars";

    private static final int POINTER_OFFSET = -183;

    /** 负数编码字符（按绝对值从大到小）。 */
    private static final int[] NEGATIVE_VALUES = {-130, -66, -34, -18, -10, -6, -4, -3, -1};
    private static final char[] NEGATIVE_CHARS = {
            '', '', '', '', '', '', '', '', ''
    };

    /** 正数编码字符（从大到小）。 */
    private static final int[] POSITIVE_VALUES = {127, 63, 31, 15, 7, 3, 1};
    private static final char[] POSITIVE_CHARS = {
            '', '', '', '', '', '', ''
    };

    private TestUiFrames() {
    }

    /** 构造指向指定 1-based 位置的一帧数据。 */
    public static List<TextPart> frame(int barNum, int position) {
        return frameWithProgress(barNum, progressForPosition(barNum, position));
    }

    public static List<TextPart> frameWithProgress(int barNum, int progress) {
        return List.of(
                new TextPart(String.valueOf(barChar(barNum)), FONT_BAR),
                new TextPart(encode(progress + POINTER_OFFSET), FONT_OFFSET),
                new TextPart("뀁", FONT_BAR),
                new TextPart(encode(123), FONT_OFFSET)
        );
    }

    public static char barChar(int barNum) {
        return (char) (0xB001 + barNum);
    }

    /** 让 position 公式精确落在指定格子的 progress。 */
    public static int progressForPosition(int barNum, int position) {
        int width = BarTable.byType(barNum).widthPerSection();
        return (position - 1) * width;
    }

    /** 用 offset_chars 字符集表示任意偏移值。 */
    public static String encode(int value) {
        StringBuilder result = new StringBuilder();
        if (value < 0) {
            int remaining = -value;
            for (int i = 0; i < NEGATIVE_VALUES.length; i++) {
                int magnitude = -NEGATIVE_VALUES[i];
                while (remaining >= magnitude) {
                    result.append(NEGATIVE_CHARS[i]);
                    remaining -= magnitude;
                }
            }
        } else {
            int remaining = value;
            for (int i = 0; i < POSITIVE_VALUES.length; i++) {
                while (remaining >= POSITIVE_VALUES[i]) {
                    result.append(POSITIVE_CHARS[i]);
                    remaining -= POSITIVE_VALUES[i];
                }
            }
        }
        if (value < 0 && -value > 272) {
            throw new IllegalArgumentException("value out of encodable range: " + value);
        }
        if (value > 247) {
            throw new IllegalArgumentException("value out of encodable range: " + value);
        }
        return result.toString();
    }
}
