package com.deathfrog.reliableroutes.navigation;

import com.deathfrog.reliableroutes.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import javax.annotation.Nonnull;
import java.util.*;

/** Persistent, dimension-local routing zones and their directional health. */
public final class WaypointPairSavedData extends SavedData
{
    private static final String NAME = Constants.MOD_ID + "_routing_zones";
    private final List<RoutingZone> zones = new ArrayList<>();
    private final Map<DirectionKey, WaypointPairDirectionHealth> health = new HashMap<>();

    public static WaypointPairSavedData get(@Nonnull ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(new Factory<>(WaypointPairSavedData::new, WaypointPairSavedData::load), NAME);
    }

    private static WaypointPairSavedData load(CompoundTag root, HolderLookup.Provider registries)
    {
        WaypointPairSavedData data = new WaypointPairSavedData();
        for (Tag raw : root.getList("Zones", Tag.TAG_COMPOUND))
        {
            CompoundTag tag = (CompoundTag) raw;
            RoutingZone zone = new RoutingZone(tag.getInt("MinX"), tag.getInt("MinZ"), tag.getInt("MaxX"), tag.getInt("MaxZ"),
                BlockPos.of(tag.getLong("First")), BlockPos.of(tag.getLong("Second")));
            data.zones.add(zone);
            data.loadHealth(tag, "FirstToSecond", zone.firstEndpoint(), zone.secondEndpoint());
            data.loadHealth(tag, "SecondToFirst", zone.secondEndpoint(), zone.firstEndpoint());
        }
        return data;
    }

    public boolean addZone(@Nonnull RoutingZone zone)
    {
        if (!zone.contains(zone.firstEndpoint()) || !zone.contains(zone.secondEndpoint()) || overlaps(zone)) return false;
        removeZoneAt(zone.firstEndpoint());
        removeZoneAt(zone.secondEndpoint());
        zones.add(zone);
        setDirty();
        return true;
    }

    public boolean removeZoneAt(@Nonnull BlockPos pos)
    {
        boolean removed = zones.removeIf(zone -> {
            if (!zone.firstEndpoint().equals(pos) && !zone.secondEndpoint().equals(pos)) return false;
            health.remove(new DirectionKey(zone.firstEndpoint().asLong(), zone.secondEndpoint().asLong()));
            health.remove(new DirectionKey(zone.secondEndpoint().asLong(), zone.firstEndpoint().asLong()));
            return true;
        });
        if (removed) setDirty();
        return removed;
    }

    public Optional<RoutingZone> zoneAtEndpoint(@Nonnull BlockPos pos)
    {
        return zones.stream().filter(zone -> zone.firstEndpoint().equals(pos) || zone.secondEndpoint().equals(pos)).findFirst();
    }

    public Collection<RoutingZone> allZones() { return List.copyOf(zones); }

    public Collection<RoutingZone> findZonesNear(@Nonnull BlockPos center, int radius)
    {
        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;
        return zones.stream().filter(zone -> zone.maxX() >= minX && zone.minX() <= maxX
            && zone.maxZ() >= minZ && zone.minZ() <= maxZ).toList();
    }

    /**
     * Returns zones that can overlap a path search. The start-to-destination rectangle is expanded
     * by the job's search range so detours considered by the pathfinder remain covered.
     */
    public Collection<RoutingZone> findZonesForPath(@Nonnull BlockPos start, @Nonnull BlockPos destination, int range)
    {
        int margin = Math.max(0, range);
        int minX = subtractClamped(Math.min(start.getX(), destination.getX()), margin);
        int maxX = addClamped(Math.max(start.getX(), destination.getX()), margin);
        int minZ = subtractClamped(Math.min(start.getZ(), destination.getZ()), margin);
        int maxZ = addClamped(Math.max(start.getZ(), destination.getZ()), margin);
        return zones.stream().filter(zone -> zone.maxX() >= minX && zone.minX() <= maxX
            && zone.maxZ() >= minZ && zone.minZ() <= maxZ).toList();
    }

    private static int subtractClamped(int value, int amount)
    {
        return (int) Math.max(Integer.MIN_VALUE, (long) value - amount);
    }

    private static int addClamped(int value, int amount)
    {
        return (int) Math.min(Integer.MAX_VALUE, (long) value + amount);
    }

    public Optional<WaypointRoutePlan> selectRoute(@Nonnull BlockPos start, @Nonnull BlockPos destination)
    {
        if (start.equals(destination)) return Optional.empty();
        return zones.stream()
            .filter(zone -> !zone.contains(destination))
            .filter(zone -> RoutingZoneMath.segmentIntersectsBox(
                start.getX(), start.getY(), start.getZ(), destination.getX(), destination.getY(), destination.getZ(),
                zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ()))
            .map(zone -> routeFor(zone, start, destination)).flatMap(Optional::stream)
            .min(Comparator.comparingDouble(WaypointRoutePlan::estimatedDistance));
    }

