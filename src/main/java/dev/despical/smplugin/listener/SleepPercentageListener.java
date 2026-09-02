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
package dev.despical.smplugin.listener;

import dev.despical.smplugin.SMPlugin;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class SleepPercentageListener implements Listener {

    private static final int REQUIRED_SLEEP_PERCENTAGE = 50;

    public SleepPercentageListener(SMPlugin plugin) {
        plugin.getServer().getWorlds().forEach(this::applySleepPercentage);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        applySleepPercentage(event.getWorld());
    }

    private void applySleepPercentage(World world) {
        world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, REQUIRED_SLEEP_PERCENTAGE);
    }
}
