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
package dev.despical.smplugin.listener;

import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.chat.ChatHistoryManager;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class PublicChatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final ChatHistoryManager historyManager;
    private final MessageService messages;

    public PublicChatListener(SMPlugin plugin) {
        this.historyManager = plugin.getChatHistoryManager();
        this.messages = plugin.getMessageService();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        historyManager.recordPublic(event.getPlayer(), PLAIN_TEXT.serialize(event.message()));
        event.renderer((source, sourceDisplayName, message, viewer) -> messages.component(
            source.isOp() ? "chat.formats.admin" : "chat.formats.player",
            Variable.of("%player%", messages.escape(source.getName())),
            Variable.of("%message%", messages.escape(PLAIN_TEXT.serialize(message)))
        ));
    }
}
