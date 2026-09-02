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
package dev.despical.smplugin.command.location;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.CommandErrorMessage;
import dev.despical.commandframework.CompleterHelper;
import dev.despical.commandframework.annotations.Command;
import dev.despical.commandframework.annotations.Completer;
import dev.despical.commandframework.annotations.Flag;
import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.death.DeathLocationManager;
import dev.despical.smplugin.death.DeathLocationManager.DeathRecord;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class LocationCommands {

    private final SMPlugin plugin = JavaPlugin.getPlugin(SMPlugin.class);
    private final MessageService messages = plugin.getMessageService();
    private final DeathLocationManager deathLocationManager = plugin.getDeathLocationManager();

    @Command(
        name = "whereami",
        usage = "/whereami [--all]",
        desc = "Shows your location and optionally shares it with everyone.",
        max = 1,
        senderType = Command.SenderType.PLAYER
    )
    @Flag({"all"})
    public void whereAmICommand(CommandArguments arguments) {
        if (!arguments.isArgumentsEmpty() && !arguments.isFlagPresent("all")) {
            arguments.sendMessage(CommandErrorMessage.LONG_ARG_SIZE);
            return;
        }

        Player player = arguments.getSender();
        Location location = player.getLocation();
        Variable[] variables = locationVariables(player, location);

        if (arguments.isFlagPresent("all")) {
            Bukkit.broadcast(messages.component("whereami.messages.broadcast", variables));
            return;
        }

        arguments.sendMessage(messages.component("whereami.messages.private", variables));
    }

    @Completer(name = "whereami")
    public List<String> completeWhereAmI(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, List.of("--all"));
        }
        return helper.empty();
    }

    @Command(
        name = "deathinfo",
        usage = "/deathinfo <player> [--sendInfo]",
        desc = "Shows a player's most recent death time and location.",
        permission = "smplugin.command.deathinfo",
        min = 1,
        max = 2
    )
    @Flag({"sendInfo"})
    public void deathInfoCommand(CommandArguments arguments) {
        DeathRecord record = deathLocationManager.findByName(arguments.getFirst()).orElse(null);

        if (record == null) {
            messages.send(
                arguments.getSender(),
                "death-location.no-record",
                Variable.of("%player%", messages.escape(arguments.getFirst()))
            );
            return;
        }

        deathLocationManager.send(arguments.getSender(), "death-location.info", record);
        if (!arguments.isFlagPresent("sendInfo")) {
            return;
        }

        Player target = Bukkit.getPlayer(record.playerId());
        if (target == null) {
            deathLocationManager.send(arguments.getSender(), "death-location.target-offline", record);
            return;
        }

        deathLocationManager.send(target, "death-location.message", record);
        deathLocationManager.send(arguments.getSender(), "death-location.resent", record);
    }

    @Completer(name = "deathinfo", permission = "smplugin.command.deathinfo")
    public List<String> completeDeathInfo(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, deathLocationManager.getKnownPlayerNames());
        }

        if (arguments.getLength() == 2) {
            return helper.copyMatches(1, List.of("--sendInfo"));
        }

        return helper.empty();
    }

    private Variable[] locationVariables(Player player, Location location) {
        return new Variable[]{
            Variable.of("%player%", messages.escape(player.getName())),
            Variable.of("%x%", location.getBlockX()),
            Variable.of("%y%", location.getBlockY()),
            Variable.of("%z%", location.getBlockZ()),
            Variable.of("%world%", messages.escape(location.getWorld().getName()))
        };
    }
}
