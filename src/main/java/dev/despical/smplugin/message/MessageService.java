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
package dev.despical.smplugin.message;

import dev.despical.smplugin.SMPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class MessageService {

    private final SMPlugin plugin;
    private final MiniMessage miniMessage;

    public MessageService(SMPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void send(CommandSender recipient, String path, Variable... variables) {
        List<String> lines = plugin.getConfig().isList(path)
            ? plugin.getConfig().getStringList(path)
            : List.of(plugin.getConfig().getString(path, ""));

        for (String line : lines) {
            if (!line.isEmpty()) {
                recipient.sendMessage(parse(line, variables));
            } else {
                recipient.sendMessage("");
            }
        }
    }

    public void sendActionBar(Player recipient, String path, Variable... variables) {
        String message = plugin.getConfig().getString(path, "");
        if (!message.isEmpty()) {
            recipient.sendActionBar(parse(message, variables));
        }
    }

    public Component component(String path, Variable... variables) {
        return parse(plugin.getConfig().getString(path, ""), variables);
    }

    public Component parse(String message, Variable... variables) {
        for (Variable variable : variables) {
            message = message.replace(variable.key(), variable.value());
        }

        return miniMessage.deserialize(message);
    }

    public String escape(String input) {
        return miniMessage.escapeTags(input);
    }

    public record Variable(String key, String value) {

        public static Variable of(String key, Object value) {
            return new Variable(key, String.valueOf(value));
        }
    }
}
