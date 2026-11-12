package cafe.yamds.fishing;

/**
 * 自动钓鱼状态机的状态。
 *
 * <pre>
 * IDLE ──cast──▶ CASTING ──hook──▶ WAITING_BITE ──bite──▶ WAITING_GAME ──ui──▶ PLAYING
 *   ▲                                  │                        │                 │
 *   │                                  │超时                    │500ms 无 UI      │点击
 *   │                                  ▼                        ▼                 ▼
 *   └──── RECAST_DELAY ◀───────────────┴────────────────────────┴────────────── REELING
 * </pre>
 */
public enum FishingState {
    /** 空闲：没有鱼钩就抛竿。 */
    IDLE,
    /** 已抛竿，等待鱼钩实体出现。 */
    CASTING,
    /** 鱼钩在水里，等待咬钩。 */
    WAITING_BITE,
    /** 已收杆，等待小游戏 UI 出现（钓到普通物品则超时重抛）。 */
    WAITING_GAME,
    /** 小游戏中：解析指针位置并决定何时点击。 */
    PLAYING,
    /** 已点击，等待收杆完成。 */
    REELING,
    /** 等待随机间隔后重新抛竿。 */
    RECAST_DELAY
}
