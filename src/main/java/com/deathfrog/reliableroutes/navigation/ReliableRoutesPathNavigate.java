package com.deathfrog.reliableroutes.navigation;

import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.ReliableRoutesConfig;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import com.minecolonies.core.entity.pathfinding.PathFindingStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** MineColonies navigator extension that delegates ordinary trips unchanged. */
public class ReliableRoutesPathNavigate extends MinecoloniesAdvancedPathNavigate
{
    private static final double MAXIMUM_PAIR_DETOUR_FACTOR = 1.5;
    private static final double MAXIMUM_PAIR_ADDITIONAL_DISTANCE = 8;
    private static final long BROKEN_PAIR_RETRY_TICKS = 20L * 60L * 5L;

    private ActiveRoute activeRoute;
    private PathResult<PathJobMoveToLocation> activeStageResult;

    public ReliableRoutesPathNavigate(@NotNull Mob entity, Level level)
    {
        super(entity, level);
    }

    @Override
    @Nullable
    protected PathResult<PathJobMoveToLocation> walkTo(
        BlockPos desiredPos,
        double speedFactor,
        boolean safeDestination)
    {
        if (activeRoute != null)
        {
            if (activeRoute.plan().destination().equals(desiredPos)) return activeStageResult;
            clearActiveRoute();
        }

        if (!ReliableRoutesConfig.isCustomPathfindingEnabled())
        {
            return super.walkTo(desiredPos, speedFactor, safeDestination);
        }

        BlockPos start = PathfindingUtils.prepareStart(ourEntity);
        WaypointRouteSelector routeSelector = new WaypointRouteSelector(
            ReliableRoutesConfig.waypointInfluenceRadius(),
            ReliableRoutesConfig.maximumWaypointDetourFactor(),
            ReliableRoutesConfig.maximumWaypointAdditionalDistance());
        WaypointPairProvider provider = WaypointPairProviders.get();
        var pairs = provider.findRelevantPairs(level, start, desiredPos, routeSelector.searchRadius());
        var routePlan = selectUsableRoute(routeSelector, start, desiredPos, pairs);

        if (routePlan.isEmpty())
        {
            return super.walkTo(desiredPos, speedFactor, safeDestination);
        }

        return walkUsingWaypointRoute(routePlan.get(), speedFactor, safeDestination);
    }

    /** Starts a physical three-leg route through the selected waypoint pair. */
    private PathResult<PathJobMoveToLocation> walkUsingWaypointRoute(
        WaypointRoutePlan routePlan,
        double speedFactor,
        boolean safeDestination)
    {
        if (!endpointsStillExist(routePlan))
        {
            recordPairHealth(routePlan, WaypointPairDirectionStatus.BROKEN, WaypointPairFailureReason.MISSING_ENDPOINT);
            return super.walkTo(routePlan.destination(), speedFactor, safeDestination);
        }
        if (safeDestination) setSafeDestinationPos(routePlan.destination());
        activeRoute = new ActiveRoute(routePlan, speedFactor, safeDestination, RouteStage.TO_ENTRANCE);
        activeStageResult = submitDirectStage(routePlan.entrance(), speedFactor, false);
        if (activeStageResult == null) clearActiveRoute();
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

        if (activeRoute != null && activeStageResult != null && activeStageResult.isDone()
            && activeStageResult.getStatus() == PathFindingStatus.CALCULATION_COMPLETE)
        {
            if (!activeStageResult.isPathReachingDestination())
            {
                if (activeRoute.stage() == RouteStage.TO_EXIT)
                {
                    recordPairHealth(WaypointPairDirectionStatus.BROKEN, WaypointPairFailureReason.NO_PATH);
                }
                fallBackToDestination();
                return;
            }

            if (activeRoute.stage() == RouteStage.TO_EXIT && !pairPathIsReasonablyDirect(activeStageResult))
            {
                recordPairHealth(WaypointPairDirectionStatus.BROKEN, WaypointPairFailureReason.EXCESSIVE_DETOUR);
                fallBackToDestination();
                return;
            }

            if (activeRoute.stage() == RouteStage.TO_EXIT)
            {
                recordPairHealth(WaypointPairDirectionStatus.VALID, WaypointPairFailureReason.NONE);
            }
        }

        super.tick();

        if (activeRoute != null && activeStageResult != null
            && activeStageResult.getStatus() == PathFindingStatus.CANCELLED)
        {
            fallBackToDestination();
            return;
        }

        if (activeRoute != null && activeStageResult != null
            && activeStageResult.getStatus() == PathFindingStatus.COMPLETE)
        {
            advanceRoute();
        }
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
        if (route.stage() != RouteStage.TO_DESTINATION && !endpointsStillExist(route.plan()))
        {
            recordPairHealth(route.plan(), WaypointPairDirectionStatus.BROKEN, WaypointPairFailureReason.MISSING_ENDPOINT);
            fallBackToDestination();
            return;
        }
        switch (route.stage())
        {
            case TO_ENTRANCE -> startStage(route.withStage(RouteStage.TO_EXIT), route.plan().exit(), false);
            case TO_EXIT -> startStage(route.withStage(RouteStage.TO_DESTINATION), route.plan().destination(), route.safeDestination());
            case TO_DESTINATION -> clearActiveRoute();
        }
    }

