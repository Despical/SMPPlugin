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
package dev.despical.smplugin.inventory;

import dev.despical.smplugin.message.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class ReadOnlyInventoryManager implements Listener {

    private final MessageService messages;
    private final Set<UUID> readOnlyViewers = new HashSet<>();

    public ReadOnlyInventoryManager(MessageService messages) {
        this.messages = messages;
    }

    public void open(Player viewer, Inventory inventory, boolean viewOnly) {
        viewer.openInventory(inventory);

        if (viewOnly) {
            readOnlyViewers.add(viewer.getUniqueId());
            messages.send(viewer, "inventory.messages.view-only");
        } else {
            readOnlyViewers.remove(viewer.getUniqueId());
        }
    }

    public void shutdown() {
        readOnlyViewers.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (readOnlyViewers.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (readOnlyViewers.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        readOnlyViewers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        readOnlyViewers.remove(event.getPlayer().getUniqueId());
    }
}
