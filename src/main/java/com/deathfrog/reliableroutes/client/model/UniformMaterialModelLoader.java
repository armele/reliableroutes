package com.deathfrog.reliableroutes.client.model;

import javax.annotation.Nonnull;
import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.Constants;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

@EventBusSubscriber(modid = ReliableRoutes.MODID, value = Dist.CLIENT)
public final class UniformMaterialModelLoader implements IGeometryLoader<UniformMaterialGeometry>
{
    @SuppressWarnings("null")
    public static final @Nonnull ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ReliableRoutes.MODID, Constants.UNIFORM_MODEL_LOADER_ID);

    @SubscribeEvent
    public static void register(ModelEvent.RegisterGeometryLoaders event)
    {
        event.register(ID, new UniformMaterialModelLoader());
    }

    @SuppressWarnings("null")
    @Override
    public UniformMaterialGeometry read(@Nonnull JsonObject json, @Nonnull JsonDeserializationContext context)
        throws JsonParseException
    {
        return new UniformMaterialGeometry(ResourceLocation.parse(json.get(Constants.MODEL_PARENT_PROPERTY).getAsString()));
    }
}
