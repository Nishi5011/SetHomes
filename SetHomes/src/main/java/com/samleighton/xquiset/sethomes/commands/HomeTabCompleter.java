package com.samleighton.xquiset.sethomes.commands;

import com.samleighton.xquiset.sethomes.Home;
import com.samleighton.xquiset.sethomes.SetHomes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        String uuid = player.getUniqueId().toString();
        if (!plugin.hasNamedHomes(uuid)) {
            return Collections.emptyList();
        }

        Map<String, Home> homes = plugin.getPlayersNamedHomes(uuid);
        List<String> suggestions = new ArrayList<>();
        for (String homeName : homes.keySet()) {
            if (StringUtil.startsWithIgnoreCase(homeName, args[0])) {
                suggestions.add(homeName);
            }
        }

        suggestions.sort(String.CASE_INSENSITIVE_ORDER);
        return suggestions;
    }
}
