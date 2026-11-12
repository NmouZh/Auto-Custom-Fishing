package cafe.yamds.detection;

/**
 * 从一帧 Title/Subtitle 数据中解析出的游戏画面信息。
 *
 * @param barType  进度条编号（1-9），未知时为 -1
 * @param position 指针所在格子（1-based）
 */
public record UiFrame(int barType, int position) {
}
