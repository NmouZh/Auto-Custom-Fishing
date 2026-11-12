package cafe.yamds.fishing;

/**
 * 状态机对游戏世界的最小依赖。
 *
 * <p>生产实现是 {@code MinecraftFishingEnvironment}；测试里可以注入假实现。
 * 所有方法都只在客户端线程调用。
 */
public interface FishingEnvironment {

    /** 主手或副手是否持有鱼竿。 */
    boolean holdingRod();

    /** 当前玩家是否有已抛出的鱼钩实体。 */
    boolean hasHook();

    /** 抛出鱼竿（useItem）。 */
    void castRod();

    /** 收杆（useItem）。 */
    void reelIn();

    /** 在 ActionBar 显示一行提示。 */
    void actionBar(String message);
}
