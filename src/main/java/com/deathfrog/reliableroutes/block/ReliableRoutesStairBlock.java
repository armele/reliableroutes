package com.deathfrog.reliableroutes.block;

import com.deathfrog.reliableroutes.Constants;
import com.google.common.collect.ImmutableList;
import com.ldtteam.domumornamentum.block.AbstractBlockStairs;
import com.ldtteam.domumornamentum.block.ICachedItemGroupBlock;
import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlockComponent;
import com.ldtteam.domumornamentum.block.components.SimpleRetexturableComponent;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.entity.block.MateriallyTexturedBlockEntity;
import com.ldtteam.domumornamentum.recipe.architectscutter.ArchitectsCutterRecipeBuilder;
import com.ldtteam.domumornamentum.util.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** A materially textured stair whose outer tread aligns with the 15-pixel road surface. */
public class ReliableRoutesStairBlock extends AbstractBlockStairs<ReliableRoutesStairBlock>
    implements IMateriallyTexturedBlock, ICachedItemGroupBlock, EntityBlock
{
    @SuppressWarnings("null")
    private static final TagKey<Block> MATERIALS = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, Constants.ROUTING_BLOCK_MATERIALS_TAG_ID));

    private static final List<IMateriallyTexturedBlockComponent> COMPONENTS =
        ImmutableList.of(new SimpleRetexturableComponent(RoutingBlock.MATERIAL_TEXTURE, MATERIALS, Blocks.STONE_BRICKS));

    private static final VoxelShape BOTTOM_BASE = Block.box(0, 0, 0, 16, ReliableRoutesStairGeometry.MIDPOINT, 16);
    private static final VoxelShape TOP_BASE = Block.box(0, ReliableRoutesStairGeometry.MIDPOINT, 0, 16, 16, 16);
    private static final VoxelShape[] BOTTOM_SHAPES =
        makeShapes(ReliableRoutesStairGeometry.MIDPOINT, ReliableRoutesStairGeometry.BOTTOM_RAISED_TOP, BOTTOM_BASE);
    private static final VoxelShape[] TOP_SHAPES =
        makeShapes(ReliableRoutesStairGeometry.TOP_LOWER_BOTTOM, ReliableRoutesStairGeometry.MIDPOINT, TOP_BASE);
    private static final int[] SHAPE_BY_STATE = {12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    private final List<ItemStack> itemGroupCache = new ArrayList<>();

    @SuppressWarnings("null")
    public ReliableRoutesStairBlock()
    {
        super(Blocks.STONE_BRICKS::defaultBlockState, Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F));
    }

    @SuppressWarnings("null")
    private static VoxelShape[] makeShapes(double minY, double maxY, VoxelShape base)
    {
        VoxelShape nnn = Block.box(0, minY, 0, 8, maxY, 8);
        VoxelShape nnp = Block.box(0, minY, 8, 8, maxY, 16);
        VoxelShape pnn = Block.box(8, minY, 0, 16, maxY, 8);
        VoxelShape pnp = Block.box(8, minY, 8, 16, maxY, 16);
        return IntStream.range(0, 16).mapToObj(index -> {
            VoxelShape shape = base;
            if ((index & 1) != 0) shape = Shapes.or(shape, nnn);
            if ((index & 2) != 0) shape = Shapes.or(shape, pnn);
            if ((index & 4) != 0) shape = Shapes.or(shape, nnp);
            if ((index & 8) != 0) shape = Shapes.or(shape, pnp);
            return shape;
        }).toArray(VoxelShape[]::new);
    }

    @Override
    public boolean isStairs(BlockState state)
    {
        return state.is(this);
    }

    @SuppressWarnings("null")
    @Override
    public @Nonnull VoxelShape getShape(BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context)
    {
        VoxelShape[] shapes = state.getValue(HALF) == Half.TOP ? TOP_SHAPES : BOTTOM_SHAPES;
        int index = state.getValue(SHAPE).ordinal() * 4 + state.getValue(FACING).get2DDataValue();
        return shapes[SHAPE_BY_STATE[index]];
    }

    @Override
    public List<IMateriallyTexturedBlockComponent> getComponents()
    {
        return COMPONENTS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state)
    {
        return new MateriallyTexturedBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(@Nonnull Level level,
        @Nonnull BlockPos pos,
        @Nonnull BlockState state,
        @Nullable LivingEntity placer,
        @Nonnull ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MateriallyTexturedBlockEntity textured)
        {
            textured.updateTextureDataWith(MaterialTextureData.readFromItemStack(stack));
        }
    }

    @Override
    public ItemStack getCloneItemStack(@Nonnull BlockState state,
        @Nonnull HitResult target,
        @Nonnull LevelReader level,
        @Nonnull BlockPos pos,
        @Nonnull Player player)
    {
        return BlockUtils.getMaterializedItemStack(level.getBlockEntity(pos), level.registryAccess());
    }

    @Override
    public float getExplosionResistance(@Nonnull BlockState state,
        @Nonnull BlockGetter level,
        @Nonnull BlockPos pos,
        @Nonnull Explosion explosion)
    {
        return getDOExplosionResistance(super::getExplosionResistance, state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(@Nonnull BlockState state,
        @Nonnull Player player,
        @Nonnull BlockGetter level,
        @Nonnull BlockPos pos)
    {
        return getDODestroyProgress(super::getDestroyProgress, state, player, level, pos);
    }

    @Override
    public SoundType getSoundType(@Nonnull BlockState state, @Nonnull LevelReader level, @Nonnull BlockPos pos, @Nonnull Entity entity)
    {
        return getDOSoundType(super::getSoundType, state, level, pos, entity);
    }

    @Override
    public IMateriallyTexturedBlockComponent getMainComponent()
    {
        return COMPONENTS.getFirst();
    }

    @Override
    public void fillItemCategory(NonNullList<ItemStack> items)
    {
        fillDOItemCategory(this, items, itemGroupCache);
    }

    @Override
    public void resetCache()
    {
        itemGroupCache.clear();
    }

    @Override
    public void buildRecipes(RecipeOutput output)
    {
        new ArchitectsCutterRecipeBuilder(this, RecipeCategory.BUILDING_BLOCKS).count(1).save(output);
    }
}
