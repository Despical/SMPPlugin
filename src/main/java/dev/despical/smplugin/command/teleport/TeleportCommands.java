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
package dev.despical.smplugin.command.teleport;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.CompleterHelper;
import dev.despical.commandframework.annotations.Command;
import dev.despical.commandframework.annotations.Completer;
import dev.despical.smplugin.util.SafeSurfaceFinder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class TeleportCommands {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Command(
        name = "tphere",
        usage = "/tphere <player>",
        desc = "Teleports an online player to you.",
        permission = "smplugin.command.tphere",
        min = 1,
        max = 1,
        senderType = Command.SenderType.PLAYER
    )
    public void teleportHereCommand(CommandArguments arguments) {
        Player sender = arguments.getSender();
        Player target = arguments.getPlayer(0).orElse(null);

        if (target == null) {
            arguments.sendMessage("<red>Online player not found: <white>{0}</white>", arguments.getFirst());
            return;
        }

        target.teleport(sender.getLocation());
        arguments.sendMessage("<#55FFFF>{0}</#55FFFF><gray> was teleported to you.</gray>", target.getName());

        if (!sender.equals(target)) {
            target.sendMessage(MINI_MESSAGE.deserialize(
                "<gray>You were teleported to </gray><#55FFFF>" + sender.getName() + "</#55FFFF><gray>.</gray>"
            ));
        }
    }

    @Command(
        name = "tpall",
        usage = "/tpall [player]",
        desc = "Teleports every online player to you or another online player.",
        permission = "smplugin.command.tpall",
        max = 1
    )
    public void teleportAllCommand(CommandArguments arguments) {
        CommandSender sender = arguments.getSender();
        Player destination;

        if (arguments.isArgumentsEmpty()) {
            if (!(sender instanceof Player player)) {
                arguments.sendMessage("<red>Console usage: /tpall <player>");
                return;
            }

            destination = player;
        } else {
            destination = arguments.getPlayer(0).orElse(null);
            if (destination == null) {
                arguments.sendMessage("<red>Online player not found: <white>{0}</white>", arguments.getFirst());
                return;
            }
        }

        Location location = destination.getLocation();
        int teleported = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.teleport(location)) {
                teleported++;
            }
        }

        arguments.sendMessage(
            "<#55FFFF>{0}</#55FFFF><gray> player(s) were teleported to </gray><#B8FFDF>{1}</#B8FFDF><gray>.</gray>",
            teleported,
            destination.getName()
        );
    }

    @Command(
        name = "surface",
        usage = "/surface [player]",
        desc = "Teleports yourself or an online player to the nearest safe surface.",
        permission = "smplugin.command.surface",
        max = 1
    )
    public void surfaceCommand(CommandArguments arguments) {
        CommandSender sender = arguments.getSender();
        Player target;

        if (arguments.isArgumentsEmpty()) {
            if (!(sender instanceof Player player)) {
                arguments.sendMessage("<red>Console usage: /surface <player>");
                return;
            }

            target = player;
        } else {
            target = arguments.getPlayer(0).orElse(null);
            if (target == null) {
                arguments.sendMessage("<red>Online player not found: <white>{0}</white>", arguments.getFirst());
                return;
            }
        }

        Location destination = SafeSurfaceFinder.find(target.getLocation()).orElse(null);
        if (destination == null) {
            arguments.sendMessage("<red>No safe surface was found near <white>{0}</white>.", target.getName());
            return;
        }

        if (!target.teleport(destination, PlayerTeleportEvent.TeleportCause.COMMAND)) {
            arguments.sendMessage("<red>Could not teleport <white>{0}</white> to the surface.", target.getName());
            return;
        }

        arguments.sendMessage("<#55FFFF>{0}</#55FFFF><gray> was teleported safely to the surface.</gray>", target.getName());

        if (!sender.equals(target)) {
            target.sendMessage(MINI_MESSAGE.deserialize(
                "<gray>You were teleported safely to the surface by </gray><#55FFFF>" + sender.getName() + "</#55FFFF><gray>.</gray>"
            ));
        }
    }

    @Completer(name = "tphere", permission = "smplugin.command.tphere")
    public List<String> completeTpHere(CommandArguments arguments, CompleterHelper helper) {
        return completePlayer(arguments, helper);
    }

    @Completer(name = "tpall", permission = "smplugin.command.tpall")
    public List<String> completeTpAll(CommandArguments arguments, CompleterHelper helper) {
        return completePlayer(arguments, helper);
    }

    @Completer(name = "surface", permission = "smplugin.command.surface")
    public List<String> completeSurface(CommandArguments arguments, CompleterHelper helper) {
        return completePlayer(arguments, helper);
    }

    private List<String> completePlayer(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, helper.playerNames());
        }

        return helper.empty();
    }
}
