package ink.myumoon.epiphany;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 顿悟槽上限
    public static final ModConfigSpec.IntValue MAX_EPIPHANY_SLOTS = BUILDER
            .comment("Maximum number of Epiphany slots a player can have")
            .defineInRange("maxEpiphanySlots", 8, 1, 32);

    // 选择模块消耗心得点
    public static final ModConfigSpec.IntValue MODULE_SELECT_COST = BUILDER
            .comment("Insight Points required to select a Module")
            .defineInRange("moduleSelectCost", 1, 0, 100);

    // 最大选择模块数量
    public static final ModConfigSpec.IntValue MAX_SELECTED_MODULES = BUILDER
            .comment("Maximum number of Modules a player can have selected at once")
            .defineInRange("maxSelectedModules", 8, 1, 64);

    // 阅历计算公式
    public static final ModConfigSpec.LongValue BASE_APTITUDE_CAP = BUILDER
            .comment("Base aptitude required to earn the first Insight Point")
            .defineInRange("baseAptitudeCap", 10L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue APTITUDE_CAP_GROWTH = BUILDER
            .comment("Additional aptitude required per Insight Point already spent. Formula: cap = baseCap + totalSpent * growth")
            .defineInRange("aptitudeCapGrowth", 1L, 0L, Long.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
