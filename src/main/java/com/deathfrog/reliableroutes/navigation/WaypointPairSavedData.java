package com.deathfrog.reliableroutes.navigation;

import com.deathfrog.reliableroutes.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;

/** Persistent, dimension-local index of bidirectional waypoint pairs. */
public final class WaypointPairSavedData extends SavedData
{
    private static final String NAME = Constants.MOD_ID + "_waypoint_pairs";
    private static final String TAG_PAIRS = "Pairs";
    private static final String TAG_FIRST = "First";
    private static final String TAG_SECOND = "Second";
    private static final String TAG_FIRST_TO_SECOND = "FirstToSecond";
    private static final String TAG_SECOND_TO_FIRST = "SecondToFirst";
    private static final String TAG_STATUS = "Status";
    private static final String TAG_REASON = "Reason";
    private static final String TAG_VALIDATED_AT = "ValidatedAt";
    private final Map<Long, Long> partners = new HashMap<>();
    private final Map<DirectionKey, WaypointPairDirectionHealth> health = new HashMap<>();

    public static WaypointPairSavedData get(@Nonnull ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(WaypointPairSavedData::new, WaypointPairSavedData::load),
            NAME);
    }

    private static WaypointPairSavedData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        WaypointPairSavedData data = new WaypointPairSavedData();
        ListTag pairs = tag.getList(TAG_PAIRS, Tag.TAG_COMPOUND);
        for (Tag entry : pairs)
        {
            CompoundTag pair = (CompoundTag) entry;
            long first = pair.getLong(TAG_FIRST);
            long second = pair.getLong(TAG_SECOND);
            data.putPair(first, second);
            data.loadHealth(pair, TAG_FIRST_TO_SECOND, first, second);
            data.loadHealth(pair, TAG_SECOND_TO_FIRST, second, first);
        }
        return data;
    }

    public void pair(@Nonnull BlockPos first, @Nonnull BlockPos second)
    {
        long firstKey = first.asLong();
        long secondKey = second.asLong();
        if (firstKey == secondKey) return;
        unpair(firstKey);
        unpair(secondKey);
        putPair(firstKey, secondKey);
        setDirty();
    }

    public boolean unpair(@Nonnull BlockPos pos)
    {
        boolean removed = unpair(pos.asLong());
        if (removed) setDirty();
        return removed;
    }

    public Optional<BlockPos> partnerOf(@Nonnull BlockPos pos)
    {
        Long partner = partners.get(pos.asLong());
        return partner == null ? Optional.empty() : Optional.of(BlockPos.of(partner));
    }

    public WaypointPairDirectionHealth health(@Nonnull BlockPos from, @Nonnull BlockPos to)
    {
        return health.getOrDefault(new DirectionKey(from.asLong(), to.asLong()), WaypointPairDirectionHealth.UNKNOWN);
    }

    public WaypointPairAggregateStatus aggregateStatus(@Nonnull WaypointPair pair)
    {
        WaypointPairDirectionStatus forward = health(pair.first(), pair.second()).status();
        WaypointPairDirectionStatus reverse = health(pair.second(), pair.first()).status();
        return WaypointPairAggregateStatus.from(forward, reverse);
    }

    public WaypointPairSnapshot snapshot(@Nonnull WaypointPair pair)
    {
        return new WaypointPairSnapshot(pair,
            health(pair.first(), pair.second()),
            health(pair.second(), pair.first()));
    }

    public void recordValidation(
        @Nonnull BlockPos from,
        @Nonnull BlockPos to,
        @Nonnull WaypointPairDirectionStatus status,
        @Nonnull WaypointPairFailureReason reason,
        long validatedAt)
    {
        if (!Long.valueOf(to.asLong()).equals(partners.get(from.asLong()))) return;
        health.put(new DirectionKey(from.asLong(), to.asLong()),
            new WaypointPairDirectionHealth(status, reason, validatedAt));
        setDirty();
    }

    public Collection<WaypointPair> findPairsNear(@Nonnull BlockPos center, int radius)
    {
        double radiusSquared = (double) radius * radius;
        Collection<WaypointPair> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (Map.Entry<Long, Long> entry : partners.entrySet())
        {
            long firstKey = entry.getKey();
            long secondKey = entry.getValue();
            if (!visited.add(firstKey) || !visited.add(secondKey)) continue;
            BlockPos first = BlockPos.of(firstKey);
            BlockPos second = BlockPos.of(secondKey);
            if (horizontalDistanceSquared(center, first) <= radiusSquared || horizontalDistanceSquared(center, second) <= radiusSquared)
            {
                result.add(new WaypointPair(first, second));
            }
        }
        return result;
    }

    public Collection<WaypointPair> findPairsNearSegment(
        @Nonnull BlockPos start,
        @Nonnull BlockPos destination,
        int radius)
    {
        Collection<WaypointPair> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (Map.Entry<Long, Long> entry : partners.entrySet())
        {
            long firstKey = entry.getKey();
            long secondKey = entry.getValue();
            if (!visited.add(firstKey) || !visited.add(secondKey)) continue;
            BlockPos first = BlockPos.of(firstKey);
            BlockPos second = BlockPos.of(secondKey);
            if (WaypointRouteSelector.horizontalDistanceToSegment(first, start, destination) <= radius
                || WaypointRouteSelector.horizontalDistanceToSegment(second, start, destination) <= radius)
            {
                result.add(new WaypointPair(first, second));
            }
        }
        return result;
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries)
    {
        ListTag pairs = new ListTag();
        Set<Long> visited = new HashSet<>();
        for (Map.Entry<Long, Long> entry : partners.entrySet())
        {
            if (!visited.add(entry.getKey()) || !visited.add(entry.getValue())) continue;
            CompoundTag pair = new CompoundTag();
            pair.putLong(TAG_FIRST, entry.getKey());
            pair.putLong(TAG_SECOND, entry.getValue());
            saveHealth(pair, TAG_FIRST_TO_SECOND, entry.getKey(), entry.getValue());
            saveHealth(pair, TAG_SECOND_TO_FIRST, entry.getValue(), entry.getKey());
            pairs.add(pair);
        }
        tag.put(TAG_PAIRS, pairs);
        return tag;
    }

    private void putPair(long first, long second)
    {
        partners.put(first, second);
        partners.put(second, first);
    }

    private boolean unpair(long endpoint)
    {
        Long partner = partners.remove(endpoint);
        if (partner == null) return false;
        partners.remove(partner);
        health.remove(new DirectionKey(endpoint, partner));
        health.remove(new DirectionKey(partner, endpoint));
        return true;
    }

    private void saveHealth(CompoundTag pair, String key, long from, long to)
    {
        WaypointPairDirectionHealth value = health.getOrDefault(new DirectionKey(from, to), WaypointPairDirectionHealth.UNKNOWN);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_STATUS, value.status().name());
        tag.putString(TAG_REASON, value.reason().name());
        tag.putLong(TAG_VALIDATED_AT, value.validatedAt());
        pair.put(key, tag);
    }

    private void loadHealth(CompoundTag pair, String key, long from, long to)
    {
        if (!pair.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag tag = pair.getCompound(key);
        try
        {
            WaypointPairDirectionStatus status = WaypointPairDirectionStatus.valueOf(tag.getString(TAG_STATUS));
            WaypointPairFailureReason reason = WaypointPairFailureReason.valueOf(tag.getString(TAG_REASON));
            health.put(new DirectionKey(from, to), new WaypointPairDirectionHealth(status, reason, tag.getLong(TAG_VALIDATED_AT)));
        }
        catch (IllegalArgumentException ignored)
        {
            // Unknown values from a newer or damaged save are safely treated as unvalidated.
        }
    }

    private static double horizontalDistanceSquared(BlockPos first, BlockPos second)
    {
        long x = first.getX() - second.getX();
        long z = first.getZ() - second.getZ();
        return x * x + z * z;
    }

    private record DirectionKey(long from, long to) {}
}
