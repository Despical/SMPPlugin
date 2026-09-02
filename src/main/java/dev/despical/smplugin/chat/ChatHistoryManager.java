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
package dev.despical.smplugin.chat;

import dev.despical.smplugin.SMPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 18.08.2026
 */
public final class ChatHistoryManager {

    public static final int MAX_ENTRIES = 50;

    private final SMPlugin plugin;
    private final File storageFile;
    private final Map<UUID, PlayerHistory> histories = new LinkedHashMap<>();

    public ChatHistoryManager(SMPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "chat-history.yml");
        this.load();
    }

    public synchronized void recordPublic(Player sender, String message) {
        record(sender, new ChatEntry(Instant.now().toEpochMilli(), ChatType.PUBLIC, null, message));
    }

    public synchronized void recordPrivate(Player sender, Player recipient, String message) {
        record(sender, new ChatEntry(Instant.now().toEpochMilli(), ChatType.PRIVATE, recipient.getName(), message));
    }

    public synchronized Optional<PlayerHistorySnapshot> findHistory(String playerName) {
        return histories.entrySet().stream()
            .filter(entry -> entry.getValue().name().equalsIgnoreCase(playerName))
            .findFirst()
            .map(entry -> new PlayerHistorySnapshot(
                entry.getKey(),
                entry.getValue().name(),
                List.copyOf(entry.getValue().entries())
            ));
    }

    public synchronized Set<String> getKnownPlayerNames() {
        return histories.values().stream()
            .map(PlayerHistory::name)
            .collect(Collectors.toUnmodifiableSet());
    }

    public synchronized void save() {
        YamlConfiguration configuration = new YamlConfiguration();

        for (var historyEntry : histories.entrySet()) {
            String path = "players." + historyEntry.getKey();
            PlayerHistory history = historyEntry.getValue();
            configuration.set(path + ".name", history.name());

            List<Map<String, Object>> serializedEntries = new ArrayList<>();

            for (ChatEntry entry : history.entries()) {
                Map<String, Object> serialized = new LinkedHashMap<>();
                serialized.put("timestamp", entry.timestamp());
                serialized.put("type", entry.type().name());

                if (entry.recipient() != null) {
                    serialized.put("recipient", entry.recipient());
                }

                serialized.put("message", entry.message());
                serializedEntries.add(serialized);
            }

            configuration.set(path + ".entries", serializedEntries);
        }

        try {
            configuration.save(storageFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save chat history: " + exception.getMessage());
        }
    }

    private void record(Player sender, ChatEntry entry) {
        PlayerHistory history = histories.computeIfAbsent(
            sender.getUniqueId(),
            ignored -> new PlayerHistory(sender.getName(), new ArrayDeque<>())
        );

        history.setName(sender.getName());
        history.entries().addLast(entry);

        while (history.entries().size() > MAX_ENTRIES) {
            history.entries().removeFirst();
        }
    }

    private void load() {
        if (!storageFile.isFile()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(storageFile);
        var players = configuration.getConfigurationSection("players");

        if (players == null) {
            return;
        }

        for (String uuidText : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                String path = "players." + uuidText;
                String name = configuration.getString(path + ".name", uuidText);
                Deque<ChatEntry> entries = new ArrayDeque<>();

                for (Map<?, ?> serialized : configuration.getMapList(path + ".entries")) {
                    Object timestampValue = serialized.get("timestamp");
                    Object typeValue = serialized.get("type");
                    Object messageValue = serialized.get("message");

                    if (!(timestampValue instanceof Number timestamp) || typeValue == null || messageValue == null) {
                        continue;
                    }

                    ChatType type = ChatType.valueOf(typeValue.toString().toUpperCase(Locale.ROOT));
                    Object recipientValue = serialized.get("recipient");
                    entries.addLast(new ChatEntry(
                        timestamp.longValue(),
                        type,
                        recipientValue == null ? null : recipientValue.toString(),
                        messageValue.toString()
                    ));
                }

                while (entries.size() > MAX_ENTRIES) {
                    entries.removeFirst();
                }

                histories.put(uuid, new PlayerHistory(name, entries));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipped an invalid chat history entry: " + uuidText);
            }
        }
    }

    public enum ChatType {
        PUBLIC,
        PRIVATE
    }

    public record ChatEntry(long timestamp, ChatType type, String recipient, String message) {
    }

    public record PlayerHistorySnapshot(UUID uuid, String name, List<ChatEntry> entries) {
    }

    private static final class PlayerHistory {

        private final Deque<ChatEntry> entries;
        private String name;

        private PlayerHistory(String name, Deque<ChatEntry> entries) {
            this.name = name;
            this.entries = entries;
        }

        private String name() {
            return name;
        }

        private void setName(String name) {
            this.name = name;
        }

        private Deque<ChatEntry> entries() {
            return entries;
        }
    }
}
