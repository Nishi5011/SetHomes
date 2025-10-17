package com.samleighton.xquiset.sethomes.commands;

import com.samleighton.xquiset.sethomes.SetHomes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HomeTabCompleter implements TabCompleter {

    private final SetHomes plugin;

    public HomeTabCompleter(SetHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length != 1) {
            return Collections.emptyList();
        }

        UUID uuid = player.getUniqueId();
        if (!plugin.hasNamedHomes(uuid.toString())) {
            return Collections.emptyList();
        }

        String lowerPrefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        plugin.getPlayersNamedHomes(uuid.toString()).keySet()
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(name -> {
                    if (name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                        matches.add(name);
                    }
                });

        return matches;
    }
}
