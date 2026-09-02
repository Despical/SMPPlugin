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
package dev.despical.smplugin.command.movement;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.CommandErrorMessage;
import dev.despical.commandframework.CompleterHelper;
import dev.despical.commandframework.annotations.Command;
import dev.despical.commandframework.annotations.Completer;
import dev.despical.commandframework.annotations.Flag;
import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class MovementCommands {

    private final SMPlugin plugin = JavaPlugin.getPlugin(SMPlugin.class);
    private final MessageService messages = plugin.getMessageService();

    @Command(
        name = "speed",
        usage = "/speed <0-10> [player] [--walk|--fly]",
        desc = "Changes walking or flying speed for yourself or another player.",
        permission = "smplugin.command.speed",
        min = 1,
        max = 3
    )
    @Flag({"walk", "fly"})
    public void speedCommand(CommandArguments arguments) {
        CommandSender sender = arguments.getSender();
        Float level = parseLevel(arguments.getFirst());

        if (level == null) {
            messages.send(sender, "speed.messages.invalid-number");
            return;
        }

        if (!Float.isFinite(level) || level < 0.0F || level > 10.0F) {
            messages.send(sender, "speed.messages.out-of-range");
            return;
        }

        boolean forceWalk = arguments.isFlagPresent("walk");
        boolean forceFly = arguments.isFlagPresent("fly");

        if (forceWalk && forceFly) {
            messages.send(sender, "speed.messages.conflicting-flags");
            return;
        }

        String targetName = findTargetName(arguments);
        if (targetName == null && hasInvalidExtraArgument(arguments)) {
            arguments.sendMessage(CommandErrorMessage.LONG_ARG_SIZE);
            return;
        }

        Player target;
        if (targetName == null) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "speed.messages.console-target-required");
                return;
            }

            target = player;
        } else {
            target = arguments.getPlayer(targetName).orElse(null);

            if (target == null) {
                messages.send(
                    sender,
                    "speed.messages.player-not-found",
                    Variable.of("%player%", messages.escape(targetName))
                );
                return;
            }
        }

        boolean flightSpeed = forceFly || (!forceWalk && target.isFlying());
        float bukkitSpeed = level / 10.0F;

        if (flightSpeed) {
            target.setFlySpeed(bukkitSpeed);
        } else {
            target.setWalkSpeed(bukkitSpeed);
        }

        String displayedLevel = BigDecimal.valueOf(level.doubleValue()).stripTrailingZeros().toPlainString();
        String mode = plugin.getConfig().getString(
            flightSpeed ? "speed.labels.fly" : "speed.labels.walk",
            flightSpeed ? "flight" : "walking"
        );

        messages.send(
            sender,
            "speed.messages.changed",
            Variable.of("%target%", messages.escape(target.getName())),
            Variable.of("%mode%", messages.escape(mode)),
            Variable.of("%speed%", displayedLevel)
        );

        if (!sender.equals(target)) {
            messages.send(
                target,
                "speed.messages.changed-by-other",
                Variable.of("%sender%", messages.escape(sender.getName())),
                Variable.of("%mode%", messages.escape(mode)),
                Variable.of("%speed%", displayedLevel)
            );
        }
    }

    @Completer(name = "speed", permission = "smplugin.command.speed")
    public List<String> completeSpeed(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, List.of("0", "1", "2", "3", "5", "10"));
        }

        if (arguments.getLength() == 2) {
            List<String> suggestions = new ArrayList<>(helper.playerNames());
            suggestions.add("--walk");
            suggestions.add("--fly");
            return helper.copyMatches(1, suggestions);
        }

        if (arguments.getLength() == 3) {
            if (isSpeedFlag(arguments.getArgument(1))) {
                return helper.copyMatches(2, helper.playerNames());
            }

            return helper.copyMatches(2, List.of("--walk", "--fly"));
        }

        return helper.empty();
    }

    private Float parseLevel(String input) {
        try {
            return Float.parseFloat(input);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String findTargetName(CommandArguments arguments) {
        String targetName = null;

        for (int index = 1; index < arguments.getLength(); index++) {
            String argument = arguments.getArgument(index);

            if (isSpeedFlag(argument)) {
                continue;
            }

            if (argument.startsWith("--") || targetName != null) {
                return null;
            }

            targetName = argument;
        }

        return targetName;
    }

    private boolean hasInvalidExtraArgument(CommandArguments arguments) {
        int nonFlagArguments = 0;

        for (int index = 1; index < arguments.getLength(); index++) {
            String argument = arguments.getArgument(index);

            if (!isSpeedFlag(argument)) {
                nonFlagArguments++;

                if (argument.startsWith("--")) {
                    return true;
                }
            }
        }

        return nonFlagArguments > 1;
    }

    private boolean isSpeedFlag(String argument) {
        return "--walk".equals(argument) || "--fly".equals(argument);
    }
}
