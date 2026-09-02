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
package dev.despical.smplugin.chat;

import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class PrivateMessageManager {

    private final MessageService messages;
    private final ChatHistoryManager historyManager;

    private final Set<UUID> spies = new HashSet<>();
    private final Map<UUID, UUID> replyTargets = new HashMap<>();

    public PrivateMessageManager(SMPlugin plugin) {
        this.historyManager = plugin.getChatHistoryManager();
        this.messages = plugin.getMessageService();
    }

    public void send(Player sender, Player recipient, String message) {
        Variable[] variables = messageVariables(sender, recipient, message);
        sender.sendMessage(messages.component("private-messages.formats.sender", variables));
        recipient.sendMessage(messages.component("private-messages.formats.recipient", variables));

        replyTargets.put(sender.getUniqueId(), recipient.getUniqueId());
        replyTargets.put(recipient.getUniqueId(), sender.getUniqueId());
        historyManager.recordPrivate(sender, recipient, message);

        for (UUID spyId : Set.copyOf(spies)) {
            Player spy = Bukkit.getPlayer(spyId);

            if (spy == null) {
                spies.remove(spyId);
            } else if (!spy.equals(sender) && !spy.equals(recipient)) {
                spy.sendMessage(messages.component("private-messages.formats.spy", variables));
            }
        }
    }

    private Variable[] messageVariables(Player sender, Player recipient, String message) {
        return new Variable[]{
            Variable.of("%sender%", messages.escape(sender.getName())),
            Variable.of("%recipient%", messages.escape(recipient.getName())),
            Variable.of("%message%", messages.escape(message))
        };
    }

    public Player getReplyTarget(Player player) {
        UUID targetId = replyTargets.get(player.getUniqueId());
        return targetId == null ? null : Bukkit.getPlayer(targetId);
    }

    public boolean toggleSpy(Player player) {
        if (!spies.add(player.getUniqueId())) {
            spies.remove(player.getUniqueId());
            return false;
        }

        return true;
    }
}
