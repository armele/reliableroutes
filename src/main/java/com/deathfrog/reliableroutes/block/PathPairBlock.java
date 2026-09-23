package com.deathfrog.reliableroutes.block;

import com.deathfrog.reliableroutes.Constants;
import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.item.PathfinderLensItem;
import com.deathfrog.reliableroutes.navigation.WaypointPairSavedData;
import com.google.common.collect.ImmutableList;
import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlockComponent;
import com.ldtteam.domumornamentum.block.components.SimpleRetexturableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
import javax.annotation.Nonnull;

/** Two-material waypoint block that can be paired using the Pathfinder Lens. */
public class PathPairBlock extends RoutingBlock
{
    @SuppressWarnings("null")
    private static final TagKey<Block> MATERIALS = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, Constants.ROUTING_BLOCK_MATERIALS_TAG_ID));

    @SuppressWarnings("null")
    private static final @Nonnull List<IMateriallyTexturedBlockComponent> COMPONENTS = ImmutableList.of(
        new SimpleRetexturableComponent(ResourceLocation.withDefaultNamespace(Constants.DEFAULT_TOP_TEXTURE), MATERIALS, Blocks.GOLD_BLOCK),
        new SimpleRetexturableComponent(ResourceLocation.withDefaultNamespace(Constants.DEFAULT_MATERIAL_TEXTURE), MATERIALS, Blocks.STONE_BRICKS));

    @Override
    public @Nonnull List<IMateriallyTexturedBlockComponent> getComponents()
    {
        return COMPONENTS;
    }

    @Override
    public IMateriallyTexturedBlockComponent getMainComponent()
    {
        return COMPONENTS.getLast();
    }

    @SuppressWarnings("null")
    @Override
    protected ItemInteractionResult useItemOn(
        @Nonnull ItemStack stack,
        @Nonnull BlockState state,
        @Nonnull Level level,
        @Nonnull BlockPos pos,
        @Nonnull Player player,
        @Nonnull InteractionHand hand,
        @Nonnull BlockHitResult hit)
    {
        if (!stack.is(ReliableRoutes.PATHFINDER_LENS.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return PathfinderLensItem.useOnWaypoint(stack, level, pos, player);
    }

    @SuppressWarnings("null")
    @Override
    protected void onRemove(
        @Nonnull BlockState state,
        @Nonnull Level level,
        @Nonnull BlockPos pos,
        @Nonnull BlockState newState,
        boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel)
        {
            WaypointPairSavedData.get(serverLevel).unpair(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
