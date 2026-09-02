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
import dev.despical.smplugin.chat.ChatHistoryManager;
import dev.despical.smplugin.chat.ChatHistoryManager.ChatEntry;
import dev.despical.smplugin.chat.ChatHistoryManager.PlayerHistorySnapshot;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class ChatHistoryCommands {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private final SMPlugin plugin = JavaPlugin.getPlugin(SMPlugin.class);
    private final ChatHistoryManager historyManager = plugin.getChatHistoryManager();
    private final MessageService messages = plugin.getMessageService();

    @Command(
        name = "chatlog",
        usage = "/chatlog <player>",
        desc = "Shows a player's last 50 public and private messages.",
        permission = "smplugin.command.chatlog",
        min = 1,
        max = 1
    )
    public void chatLogCommand(CommandArguments arguments) {
        PlayerHistorySnapshot history = historyManager.findHistory(arguments.getFirst()).orElse(null);
        if (history == null || history.entries().isEmpty()) {
            messages.send(
                arguments.getSender(),
                "chatlog.messages.no-history",
                Variable.of("%player%", messages.escape(arguments.getFirst()))
            );
            return;
        }

        messages.send(
            arguments.getSender(),
            "chatlog.messages.header",
            Variable.of("%count%", history.entries().size()),
            Variable.of("%player%", messages.escape(history.name()))
        );

        for (ChatEntry entry : history.entries()) {
            String path = entry.type() == ChatHistoryManager.ChatType.PUBLIC
                ? "chatlog.messages.chat-entry"
                : "chatlog.messages.private-entry";
            arguments.sendMessage(messages.component(
                path,
                Variable.of("%timestamp%", TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp()))),
                Variable.of("%player%", messages.escape(history.name())),
                Variable.of("%recipient%", messages.escape(entry.recipient() == null ? "?" : entry.recipient())),
                Variable.of("%message%", messages.escape(entry.message()))
            ));
        }
    }

    @Completer(
        name = "chatlog",
        permission = "smplugin.command.chatlog"
    )
    public List<String> completeChatLog(CommandArguments arguments, CompleterHelper helper) {
        if (arguments.getLength() == 1) {
            return helper.copyMatches(0, historyManager.getKnownPlayerNames());
        }

        return helper.empty();
    }
}
