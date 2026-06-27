package ink.myumoon.epiphany;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_EPIPHANY_SLOTS = BUILDER
            .comment("Maximum number of Epiphany slots a player can have")
            .defineInRange("maxEpiphanySlots", 8, 1, 32);

    public static final ModConfigSpec.LongValue BASE_APTITUDE_CAP = BUILDER
            .comment("Base aptitude required to earn the first Insight Point")
            .defineInRange("baseAptitudeCap", 100L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue APTITUDE_CAP_GROWTH = BUILDER
            .comment("Additional aptitude required per Insight Point already spent. Formula: cap = baseCap + totalSpent * growth")
            .defineInRange("aptitudeCapGrowth", 50L, 0L, Long.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
