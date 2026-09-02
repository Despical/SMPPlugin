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
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import dev.despical.smplugin.vanish.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class PlayerConnectionListener implements Listener {

    private final SMPlugin plugin;
    private final MessageService messages;
    private final VanishManager vanishManager;

    public PlayerConnectionListener(SMPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageService();
        this.vanishManager = plugin.getVanishManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var config = plugin.getConfig();
        var player = event.getPlayer();

        if (!config.getBoolean("connection-messages.join.enabled", true) || vanishManager.isVanished(player)) {
            event.joinMessage(null);
            return;
        }

        event.joinMessage(messages.parse(
            config.getString(
                "connection-messages.join.message",
                "<#475569>[</#475569><#55FF55>+</#55FF55><#475569>]</#475569> <#CBD5E1>%player%</#CBD5E1>"
            ),
            Variable.of("%player%", player.getName())
        ));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var config = plugin.getConfig();
        var player = event.getPlayer();

        if (!config.getBoolean("connection-messages.quit.enabled", true) || vanishManager.isVanished(player)) {
            event.quitMessage(null);
            return;
        }

        event.quitMessage(messages.parse(
            config.getString(
                "connection-messages.quit.message",
                "<#475569>[</#475569><#FF5555>-</#FF5555><#475569>]</#475569> <#CBD5E1>%player%</#CBD5E1>"
            ),
            Variable.of("%player%", player.getName())
        ));
    }
}
