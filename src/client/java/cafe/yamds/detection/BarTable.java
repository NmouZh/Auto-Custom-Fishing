package cafe.yamds.detection;

/**
 * Custom-Fishing 各进度条（bar）的静态参数表。
 *
 * <p>这是 bar 元数据的<b>唯一</b>来源：格子数量、每格宽度、以及每个格子的成功率。
 * 运行时判定与配置界面可视化都必须从这里取数，避免多份表各说各话。
 */
public final class BarTable {

    private static final BarInfo FALLBACK = new BarInfo(11, 16, new double[11]);

    private static final BarInfo[] BARS = {
            // Bar 1-3：11 格，每格 16px
            new BarInfo(11, 16, rates(11, 4, 0.2, 5, 0.6, 6, 1.0, 7, 0.6, 8, 0.2)),
            new BarInfo(11, 16, rates(11, 2, 0.2, 3, 0.6, 4, 1.0, 5, 0.6, 6, 0.2)),
            new BarInfo(11, 16, rates(11, 6, 0.2, 7, 0.6, 8, 1.0, 9, 0.6, 10, 0.2)),
            // Bar 4-6：22 格，每格 8px
            new BarInfo(22, 8, rates(22,
                    3, 0.1, 4, 0.1, 7, 0.2, 8, 0.2, 9, 0.6, 10, 0.6, 11, 1.0,
                    12, 0.6, 13, 0.2, 15, 0.6, 16, 0.6, 17, 1.0, 18, 0.6, 19, 0.2, 20, 0.1)),
            new BarInfo(22, 8, rates(22,
                    1, 0.6, 3, 0.1, 4, 0.2, 5, 0.2, 6, 0.6, 7, 0.6, 8, 1.0,
                    9, 0.6, 10, 0.2, 12, 0.2, 13, 0.6, 14, 1.0, 15, 0.6, 16, 0.2,
                    18, 1.0, 19, 0.2, 20, 0.1)),
            new BarInfo(22, 8, rates(22,
                    1, 0.1, 2, 0.1, 3, 0.1, 4, 0.1, 5, 0.1, 6, 0.1, 7, 0.1, 8, 0.1,
                    9, 1.0, 11, 1.0, 13, 1.0,
                    15, 0.1, 16, 0.1, 17, 0.1, 18, 0.1, 19, 0.1, 20, 0.1, 21, 0.1, 22, 0.1)),
            // Bar 7-9：44 格，每格 4px（高难度）
            new BarInfo(44, 4, rates(44, 35, 0.1, 36, 0.1, 39, 1.0, 42, 0.3, 43, 0.3, 44, 0.3)),
            new BarInfo(44, 4, rates(44,
                    17, 0.2, 18, 0.2, 19, 0.2, 20, 0.6, 21, 1.0, 22, 0.6,
                    23, 0.2, 24, 0.2, 25, 0.2, 26, 0.2)),
            new BarInfo(44, 4, rates(44, 44, 1.0)),
    };

    private BarTable() {
    }

    /**
     * @param barType 1-9 为已知 bar；10/11（rainbow、bar10）等未知类型返回保守的默认参数
     */
    public static BarInfo byType(int barType) {
        if (barType >= 1 && barType <= BARS.length) {
            return BARS[barType - 1];
        }
        return FALLBACK;
    }

    public static int count() {
        return BARS.length;
    }

    /**
     * 按 "位置, 成功率, 位置, 成功率..." 构造成功率数组。
     */
    private static double[] rates(int length, double... positionRatePairs) {
        double[] result = new double[length];
        for (int i = 0; i < positionRatePairs.length; i += 2) {
            int position = (int) positionRatePairs[i];
            result[position - 1] = positionRatePairs[i + 1];
        }
        return result;
    }

    /**
     * 单个 bar 的参数。
     */
    public static final class BarInfo {
        private final int maxPosition;
        private final int widthPerSection;
        private final double[] successRates;

        private BarInfo(int maxPosition, int widthPerSection, double[] successRates) {
            this.maxPosition = maxPosition;
            this.widthPerSection = widthPerSection;
            this.successRates = successRates;
        }

        public int maxPosition() {
            return maxPosition;
        }

        public int widthPerSection() {
            return widthPerSection;
        }

        /** @param position 1-based 位置；越界返回 0 */
        public double successRate(int position) {
            if (position < 1 || position > maxPosition) {
                return 0.0;
            }
            return successRates[position - 1];
        }

        public boolean isPerfect(int position) {
            return successRate(position) >= 1.0;
        }

        /**
         * 从 {@code from} 开始（含）向后查找第一个完美位置。
         *
         * @param maxLookahead 最多向前看几格
         * @return 完美位置，找不到返回 -1
         */
        public int nextPerfectPosition(int from, int maxLookahead) {
            int end = Math.min(maxPosition, from + maxLookahead);
            for (int position = Math.max(1, from); position <= end; position++) {
                if (isPerfect(position)) {
                    return position;
                }
            }
            return -1;
        }
    }
}
