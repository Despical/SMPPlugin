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
package dev.despical.smplugin.command.administration;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.annotations.Command;
import dev.despical.smplugin.SMPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 19.08.2026
 */
public final class AdministrationCommands {

    private final SMPlugin plugin = JavaPlugin.getPlugin(SMPlugin.class);

    @Command(
        name = "vanish",
        aliases = "v",
        usage = "/vanish",
        desc = "Toggles your visibility to other online players.",
        permission = "smplugin.command.vanish",
        max = 0,
        senderType = Command.SenderType.PLAYER
    )
    public void vanishCommand(CommandArguments arguments) {
        Player player = arguments.getSender();
        plugin.getVanishManager().toggle(player);
    }

    @Command(
        name = "smp.reload",
        usage = "/smp reload",
        desc = "Reloads SMPlugin's configuration and messages.",
        permission = "smplugin.command.reload",
        max = 0
    )
    public void reloadCommand(CommandArguments arguments) {
        plugin.reloadConfig();
        plugin.getMessageService().send(arguments.getSender(), "plugin.messages.reloaded");
    }
}
