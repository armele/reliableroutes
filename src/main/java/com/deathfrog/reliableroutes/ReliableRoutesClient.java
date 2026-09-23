package com.deathfrog.reliableroutes;

import com.deathfrog.reliableroutes.item.PathfinderLensItem;
import com.deathfrog.reliableroutes.navigation.WaypointPair;
import com.deathfrog.reliableroutes.navigation.RoutingZone;
import com.deathfrog.reliableroutes.navigation.RoutingZoneSnapshot;
import com.deathfrog.reliableroutes.navigation.WaypointPairDirectionHealth;
import com.deathfrog.reliableroutes.navigation.WaypointPairDirectionStatus;
import com.deathfrog.reliableroutes.network.ClientWaypointPairCache;
import com.deathfrog.reliableroutes.network.RequestWaypointPairsPayload;
import com.minecolonies.api.items.ModTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

@Mod(value = ReliableRoutes.MODID, dist = Dist.CLIENT)
public class ReliableRoutesClient
{
    public ReliableRoutesClient(ModContainer container)
    {
        NeoForge.EVENT_BUS.register(LensOverlay.class);
    }

    private static final class LensOverlay
    {
        private static final int HORIZONTAL_RADIUS = 24;
        private static final int VERTICAL_RADIUS = 8;
        private static final int REFRESH_TICKS = 8;
        private static final int MAX_HIGHLIGHTS = 4096;
        private static final int PAIR_SYNC_RADIUS = 64;
        private static final int PAIR_SYNC_TICKS = 20;
        private static final double BEAM_HEIGHT = 32.0;
        private static final float INSET = 0.002F;
        private static final float LANE_OFFSET = 0.30F;
        private static final double STATUS_LANE_HEIGHT = 1.25;
        private static final List<Highlight> CACHE = new ArrayList<>();
        private static final List<BlockPos> WAYPOINTS = new ArrayList<>();
        private static BlockPos cachedCenter;
        private static long lastRefresh = Long.MIN_VALUE;
        private static long lastPairSync = Long.MIN_VALUE;

        @SuppressWarnings("null")
        @SubscribeEvent
        public static void render(RenderLevelStageEvent event)
        {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null ||
                !minecraft.player.getMainHandItem().is(ReliableRoutes.PATHFINDER_LENS.get()))
            {
                clear();
                return;
            }

            BlockPos center = minecraft.player.blockPosition();
            long gameTime = minecraft.level.getGameTime();
            if (lastPairSync == Long.MIN_VALUE || gameTime < lastPairSync || gameTime - lastPairSync >= PAIR_SYNC_TICKS)
            {
                PacketDistributor.sendToServer(new RequestWaypointPairsPayload(center, PAIR_SYNC_RADIUS));
                lastPairSync = gameTime;
            }
            if (cachedCenter == null || gameTime - lastRefresh >= REFRESH_TICKS || cachedCenter.distManhattan(center) >= 4)
            {
                rebuild(minecraft.level, center, gameTime);
            }

            PoseStack poseStack = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            for (Highlight highlight : CACHE)
            {
                float red = highlight.dangerous ? 1.0F : 0.1F;
                float green = highlight.dangerous ? 0.1F : 1.0F;
                LevelRenderer.renderLineBox(poseStack,
                    lines,
                    highlight.pos.getX() + INSET,
                    highlight.pos.getY() + INSET,
                    highlight.pos.getZ() + INSET,
                    highlight.pos.getX() + 1.0F - INSET,
                    highlight.pos.getY() + 1.0F - INSET,
                    highlight.pos.getZ() + 1.0F - INSET,
                    red,
                    green,
                    0.1F,
                    highlight.occupancy ? 0.55F : 0.9F);
            }
            renderWaypoints(poseStack, lines, minecraft, gameTime);
            poseStack.popPose();
            buffers.endBatch(RenderType.lines());
        }

