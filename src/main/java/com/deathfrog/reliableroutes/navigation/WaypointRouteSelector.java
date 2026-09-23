package com.deathfrog.reliableroutes.navigation;

import net.minecraft.core.BlockPos;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import javax.annotation.Nonnull;

/** Identifies trips that should cross a paired waypoint instead of using an ordinary direct route. */
public final class WaypointRouteSelector
{
    private final double influenceRadius;
    private final double maximumDetourFactor;
    private final double maximumAdditionalDistance;

    public WaypointRouteSelector(double influenceRadius, double maximumDetourFactor, double maximumAdditionalDistance)
    {
        if (influenceRadius <= 0 || maximumDetourFactor < 1 || maximumAdditionalDistance < 0)
        {
            throw new IllegalArgumentException("Invalid waypoint route selector limits");
        }

        this.influenceRadius = influenceRadius;
        this.maximumDetourFactor = maximumDetourFactor;
        this.maximumAdditionalDistance = maximumAdditionalDistance;
    }

    public int searchRadius()
    {
        return (int) Math.ceil(influenceRadius);
    }

    @Nonnull
    public Optional<WaypointRoutePlan> select(
        @Nonnull BlockPos start,
        @Nonnull BlockPos destination,
        @Nonnull Collection<WaypointPair> pairs)
    {
        return pairs.stream()
            .flatMap(pair -> java.util.stream.Stream.of(
                evaluate(start, destination, pair.first(), pair.second()),
                evaluate(start, destination, pair.second(), pair.first())))
            .flatMap(Optional::stream)
            .min(Comparator.comparingDouble(WaypointRoutePlan::estimatedDistance));
    }

    private Optional<WaypointRoutePlan> evaluate(
        BlockPos start,
        BlockPos destination,
        BlockPos entrance,
        BlockPos exit)
    {
        double axisX = exit.getX() - entrance.getX();
        double axisZ = exit.getZ() - entrance.getZ();
        double axisLengthSquared = axisX * axisX + axisZ * axisZ;
        if (axisLengthSquared == 0)
        {
            return Optional.empty();
        }

        double startToEntrance = horizontalDistance(start, entrance);
        if (horizontalDistanceToSegment(entrance, start, destination) > influenceRadius)
        {
            return Optional.empty();
        }

        double midpointX = (entrance.getX() + exit.getX()) / 2.0;
        double midpointZ = (entrance.getZ() + exit.getZ()) / 2.0;
        double startSide = (start.getX() - midpointX) * axisX + (start.getZ() - midpointZ) * axisZ;
        double destinationSide = (destination.getX() - midpointX) * axisX + (destination.getZ() - midpointZ) * axisZ;
        if (startSide >= 0 || destinationSide <= 0)
        {
            return Optional.empty();
        }

        double directDistance = horizontalDistance(start, destination);
        double waypointDistance = startToEntrance
            + horizontalDistance(entrance, exit)
            + horizontalDistance(exit, destination);
        if (waypointDistance > directDistance * maximumDetourFactor + maximumAdditionalDistance)
        {
            return Optional.empty();
        }

        return Optional.of(new WaypointRoutePlan(entrance, exit, destination, waypointDistance));
    }

    private static double horizontalDistance(BlockPos first, BlockPos second)
    {
        return Math.hypot(first.getX() - second.getX(), first.getZ() - second.getZ());
    }

    static double horizontalDistanceToSegment(BlockPos point, BlockPos start, BlockPos end)
    {
        double segmentX = end.getX() - start.getX();
        double segmentZ = end.getZ() - start.getZ();
        double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
        if (lengthSquared == 0) return horizontalDistance(point, start);

        double projection = ((point.getX() - start.getX()) * segmentX
            + (point.getZ() - start.getZ()) * segmentZ) / lengthSquared;
        double clamped = Math.max(0, Math.min(1, projection));
        double nearestX = start.getX() + clamped * segmentX;
        double nearestZ = start.getZ() + clamped * segmentZ;
        return Math.hypot(point.getX() - nearestX, point.getZ() - nearestZ);
    }
}