    private void startStage(ActiveRoute route, BlockPos destination, boolean safeDestination)
    {
        activeRoute = route;
        activeStageResult = submitDirectStage(destination, route.speedFactor(), safeDestination);
        if (activeStageResult == null) fallBackToDestination();
    }

    private PathResult<PathJobMoveToLocation> submitDirectStage(BlockPos destination, double speedFactor, boolean safeDestination)
    {
        BlockPos start = PathfindingUtils.prepareStart(ourEntity);
        return setPathJob(new PathJobMoveToLocation(
            ourEntity.level(),
            start,
            destination,
            (int) ourEntity.getAttribute(Attributes.FOLLOW_RANGE).getValue(),
            ourEntity), destination, speedFactor, safeDestination);
    }

    private boolean pairPathIsReasonablyDirect(PathResult<PathJobMoveToLocation> result)
    {
        double directDistance = horizontalDistance(activeRoute.plan().entrance(), activeRoute.plan().exit());
        return result.getPathLength() <= directDistance * MAXIMUM_PAIR_DETOUR_FACTOR + MAXIMUM_PAIR_ADDITIONAL_DISTANCE;
    }

    private Optional<WaypointRoutePlan> selectUsableRoute(
        WaypointRouteSelector routeSelector,
        BlockPos start,
        BlockPos destination,
        Collection<WaypointPair> candidates)
    {
        Collection<WaypointPair> remaining = new ArrayList<>(candidates);
        while (!remaining.isEmpty())
        {
            var selected = routeSelector.select(start, destination, remaining);
            if (selected.isEmpty()) return selected;
            WaypointRoutePlan plan = selected.get();
            if (!directionIsInRetryCooldown(plan)) return selected;
            remaining.removeIf(pair -> pairContainsDirection(pair, plan.entrance(), plan.exit()));
        }
        return Optional.empty();
    }

    private boolean directionIsInRetryCooldown(WaypointRoutePlan plan)
    {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        WaypointPairDirectionHealth value = WaypointPairSavedData.get(serverLevel).health(plan.entrance(), plan.exit());
        return value.status() == WaypointPairDirectionStatus.BROKEN
            && level.getGameTime() - value.validatedAt() < BROKEN_PAIR_RETRY_TICKS;
    }

    private static boolean pairContainsDirection(WaypointPair pair, BlockPos entrance, BlockPos exit)
    {
        return pair.first().equals(entrance) && pair.second().equals(exit)
            || pair.second().equals(entrance) && pair.first().equals(exit);
    }

    private void recordPairHealth(WaypointPairDirectionStatus status, WaypointPairFailureReason reason)
    {
        recordPairHealth(activeRoute.plan(), status, reason);
    }

    private void recordPairHealth(
        WaypointRoutePlan plan,
        WaypointPairDirectionStatus status,
        WaypointPairFailureReason reason)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            WaypointPairSavedData.get(serverLevel).recordValidation(
                plan.entrance(), plan.exit(), status, reason, level.getGameTime());
        }
    }

    private boolean endpointsStillExist(WaypointRoutePlan plan)
    {
        return level.getBlockState(plan.entrance()).is(ReliableRoutes.PATH_PAIR.get())
            && level.getBlockState(plan.exit()).is(ReliableRoutes.PATH_PAIR.get());
    }

    private void fallBackToDestination()
    {
        ActiveRoute route = activeRoute;
        clearActiveRoute();
        super.walkTo(route.plan().destination(), route.speedFactor(), route.safeDestination());
    }

    private void clearActiveRoute()
    {
        activeRoute = null;
        activeStageResult = null;
    }

    private static double horizontalDistance(BlockPos first, BlockPos second)
    {
        return Math.hypot(first.getX() - second.getX(), first.getZ() - second.getZ());
    }

    private enum RouteStage
    {
        TO_ENTRANCE,
        TO_EXIT,
        TO_DESTINATION
    }

    private record ActiveRoute(WaypointRoutePlan plan, double speedFactor, boolean safeDestination, RouteStage stage)
    {
        private ActiveRoute withStage(RouteStage newStage)
        {
            return new ActiveRoute(plan, speedFactor, safeDestination, newStage);
        }
    }
}
