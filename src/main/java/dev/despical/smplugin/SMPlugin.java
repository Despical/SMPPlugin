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
package dev.despical.smplugin;

import dev.despical.commandframework.CommandArguments;
import dev.despical.commandframework.CommandErrorMessage;
import dev.despical.commandframework.CommandFramework;
import dev.despical.smplugin.chat.ChatHistoryManager;
import dev.despical.smplugin.chat.PrivateMessageManager;
import dev.despical.smplugin.death.DeathLocationManager;
import dev.despical.smplugin.inventory.ReadOnlyInventoryManager;
import dev.despical.smplugin.listener.MotdListener;
import dev.despical.smplugin.listener.PlayerConnectionListener;
import dev.despical.smplugin.listener.PublicChatListener;
import dev.despical.smplugin.listener.SleepPercentageListener;
import dev.despical.smplugin.message.MessageService;
import dev.despical.smplugin.message.MessageService.Variable;
import dev.despical.smplugin.teleport.TeleportRequestManager;
import dev.despical.smplugin.vanish.VanishManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BiFunction;

/**
 * @author Despical
 * <p>
 * Created at 17.08.2026
 */
public final class SMPlugin extends JavaPlugin {

    private CommandFramework commandFramework;
    private MessageService messageService;
    private TeleportRequestManager teleportRequestManager;
    private ChatHistoryManager chatHistoryManager;
    private PrivateMessageManager privateMessageManager;
    private ReadOnlyInventoryManager readOnlyInventoryManager;
    private VanishManager vanishManager;
    private DeathLocationManager deathLocationManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageService = new MessageService(this);
        teleportRequestManager = new TeleportRequestManager(this);
        chatHistoryManager = new ChatHistoryManager(this);
        privateMessageManager = new PrivateMessageManager(this);
        readOnlyInventoryManager = new ReadOnlyInventoryManager(messageService);
        vanishManager = new VanishManager(this);
        deathLocationManager = new DeathLocationManager(this);

        configureCommandErrors();
        registerCommands();

        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        getServer().getPluginManager().registerEvents(deathLocationManager, this);
        getServer().getPluginManager().registerEvents(new SleepPercentageListener(this), this);
        getServer().getPluginManager().registerEvents(teleportRequestManager, this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PublicChatListener(this), this);
        getServer().getPluginManager().registerEvents(readOnlyInventoryManager, this);
        getServer().getPluginManager().registerEvents(vanishManager, this);
    }

    @Override
    public void onDisable() {
        if (teleportRequestManager != null) {
            teleportRequestManager.shutdown();
        }

        if (chatHistoryManager != null) {
            chatHistoryManager.save();
        }

        if (readOnlyInventoryManager != null) {
            readOnlyInventoryManager.shutdown();
        }

        if (vanishManager != null) {
            vanishManager.shutdown();
        }

        if (deathLocationManager != null) {
            deathLocationManager.save();
        }

        if (commandFramework != null) {
            commandFramework.unregisterCommands();
        }

        resetCommandErrors();
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public TeleportRequestManager getTeleportRequestManager() {
        return teleportRequestManager;
    }

    public ChatHistoryManager getChatHistoryManager() {
        return chatHistoryManager;
    }

    public PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public ReadOnlyInventoryManager getReadOnlyInventoryManager() {
        return readOnlyInventoryManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public DeathLocationManager getDeathLocationManager() {
        return deathLocationManager;
    }

    private void registerCommands() {
        commandFramework = new CommandFramework(this);
        commandFramework.registerAllInPackage("dev.despical.smplugin.command");
    }

    private void configureCommandErrors() {
        BiFunction<
            dev.despical.commandframework.annotations.Command,
            CommandArguments,
            Boolean
            > correctUsageHandler = (command, arguments) -> {
            messageService.send(
                arguments.getSender(),
                "command-errors.correct-usage",
                Variable.of("%usage%", messageService.escape(command.usage()))
            );
            return true;
        };

        CommandErrorMessage.SHORT_ARG_SIZE.setHandler(correctUsageHandler);
        CommandErrorMessage.LONG_ARG_SIZE.setHandler(correctUsageHandler);
        CommandErrorMessage.ONLY_BY_PLAYERS.setHandler((command, arguments) -> sendCommandError(arguments, "only-players"));
        CommandErrorMessage.ONLY_BY_CONSOLE.setHandler((command, arguments) -> sendCommandError(arguments, "only-console"));
        CommandErrorMessage.NO_PERMISSION.setHandler((command, arguments) -> sendCommandError(arguments, "no-permission"));
        CommandErrorMessage.MUST_HAVE_OP.setHandler((command, arguments) -> sendCommandError(arguments, "must-be-op"));
        CommandErrorMessage.WAIT_BEFORE_USING_AGAIN.setHandler((command, arguments) -> sendCommandError(arguments, "cooldown"));
    }

    private boolean sendCommandError(CommandArguments arguments, String key) {
        messageService.send(arguments.getSender(), "command-errors." + key);
        return true;
    }

    private void resetCommandErrors() {
        CommandErrorMessage.SHORT_ARG_SIZE.resetHandler();
        CommandErrorMessage.LONG_ARG_SIZE.resetHandler();
        CommandErrorMessage.ONLY_BY_PLAYERS.resetHandler();
        CommandErrorMessage.ONLY_BY_CONSOLE.resetHandler();
        CommandErrorMessage.NO_PERMISSION.resetHandler();
        CommandErrorMessage.MUST_HAVE_OP.resetHandler();
        CommandErrorMessage.WAIT_BEFORE_USING_AGAIN.resetHandler();
    }
}
