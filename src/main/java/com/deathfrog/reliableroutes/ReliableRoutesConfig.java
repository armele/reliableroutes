package com.deathfrog.reliableroutes;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side Reliable Routes behavior settings. */
public final class ReliableRoutesConfig
{
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_PATHFINDING;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLE_CUSTOM_PATHFINDING = builder
            .comment(
                "Whether MineColonies citizens may use the Reliable Routes custom navigator.",
                "When false, Path Pair blocks remain craftable and pairable but are purely decorative for citizen pathfinding.",
                "Citizens that already cached the custom navigator also fall back to normal MineColonies navigation.")
            .define("enableCustomPathfinding", true);
        SPEC = builder.build();
    }

    private ReliableRoutesConfig()
    {
        throw new IllegalStateException("ReliableRoutesConfig cannot be instantiated");
    }

    public static boolean isCustomPathfindingEnabled()
    {
        return ENABLE_CUSTOM_PATHFINDING.get();
    }

}
