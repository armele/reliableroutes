package com.deathfrog.reliableroutes;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side Reliable Routes behavior settings. */
public final class ReliableRoutesConfig
{
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_PATHFINDING;
    private static final ModConfigSpec.IntValue WAYPOINT_INFLUENCE_RADIUS;
    private static final ModConfigSpec.DoubleValue MAXIMUM_WAYPOINT_DETOUR_FACTOR;
    private static final ModConfigSpec.IntValue MAXIMUM_WAYPOINT_ADDITIONAL_DISTANCE;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLE_CUSTOM_PATHFINDING = builder
            .comment(
                "Whether MineColonies citizens may use the Reliable Routes custom navigator.",
                "When false, Path Pair blocks remain craftable and pairable but are purely decorative for citizen pathfinding.",
                "Citizens that already cached the custom navigator also fall back to normal MineColonies navigation.")
            .define("enableCustomPathfinding", true);
        WAYPOINT_INFLUENCE_RADIUS = builder
            .comment("Maximum horizontal distance between a Path Pair entrance and the citizen's direct trip corridor.")
            .defineInRange("waypointInfluenceRadius", 32, 1, 256);
        MAXIMUM_WAYPOINT_DETOUR_FACTOR = builder
            .comment("Maximum paired-route distance as a multiple of the direct start-to-destination distance.")
            .defineInRange("maximumWaypointDetourFactor", 1.75, 1.0, 10.0);
        MAXIMUM_WAYPOINT_ADDITIONAL_DISTANCE = builder
            .comment("Additional blocks a paired route may add after applying the detour factor.")
            .defineInRange("maximumWaypointAdditionalDistance", 16, 0, 256);
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

    public static int waypointInfluenceRadius()
    {
        return WAYPOINT_INFLUENCE_RADIUS.get();
    }

    public static double maximumWaypointDetourFactor()
    {
        return MAXIMUM_WAYPOINT_DETOUR_FACTOR.get();
    }

    public static int maximumWaypointAdditionalDistance()
    {
        return MAXIMUM_WAYPOINT_ADDITIONAL_DISTANCE.get();
    }
}
