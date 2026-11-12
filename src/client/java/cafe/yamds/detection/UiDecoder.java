package cafe.yamds.detection;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Custom-Fishing 标题 UI 协议解码器。
 *
 * <p>协议结构（Subtitle 中的四个片段）：
 * <ol>
 *     <li>进度条图形（字体 {@code customfishing:default}）</li>
 *     <li>第一组偏移字符（字体 {@code customfishing:offset_chars}），值 = pointerOffset + progress</li>
 *     <li>指针图形（字体 {@code customfishing:default}）</li>
 *     <li>第二组偏移字符，值 = totalWidth - progress - pointerWidth（仅用于确认这是一帧有效数据）</li>
 * </ol>
 *
 * <p>本类只做纯计算，不依赖 Minecraft 类型，可直接单元测试。
 */
public final class UiDecoder {

    public static final String FONT_OFFSET_CHARS = "customfishing:offset_chars";
    public static final String FONT_DEFAULT = "customfishing:default";

    /** 第一组偏移 = POINTER_OFFSET + progress */
    private static final int POINTER_OFFSET = -183;
    private static final int EXPECTED_OFFSET_GROUPS = 2;

    private UiDecoder() {
    }

    /** Title 中出现该关键字说明小游戏界面已经开始。 */
    public static boolean hintsGame(String text) {
        return text != null && text.toLowerCase(Locale.ROOT).contains("progress");
    }

    /**
     * 从一帧 UI 片段中解码出进度条与指针位置。
     *
     * @return 不是 Custom-Fishing 小游戏帧时返回空
     */
    public static Optional<UiFrame> decodeFrame(List<TextPart> parts) {
        int barType = -1;
        int firstOffset = 0;
        int offsetGroups = 0;

        for (TextPart part : parts) {
            String font = part.font();
            if (font == null) {
                continue;
            }
            if (font.contains(FONT_OFFSET_CHARS)) {
                if (offsetGroups == 0) {
                    firstOffset = sumOffsets(part.text());
                }
                offsetGroups++;
            } else if (font.contains(FONT_DEFAULT)) {
                int decoded = barTypeOf(part.text());
                if (decoded > 0) {
                    barType = decoded;
                }
            }
        }

        if (offsetGroups != EXPECTED_OFFSET_GROUPS || barType <= 0) {
            return Optional.empty();
        }

        BarTable.BarInfo bar = BarTable.byType(barType);

        // progress 是像素偏移；四舍五入到格子中心后换算成 1-based 位置。
        int progress = firstOffset - POINTER_OFFSET;
        int position = (progress + bar.widthPerSection() / 2) / bar.widthPerSection() + 1;
        position = Math.max(1, Math.min(bar.maxPosition(), position));

        return Optional.of(new UiFrame(barType, position));
    }

    private static int sumOffsets(String text) {
        int sum = 0;
        for (int i = 0; i < text.length(); i++) {
            sum += offsetForChar(text.charAt(i));
        }
        return sum;
    }

    private static int barTypeOf(String text) {
        int result = -1;
        for (int i = 0; i < text.length(); i++) {
            int type = barTypeForChar(text.charAt(i));
            if (type > 0) {
                result = type;
            }
        }
        return result;
    }

    /**
     * offset_chars 字体中每个私用区字符代表的像素偏移。
     * 数值来自 Custom-Fishing 的 offset_chars.json。
     */
    static int offsetForChar(char c) {
        return switch (c) {
            case '' -> -3;
            case '' -> -4;
            case '' -> -6;
            case '' -> -10;
            case '' -> -18;
            case '' -> -34;
            case '' -> -66;
            case '' -> -130;
            case '' -> -1;
            case '' -> 1;
            case '' -> 3;
            case '' -> 7;
            case '' -> 15;
            case '' -> 31;
            case '' -> 63;
            case '' -> 127;
            default -> 0;
        };
    }

    /**
     * 进度条字符 → bar 编号。0 表示指针，-1 表示未知字符。
     */
    static int barTypeForChar(char c) {
        return switch (c) {
            case '뀁' -> 0;  // pointer
            case '뀂' -> 1;
            case '뀃' -> 2;
            case '뀄' -> 3;
            case '뀅' -> 4;
            case '뀆' -> 5;
            case '뀇' -> 6;
            case '뀈' -> 7;
            case '뀉' -> 8;
            case '뀊' -> 9;
            case '뀋' -> 10; // bar_rainbow
            case '뀌' -> 11; // bar10
            default -> -1;
        };
    }
}
