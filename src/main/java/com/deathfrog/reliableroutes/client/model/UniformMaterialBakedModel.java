package com.deathfrog.reliableroutes.client.model;

import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.client.model.properties.ModProperties;
import com.ldtteam.domumornamentum.client.model.utils.ModelSpriteQuadTransformer;
import com.ldtteam.domumornamentum.client.model.utils.ModelSpriteQuadTransformerData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Optional;

/** Uses the selected block's upward face as the replacement texture on every face. */
public class UniformMaterialBakedModel extends BakedModelWrapper<BakedModel>
{
    public UniformMaterialBakedModel(BakedModel delegate)
    {
        super(delegate);
    }

    @SuppressWarnings("null")
    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state,
        @Nullable Direction side,
        @Nonnull RandomSource random,
        @Nonnull ModelData data,
        @Nullable RenderType renderType)
    {
        MaterialTextureData textures = data.get(ModProperties.MATERIAL_TEXTURE_PROPERTY);
        if (textures == null || textures.getTexturedComponents().isEmpty())
        {
            return originalModel.getQuads(state, side, random, data, renderType);
        }
        Block material = textures.getTexturedComponents().values().iterator().next();
        List<BakedQuad> quads = originalModel.getQuads(state, side, random, data, renderType);
        return remapToTop(quads, material, random, renderType);
    }

    @SuppressWarnings("null")
    private static List<BakedQuad> remapToTop(List<BakedQuad> quads,
        Block material,
        @Nonnull RandomSource random,
        @Nonnull RenderType renderType)
    {
        if (quads.isEmpty()) return quads;
        BlockState materialState = material.defaultBlockState();

        if (materialState == null) return quads;

        BakedModel source = net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModel(materialState);
        Optional<BakedQuad> topQuad = findTopQuad(source, materialState, random, renderType);
        if (topQuad.isEmpty()) return quads;

        IQuadTransformer transformer =
            ModelSpriteQuadTransformer.create(new ModelSpriteQuadTransformerData(topQuad.get(), materialState));
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) result.add(transformer.process(quad));
        return result;
    }

    private static Optional<BakedQuad> findTopQuad(BakedModel source,
        BlockState materialState,
        RandomSource random,
        @Nullable RenderType renderType)
    {
        List<BakedQuad> candidates = source.getQuads(null, Direction.UP, random, ModelData.EMPTY, renderType);
        if (candidates.isEmpty()) candidates = source.getQuads(materialState, Direction.UP, random, ModelData.EMPTY, renderType);
        if (candidates.isEmpty()) candidates = source.getQuads(null, Direction.UP, random, ModelData.EMPTY, null);
        if (candidates.isEmpty()) candidates = source.getQuads(materialState, Direction.UP, random, ModelData.EMPTY, null);
        if (!candidates.isEmpty()) return Optional.of(candidates.getFirst());

        candidates = source.getQuads(null, null, random, ModelData.EMPTY, renderType);
        if (candidates.isEmpty()) candidates = source.getQuads(materialState, null, random, ModelData.EMPTY, null);
        return candidates.stream().filter(quad -> quad.getDirection() == Direction.UP).findFirst();
    }

    @Override
    public List<BakedModel> getRenderPasses(@Nonnull ItemStack stack, boolean fabulous)
    {
        MaterialTextureData textures = MaterialTextureData.readFromItemStack(stack);
        if (textures.isEmpty()) return originalModel.getRenderPasses(stack, fabulous);
        Block material = textures.getTexturedComponents().values().iterator().next();
        return originalModel.getRenderPasses(stack, fabulous)
            .stream()
            .<BakedModel>map(model -> new FixedUniformModel(model, material))
            .toList();
    }

    private static final class FixedUniformModel extends BakedModelWrapper<BakedModel>
    {
        private final Block material;

        private FixedUniformModel(BakedModel inner, Block material)
        {
            super(inner);
            this.material = material;
        }

        @Override
        public List<BakedQuad> getQuads(@Nonnull BlockState state,
            @Nonnull Direction side,
            @Nonnull RandomSource random,
            @Nonnull ModelData data,
            @Nonnull RenderType type)
        {
            return remapToTop(originalModel.getQuads(state, side, random, data, type), material, random, type);
        }

        @Override
        public List<BakedModel> getRenderPasses(@Nonnull ItemStack stack, boolean fabulous)
        {
            return originalModel.getRenderPasses(stack, fabulous)
                .stream()
                .<BakedModel>map(model -> new FixedUniformModel(model, material))
                .toList();
        }
    }
}
