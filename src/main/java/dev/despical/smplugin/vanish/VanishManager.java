/*
 * SMPlugin - A utility plugin for Minecraft servers.
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
package dev.despical.smplugin.vanish;

import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Despical
 * <p>
 * Created at 19.08.2026
 */
public final class VanishManager implements Listener {

    private final SMPlugin plugin;
    private final MessageService messages;
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishManager(SMPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageService();
    }

    public void toggle(Player player) {
        if (vanishedPlayers.remove(player.getUniqueId())) {
            showToEveryone(player);
            messages.send(player, "vanish.messages.disabled");
            return;
        }

        vanishedPlayers.add(player.getUniqueId());
        hideFromEveryone(player);
        messages.send(player, "vanish.messages.enabled");
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public void shutdown() {
        for (UUID playerId : Set.copyOf(vanishedPlayers)) {
            Player player = Bukkit.getPlayer(playerId);

            if (player != null) {
                showToEveryone(player);
            }
        }

        vanishedPlayers.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player viewer = event.getPlayer();

        for (UUID playerId : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(playerId);

            if (vanished != null && !vanished.equals(viewer)) {
                viewer.hidePlayer(plugin, vanished);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        vanishedPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void hideFromEveryone(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.hidePlayer(plugin, player);
            }
        }
    }

    private void showToEveryone(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.showPlayer(plugin, player);
            }
        }
    }
}
