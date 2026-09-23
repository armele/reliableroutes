package com.deathfrog.reliableroutes.block;

import com.deathfrog.reliableroutes.Constants;
import com.google.common.collect.ImmutableList;
import com.ldtteam.domumornamentum.block.AbstractBlock;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RoutingBlock extends AbstractBlock<RoutingBlock> implements IMateriallyTexturedBlock, ICachedItemGroupBlock, EntityBlock
{
    public static final ResourceLocation MATERIAL_TEXTURE = ResourceLocation.withDefaultNamespace(Constants.DEFAULT_MATERIAL_TEXTURE);

    @SuppressWarnings("null")
    private static final TagKey<Block> MATERIALS = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, Constants.ROUTING_BLOCK_MATERIALS_TAG_ID));
        
    @SuppressWarnings("null")
    private static final @Nonnull List<IMateriallyTexturedBlockComponent> COMPONENTS =
        ImmutableList.of(new SimpleRetexturableComponent(MATERIAL_TEXTURE, MATERIALS, Blocks.STONE_BRICKS));
    private final List<ItemStack> itemGroupCache = new ArrayList<>();

    @SuppressWarnings("null")
    public RoutingBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F));
    }

    @Override
    public @Nonnull List<IMateriallyTexturedBlockComponent> getComponents()
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
