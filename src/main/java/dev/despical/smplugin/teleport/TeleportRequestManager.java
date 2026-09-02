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
package dev.despical.smplugin.teleport;

import dev.despical.smplugin.SMPlugin;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class TeleportRequestManager implements Listener {

    private final SMPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, LinkedHashMap<UUID, TeleportRequest>> requestsByTarget;
    private final Map<UUID, PendingTeleport> pendingTeleports;
    private final Set<UUID> disabledRequests;

    public TeleportRequestManager(SMPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageService();
        this.requestsByTarget = new HashMap<>();
        this.pendingTeleports = new HashMap<>();
        this.disabledRequests = new HashSet<>();
    }

    public void sendRequest(Player requester, Player target) {
        if (requester.equals(target)) {
            messages.send(requester, "tpa.messages.cannot-request-self");
            return;
        }

        if (disabledRequests.contains(target.getUniqueId())) {
            messages.send(
                requester,
                "tpa.messages.target-requests-disabled",
                Variable.of("%target%", target.getName())
            );
            return;
        }

        TeleportRequest outgoing = findOutgoingRequest(requester.getUniqueId());
        if (outgoing != null && !outgoing.targetId().equals(target.getUniqueId())) {
            Player currentTarget = Bukkit.getPlayer(outgoing.targetId());
            String currentTargetName = currentTarget == null ? "another player" : currentTarget.getName();
            messages.send(
                requester,
                "tpa.messages.outgoing-request-pending",
                Variable.of("%target%", currentTargetName)
            );
            return;
        }

        var targetRequests = requestsByTarget.computeIfAbsent(
            target.getUniqueId(),
            ignored -> new LinkedHashMap<>()
        );

        TeleportRequest previous = targetRequests.remove(requester.getUniqueId());
        if (previous != null) {
            previous.cancelTimeout();
        }

        int timeoutSeconds = Math.max(1, plugin.getConfig().getInt("tpa.timeout-seconds", 60));
        TeleportRequest request = new TeleportRequest(requester.getUniqueId(), requester.getName(), target.getUniqueId());
        targetRequests.put(requester.getUniqueId(), request);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(
            plugin,
            () -> expire(request),
            timeoutSeconds * 20L
        );
        request.setTimeoutTask(task);

        messages.send(
            requester,
            "tpa.messages.request-sent",
            Variable.of("%target%", target.getName()),
            Variable.of("%timeout%", timeoutSeconds)
        );
        messages.send(
            target,
            "tpa.messages.request-received",
            Variable.of("%requester%", requester.getName()),
            Variable.of("%timeout%", timeoutSeconds)
        );
    }

    public void accept(Player target, String requesterName) {
        TeleportRequest request = takeRequest(target, requesterName);
        if (request == null) {
            messages.send(target, "tpa.messages.no-pending-request");
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null) {
            messages.send(target, "tpa.messages.requester-offline", Variable.of("%requester%", request.requesterName()));
            return;
        }

        messages.send(target, "tpa.messages.request-accepted-target", Variable.of("%requester%", requester.getName()));
        messages.send(requester, "tpa.messages.request-accepted-requester", Variable.of("%target%", target.getName()));

        startTeleportCountdown(requester, target);
    }

    public void reject(Player target, String requesterName) {
        TeleportRequest request = takeRequest(target, requesterName);
        if (request == null) {
            messages.send(target, "tpa.messages.no-pending-request");
            return;
        }

        messages.send(target, "tpa.messages.request-rejected-target", Variable.of("%requester%", request.requesterName()));

        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null) {
            messages.send(requester, "tpa.messages.request-rejected-requester", Variable.of("%target%", target.getName()));
        }
    }

    public List<String> getRequesterNames(Player target) {
        var requests = requestsByTarget.get(target.getUniqueId());
        if (requests == null) {
            return List.of();
        }

        return requests.values().stream().map(TeleportRequest::requesterName).toList();
    }

    public void setRequestsEnabled(Player player, boolean enabled) {
        UUID playerId = player.getUniqueId();
        if (enabled) {
            disabledRequests.remove(playerId);
            messages.send(player, "tpa.messages.requests-enabled");
            return;
        }

        disabledRequests.add(playerId);

        var incoming = requestsByTarget.remove(playerId);
        if (incoming != null) {
            for (TeleportRequest request : incoming.values()) {
                request.cancelTimeout();

                Player requester = Bukkit.getPlayer(request.requesterId());
                if (requester != null) {
                    messages.send(
                        requester,
                        "tpa.messages.target-requests-disabled",
                        Variable.of("%target%", player.getName())
                    );
                }
            }
        }

        messages.send(player, "tpa.messages.requests-disabled");
    }

    public boolean areRequestsEnabled(Player player) {
        return !disabledRequests.contains(player.getUniqueId());
    }

    public void shutdown() {
        requestsByTarget.values().stream()
            .flatMap(requests -> requests.values().stream())
            .forEach(TeleportRequest::cancelTimeout);
        requestsByTarget.clear();

        pendingTeleports.values().forEach(PendingTeleport::cancelTask);
        pendingTeleports.clear();
        disabledRequests.clear();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) {
            return;
        }

        Player requester = event.getPlayer();
        PendingTeleport pending = pendingTeleports.remove(requester.getUniqueId());

        if (pending == null) {
            return;
        }

        pending.cancelTask();
        messages.sendActionBar(requester, "tpa.messages.teleport-cancelled-moved-actionbar");

        Player target = Bukkit.getPlayer(pending.targetId());
        if (target != null) {
            messages.send(
                target,
                "tpa.messages.teleport-cancelled-target",
                Variable.of("%requester%", requester.getName())
            );
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        PendingTeleport ownTeleport = pendingTeleports.remove(playerId);
        if (ownTeleport != null) {
            ownTeleport.cancelTask();

            Player target = Bukkit.getPlayer(ownTeleport.targetId());
            if (target != null) {
                messages.send(target, "tpa.messages.requester-unavailable", Variable.of("%requester%", player.getName()));
            }
        }

        for (PendingTeleport pending : new ArrayList<>(pendingTeleports.values())) {
            if (!pending.targetId().equals(playerId) || !pendingTeleports.remove(pending.requesterId(), pending)) {
                continue;
            }

            pending.cancelTask();

            Player requester = Bukkit.getPlayer(pending.requesterId());
            if (requester != null) {
                messages.sendActionBar(
                    requester,
                    "tpa.messages.teleport-cancelled-target-left-actionbar",
                    Variable.of("%target%", player.getName())
                );
            }
        }

        LinkedHashMap<UUID, TeleportRequest> incoming = requestsByTarget.remove(playerId);
        if (incoming != null) {
            for (TeleportRequest request : incoming.values()) {
                request.cancelTimeout();

                Player requester = Bukkit.getPlayer(request.requesterId());
                if (requester != null) {
                    messages.send(requester, "tpa.messages.target-unavailable", Variable.of("%target%", player.getName()));
                }
            }
        }

        for (var entry : new ArrayList<>(requestsByTarget.entrySet())) {
            TeleportRequest request = entry.getValue().remove(playerId);

            if (request != null) {
                request.cancelTimeout();

                Player target = Bukkit.getPlayer(entry.getKey());
                if (target != null) {
                    messages.send(target, "tpa.messages.requester-unavailable", Variable.of("%requester%", player.getName()));
                }
            }

            if (entry.getValue().isEmpty()) {
                requestsByTarget.remove(entry.getKey());
            }
        }
    }

    private TeleportRequest takeRequest(Player target, String requesterName) {
        var requests = requestsByTarget.get(target.getUniqueId());
        if (requests == null || requests.isEmpty()) {
            return null;
        }

        TeleportRequest selected = null;

        if (requesterName == null) {
            for (TeleportRequest request : requests.values()) {
                selected = request;
            }
        } else {
            for (TeleportRequest request : requests.values()) {
                if (request.requesterName().equalsIgnoreCase(requesterName)) {
                    selected = request;
                    break;
                }
            }
        }

        if (selected == null) {
            return null;
        }

        requests.remove(selected.requesterId());
        selected.cancelTimeout();

        if (requests.isEmpty()) {
            requestsByTarget.remove(target.getUniqueId());
        }

        return selected;
    }

    private TeleportRequest findOutgoingRequest(UUID requesterId) {
        for (var requests : requestsByTarget.values()) {
            TeleportRequest request = requests.get(requesterId);

            if (request != null) {
                return request;
            }
        }

        return null;
    }

    private void startTeleportCountdown(Player requester, Player target) {
        PendingTeleport previous = pendingTeleports.remove(requester.getUniqueId());
        if (previous != null) {
            previous.cancelTask();
        }

        int delaySeconds = Math.max(0, plugin.getConfig().getInt("tpa.teleport-delay-seconds", 3));
        PendingTeleport pending = new PendingTeleport(requester.getUniqueId(), target.getUniqueId(), delaySeconds);

        if (delaySeconds == 0) {
            completeTeleport(pending);
            return;
        }

        pendingTeleports.put(requester.getUniqueId(), pending);
        sendCountdownActionBar(requester, delaySeconds);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickTeleport(pending), 20L, 20L);
        pending.setTask(task);
    }

    private void tickTeleport(PendingTeleport pending) {
        if (pendingTeleports.get(pending.requesterId()) != pending) {
            pending.cancelTask();
            return;
        }

        int secondsRemaining = pending.decrementSecondsRemaining();
        if (secondsRemaining <= 0) {
            completeTeleport(pending);
            return;
        }

        Player requester = Bukkit.getPlayer(pending.requesterId());
        if (requester != null) {
            sendCountdownActionBar(requester, secondsRemaining);
        }
    }

    private void completeTeleport(PendingTeleport pending) {
        pendingTeleports.remove(pending.requesterId(), pending);
        pending.cancelTask();

        Player requester = Bukkit.getPlayer(pending.requesterId());
        Player target = Bukkit.getPlayer(pending.targetId());

        if (requester == null) {
            return;
        }

        if (target == null) {
            messages.sendActionBar(requester, "tpa.messages.teleport-cancelled-target-left-actionbar");
            return;
        }

        boolean teleported = requester.teleport(target.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        if (!teleported) {
            messages.send(target, "tpa.messages.teleport-failed", Variable.of("%requester%", requester.getName()));
            messages.send(requester, "tpa.messages.teleport-failed", Variable.of("%requester%", requester.getName()));
            messages.sendActionBar(requester, "tpa.messages.teleport-failed-actionbar");
            return;
        }

        messages.sendActionBar(requester, "tpa.messages.teleport-complete-actionbar");
    }

    private void sendCountdownActionBar(Player requester, int secondsRemaining) {
        messages.sendActionBar(
            requester,
            "tpa.messages.teleport-countdown-actionbar",
            Variable.of("%seconds%", secondsRemaining),
            Variable.of("%unit%", secondsRemaining == 1 ? "second" : "seconds")
        );
    }

    private void expire(TeleportRequest request) {
        var requests = requestsByTarget.get(request.targetId());
        if (requests == null || !requests.remove(request.requesterId(), request)) {
            return;
        }

        if (requests.isEmpty()) {
            requestsByTarget.remove(request.targetId());
        }

        Player requester = Bukkit.getPlayer(request.requesterId());
        Player target = Bukkit.getPlayer(request.targetId());

        if (requester != null) {
            String targetName = target == null ? "the target player" : target.getName();
            messages.send(requester, "tpa.messages.request-expired-requester", Variable.of("%target%", targetName));
        }

        if (target != null) {
            messages.send(target, "tpa.messages.request-expired-target", Variable.of("%requester%", request.requesterName()));
        }
    }

    private static final class TeleportRequest {

        private final UUID requesterId;
        private final String requesterName;
        private final UUID targetId;
        private BukkitTask timeoutTask;

        private TeleportRequest(UUID requesterId, String requesterName, UUID targetId) {
            this.requesterId = requesterId;
            this.requesterName = requesterName;
            this.targetId = targetId;
        }

        private UUID requesterId() {
            return requesterId;
        }

        private String requesterName() {
            return requesterName;
        }

        private UUID targetId() {
            return targetId;
        }

        private void setTimeoutTask(BukkitTask timeoutTask) {
            this.timeoutTask = timeoutTask;
        }

        private void cancelTimeout() {
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
        }
    }

    private static final class PendingTeleport {

        private final UUID requesterId;
        private final UUID targetId;
        private int secondsRemaining;
        private BukkitTask task;

        private PendingTeleport(UUID requesterId, UUID targetId, int secondsRemaining) {
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.secondsRemaining = secondsRemaining;
        }

        private UUID requesterId() {
            return requesterId;
        }

        private UUID targetId() {
            return targetId;
        }

        private int decrementSecondsRemaining() {
            return --secondsRemaining;
        }

        private void setTask(BukkitTask task) {
            this.task = task;
        }

        private void cancelTask() {
            if (task != null) {
                task.cancel();
            }
        }
    }
}
