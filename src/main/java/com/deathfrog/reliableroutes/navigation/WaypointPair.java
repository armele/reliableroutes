package com.deathfrog.reliableroutes.navigation;

import net.minecraft.core.BlockPos;
import javax.annotation.Nonnull;

/** Two bidirectional endpoints connected by a preferred route. */
public record WaypointPair(@Nonnull BlockPos first, @Nonnull BlockPos second)
{
    public WaypointPair
    {
        first = first.immutable();
        second = second.immutable();
    }
}
