package com.deathfrog.reliableroutes;

/** Shared identifiers and serialization keys used by Reliable Routes. */
public final class Constants
{
    public static final String MOD_ID = "reliableroutes";

    public static final String RELIABLEROUTES_ROAD_ID = "reliableroutes_road";
    public static final String FORBIDDEN_GROUND_ID = "forbidden_ground";
    public static final String FORBIDDEN_GROUND_CURB_ID = "forbidden_ground_curb";
    public static final String PATHFINDER_LENS_ID = "pathfinder_lens";
    public static final String PATH_PAIR_ID = "path_pair";
    public static final String CREATIVE_TAB_ID = "reliable_routes";
    public static final String CREATIVE_TAB_TRANSLATION_KEY = "itemGroup.reliableroutes";

    public static final String ROUTING_BLOCKS_GROUP_ID = "routing_blocks";
    public static final String ROUTING_BLOCK_MATERIALS_TAG_ID = "routing_block_materials";
    public static final String DEFAULT_MATERIAL_TEXTURE = "block/stone_bricks";
    public static final String DEFAULT_TOP_TEXTURE = "block/gold_block";

    private Constants()
    {
        throw new IllegalStateException("Constants cannot be instantiated");
    }
}
