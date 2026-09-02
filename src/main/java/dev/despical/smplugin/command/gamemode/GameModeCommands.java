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
package dev.despical.smplugin.command.gamemode;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.CompleterHelper;
import dev.despical.commandframework.annotations.Command;
import dev.despical.commandframework.annotations.Completer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class GameModeCommands {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Command(
        name = "gmc",
        usage = "/gmc [player]",
        desc = "Sets creative mode for yourself or an online player.",
        permission = "smplugin.command.gmc",
        max = 1
    )
    public void creativeCommand(CommandArguments arguments) {
        changeGameMode(arguments, GameMode.CREATIVE, "Creative");
    }

    @Command(
        name = "gms",
        usage = "/gms [player]",
        desc = "Sets survival mode for yourself or an online player.",
        permission = "smplugin.command.gms",
        max = 1
    )
    public void survivalCommand(CommandArguments arguments) {
        changeGameMode(arguments, GameMode.SURVIVAL, "Survival");
    }

    @Command(
        name = "gma",
        usage = "/gma [player]",
        desc = "Sets adventure mode for yourself or an online player.",
        permission = "smplugin.command.gma",
        max = 1
    )
    public void adventureCommand(CommandArguments arguments) {
        changeGameMode(arguments, GameMode.ADVENTURE, "Adventure");
    }

    @Completer(name = "gmc", permission = "smplugin.command.gmc")
    public List<String> completeGmc(CommandArguments arguments, CompleterHelper helper) {
        return completePlayer(arguments, helper);
    }

    @Completer(name = "gms", permission = "smplugin.command.gms")
    public List<String> completeGms(CommandArguments arguments, CompleterHelper helper) {
        return completePlayer(arguments, helper);
    }

    @Completer(name = "gma", permission = "smplugin.command.gma")
    public List<String> completeGma(CommandArguments arguments, CompleterHelper helper) {
        return completePlayer(arguments, helper);
    }

    private void changeGameMode(CommandArguments arguments, GameMode gameMode, String displayName) {
        CommandSender sender = arguments.getSender();
        Player target;

        if (arguments.isArgumentsEmpty()) {
            if (!(sender instanceof Player player)) {
                arguments.sendMessage("<red>Console usage: /{0} <player>", arguments.getLabel());
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

        target.setGameMode(gameMode);
        arguments.sendMessage(
            "<#55FFFF>{0}</#55FFFF><gray> is now in </gray><#B8FFDF>{1}</#B8FFDF><gray> mode.</gray>",
            target.getName(),
            displayName
        );

        if (!sender.equals(target)) {
            target.sendMessage(MINI_MESSAGE.deserialize(
                "<gray>Your game mode was set to </gray><#B8FFDF>" + displayName + "</#B8FFDF><gray>.</gray>"
            ));
        }
    }

    private List<String> completePlayer(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, helper.playerNames());
        }

        return helper.empty();
    }
}
