package net.worldmc.townyDiscord.towny.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TownDiscordCommand implements CommandExecutor {
    private static final String PERMISSION = "towny.command.town.set.discord";
    private static final String DISCORD_METADATA_KEY = "discordLink";

    private static final Component PERM_DENY_MSG = Component.text("You do not have permission to use this command!", NamedTextColor.RED);
    private static final Component PLAYER_ONLY_MSG = Component.text("This command can only be used by players.", NamedTextColor.RED);
    private static final Component USAGE_MSG = Component.text("Usage: /town set discord <link>", NamedTextColor.RED);
    private static final Component INVALID_LINK_MSG = Component.text("Invalid Discord invite link. Please provide a valid Discord invite.", NamedTextColor.RED);

    private static final Pattern DISCORD_INVITE_PATTERN = Pattern.compile("(?:https?://)?(?:www\\.)?(?:discord\\.(?:gg|io|me|li)|discordapp\\.com/invite)/([a-zA-Z0-9-]+)");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYER_ONLY_MSG);
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(PERM_DENY_MSG);
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(USAGE_MSG);
            return true;
        }

        String discordLink = args[0];
        String standardizedLink = validateAndStandardizeDiscordLink(discordLink);

        if (standardizedLink == null) {
            player.sendMessage(INVALID_LINK_MSG);
            return true;
        }

        Resident resident = TownyAPI.getInstance().getResident(player);
        assert resident != null;
        Town town = resident.getTownOrNull();
        assert town != null;

        StringDataField discordMetadata = new StringDataField(DISCORD_METADATA_KEY, discordLink);
        town.addMetaData(discordMetadata);

        Component successMsg = Component.text()
                .append(Component.text("[" + town.getName() + "] ", NamedTextColor.GOLD))
                .append(Component.text(resident.getName() + " has set the town discord to " + standardizedLink, NamedTextColor.AQUA))
                .build();

        player.sendMessage(successMsg);

        return true;
    }

    private String validateAndStandardizeDiscordLink(String input) {
        Matcher matcher = DISCORD_INVITE_PATTERN.matcher(input);
        if (matcher.find()) {
            String inviteCode = matcher.group(1);
            return "https://discord.gg/" + inviteCode;
        } else if (input.matches("[a-zA-Z0-9-]+")) {
            // If the input is just the invite code
            return "https://discord.gg/" + input;
        }
        return null;
    }
}