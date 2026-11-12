package cafe.yamds.detection;

/**
 * 聊天组件中的一个文本片段及其生效字体。
 *
 * <p>Custom-Fishing 通过自定义字体区分 UI 元素：
 * <ul>
 *     <li>{@code customfishing:default} —— 进度条本体与指针</li>
 *     <li>{@code customfishing:offset_chars} —— 编码偏移量的不可见字符</li>
 * </ul>
 *
 * <p>这是纯数据类，不依赖 Minecraft 运行时，便于单元测试。
 */
public record TextPart(String text, String font) {
}
