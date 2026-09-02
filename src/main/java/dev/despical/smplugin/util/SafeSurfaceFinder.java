/*
 * SMPlugin - A utility plugin for KAYIK SMP.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dev.despical.smplugin.util;

import org.bukkit.*;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class SafeSurfaceFinder {

    private static final int SEARCH_RADIUS = 32;
    private static final Set<Material> UNSAFE_FLOORS = EnumSet.of(
        Material.BEDROCK,
        Material.CACTUS,
        Material.CAMPFIRE,
        Material.END_GATEWAY,
        Material.END_PORTAL,
        Material.FIRE,
        Material.LAVA,
        Material.MAGMA_BLOCK,
        Material.POWDER_SNOW,
        Material.SOUL_CAMPFIRE,
        Material.SOUL_FIRE,
        Material.SWEET_BERRY_BUSH,
        Material.WITHER_ROSE
    );

    private SafeSurfaceFinder() {
    }

    public static Optional<Location> find(Location origin) {
        World world = origin.getWorld();
        int originX = origin.getBlockX();
        int originZ = origin.getBlockZ();

        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (radius > 0 && Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) {
                        continue;
                    }

                    Optional<Location> destination = findInColumn(
                        world,
                        originX + offsetX,
                        originZ + offsetZ,
                        origin.getYaw(),
                        origin.getPitch()
                    );

                    if (destination.isPresent()) {
                        return destination;
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<Location> findInColumn(World world, int x, int z, float yaw, float pitch) {
        int highestY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int maxFloorY = Math.min(highestY, world.getMaxHeight() - 3);

        for (int floorY = maxFloorY; floorY >= world.getMinHeight(); floorY--) {
            Block floor = world.getBlockAt(x, floorY, z);
            Block feet = floor.getRelative(0, 1, 0);
            Block head = floor.getRelative(0, 2, 0);

            if (!isSafeFloor(floor) || !isOpen(feet) || !isOpen(head)) {
                continue;
            }

            Location destination = new Location(world, x + 0.5, floorY + 1.0, z + 0.5, yaw, pitch);
            if (world.getWorldBorder().isInside(destination)) {
                return Optional.of(destination);
            }
        }

        return Optional.empty();
    }

    private static boolean isSafeFloor(Block block) {
        Material material = block.getType();

        return material.isSolid()
            && !UNSAFE_FLOORS.contains(material)
            && !Tag.LEAVES.isTagged(material)
            && !Tag.LOGS.isTagged(material);
    }

    private static boolean isOpen(Block block) {
        return block.isPassable() && !block.isLiquid();
    }
}
