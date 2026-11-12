package cafe.yamds.detection;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 把 Minecraft 的 {@link Component} 拆成带字体的纯文本片段。
 *
 * <p>使用 {@link Component#visit} 而不是解析 {@code toString()}：后者依赖实现细节，
 * 前者直接读取组件的有效样式（含父级继承），更稳也更省。
 */
public final class ComponentScanner {

    private ComponentScanner() {
    }

    public static List<TextPart> scan(Component component) {
        if (component == null) {
            return List.of();
        }

        List<TextPart> parts = new ArrayList<>(8);
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                parts.add(new TextPart(text, fontName(style)));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return parts;
    }

    private static String fontName(Style style) {
        FontDescription font = style.getFont();
        if (font instanceof FontDescription.Resource resource) {
            return resource.id().toString();
        }
        return String.valueOf(font);
    }
}