    private Optional<WaypointRoutePlan> routeFor(RoutingZone zone, BlockPos start, BlockPos destination)
    {
        BlockPos first = zone.firstEndpoint(), second = zone.secondEndpoint();
        double axisX = second.getX() - first.getX(), axisZ = second.getZ() - first.getZ();
        if (axisX * axisX + axisZ * axisZ == 0) return Optional.empty();
        double midpointX = (first.getX() + second.getX()) / 2.0, midpointZ = (first.getZ() + second.getZ()) / 2.0;
        double startSide = (start.getX() - midpointX) * axisX + (start.getZ() - midpointZ) * axisZ;
        double destinationSide = (destination.getX() - midpointX) * axisX + (destination.getZ() - midpointZ) * axisZ;
        if (zone.contains(start))
        {
            BlockPos escape = destinationSide >= 0 ? second : first;
            return Optional.of(new WaypointRoutePlan(escape, escape, destination, 0, true));
        }
        if (startSide == 0 || destinationSide == 0 || Math.signum(startSide) == Math.signum(destinationSide)) return Optional.empty();
        BlockPos entrance = startSide < 0 ? first : second, exit = startSide < 0 ? second : first;
        double order = RoutingZoneMath.intersectionOrder(
            start.getX(), start.getY(), start.getZ(), destination.getX(), destination.getY(), destination.getZ(),
            zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ());
        return Optional.of(new WaypointRoutePlan(entrance, exit, destination, order, false));
    }

    public WaypointPairDirectionHealth health(BlockPos from, BlockPos to)
    {
        return health.getOrDefault(new DirectionKey(from.asLong(), to.asLong()), WaypointPairDirectionHealth.UNKNOWN);
    }

    public RoutingZoneSnapshot snapshot(RoutingZone zone)
    {
        return new RoutingZoneSnapshot(zone, health(zone.firstEndpoint(), zone.secondEndpoint()), health(zone.secondEndpoint(), zone.firstEndpoint()));
    }

    public void recordValidation(BlockPos from, BlockPos to, WaypointPairDirectionStatus status, WaypointPairFailureReason reason, long validatedAt)
    {
        if (zones.stream().noneMatch(zone -> zone.firstEndpoint().equals(from) && zone.secondEndpoint().equals(to)
            || zone.secondEndpoint().equals(from) && zone.firstEndpoint().equals(to))) return;
        health.put(new DirectionKey(from.asLong(), to.asLong()), new WaypointPairDirectionHealth(status, reason, validatedAt));
        setDirty();
    }

    private boolean overlaps(RoutingZone candidate)
    {
        return zones.stream().anyMatch(zone -> candidate.minX() <= zone.maxX() && candidate.maxX() >= zone.minX()
            && candidate.minZ() <= zone.maxZ() && candidate.maxZ() >= zone.minZ());
    }

    @Override public CompoundTag save(@Nonnull CompoundTag root, @Nonnull HolderLookup.Provider registries)
    {
        ListTag list = new ListTag();
        for (RoutingZone zone : zones)
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt("MinX", zone.minX()); tag.putInt("MinZ", zone.minZ());
            tag.putInt("MaxX", zone.maxX()); tag.putInt("MaxZ", zone.maxZ());
            tag.putLong("First", zone.firstEndpoint().asLong()); tag.putLong("Second", zone.secondEndpoint().asLong());
            saveHealth(tag, "FirstToSecond", zone.firstEndpoint(), zone.secondEndpoint());
            saveHealth(tag, "SecondToFirst", zone.secondEndpoint(), zone.firstEndpoint());
            list.add(tag);
        }
        root.put("Zones", list);
        return root;
    }

    private void saveHealth(CompoundTag parent, String name, BlockPos from, BlockPos to)
    {
        WaypointPairDirectionHealth value = health(from, to);
        CompoundTag tag = new CompoundTag();
        tag.putString("Status", value.status().name()); tag.putString("Reason", value.reason().name()); tag.putLong("ValidatedAt", value.validatedAt());
        parent.put(name, tag);
    }

    private void loadHealth(CompoundTag parent, String name, BlockPos from, BlockPos to)
    {
        if (!parent.contains(name, Tag.TAG_COMPOUND)) return;
        CompoundTag tag = parent.getCompound(name);
        try { health.put(new DirectionKey(from.asLong(), to.asLong()), new WaypointPairDirectionHealth(
            WaypointPairDirectionStatus.valueOf(tag.getString("Status")), WaypointPairFailureReason.valueOf(tag.getString("Reason")), tag.getLong("ValidatedAt"))); }
        catch (IllegalArgumentException ignored) {}
    }

    private record DirectionKey(long from, long to) {}
}
