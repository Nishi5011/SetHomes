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
import java.util.Map;

public class HomeTabCompleter implements TabCompleter {

    private final SetHomes plugin;

    public HomeTabCompleter(SetHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String uuid = player.getUniqueId().toString();
        if (!plugin.hasNamedHomes(uuid)) {
            return Collections.emptyList();
        }

        Map<String, ?> homes = plugin.getPlayersNamedHomes(uuid);
        String current = args[0].toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<>();
        for (String homeName : homes.keySet()) {
            if (homeName.toLowerCase(Locale.ENGLISH).startsWith(current)) {
                suggestions.add(homeName);
            }
        }
        Collections.sort(suggestions);
        return suggestions;
    }
}
