package com.deathfrog.reliableroutes.client.model;

import com.ldtteam.domumornamentum.client.model.geometry.MateriallyTexturedGeometry;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import java.util.function.Function;
import javax.annotation.Nonnull;

public record UniformMaterialGeometry(ResourceLocation parent) implements IUnbakedGeometry<UniformMaterialGeometry>
{
    @Override
    public BakedModel bake(@Nonnull IGeometryBakingContext context,
        @Nonnull ModelBaker baker,
        @Nonnull Function<Material, TextureAtlasSprite> spriteGetter,
        @Nonnull ModelState modelState,
        @Nonnull ItemOverrides overrides)
    {
        BakedModel doModel = new MateriallyTexturedGeometry(parent).bake(context, baker, spriteGetter, modelState, overrides);
        return new UniformMaterialBakedModel(doModel);
    }
}