        private static void rebuild(ClientLevel level, BlockPos center, long gameTime)
        {
            CACHE.clear();
            WAYPOINTS.clear();
            cachedCenter = center.immutable();
            lastRefresh = gameTime;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int y = center.getY() - VERTICAL_RADIUS; y <= center.getY() + VERTICAL_RADIUS; y++)
            {
                for (int x = center.getX() - HORIZONTAL_RADIUS; x <= center.getX() + HORIZONTAL_RADIUS; x++)
                {
                    for (int z = center.getZ() - HORIZONTAL_RADIUS; z <= center.getZ() + HORIZONTAL_RADIUS; z++)
                    {
                        cursor.set(x, y, z);
                        if (!level.getChunkSource().hasChunk(cursor.getX() >> 4, cursor.getZ() >> 4)) continue;
                        addHighlightsAt(level, cursor);
                        if (CACHE.size() >= MAX_HIGHLIGHTS) return;
                    }
                }
            }
        }

        @SuppressWarnings("null")
        private static void addHighlightsAt(ClientLevel level, @Nonnull BlockPos pos)
        {
            BlockState state = level.getBlockState(pos);
            if (state.is(ReliableRoutes.PATH_PAIR.get()))
            {
                WAYPOINTS.add(pos.immutable());
            }
            else if (state.is(ModTags.dangerousBlocks))
            {
                CACHE.add(new Highlight(pos.immutable(), true, false));
                addDangerousOccupancyHighlight(level, pos);
            }
            else if (state.is(ModTags.pathingBlocks))
            {
                CACHE.add(new Highlight(pos.immutable(), false, false));
            }
        }

        private static void renderWaypoints(
            PoseStack poseStack,
            VertexConsumer lines,
            Minecraft minecraft,
            long gameTime)
        {
            List<RoutingZoneSnapshot> pairs = ClientWaypointPairCache.get();
            Set<Long> pairedEndpoints = new HashSet<>();
            ItemStack lens = minecraft.player.getMainHandItem();
            PathfinderLensItem.PendingWaypoint pending = PathfinderLensItem.getPending(lens);
            PathfinderLensItem.Draft draft = PathfinderLensItem.getDraft(lens);
            BlockPos pendingPos = pending != null && pending.dimension().equals(minecraft.level.dimension().location()) ? pending.pos() : null;
            float pulse = 0.65F + 0.35F * (float) Math.sin(gameTime * 0.2);
            if (draft != null && draft.dimension().equals(minecraft.level.dimension().location()) && draft.secondEndpoint() != null)
            {
                renderWaypoint(poseStack, lines, draft.firstEndpoint(), 0.2F, 0.9F, 1.0F, pulse, true);
                renderWaypoint(poseStack, lines, draft.secondEndpoint(), 0.2F, 0.9F, 1.0F, pulse, true);
                renderConnection(poseStack, lines, draft.firstEndpoint(), draft.secondEndpoint(), 0.2F, 0.9F, 1.0F, 0.8F);
                if (draft.firstCorner() != null && draft.secondCorner() != null)
                {
                    renderZone(poseStack, lines, RoutingZone.fromCorners(draft.firstCorner(), draft.secondCorner(),
                        draft.firstEndpoint(), draft.secondEndpoint()), 0.2F, 0.9F, 1.0F);
                }
            }
            for (RoutingZoneSnapshot snapshot : pairs)
            {
                RoutingZone zone = snapshot.zone();
                WaypointPair pair = zone.pair();
                pairedEndpoints.add(pair.first().asLong());
                pairedEndpoints.add(pair.second().asLong());
                int color = pairColor(pair);
                float red = ((color >> 16) & 255) / 255.0F;
                float green = ((color >> 8) & 255) / 255.0F;
                float blue = (color & 255) / 255.0F;
                boolean firstSelected = pair.first().equals(pendingPos);
                boolean secondSelected = pair.second().equals(pendingPos);
                renderWaypoint(poseStack, lines, pair.first(),
                    firstSelected ? 1.0F : red, firstSelected ? 1.0F : green, firstSelected ? 1.0F : blue,
                    firstSelected ? pulse : 0.9F, true);
                renderWaypoint(poseStack, lines, pair.second(),
                    secondSelected ? 1.0F : red, secondSelected ? 1.0F : green, secondSelected ? 1.0F : blue,
                    secondSelected ? pulse : 0.9F, true);
                // The directional lanes below are the authoritative connection display.
                // A centered undirected line obscures their status colors and arrows.
                renderZone(poseStack, lines, zone, 0.15F, 0.8F, 1.0F);
                Vec3 laneOffset = laneOffset(pair.first(), pair.second());
                renderDirectionalLane(poseStack, lines, pair.first(), pair.second(), laneOffset,
                    snapshot.firstToSecond(), gameTime);
                renderDirectionalLane(poseStack, lines, pair.second(), pair.first(), laneOffset.scale(-1),
                    snapshot.secondToFirst(), gameTime);
            }

            for (BlockPos waypoint : WAYPOINTS)
            {
                if (pairedEndpoints.contains(waypoint.asLong())) continue;
                boolean selected = waypoint.equals(pendingPos);
                renderWaypoint(poseStack, lines, waypoint,
                    1.0F,
                    selected ? 1.0F : 0.55F,
                    selected ? 1.0F : 0.05F,
                    selected ? pulse : 0.85F,
                    selected);
            }
        }

        @SuppressWarnings("null")
        private static void renderConnection(
            PoseStack poseStack,
            VertexConsumer lines,
            BlockPos first,
            BlockPos second,
            float red,
            float green,
            float blue,
            float alpha)
        {
            float x1 = first.getX() + 0.5F;
            float y1 = first.getY() + 1.2F;
            float z1 = first.getZ() + 0.5F;
            float x2 = second.getX() + 0.5F;
            float y2 = second.getY() + 1.2F;
            float z2 = second.getZ() + 0.5F;
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length == 0) return;
            lines.addVertex(poseStack.last().pose(), x1, y1, z1)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), dx / length, dy / length, dz / length);
            lines.addVertex(poseStack.last().pose(), x2, y2, z2)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), dx / length, dy / length, dz / length);
        }

        private static Vec3 laneOffset(BlockPos first, BlockPos second)
        {
            double dx = second.getX() - first.getX();
            double dz = second.getZ() - first.getZ();
            double horizontalLength = Math.hypot(dx, dz);
            if (horizontalLength < 0.001) return new Vec3(LANE_OFFSET, 0, 0);
            return new Vec3(-dz / horizontalLength * LANE_OFFSET, 0, dx / horizontalLength * LANE_OFFSET);
        }

        private static void renderDirectionalLane(
            PoseStack poseStack,
            VertexConsumer lines,
            BlockPos from,
            BlockPos to,
            Vec3 offset,
            WaypointPairDirectionHealth health,
            long gameTime)
        {
            Vec3 start = new Vec3(from.getX() + 0.5, from.getY() + STATUS_LANE_HEIGHT, from.getZ() + 0.5).add(offset);
            Vec3 end = new Vec3(to.getX() + 0.5, to.getY() + STATUS_LANE_HEIGHT, to.getZ() + 0.5).add(offset);
            Vec3 direction = end.subtract(start);
            double length = direction.length();
            if (length < 0.001) return;
            Vec3 unit = direction.scale(1.0 / length);
            Vec3 side = new Vec3(-unit.z, 0, unit.x);
            if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
            else side = side.normalize();

            float[] color = statusColor(health.status(), gameTime);
            Vec3 endpointTop = new Vec3(from.getX() + 0.5, from.getY() + 1.05, from.getZ() + 0.5).add(offset);
            renderLine(poseStack, lines, endpointTop, start, color[0], color[1], color[2], color[3]);
            if (health.status() == WaypointPairDirectionStatus.UNKNOWN)
            {
                renderDottedLine(poseStack, lines, start, end, color);
            }
            else
            {
                renderLine(poseStack, lines, start, end, color[0], color[1], color[2], color[3]);
            }

            renderArrow(poseStack, lines, start.add(direction.scale(0.10)), unit, side, color);
            renderArrow(poseStack, lines, start.add(direction.scale(0.42)), unit, side, color);
            renderArrow(poseStack, lines, start.add(direction.scale(0.72)), unit, side, color);
            if (health.status() == WaypointPairDirectionStatus.BROKEN)
            {
                renderCross(poseStack, lines, start.add(direction.scale(0.56)), unit, side, color);
            }
        }

        private static float[] statusColor(WaypointPairDirectionStatus status, long gameTime)
        {
            return switch (status)
            {
                case VALID -> new float[] {0.15F, 1.0F, 0.25F, 0.95F};
                case UNKNOWN -> new float[] {1.0F, 0.72F, 0.12F, 0.9F};
                case BROKEN -> new float[] {1.0F, 0.08F, 0.08F,
                    0.55F + 0.4F * (float) Math.abs(Math.sin(gameTime * 0.18))};
            };
        }

        private static void renderDottedLine(PoseStack poseStack, VertexConsumer lines, Vec3 start, Vec3 end, float[] color)
        {
            Vec3 delta = end.subtract(start);
            double length = delta.length();
            int segments = Math.max(1, (int) Math.ceil(length / 0.8));
            for (int index = 0; index < segments; index += 2)
            {
                double from = (double) index / segments;
                double to = Math.min(1.0, (double) (index + 1) / segments);
                renderLine(poseStack, lines, start.add(delta.scale(from)), start.add(delta.scale(to)),
                    color[0], color[1], color[2], color[3]);
            }
        }

        private static void renderArrow(PoseStack poseStack, VertexConsumer lines, Vec3 tip, Vec3 unit, Vec3 side, float[] color)
        {
            Vec3 back = tip.subtract(unit.scale(0.42));
            renderLine(poseStack, lines, tip, back.add(side.scale(0.22)), color[0], color[1], color[2], color[3]);
            renderLine(poseStack, lines, tip, back.subtract(side.scale(0.22)), color[0], color[1], color[2], color[3]);
        }

        private static void renderCross(PoseStack poseStack, VertexConsumer lines, Vec3 center, Vec3 unit, Vec3 side, float[] color)
        {
            Vec3 along = unit.scale(0.24);
            Vec3 across = side.scale(0.24);
            renderLine(poseStack, lines, center.subtract(along).subtract(across), center.add(along).add(across),
                color[0], color[1], color[2], color[3]);
            renderLine(poseStack, lines, center.subtract(along).add(across), center.add(along).subtract(across),
                color[0], color[1], color[2], color[3]);
        }

        private static void renderLine(
            PoseStack poseStack, VertexConsumer lines, Vec3 start, Vec3 end,
            float red, float green, float blue, float alpha)
        {
            Vec3 normal = end.subtract(start).normalize();
            lines.addVertex(poseStack.last().pose(), (float) start.x, (float) start.y, (float) start.z)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), (float) normal.x, (float) normal.y, (float) normal.z);
            lines.addVertex(poseStack.last().pose(), (float) end.x, (float) end.y, (float) end.z)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), (float) normal.x, (float) normal.y, (float) normal.z);
        }

        @SubscribeEvent
        public static void renderHud(RenderGuiEvent.Post event)
        {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
                || !minecraft.player.getMainHandItem().is(ReliableRoutes.PATHFINDER_LENS.get())
                || !(minecraft.hitResult instanceof BlockHitResult blockHit)) return;

            RoutingZoneSnapshot snapshot = findPair(blockHit.getBlockPos());
            if (snapshot == null) return;
            boolean lookingAtFirst = snapshot.zone().firstEndpoint().equals(blockHit.getBlockPos());
            WaypointPairDirectionHealth toPartner = lookingAtFirst ? snapshot.firstToSecond() : snapshot.secondToFirst();
            WaypointPairDirectionHealth fromPartner = lookingAtFirst ? snapshot.secondToFirst() : snapshot.firstToSecond();
            GuiGraphics graphics = event.getGuiGraphics();
            Component title = Component.translatable("gui.reliableroutes.path_pair");
            Component outbound = healthLine("gui.reliableroutes.to_partner", toPartner, minecraft.level.getGameTime());
            Component inbound = healthLine("gui.reliableroutes.from_partner", fromPartner, minecraft.level.getGameTime());
            int width = Math.max(minecraft.font.width(title), Math.max(minecraft.font.width(outbound), minecraft.font.width(inbound))) + 12;
            int x = graphics.guiWidth() / 2 + 12;
            int y = graphics.guiHeight() / 2 + 14;
            graphics.fill(x - 5, y - 5, x + width, y + 31, 0xA0000000);
            graphics.drawString(minecraft.font, title, x, y, 0xFFFFFF, true);
            graphics.drawString(minecraft.font, outbound, x, y + 10, statusTextColor(toPartner.status()), true);
            graphics.drawString(minecraft.font, inbound, x, y + 20, statusTextColor(fromPartner.status()), true);
        }

        private static RoutingZoneSnapshot findPair(BlockPos pos)
        {
            for (RoutingZoneSnapshot snapshot : ClientWaypointPairCache.get())
            {
                if (snapshot.zone().firstEndpoint().equals(pos) || snapshot.zone().secondEndpoint().equals(pos)) return snapshot;
            }
            return null;
        }

        private static void renderZone(PoseStack poseStack, VertexConsumer lines, RoutingZone zone,
            float red, float green, float blue)
        {
            double minX = zone.minX(), minZ = zone.minZ();
            double maxX = zone.maxX() + 1.0, maxZ = zone.maxZ() + 1.0;
            double bottom = zone.minY();
            double top = zone.maxY() + 1.0;
            LevelRenderer.renderLineBox(poseStack, lines, minX, bottom, minZ, maxX, top, maxZ,
                red, green, blue, 0.55F);
        }

        private static Component healthLine(String labelKey, WaypointPairDirectionHealth health, long gameTime)
        {
            Component status = Component.translatable("gui.reliableroutes.status." + health.status().name().toLowerCase(java.util.Locale.ROOT));
            if (health.status() == WaypointPairDirectionStatus.BROKEN)
            {
                Component reason = Component.translatable("gui.reliableroutes.reason." + health.reason().name().toLowerCase(java.util.Locale.ROOT));
                status = Component.translatable("gui.reliableroutes.broken_detail", status, reason);
            }
            if (health.validatedAt() > 0)
            {
                long seconds = Math.max(0, (gameTime - health.validatedAt()) / 20);
                status = Component.translatable("gui.reliableroutes.checked_age", status, formatAge(seconds));
            }
            return Component.translatable(labelKey, status);
        }

        private static String formatAge(long seconds)
        {
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return seconds / 60 + "m";
            return seconds / 3600 + "h";
        }

        private static int statusTextColor(WaypointPairDirectionStatus status)
        {
            return switch (status)
            {
                case VALID -> 0x55FF66;
                case UNKNOWN -> 0xFFB82E;
                case BROKEN -> 0xFF4444;
            };
        }

        private static void renderWaypoint(
            PoseStack poseStack,
            VertexConsumer lines,
            BlockPos pos,
            float red,
            float green,
            float blue,
            float alpha,
            boolean beam)
        {
            LevelRenderer.renderLineBox(poseStack, lines,
                pos.getX() + INSET, pos.getY() + INSET, pos.getZ() + INSET,
                pos.getX() + 1.0F - INSET, pos.getY() + 1.0F - INSET, pos.getZ() + 1.0F - INSET,
                red, green, blue, alpha);
            if (beam)
            {
                LevelRenderer.renderLineBox(poseStack, lines,
                    pos.getX() + 0.46, pos.getY() + 1.0, pos.getZ() + 0.46,
                    pos.getX() + 0.54, pos.getY() + 1.0 + BEAM_HEIGHT, pos.getZ() + 0.54,
                    red, green, blue, alpha);
            }
        }

        private static int pairColor(WaypointPair pair)
        {
            long first = Math.min(pair.first().asLong(), pair.second().asLong());
            long second = Math.max(pair.first().asLong(), pair.second().asLong());
            long mixed = first * 31L + second;
            float hue = (float) Math.floorMod(mixed, 360L) / 360.0F;
            return Mth.hsvToRgb(hue, 0.8F, 1.0F);
        }

        private static void addDangerousOccupancyHighlight(ClientLevel level, BlockPos supportingBlock)
        {
            BlockPos feet = supportingBlock.above();
            BlockPos head = supportingBlock.above(2);

            if (feet == null || head == null) return;

            boolean feetClear = level.getBlockState(feet).getCollisionShape(level, feet).isEmpty();
            boolean headClear = level.getBlockState(head).getCollisionShape(level, head).isEmpty();
            if (feetClear && headClear)
            {
                CACHE.add(new Highlight(feet, true, true));
            }
        }

        private static void clear()
        {
            CACHE.clear();
            WAYPOINTS.clear();
            ClientWaypointPairCache.clear();
            cachedCenter = null;
            lastRefresh = Long.MIN_VALUE;
            lastPairSync = Long.MIN_VALUE;
        }

        private record Highlight(BlockPos pos, boolean dangerous, boolean occupancy)
        {}
    }
}
