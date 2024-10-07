package net.worldmc.townyDiscord.discord.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.OptionData;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BroadcastCommand implements SlashCommandProvider {
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final long COOLDOWN_TIME = TimeUnit.HOURS.toMillis(1); // 1 hour cooldown

    @SlashCommand(path = "broadcast")
    public void onBroadcastCommand(SlashCommandEvent event) {
        UUID playerUUID = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (playerUUID == null) {
            event.reply("Error: Your Discord account is not linked to a Minecraft account.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Resident resident = TownyAPI.getInstance().getResident(playerUUID);
        if (resident == null || !resident.isKing()) {
            event.reply("Error: You must be a king to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsage = cooldowns.getOrDefault(playerUUID, 0L);
        if (currentTime - lastUsage < COOLDOWN_TIME) {
            long remainingCooldown = COOLDOWN_TIME - (currentTime - lastUsage);
            long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(remainingCooldown);
            event.reply("You must wait " + minutesLeft + " minutes before using this command again.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Nation nation = resident.getNationOrNull();
        assert nation != null;

        String message = Objects.requireNonNull(event.getOption("message")).getAsString();

        TextChannel broadcastChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("broadcasts");
        if (broadcastChannel == null) {
            event.reply("Error: The broadcasts channel could not be found.").queue();
            return;
        }

        broadcastChannel.sendMessage("**[" + nation.getName() + "]** " + resident.getFormattedTitleName() +
                " (" + Objects.requireNonNull(event.getMember()).getAsMention() + ")\n" + message).queue();

        cooldowns.put(playerUUID, currentTime);

        event.reply("Your message has been broadcasted: " + message)
                .setEphemeral(true)
                .queue();
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(List.of(
                new PluginSlashCommand((Plugin) this, new CommandData("broadcast", "broadcasts a message to all players on the discord server (kings only)")
                        .addOptions(new OptionData(OptionType.STRING, "message", "message to broadcast").setRequired(true))
                )
        ));
    }
}