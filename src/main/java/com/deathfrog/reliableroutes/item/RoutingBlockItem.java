package com.deathfrog.reliableroutes.item;

import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.Constants;
import com.ldtteam.domumornamentum.item.interfaces.IDoItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class RoutingBlockItem extends BlockItem implements IDoItem
{
    public RoutingBlockItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public ResourceLocation getGroup()
    {
        return ResourceLocation.fromNamespaceAndPath(ReliableRoutes.MODID, Constants.ROUTING_BLOCKS_GROUP_ID);
    }
}
