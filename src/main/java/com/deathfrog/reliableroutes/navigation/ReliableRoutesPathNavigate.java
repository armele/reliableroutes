package com.deathfrog.reliableroutes.navigation;

import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.ReliableRoutesConfig;
import com.minecolonies.core.entity.pathfinding.PathFindingStatus;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Optional;

/**
 * MineColonies navigator that routes qualifying trips through protected-zone crossings.
 */
public class ReliableRoutesPathNavigate extends MinecoloniesAdvancedPathNavigate
{
    private ActiveRoute activeRoute;
    private PathResult<PathJobMoveToLocation> activeStageResult;

    public ReliableRoutesPathNavigate(@NotNull Mob entity, Level level)
    {
        super(entity, level);
    }

    @Override
    @Nullable
    protected PathResult<PathJobMoveToLocation> walkTo(BlockPos desiredPos, double speedFactor, boolean safeDestination)
    {
        if (activeRoute != null)
        {
            if (activeRoute.plan().destination().equals(desiredPos)) return activeStageResult;
            clearActiveRoute();
        }
        if (!ReliableRoutesConfig.isCustomPathfindingEnabled() || !(level instanceof ServerLevel serverLevel))
        {
            return super.walkTo(desiredPos, speedFactor, safeDestination);
        }
        BlockPos start = PathfindingUtils.prepareStart(ourEntity);

        @SuppressWarnings("null")

        Optional<WaypointRoutePlan> selected = WaypointPairSavedData.get(serverLevel).selectRoute(start, desiredPos);
        if (selected.isEmpty()) return super.walkTo(desiredPos, speedFactor, safeDestination);

        return startRoute(selected.get(),
            speedFactor,
            safeDestination,
            () -> ReliableRoutesPathNavigate.super.walkTo(desiredPos, speedFactor, safeDestination));
    }

    @Override
    @Nullable
    protected PathResult<PathJobMoveCloseToXNearY> walkCloseToXNearY(BlockPos desiredPosition,
        BlockPos nearbyPosition,
        int distToDesired,
        double speedFactor,
        boolean safeDestination)
    {
        if (activeRoute != null)
        {
            if (activeRoute.plan().destination().equals(desiredPosition)) return castStageResult();
            clearActiveRoute();
        }
        if (!ReliableRoutesConfig.isCustomPathfindingEnabled() || !(level instanceof ServerLevel serverLevel))
        {
            return super.walkCloseToXNearY(desiredPosition, nearbyPosition, distToDesired, speedFactor, safeDestination);
        }

        BlockPos start = PathfindingUtils.prepareStart(ourEntity);

        @SuppressWarnings("null")
        Optional<WaypointRoutePlan> selected = WaypointPairSavedData.get(serverLevel).selectRoute(start, desiredPosition);
        
        if (selected.isEmpty())
        {
            return super.walkCloseToXNearY(desiredPosition, nearbyPosition, distToDesired, speedFactor, safeDestination);
        }
        startRoute(selected.get(),
            speedFactor,
            safeDestination,
            () -> ReliableRoutesPathNavigate.super.walkCloseToXNearY(desiredPosition,
                nearbyPosition,
                distToDesired,
                speedFactor,
                safeDestination));
        return castStageResult();
    }

    private PathResult<PathJobMoveToLocation> startRoute(WaypointRoutePlan plan,
        double speedFactor,
        boolean safeDestination,
        RouteContinuation continuation)
    {
        if (!endpointsStillExist(plan))
        {
            if (!plan.escapingZone())
                recordHealth(plan, WaypointPairDirectionStatus.BROKEN, WaypointPairFailureReason.MISSING_ENDPOINT);
            continuation.resume();
            return null;
        }
        if (safeDestination) setSafeDestinationPos(plan.destination());
        activeRoute = new ActiveRoute(plan, speedFactor, safeDestination, RouteStage.TO_ENTRANCE, continuation);
        activeStageResult = submitStage(plan.entrance(), speedFactor, plan.escapingZone() ? plan.entrance() : null);
        if (activeStageResult == null) fallBackToDestination();
        return activeStageResult;
    }

    @Override
    public void tick()
    {
        if (activeRoute != null && !ReliableRoutesConfig.isCustomPathfindingEnabled())
        {
            fallBackToDestination();
            return;
        }
        if (activeRoute != null && activeStageResult != null &&
            activeStageResult.isDone() &&
            activeStageResult.getStatus() == PathFindingStatus.CALCULATION_COMPLETE &&
            !activeStageResult.isPathReachingDestination())
        {
            if (!activeRoute.plan().escapingZone())
                recordHealth(activeRoute.plan(), WaypointPairDirectionStatus.BROKEN, WaypointPairFailureReason.NO_PATH);
            fallBackToDestination();
            return;
        }
        super.tick();
        if (activeRoute == null || activeStageResult == null) return;
        if (activeRoute.stage() == RouteStage.LEAVING_ZONE && hasLeftActiveZone())
        {
            ActiveRoute route = activeRoute;
            clearActiveRoute();
            super.stop();
            continueRouteOrResume(route);
            return;
        }
        if (activeStageResult.getStatus() == PathFindingStatus.CANCELLED)
        {
            fallBackToDestination();
            return;
        }
        if (activeStageResult.getStatus() == PathFindingStatus.COMPLETE) advanceRoute();
    }

