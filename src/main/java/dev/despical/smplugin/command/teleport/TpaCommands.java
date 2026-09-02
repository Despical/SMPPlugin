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
import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.message.MessageService.Variable;
import dev.despical.smplugin.teleport.TeleportRequestManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class TpaCommands {

    private final SMPlugin plugin = JavaPlugin.getPlugin(SMPlugin.class);
    private final TeleportRequestManager requestManager = plugin.getTeleportRequestManager();

    @Command(
        name = "tpa",
        usage = "/tpa <player>",
        desc = "Sends a teleport request to an online player.",
        permission = "smplugin.command.tpa",
        min = 1,
        max = 1,
        senderType = Command.SenderType.PLAYER
    )
    public void teleportAskCommand(CommandArguments arguments) {
        Player requester = arguments.getSender();
        Player target = arguments.getPlayer(0).orElse(null);

        if (target == null) {
            plugin.getMessageService().send(
                requester,
                "tpa.messages.player-not-found",
                Variable.of("%player%", arguments.getFirst())
            );
            return;
        }

        requestManager.sendRequest(requester, target);
    }

    @Command(
        name = "tpaccept",
        usage = "/tpaccept [player]",
        desc = "Accepts a pending teleport request.",
        permission = "smplugin.command.tpa",
        max = 1,
        senderType = Command.SenderType.PLAYER
    )
    public void teleportAcceptCommand(CommandArguments arguments) {
        requestManager.accept(arguments.getSender(), arguments.getFirst());
    }

    @Command(
        name = "tpreject",
        usage = "/tpreject [player]",
        desc = "Rejects a pending teleport request.",
        permission = "smplugin.command.tpa",
        max = 1,
        senderType = Command.SenderType.PLAYER
    )
    public void teleportRejectCommand(CommandArguments arguments) {
        requestManager.reject(arguments.getSender(), arguments.getFirst());
    }

    @Command(
        name = "tpa.disable",
        usage = "/tpa disable",
        desc = "Stops other players from sending you teleport requests.",
        permission = "smplugin.command.tpa",
        max = 0,
        senderType = Command.SenderType.PLAYER
    )
    public void disableTeleportRequestsCommand(CommandArguments arguments) {
        requestManager.setRequestsEnabled(arguments.getSender(), false);
    }

    @Command(
        name = "tpa.enable",
        usage = "/tpa enable",
        desc = "Allows other players to send you teleport requests.",
        permission = "smplugin.command.tpa",
        max = 0,
        senderType = Command.SenderType.PLAYER
    )
    public void enableTeleportRequestsCommand(CommandArguments arguments) {
        requestManager.setRequestsEnabled(arguments.getSender(), true);
    }

    @Completer(name = "tpa", permission = "smplugin.command.tpa")
    public List<String> completeTpa(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            List<String> suggestions = new ArrayList<>(helper.playerNames());

            if (arguments.getSender() instanceof Player player) {
                suggestions.add(requestManager.areRequestsEnabled(player) ? "disable" : "enable");
            }

            return helper.copyMatches(0, suggestions);
        }

        return helper.empty();
    }

    @Completer(name = "tpaccept", permission = "smplugin.command.tpa")
    public List<String> completeTpAccept(CommandArguments arguments, CompleterHelper helper) {
        return completePendingRequester(arguments, helper);
    }

    @Completer(name = "tpreject", permission = "smplugin.command.tpa")
    public List<String> completeTpReject(CommandArguments arguments, CompleterHelper helper) {
        return completePendingRequester(arguments, helper);
    }

    private List<String> completePendingRequester(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() != 1) {
            return helper.empty();
        }

        Player target = arguments.getSender();
        return helper.copyMatches(0, requestManager.getRequesterNames(target));
    }
}
