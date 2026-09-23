package com.deathfrog.reliableroutes.item;

import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.navigation.WaypointPairSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Selects and pairs Path Pair blocks and enables the routing overlay. */
public class PathfinderLensItem extends Item
{
    private static final String TAG_PENDING_POS = "PendingWaypoint";
    private static final String TAG_PENDING_DIMENSION = "PendingDimension";
    private static final double MAX_PAIR_DISTANCE_SQUARED = 256.0 * 256.0;

    public PathfinderLensItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide)
        {
            clearPending(stack);
            player.displayClientMessage(Component.translatable("message.reliableroutes.path_pair.selection_cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static ItemInteractionResult useOnWaypoint(ItemStack stack, Level level, BlockPos pos, Player player)
    {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return ItemInteractionResult.FAIL;

        WaypointPairSavedData pairs = WaypointPairSavedData.get(serverLevel);
        if (player.isShiftKeyDown())
        {
            boolean removed = pairs.unpair(pos);
            clearPending(stack);
            player.displayClientMessage(Component.translatable(removed
                ? "message.reliableroutes.path_pair.removed"
                : "message.reliableroutes.path_pair.selection_cleared"), true);
            return ItemInteractionResult.CONSUME;
        }

        PendingWaypoint pending = getPending(stack);
        ResourceLocation dimension = serverLevel.dimension().location();
        if (pending == null || !pending.dimension().equals(dimension) || !serverLevel.getBlockState(pending.pos()).is(ReliableRoutes.PATH_PAIR.get()))
        {
            setPending(stack, pos, dimension);
            player.displayClientMessage(Component.translatable("message.reliableroutes.path_pair.selected", pos.getX(), pos.getY(), pos.getZ()), true);
            return ItemInteractionResult.CONSUME;
        }

        if (pending.pos().equals(pos))
        {
            clearPending(stack);
            player.displayClientMessage(Component.translatable("message.reliableroutes.path_pair.selection_cleared"), true);
            return ItemInteractionResult.CONSUME;
        }

        if (pending.pos().distSqr(pos) > MAX_PAIR_DISTANCE_SQUARED)
        {
            player.displayClientMessage(Component.translatable("message.reliableroutes.path_pair.too_far"), true);
            return ItemInteractionResult.CONSUME;
        }

        pairs.pair(pending.pos(), pos);
        clearPending(stack);
        player.displayClientMessage(Component.translatable("message.reliableroutes.path_pair.paired"), true);
        return ItemInteractionResult.CONSUME;
    }

    @Nullable
    public static PendingWaypoint getPending(ItemStack stack)
    {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_PENDING_POS) || !tag.contains(TAG_PENDING_DIMENSION)) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(TAG_PENDING_DIMENSION));
        return dimension == null ? null : new PendingWaypoint(BlockPos.of(tag.getLong(TAG_PENDING_POS)), dimension);
    }

    private static void setPending(ItemStack stack, BlockPos pos, ResourceLocation dimension)
    {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(TAG_PENDING_POS, pos.asLong());
        tag.putString(TAG_PENDING_DIMENSION, dimension.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearPending(ItemStack stack)
    {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(TAG_PENDING_POS);
        tag.remove(TAG_PENDING_DIMENSION);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public record PendingWaypoint(BlockPos pos, ResourceLocation dimension) {}
}
