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
package dev.despical.smplugin.death;

import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 19.08.2026
 */
public final class DeathLocationManager implements Listener {

    private static final String DEFAULT_DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";

    private final SMPlugin plugin;
    private final MessageService messages;
    private final File storageFile;
    private final Set<UUID> pendingRespawnMessages = new HashSet<>();
    private final Map<UUID, DeathRecord> deathRecords = new HashMap<>();

    public DeathLocationManager(SMPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageService();
        this.storageFile = new File(plugin.getDataFolder(), "death-locations.yml");
        this.load();
    }

    public synchronized Optional<DeathRecord> findByName(String playerName) {
        return deathRecords.values().stream()
            .filter(record -> record.playerName().equalsIgnoreCase(playerName))
            .findFirst();
    }

    public synchronized Set<String> getKnownPlayerNames() {
        return deathRecords.values().stream()
            .map(DeathRecord::playerName)
            .collect(Collectors.toUnmodifiableSet());
    }

    public void send(CommandSender recipient, String messagePath, DeathRecord record) {
        messages.send(recipient, messagePath, variables(record));
    }

    public synchronized void save() {
        YamlConfiguration configuration = new YamlConfiguration();

        for (DeathRecord record : deathRecords.values()) {
            String path = "players." + record.playerId();
            configuration.set(path + ".name", record.playerName());
            configuration.set(path + ".timestamp", record.timestamp());
            configuration.set(path + ".world", record.world());
            configuration.set(path + ".x", record.x());
            configuration.set(path + ".y", record.y());
            configuration.set(path + ".z", record.z());
        }

        try {
            configuration.save(storageFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save death locations: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location location = player.getLocation();
        DeathRecord record = new DeathRecord(
            player.getUniqueId(),
            player.getName(),
            Instant.now().toEpochMilli(),
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );

        synchronized (this) {
            deathRecords.put(player.getUniqueId(), record);

            if (plugin.getConfig().getBoolean("death-location.enabled", true)) {
                pendingRespawnMessages.add(player.getUniqueId());
            } else {
                pendingRespawnMessages.remove(player.getUniqueId());
            }

            save();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        DeathRecord record;

        synchronized (this) {
            if (!pendingRespawnMessages.remove(event.getPlayer().getUniqueId())) {
                return;
            }

            record = deathRecords.get(event.getPlayer().getUniqueId());
        }

        if (record != null && plugin.getConfig().getBoolean("death-location.enabled", true)) {
            send(event.getPlayer(), "death-location.message", record);
        }
    }

    private Variable[] variables(DeathRecord record) {
        return new Variable[]{
            Variable.of("%player%", messages.escape(record.playerName())),
            Variable.of("%time%", messages.escape(formatTimestamp(record.timestamp()))),
            Variable.of("%world%", messages.escape(record.world())),
            Variable.of("%x%", record.x()),
            Variable.of("%y%", record.y()),
            Variable.of("%z%", record.z())
        };
    }

    private String formatTimestamp(long timestamp) {
        String pattern = plugin.getConfig().getString("death-location.date-format", DEFAULT_DATE_FORMAT);
        try {
            return DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestamp));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            plugin.getLogger().warning("Invalid death-location.date-format; using the default format.");
            return DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestamp));
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
                UUID playerId = UUID.fromString(uuidText);
                String path = "players." + uuidText;
                String playerName = configuration.getString(path + ".name");
                String world = configuration.getString(path + ".world");
                if (playerName == null || world == null || !configuration.isLong(path + ".timestamp")) {
                    continue;
                }

                deathRecords.put(playerId, new DeathRecord(
                    playerId,
                    playerName,
                    configuration.getLong(path + ".timestamp"),
                    world,
                    configuration.getInt(path + ".x"),
                    configuration.getInt(path + ".y"),
                    configuration.getInt(path + ".z")
                ));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipped an invalid death location entry: " + uuidText);
            }
        }
    }

    public record DeathRecord(
        UUID playerId,
        String playerName,
        long timestamp,
        String world,
        int x,
        int y,
        int z
    ) {
    }
}