    @Override
    public void stop()
    {
        clearActiveRoute();
        super.stop();
    }

    private void advanceRoute()
    {
        ActiveRoute route = activeRoute;
        if (route.stage() == RouteStage.TO_ENTRANCE && !route.plan().escapingZone())
        {
            if (!endpointsStillExist(route.plan()))
            {
                recordHealth(route.plan(), WaypointPairDirectionStatus.BROKEN, WaypointPairFailureReason.MISSING_ENDPOINT);
                fallBackToDestination();
                return;
            }
            activeRoute = route.withStage(RouteStage.TO_EXIT);
            activeStageResult = submitStage(route.plan().exit(), route.speedFactor(), null);
            if (activeStageResult == null) fallBackToDestination();
            return;
        }
        if (!route.plan().escapingZone())
            recordHealth(route.plan(), WaypointPairDirectionStatus.VALID, WaypointPairFailureReason.NONE);
        activeRoute = route.withStage(RouteStage.LEAVING_ZONE);
        activeStageResult =
            submitStage(route.plan().destination(), route.speedFactor(), route.plan().escapingZone() ? route.plan().entrance() : null);
        if (activeStageResult == null) fallBackToDestination();
    }

    @SuppressWarnings("null")
    private PathResult<PathJobMoveToLocation> submitStage(BlockPos destination, double speedFactor, BlockPos escapeTarget)
    {
        BlockPos start = PathfindingUtils.prepareStart(ourEntity);

        int range = (int) ourEntity.getAttribute(Attributes.FOLLOW_RANGE).getValue();

        Collection<RoutingZone> zones =
            level instanceof ServerLevel serverLevel
                ? WaypointPairSavedData.get(serverLevel).findZonesForPath(start, destination, range)
                : java.util.List.of();
                
        return setPathJob(new PathJobMoveToLocationInZones(ourEntity.level(),
            start,
            destination,
            range,
            ourEntity,
            zones,
            escapeTarget), destination, speedFactor, false);
    }

    @SuppressWarnings("null")
    private boolean endpointsStillExist(WaypointRoutePlan plan)
    {
        return level.getBlockState(plan.entrance()).is(ReliableRoutes.PATH_PAIR.get()) &&
            level.getBlockState(plan.exit()).is(ReliableRoutes.PATH_PAIR.get());
    }

    private boolean hasLeftActiveZone()
    {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        return WaypointPairSavedData.get(serverLevel)
            .zoneAtEndpoint(activeRoute.plan().entrance())
            .map(zone -> !zone.contains(ourEntity.blockPosition()))
            .orElse(true);
    }

    private void recordHealth(WaypointRoutePlan plan, WaypointPairDirectionStatus status, WaypointPairFailureReason reason)
    {
        if (level instanceof ServerLevel serverLevel)
            WaypointPairSavedData.get(serverLevel).recordValidation(plan.entrance(), plan.exit(), status, reason, level.getGameTime());
    }

    private void fallBackToDestination()
    {
        if (activeRoute == null) return;
        ActiveRoute route = activeRoute;
        clearActiveRoute();
        route.continuation().resume();
    }

    @SuppressWarnings("null")
    private void continueRouteOrResume(ActiveRoute route)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            BlockPos start = PathfindingUtils.prepareStart(ourEntity);
            
            Optional<WaypointRoutePlan> selected = WaypointPairSavedData.get(serverLevel).selectRoute(start, route.plan().destination());
            if (selected.isPresent())
            {
                startRoute(selected.get(), route.speedFactor(), route.safeDestination(), route.continuation());
                return;
            }
        }
        route.continuation().resume();
    }

    @SuppressWarnings("unchecked")
    private PathResult<PathJobMoveCloseToXNearY> castStageResult()
    {
        // The generic type describes the job that originally initiated navigation. While a routing
        // zone is active, the navigator intentionally returns its temporary move-to stage instead.
        return (PathResult<PathJobMoveCloseToXNearY>) (PathResult<?>) activeStageResult;
    }

    private void clearActiveRoute()
    {
        activeRoute = null;
        activeStageResult = null;
    }

    private enum RouteStage
    {
        TO_ENTRANCE, TO_EXIT, LEAVING_ZONE
    }

    @FunctionalInterface
    private interface RouteContinuation
    {
        void resume();
    }

    private record ActiveRoute(WaypointRoutePlan plan,
        double speedFactor,
        boolean safeDestination,
        RouteStage stage,
        RouteContinuation continuation)
    {
        ActiveRoute withStage(RouteStage value)
        {
            return new ActiveRoute(plan, speedFactor, safeDestination, value, continuation);
        }
    }
}
