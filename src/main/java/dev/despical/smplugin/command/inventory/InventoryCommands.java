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
package dev.despical.smplugin.command.inventory;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.CommandErrorMessage;
import dev.despical.commandframework.CompleterHelper;
import dev.despical.commandframework.annotations.Command;
import dev.despical.commandframework.annotations.Completer;
import dev.despical.commandframework.annotations.Flag;
import dev.despical.commandframework.annotations.Option;
import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.inventory.ReadOnlyInventoryManager;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class InventoryCommands {

    private final SMPlugin plugin = JavaPlugin.getPlugin(SMPlugin.class);
    private final MessageService messages = plugin.getMessageService();
    private final ReadOnlyInventoryManager inventoryManager = plugin.getReadOnlyInventoryManager();

    @Command(
        name = "invsee",
        usage = "/invsee <player> [--viewer=<player>] [--viewOnly]",
        desc = "Opens an online player's inventory for you or another online player.",
        permission = "smplugin.command.invsee",
        min = 1,
        max = 3
    )
    @Option(value = "viewer", allowSeparating = false)
    @Flag({"viewOnly"})
    public void inventorySeeCommand(CommandArguments arguments) {
        CommandSender sender = arguments.getSender();
        if (!hasValidArguments(arguments)) {
            arguments.sendMessage(CommandErrorMessage.LONG_ARG_SIZE);
            return;
        }

        Player target = resolveTarget(arguments, sender);
        if (target == null) {
            return;
        }

        Player viewer = resolveViewer(arguments, sender);
        if (viewer == null) {
            return;
        }

        inventoryManager.open(viewer, target.getInventory(), arguments.isFlagPresent("viewOnly"));
        messages.send(
            sender,
            "inventory.messages.opened",
            Variable.of("%viewer%", messages.escape(viewer.getName())),
            Variable.of("%target%", messages.escape(target.getName()))
        );
    }

    @Command(
        name = "endersee",
        usage = "/endersee <player> [--viewer=<player>] [--viewOnly]",
        desc = "Opens an online player's Ender Chest for you or another online player.",
        permission = "smplugin.command.endersee",
        min = 1,
        max = 3
    )
    @Option(value = "viewer", allowSeparating = false)
    @Flag({"viewOnly"})
    public void enderSeeCommand(CommandArguments arguments) {
        CommandSender sender = arguments.getSender();
        if (!hasValidArguments(arguments)) {
            arguments.sendMessage(CommandErrorMessage.LONG_ARG_SIZE);
            return;
        }

        Player target = resolveTarget(arguments, sender);
        if (target == null) {
            return;
        }

        Player viewer = resolveViewer(arguments, sender);
        if (viewer == null) {
            return;
        }

        inventoryManager.open(viewer, target.getEnderChest(), arguments.isFlagPresent("viewOnly"));
        messages.send(
            sender,
            "inventory.messages.ender-opened",
            Variable.of("%viewer%", messages.escape(viewer.getName())),
            Variable.of("%target%", messages.escape(target.getName()))
        );
    }

    @Completer(name = "invsee", permission = "smplugin.command.invsee")
    public List<String> completeInvSee(CommandArguments arguments, CompleterHelper helper) {
        return completeInventoryViewer(arguments, helper);
    }

    @Completer(name = "endersee", permission = "smplugin.command.endersee")
    public List<String> completeEnderSee(CommandArguments arguments, CompleterHelper helper) {
        return completeInventoryViewer(arguments, helper);
    }

    private Player resolveTarget(CommandArguments arguments, CommandSender sender) {
        Player target = arguments.getPlayer(0).orElse(null);

        if (target == null) {
            messages.send(
                sender,
                "inventory.messages.player-not-found",
                Variable.of("%player%", messages.escape(arguments.getFirst()))
            );
        }

        return target;
    }

    private Player resolveViewer(CommandArguments arguments, CommandSender sender) {
        List<String> viewerOption = arguments.getOption("viewer");

        if (viewerOption == null) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "inventory.messages.console-viewer-required");
                return null;
            }

            return player;
        }

        String viewerName = viewerOption.isEmpty() ? "" : viewerOption.getFirst();
        Player viewer = arguments.getPlayer(viewerName).orElse(null);

        if (viewer == null) {
            messages.send(
                sender,
                "inventory.messages.viewer-not-found",
                Variable.of("%viewer%", messages.escape(viewerName))
            );
        }

        return viewer;
    }

    private List<String> completeInventoryViewer(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, helper.playerNames());
        }

        if (arguments.getLength() == 2) {
            List<String> suggestions = new java.util.ArrayList<>(
                helper.playerNames().stream().map(name -> "--viewer=" + name).toList()
            );

            suggestions.add("--viewOnly");
            return helper.copyMatches(1, suggestions);
        }

        if (arguments.getLength() == 3) {
            if ("--viewOnly".equals(arguments.getArgument(1))) {
                return helper.copyMatches(2, helper.playerNames().stream().map(name -> "--viewer=" + name).toList());
            }

            return helper.copyMatches(2, List.of("--viewOnly"));
        }

        return helper.empty();
    }

    private boolean hasValidArguments(CommandArguments arguments) {
        boolean foundViewer = false;
        boolean foundViewOnly = false;

        for (int index = 1; index < arguments.getLength(); index++) {
            String argument = arguments.getArgument(index);

            if ("--viewOnly".equals(argument)) {
                if (foundViewOnly) {
                    return false;
                }

                foundViewOnly = true;
            } else if (argument.startsWith("--viewer=")) {
                if (foundViewer) {
                    return false;
                }

                foundViewer = true;
            } else {
                return false;
            }
        }

        return true;
    }
}
