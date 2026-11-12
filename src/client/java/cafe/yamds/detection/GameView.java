package cafe.yamds.detection;

/**
 * 某一时刻小游戏画面的只读快照，供状态机与 HUD 使用。
 *
 * @param active      小游戏界面是否仍然有效（收到 UI 后 1 秒内）
 * @param barType     进度条编号
 * @param position    指针当前格子（1-based）
 * @param maxPosition 该 bar 的总格子数
 * @param speedMs     估算速度（毫秒/格）
 * @param samples     速度样本数
 * @param successRate 当前格子的成功率
 */
public record GameView(
        boolean active,
        int barType,
        int position,
        int maxPosition,
        double speedMs,
        int samples,
        double successRate
) {

    public static final GameView INACTIVE =
            new GameView(false, -1, -1, 0, 0.0, 0, 0.0);

    public String hudLine() {
        String color;
        String name;
        if (successRate >= 1.0) {
            color = "§a";
            name = "绿色";
        } else if (successRate >= 0.6) {
            color = "§e";
            name = "黄色";
        } else if (successRate >= 0.2) {
            color = "§6";
            name = "橙色";
        } else {
            color = "§c";
            name = "红色";
        }
        return String.format("%sBar %d §7| §f位置: %d/%d §7| %s%s §7| §f速度: %.1fms §7",
                color, barType, position, maxPosition, color, name, speedMs);
    }
}
