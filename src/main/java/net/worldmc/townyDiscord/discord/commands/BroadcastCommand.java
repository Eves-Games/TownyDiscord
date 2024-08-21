package net.worldmc.townyDiscord.discord.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.essentialsx.api.v2.services.discord.*;
import net.essentialsx.api.v2.services.discordlink.DiscordLinkService;
import net.worldmc.townyDiscord.TownyDiscord;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BroadcastCommand implements InteractionCommand {
    private final DiscordService discordService;
    private final DiscordLinkService discordLinkService;
    private final MessageType broadcastChannel;
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public BroadcastCommand(TownyDiscord plugin, DiscordService discordService, DiscordLinkService discordLinkService) {
        this.discordService = discordService;
        this.discordLinkService = discordLinkService;
        this.broadcastChannel = new MessageType("broadcast");
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        discordService.registerMessageType(plugin, this.broadcastChannel);

        scheduler.scheduleAtFixedRate(this::cleanupCooldowns, 1, 1, TimeUnit.HOURS);

        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                    @EventHandler
                    public void onPluginDisable(org.bukkit.event.server.PluginDisableEvent event) {
                        if (event.getPlugin() == plugin) {
                            shutdown();
                        }
                    }
                }, plugin)
        );
    }

    private void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void onCommand(InteractionEvent event) {
        String discordId = event.getMember().getId();

        UUID playerUUID = discordLinkService.getUUID(discordId);
        if (playerUUID == null) {
            event.reply("Error: Your Discord account is not linked to a Minecraft account.");
            return;
        }

        Resident resident = TownyAPI.getInstance().getResident(playerUUID);
        if (resident == null || !resident.isKing()) {
            event.reply("Error: You must be a king to use this command.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsage = cooldowns.getOrDefault(playerUUID, 0L);
        if (currentTime - lastUsage < TimeUnit.HOURS.toMillis(2)) {
            long remainingCooldown = TimeUnit.HOURS.toMillis(2) - (currentTime - lastUsage);
            event.reply("You must wait " + TimeUnit.MILLISECONDS.toMinutes(remainingCooldown) + " minutes before using this command again.");
            return;
        }

        Nation nation = resident.getNationOrNull();
        assert nation != null;

        String message = event.getStringArgument("message");

        discordService.sendMessage(broadcastChannel, "**[" + nation.getName() + "]** " + resident.getFormattedTitleName() + " (" + event.getMember().getAsMention() + ")\n" + message, false);

        // Set cooldown
        cooldowns.put(playerUUID, currentTime);

        event.reply("Your message has been broadcasted: " + message);
    }

    private void cleanupCooldowns() {
        long currentTime = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() >= TimeUnit.HOURS.toMillis(2));
    }

    @Override
    public String getName() {
        return "broadcast";
    }

    @Override
    public String getDescription() {
        return "Broadcasts a message to all players on the server (Kings only)";
    }

    @Override
    public List<InteractionCommandArgument> getArguments() {
        return List.of(
                new InteractionCommandArgument(
                        "message",
                        "The message to broadcast",
                        InteractionCommandArgumentType.STRING,
                        true
                )
        );
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }
}