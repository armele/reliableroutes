package com.deathfrog.reliableroutes;

import com.deathfrog.reliableroutes.block.RoutingBlock;
import com.deathfrog.reliableroutes.block.ForbiddenGroundCurbBlock;
import com.deathfrog.reliableroutes.block.PathPairBlock;
import com.deathfrog.reliableroutes.block.RoadRoutingBlock;
import com.deathfrog.reliableroutes.item.PathfinderLensItem;
import com.deathfrog.reliableroutes.item.RoutingBlockItem;
import com.deathfrog.reliableroutes.navigation.ReliableRoutesPathNavigate;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.pathfinding.registry.IPathNavigateRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import com.deathfrog.reliableroutes.network.ReliableRoutesNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import javax.annotation.Nonnull;
import org.slf4j.Logger;

@Mod(ReliableRoutes.MODID)
public class ReliableRoutes
{
    public static final String MODID = Constants.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    @SuppressWarnings("null")
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<RoadRoutingBlock> RELIABLEROUTES_ROAD = roadRoutingBlock(Constants.RELIABLEROUTES_ROAD_ID);
    public static final DeferredBlock<RoutingBlock> FORBIDDEN_GROUND = routingBlock(Constants.FORBIDDEN_GROUND_ID);
    public static final DeferredBlock<ForbiddenGroundCurbBlock> FORBIDDEN_GROUND_CURB =
        BLOCKS.register(Constants.FORBIDDEN_GROUND_CURB_ID, ForbiddenGroundCurbBlock::new);
    public static final DeferredBlock<PathPairBlock> PATH_PAIR = BLOCKS.register(Constants.PATH_PAIR_ID, PathPairBlock::new);

    public static final DeferredItem<RoutingBlockItem> RELIABLEROUTES_ROAD_ITEM =
        routingItem(Constants.RELIABLEROUTES_ROAD_ID, RELIABLEROUTES_ROAD);
    public static final DeferredItem<RoutingBlockItem> FORBIDDEN_GROUND_ITEM =
        routingItem(Constants.FORBIDDEN_GROUND_ID, FORBIDDEN_GROUND);
    public static final DeferredItem<RoutingBlockItem> FORBIDDEN_GROUND_CURB_ITEM =
        routingItem(Constants.FORBIDDEN_GROUND_CURB_ID, FORBIDDEN_GROUND_CURB);
    public static final DeferredItem<RoutingBlockItem> PATH_PAIR_ITEM =
        ITEMS.register(Constants.PATH_PAIR_ID, () -> new RoutingBlockItem(PATH_PAIR.get(), new Item.Properties()));

    public static final DeferredItem<PathfinderLensItem> PATHFINDER_LENS =
        ITEMS.register(Constants.PATHFINDER_LENS_ID, () -> new PathfinderLensItem(new Item.Properties().stacksTo(1)));

    @SuppressWarnings("null")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(Constants.CREATIVE_TAB_ID,
        () -> CreativeModeTab.builder()
            .title(Component.translatable(Constants.CREATIVE_TAB_TRANSLATION_KEY))
            .icon(() -> PATHFINDER_LENS.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PATHFINDER_LENS.get());
                output.accept(RELIABLEROUTES_ROAD_ITEM.get());
                output.accept(FORBIDDEN_GROUND_ITEM.get());
                output.accept(FORBIDDEN_GROUND_CURB_ITEM.get());
                output.accept(PATH_PAIR_ITEM.get());
            })
            .build());

    public ReliableRoutes(@Nonnull IEventBus modEventBus, ModContainer modContainer)
    {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, ReliableRoutesConfig.SPEC, MODID + "-server.toml");
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ReliableRoutesNetwork::register);
    }

    private void commonSetup(@Nonnull FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            IPathNavigateRegistry.getInstance().registerNewPathNavigate(
                mob -> ReliableRoutesConfig.isCustomPathfindingEnabled() && mob instanceof AbstractEntityCitizen,
                mob -> new ReliableRoutesPathNavigate(mob, mob.level()));
            LOGGER.info("Registered config-controlled Reliable Routes navigator for MineColonies citizens");
        });
    }

    private static DeferredBlock<RoutingBlock> routingBlock(@Nonnull String id)
    {
        return BLOCKS.register(id, RoutingBlock::new);
    }

    private static DeferredBlock<RoadRoutingBlock> roadRoutingBlock(@Nonnull String id)
    {
        return BLOCKS.register(id, RoadRoutingBlock::new);
    }

    private static DeferredItem<RoutingBlockItem> routingItem(@Nonnull String id, DeferredBlock<? extends RoutingBlock> block)
    {
        return ITEMS.register(id, () -> new RoutingBlockItem(block.get(), new Item.Properties()));
    }
}
