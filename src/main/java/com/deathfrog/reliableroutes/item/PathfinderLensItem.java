package com.deathfrog.reliableroutes.item;

import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.navigation.RoutingZone;
import com.deathfrog.reliableroutes.navigation.WaypointPairSavedData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Configures a protected routing zone from two endpoints and two opposite corners. */
public class PathfinderLensItem extends Item
{
    private static final String FIRST = "FirstEndpoint";
    private static final String SECOND = "SecondEndpoint";
    private static final String CORNER_ONE = "CornerOne";
    private static final String CORNER_TWO = "CornerTwo";
    private static final String DIMENSION = "Dimension";
    private static final double MAX_PAIR_DISTANCE_SQUARED = 256.0 * 256.0;
    private static final int MAX_ZONE_SPAN = 512;

    public PathfinderLensItem(Properties properties)
    {
        super(properties);
    }

    @SuppressWarnings("null")
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (stack == null) return InteractionResultHolder.pass(ItemStack.EMPTY);

        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide)
        {
            clearDraft(stack);
            player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.selection_cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @SuppressWarnings("null")
    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.getBlockState(context.getClickedPos()).is(ReliableRoutes.PATH_PAIR.get())) return InteractionResult.PASS;
        if (level.isClientSide) return getDraft(context.getItemInHand()) == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        Draft draft = getDraft(context.getItemInHand());
        if (draft == null || draft.secondEndpoint() == null)
        {
            player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.select_endpoints_first"), true);
            return InteractionResult.CONSUME;
        }
        if (!draft.dimension().equals(level.dimension().location()))
        {
            clearDraft(context.getItemInHand());
            return InteractionResult.CONSUME;
        }
        BlockPos clicked = context.getClickedPos();
        if (draft.firstCorner() == null)
        {
            updateDraft(context.getItemInHand(), draft.withFirstCorner(clicked));
            player.displayClientMessage(
                Component.translatable("message.reliableroutes.routing_zone.corner_selected", clicked.getX(), clicked.getZ()),
                true);
        }
        else
        {
            if (Math.abs(clicked.getX() - draft.firstCorner().getX()) > MAX_ZONE_SPAN ||
                Math.abs(clicked.getZ() - draft.firstCorner().getZ()) > MAX_ZONE_SPAN)
            {
                player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.too_large"), true);
                return InteractionResult.CONSUME;
            }
            Draft completed = draft.withSecondCorner(clicked);
            RoutingZone zone = RoutingZone
                .fromCorners(completed.firstCorner(), completed.secondCorner(), completed.firstEndpoint(), completed.secondEndpoint());
            if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.CONSUME;
            if (!endpointsShareColony(serverLevel, completed.firstEndpoint(), completed.secondEndpoint()))
            {
                updateDraft(context.getItemInHand(), completed);
                showColonyBoundaryError(player);
            }
            else if (WaypointPairSavedData.get(serverLevel).addZone(zone))
            {
                clearDraft(context.getItemInHand());
                player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.created"), true);
            }
            else
            {
                updateDraft(context.getItemInHand(), completed);
                player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.invalid"), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("null")
    public static ItemInteractionResult useOnWaypoint(ItemStack stack, Level level, BlockPos pos, Player player)
    {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return ItemInteractionResult.FAIL;
        WaypointPairSavedData data = WaypointPairSavedData.get(serverLevel);
        Draft draft = getDraft(stack);
        ResourceLocation dimension = serverLevel.dimension().location();

        if (player.isShiftKeyDown())
        {
            if (draft != null && draft.isComplete() &&
                draft.dimension().equals(dimension) &&
                (draft.firstEndpoint().equals(pos) || draft.secondEndpoint().equals(pos)))
            {
                RoutingZone zone =
                    RoutingZone.fromCorners(draft.firstCorner(), draft.secondCorner(), draft.firstEndpoint(), draft.secondEndpoint());

                if (!endpointsShareColony(serverLevel, draft.firstEndpoint(), draft.secondEndpoint()))
                {
                    showColonyBoundaryError(player);
                }
                else if (data.addZone(zone))
                {
                    clearDraft(stack);
                    player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.created"), true);
                }
                else player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.invalid"), true);
                return ItemInteractionResult.CONSUME;
            }
            boolean removed = data.removeZoneAt(pos);
            clearDraft(stack);
            player.displayClientMessage(
                Component.translatable(
                    removed ? "message.reliableroutes.routing_zone.removed" : "message.reliableroutes.routing_zone.selection_cleared"),
                true);
            return ItemInteractionResult.CONSUME;
        }

        if (data.zoneAtEndpoint(pos).isPresent())
        {
            player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.already_active"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (draft == null || !draft.dimension().equals(dimension) ||
            !serverLevel.getBlockState(draft.firstEndpoint()).is(ReliableRoutes.PATH_PAIR.get()))
        {
            if (colonyAt(serverLevel, pos) == null)
            {
                showColonyBoundaryError(player);
                return ItemInteractionResult.CONSUME;
            }
            updateDraft(stack, new Draft(pos.immutable(), null, null, null, dimension));
            player.displayClientMessage(
                Component.translatable("message.reliableroutes.routing_zone.endpoint_selected", pos.getX(), pos.getY(), pos.getZ()),
                true);
            return ItemInteractionResult.CONSUME;
        }
        if (draft.secondEndpoint() != null)
        {
            player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.select_corners"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (draft.firstEndpoint().equals(pos))
        {
            clearDraft(stack);
            return ItemInteractionResult.CONSUME;
        }
        if (!EndpointPairMath
            .hasHorizontalSeparation(draft.firstEndpoint().getX(), draft.firstEndpoint().getZ(), pos.getX(), pos.getZ()))
        {
            clearDraft(stack);
            player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.endpoints_same_column"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (draft.firstEndpoint().distSqr(pos) > MAX_PAIR_DISTANCE_SQUARED)
        {
            player.displayClientMessage(Component.translatable("message.reliableroutes.path_pair.too_far"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!endpointsShareColony(serverLevel, draft.firstEndpoint(), pos))
        {
            showColonyBoundaryError(player);
            return ItemInteractionResult.CONSUME;
        }
        updateDraft(stack, draft.withSecondEndpoint(pos));
        player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.endpoints_paired"), true);
        return ItemInteractionResult.CONSUME;
    }

    @SuppressWarnings("null")
    @Nullable
    public static Draft getDraft(ItemStack stack)
    {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(FIRST) || !tag.contains(DIMENSION)) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION));
        if (dimension == null) return null;
        return new Draft(BlockPos
            .of(tag.getLong(FIRST)), readPos(tag, SECOND), readPos(tag, CORNER_ONE), readPos(tag, CORNER_TWO), dimension);
    }

    /** Compatibility accessor used by the existing endpoint highlight. */
    @Nullable
    public static PendingWaypoint getPending(ItemStack stack)
    {
        Draft draft = getDraft(stack);
        return draft == null ? null : new PendingWaypoint(draft.firstEndpoint(), draft.dimension());
    }

    private static BlockPos readPos(CompoundTag tag, @Nonnull String name)
    {
        return tag.contains(name) ? BlockPos.of(tag.getLong(name)) : null;
    }

    @SuppressWarnings("null")
    private static void updateDraft(ItemStack stack, Draft draft)
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong(FIRST, draft.firstEndpoint().asLong());
        if (draft.secondEndpoint() != null) tag.putLong(SECOND, draft.secondEndpoint().asLong());
        if (draft.firstCorner() != null) tag.putLong(CORNER_ONE, draft.firstCorner().asLong());
        if (draft.secondCorner() != null) tag.putLong(CORNER_TWO, draft.secondCorner().asLong());
        tag.putString(DIMENSION, draft.dimension().toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @SuppressWarnings("null")
    private static void clearDraft(ItemStack stack)
    {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    }

    private static boolean endpointsShareColony(ServerLevel level, BlockPos first, BlockPos second)
    {
        IColony firstColony = colonyAt(level, first);
        IColony secondColony = colonyAt(level, second);
        return firstColony != null && secondColony != null && firstColony.getID() == secondColony.getID();
    }

    @Nullable
    private static IColony colonyAt(ServerLevel level, BlockPos pos)
    {
        return IColonyManager.getInstance().getColonyByPosFromWorld(level, pos);
    }

    @SuppressWarnings("null")
    private static void showColonyBoundaryError(Player player)
    {
        player.displayClientMessage(Component.translatable("message.reliableroutes.routing_zone.same_colony_required"), true);
    }

    public record Draft(BlockPos firstEndpoint,
        BlockPos secondEndpoint,
        BlockPos firstCorner,
        BlockPos secondCorner,
        ResourceLocation dimension)
    {
        public boolean isComplete()
        {
            return secondEndpoint != null && firstCorner != null && secondCorner != null;
        }

        Draft withSecondEndpoint(BlockPos value)
        {
            return new Draft(firstEndpoint, value.immutable(), firstCorner, secondCorner, dimension);
        }

        Draft withFirstCorner(BlockPos value)
        {
            return new Draft(firstEndpoint, secondEndpoint, value.immutable(), null, dimension);
        }

        Draft withSecondCorner(BlockPos value)
        {
            return new Draft(firstEndpoint, secondEndpoint, firstCorner, value.immutable(), dimension);
        }
    }

    public record PendingWaypoint(BlockPos pos, ResourceLocation dimension)
    {}
}
