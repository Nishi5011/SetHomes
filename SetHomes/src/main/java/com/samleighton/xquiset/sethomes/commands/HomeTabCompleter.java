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

        if (args.length != 1) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();
        if (!plugin.hasNamedHomes(uuid)) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>(plugin.getPlayersNamedHomes(uuid).keySet());
        suggestions.sort(String.CASE_INSENSITIVE_ORDER);

        String current = args[0].toLowerCase(Locale.ENGLISH);
        if (!current.isEmpty()) {
            suggestions.removeIf(name -> !name.toLowerCase(Locale.ENGLISH).startsWith(current));
        }

        return suggestions;
    }
}
