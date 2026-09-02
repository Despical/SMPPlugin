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
package dev.despical.smplugin.command.chat;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.CompleterHelper;
import dev.despical.commandframework.annotations.Command;
import dev.despical.commandframework.annotations.Completer;
import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.chat.PrivateMessageManager;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class PrivateMessageCommands {

    private final SMPlugin plugin = JavaPlugin.getPlugin(SMPlugin.class);
    private final PrivateMessageManager messageManager = plugin.getPrivateMessageManager();
    private final MessageService messages = plugin.getMessageService();

    @Command(
        name = "msg",
        aliases = {"w", "whisper"},
        usage = "/msg <player> <message>",
        desc = "Sends a private message to an online player.",
        permission = "smplugin.command.msg",
        min = 2,
        senderType = Command.SenderType.PLAYER
    )
    public void messageCommand(CommandArguments arguments) {
        Player sender = arguments.getSender();
        Player target = arguments.getPlayer(0).orElse(null);

        if (target == null) {
            messages.send(
                sender,
                "private-messages.messages.player-not-found",
                Variable.of("%player%", messages.escape(arguments.getFirst()))
            );
            return;
        }

        if (target.equals(sender)) {
            messages.send(sender, "private-messages.messages.cannot-message-self");
            return;
        }

        messageManager.send(sender, target, arguments.concatRangeOf(1, arguments.getLength()));
    }

    @Command(
        name = "r",
        usage = "/r <message>",
        desc = "Replies to the last private-message conversation.",
        permission = "smplugin.command.msg",
        min = 1,
        senderType = Command.SenderType.PLAYER
    )
    public void replyCommand(CommandArguments arguments) {
        Player sender = arguments.getSender();
        Player target = messageManager.getReplyTarget(sender);

        if (target == null) {
            messages.send(sender, "private-messages.messages.reply-target-offline");
            return;
        }

        messageManager.send(sender, target, arguments.concatArguments());
    }

    @Command(
        name = "spy",
        usage = "/spy",
        desc = "Toggles global private-message spying.",
        permission = "smplugin.command.spy",
        max = 0,
        senderType = Command.SenderType.PLAYER
    )
    public void spyCommand(CommandArguments arguments) {
        boolean enabled = messageManager.toggleSpy(arguments.getSender());
        messages.send(
            arguments.getSender(),
            enabled ? "private-messages.messages.spy-enabled" : "private-messages.messages.spy-disabled"
        );
    }

    @Completer(name = "msg", aliases = {"w", "whisper"}, permission = "smplugin.command.msg")
    public List<String> completeMessage(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, helper.playerNames());
        }

        return helper.empty();
    }
}
