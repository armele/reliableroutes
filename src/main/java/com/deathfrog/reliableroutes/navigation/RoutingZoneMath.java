package com.deathfrog.reliableroutes.navigation;

/** Pure geometry for routing-zone selection. */
final class RoutingZoneMath
{
    private static final double EPSILON = 1.0E-9;

    private RoutingZoneMath() {}

    static boolean segmentIntersectsBox(
        double startX, double startY, double startZ,
        double endX, double endY, double endZ,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ)
    {
        return intersectionOrder(startX, startY, startZ, endX, endY, endZ,
            minX, minY, minZ, maxX, maxY, maxZ) >= 0;
    }

    /** Returns the segment entry parameter in [0,1], or -1 when the segment misses the box. */
    static double intersectionOrder(
        double startX, double startY, double startZ,
        double endX, double endY, double endZ,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ)
    {
        double entry = 0;
        double exit = 1;

        double delta = endX - startX;
        if (Math.abs(delta) < EPSILON)
        {
            if (startX < minX || startX > maxX) return -1;
        }
        else
        {
            double first = (minX - startX) / delta;
            double second = (maxX - startX) / delta;
            if (first > second) { double swap = first; first = second; second = swap; }
            entry = Math.max(entry, first);
            exit = Math.min(exit, second);
            if (entry > exit) return -1;
        }

        delta = endY - startY;
        if (Math.abs(delta) < EPSILON)
        {
            if (startY < minY || startY > maxY) return -1;
        }
        else
        {
            double first = (minY - startY) / delta;
            double second = (maxY - startY) / delta;
            if (first > second) { double swap = first; first = second; second = swap; }
            entry = Math.max(entry, first);
            exit = Math.min(exit, second);
            if (entry > exit) return -1;
        }

        delta = endZ - startZ;
        if (Math.abs(delta) < EPSILON)
        {
            if (startZ < minZ || startZ > maxZ) return -1;
        }
        else
        {
            double first = (minZ - startZ) / delta;
            double second = (maxZ - startZ) / delta;
            if (first > second) { double swap = first; first = second; second = swap; }
            entry = Math.max(entry, first);
            exit = Math.min(exit, second);
            if (entry > exit) return -1;
        }

        return entry;
    }
}
