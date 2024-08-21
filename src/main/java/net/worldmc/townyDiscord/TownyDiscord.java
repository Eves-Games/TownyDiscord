package net.worldmc.townyDiscord;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import net.essentialsx.api.v2.services.discord.InteractionException;
import net.essentialsx.api.v2.services.discordlink.DiscordLinkService;
import net.worldmc.townyDiscord.discord.commands.BroadcastCommand;
import net.essentialsx.api.v2.services.discord.DiscordService;
import net.worldmc.townyDiscord.towny.commands.NationDiscordCommand;
import net.worldmc.townyDiscord.towny.commands.TownDiscordCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TownyDiscord extends JavaPlugin {
    @Override
    public void onEnable() {
        DiscordService discordService = Bukkit.getServicesManager().load(DiscordService.class);
        DiscordLinkService discordLinkService = Bukkit.getServicesManager().load(DiscordLinkService.class);

        try {
            assert discordService != null;
            discordService.getInteractionController().registerCommand(new BroadcastCommand(this, discordService, discordLinkService));
        } catch (InteractionException e) {
            throw new RuntimeException(e);
        }

        TownyCommandAddonAPI.addSubCommand(TownyCommandAddonAPI.CommandType.NATION_SET, "discord", new NationDiscordCommand());
        TownyCommandAddonAPI.addSubCommand(TownyCommandAddonAPI.CommandType.TOWN_SET, "discord", new TownDiscordCommand());
    }
}