package ink.myumoon.epiphany.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
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

    // ============================================================
    // Notifications
    // Current implementation sends a chat message + advancement sound.
    // Planned: replace chat with Toast popup in a future client-side phase.
    // ============================================================
    public static final ModConfigSpec.BooleanValue NOTIFY_INSIGHT_POINTS = BUILDER
            .comment("Notify the player when they gain Insight Points (level-up, command, reward)")
            .define("notifyInsightPoints", true);

    public static final ModConfigSpec.BooleanValue NOTIFY_MODULE_UNLOCK = BUILDER
            .comment("Notify the player when a Module is unlocked via condition or command. Selectable modules are NOT notified.")
            .define("notifyModuleUnlock", true);

    public static final ModConfigSpec.BooleanValue NOTIFY_EPIPHANY_UNLOCK = BUILDER
            .comment("Notify the player when an Epiphany is unlocked via condition or command. Selectable epiphanies are NOT notified.")
            .define("notifyEpiphanyUnlock", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

}
