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
import dev.despical.smplugin.util.MotdCenterer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import javax.imageio.ImageIO;
import java.io.InputStream;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class MotdListener implements Listener {

    private static final String DEFAULT_FIRST_LINE = "<bold><gradient:#00E5FF:#00FF85>K A Y I K</gradient></bold>";
    private static final String DEFAULT_SECOND_LINE = "<#5DEBFF><bold>SMP</bold></#5DEBFF> <dark_gray>•</dark_gray> <#B8FFDF>Survival Multiplayer Minecraft</#B8FFDF>";

    private final boolean enabled;
    private final Component motd;
    private final CachedServerIcon serverIcon;

    public MotdListener(SMPlugin plugin) {
        enabled = plugin.getConfig().getBoolean("motd.enabled", true);

        int centerWidth = Math.max(0, plugin.getConfig().getInt("motd.center-width", 270));
        MiniMessage miniMessage = MiniMessage.miniMessage();

        Component firstLine = deserialize(plugin, miniMessage, "motd.line-1", DEFAULT_FIRST_LINE);
        Component secondLine = deserialize(plugin, miniMessage, "motd.line-2", DEFAULT_SECOND_LINE);

        motd = MotdCenterer.center(firstLine, centerWidth)
            .append(Component.newline())
            .append(MotdCenterer.center(secondLine, centerWidth));

        serverIcon = loadServerIcon(plugin);
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        if (enabled) {
            event.motd(motd);
        }

        if (serverIcon != null) {
            event.setServerIcon(serverIcon);
        }
    }

    private Component deserialize(SMPlugin plugin, MiniMessage miniMessage, String path, String fallback) {
        String input = plugin.getConfig().getString(path, fallback);

        try {
            return miniMessage.deserialize(input == null ? fallback : input);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Invalid MiniMessage at '" + path + "'; using the default value.");
            return miniMessage.deserialize(fallback);
        }
    }

    private CachedServerIcon loadServerIcon(SMPlugin plugin) {
        try (InputStream input = plugin.getResource("server-icon.png")) {
            if (input == null) {
                plugin.getLogger().warning("Bundled server-icon.png was not found.");
                return null;
            }

            return Bukkit.loadServerIcon(ImageIO.read(input));
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not load server-icon.png: " + exception.getMessage());
            return null;
        }
    }
}
