package com.deathfrog.reliableroutes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/** Data-driven block and entity classifications used by Reliable Routes. */
public final class ReliableRoutesTags
{
    public static final TagKey<Block> PATHING = blockTag(Constants.PATHING_TAG_ID);
    public static final TagKey<Block> FORBIDDEN_GROUND = blockTag(Constants.FORBIDDEN_GROUND_TAG_ID);
    
    public static final TagKey<EntityType<?>> USES_RELIABLE_ROUTES_NAVIGATOR =
        entityTypeTag(Constants.RELIABLE_ROUTES_NAVIGATOR_TAG_ID);

    public static final TagKey<EntityType<?>> TRAVERSE_FORBIDDEN_GROUND = entityTypeTag(Constants.TRAVERSE_FORBIDDEN_GROUND_TAG_ID);

    private ReliableRoutesTags()
    {
        throw new IllegalStateException("ReliableRoutesTags cannot be instantiated");
    }

    @SuppressWarnings("null")
    private static TagKey<Block> blockTag(String path)
    {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
    }

    @SuppressWarnings("null")
    private static TagKey<EntityType<?>> entityTypeTag(String path)
    {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
    }
}
