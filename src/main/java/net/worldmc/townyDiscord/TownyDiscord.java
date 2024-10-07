package net.worldmc.townyDiscord;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import github.scarsz.discordsrv.DiscordSRV;
import net.worldmc.townyDiscord.discord.commands.BroadcastCommand;
import net.worldmc.townyDiscord.towny.commands.NationDiscordCommand;
import net.worldmc.townyDiscord.towny.commands.TownDiscordCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TownyDiscord extends JavaPlugin {
    @Override
    public void onEnable() {
        DiscordSRV.api.addSlashCommandProvider(new BroadcastCommand());

        TownyCommandAddonAPI.addSubCommand(TownyCommandAddonAPI.CommandType.NATION_SET, "discord", new NationDiscordCommand());
        TownyCommandAddonAPI.addSubCommand(TownyCommandAddonAPI.CommandType.TOWN_SET, "discord", new TownDiscordCommand());
    }
}