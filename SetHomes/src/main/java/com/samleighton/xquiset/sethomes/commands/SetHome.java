package com.samleighton.xquiset.sethomes.commands;

import com.samleighton.xquiset.sethomes.Home;
import com.samleighton.xquiset.sethomes.SetHomes;
import com.samleighton.xquiset.sethomes.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SetHome implements CommandExecutor {
    private final SetHomes pl;
    private final HashMap<String, Integer> maxHomesList;
    private final Permission vaultPerms;
    private final LuckPerms luckPerms;
    private final boolean permissions;
    private final Map<UUID, PendingHome> pendingConfirmations = new HashMap<>();

    public SetHome(SetHomes plugin) {
        pl = plugin;
        maxHomesList = pl.getMaxHomes();
        luckPerms = pl.getLuckPermsApi();
        vaultPerms = pl.getVaultPermissions();
        permissions = luckPerms != null || vaultPerms != null;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        //Make sure the sender of the command is a player
        if (!(sender instanceof Player)) {
            //Sends message to sender of command that they're not a player
            ChatUtils.notPlayerError(sender);
            return false;
        }
        //Checks if the command sent is /sethome
        if (cmd.getName().equalsIgnoreCase("sethome")) {
            //Since we know sender is a player we can Cast sender as such
            Player p = (Player) sender;
            String uuid = p.getUniqueId().toString();
            Location home = p.getLocation();

            // Check to make sure the home world is not blacklisted
            if (pl.getBlacklistedWorlds().contains(Objects.requireNonNull(home.getWorld()).getName()) && !p.hasPermission("homes.config_bypass")) {
                ChatUtils.sendError(p, "This world does not allow the usage of homes!");
                return true;
            }

            //Create a home at the players location
            Home playersHome = new Home(home);

            if (args.length < 1) {
                pl.saveUnknownHome(uuid, playersHome);
                pendingConfirmations.remove(p.getUniqueId());
                ChatUtils.sendSuccess(p, "You have set a default home!");
            } else {
                if (!p.hasPermission("homes.sethome")) {
                    ChatUtils.permissionError(p);
                    return true;
                }

                UUID playerId = p.getUniqueId();
                boolean confirmRequested = args.length > 1 && "confirm".equalsIgnoreCase(args[args.length - 1]);
                int descArgsLength = confirmRequested ? args.length - 1 : args.length;

                String homeName = args[0].replaceAll("[^a-zA-Z0-9]", "");
                if (homeName.isEmpty()) {
                    ChatUtils.sendError(p, "Please use a valid home name! Only a-z & 0-9 characters are allowed.");
                    return true;
                }
                playersHome.setHomeName(homeName);

                StringBuilder desc = new StringBuilder();
                for (int i = 1; i < descArgsLength; i++) {
                    desc.append(args[i]).append(" ");
                }
                if (desc.length() > 0) {
                    playersHome.setDesc(desc.substring(0, desc.length() - 1));
                }

                Map<String, Home> existingHomes = pl.hasNamedHomes(uuid) ? pl.getPlayersNamedHomes(uuid) : Collections.emptyMap();
                boolean homeExists = existingHomes.containsKey(homeName);

                if (confirmRequested) {
                    PendingHome pending = pendingConfirmations.get(playerId);
                    if (pending == null || !pending.getHome().getHomeName().equalsIgnoreCase(homeName)) {
                        ChatUtils.sendError(p, "There is no pending confirmation for the home '" + homeName + "'.");
                        return true;
                    }

                    if (!homeExists) {
                        ChatUtils.sendError(p, "You no longer have a home named '" + homeName + "' to replace.");
                        pendingConfirmations.remove(playerId);
                        return true;
                    }

                    pl.deleteNamedHome(uuid, homeName);
                    pl.saveNamedHome(uuid, pending.getHome());
                    pendingConfirmations.remove(playerId);
                    ChatUtils.sendSuccess(p, "Your home '" + homeName + "' has been updated!");
                    return true;
                }

                if (!homeExists) {
                    int maxHomes = getMaxHomesAllowed(p);
                    Bukkit.getServer().getLogger().info("Max Homes: " + maxHomes);
                    if ((existingHomes.size() >= maxHomes && maxHomes != 0) && !p.hasPermission("homes.config_bypass")) {
                        ChatUtils.sendInfo(p, pl.config.getString("max-homes-msg"));
                        return true;
                    }
                }

                pendingConfirmations.remove(playerId);

                if (homeExists) {
                    pendingConfirmations.put(playerId, new PendingHome(playersHome));
                    ChatUtils.sendError(p, "You already have a home with that name. Confirm to overwrite it.");
                    TextComponent.Builder confirmBuilder = Component.text();
                    confirmBuilder.append(Component.text("[Click here to confirm]", NamedTextColor.GOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to confirm replacing '" + homeName + "'", NamedTextColor.GOLD)))
                            .clickEvent(ClickEvent.runCommand("/sethome " + homeName + " confirm")));
                    confirmBuilder.append(Component.text(" or run /sethome " + homeName + " confirm", NamedTextColor.YELLOW));
                    p.sendMessage(confirmBuilder.build());
                    return true;
                }

                pl.saveNamedHome(uuid, playersHome);
                ChatUtils.sendSuccess(p, "Your home '" + playersHome.getHomeName() + "' has been set!");
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the maximum homes allowed for a player. Will take the greatest value when player
     * has multiple groups assigned to them
     *
     * @param p, The player we're attempting to get the homes for
     * @return maximum number of homes allowed for that player
     */
    private int getMaxHomesAllowed(Player p) {
        int maxHomes = 0;

        // Check to see if permissions are enabled
        if (permissions) {
            // First try luck perms
            if (luckPerms != null) {
                // Loop over groups found in config list
                for (String group : maxHomesList.keySet()) {
                    if (p.hasPermission("group." + group)) {
                        int max_home_val = maxHomesList.get(group);
                        if (maxHomes < max_home_val) {
                            maxHomes = max_home_val;
                        }
                    }
                }
            } else {
                // Loop over groups found by Vault
                for (String group : vaultPerms.getPlayerGroups(p)) {
                    for (String g : maxHomesList.keySet()) {
                        if (group.equalsIgnoreCase(g)) {
                            if (maxHomesList.get(g) > maxHomes) {
                                maxHomes = maxHomesList.get(g);
                            }
                        }
                    }
                }
            }
        }

        return maxHomes;
    }

    private static class PendingHome {
        private final Home home;

        private PendingHome(Home home) {
            this.home = home;
        }

        public Home getHome() {
            return home;
        }
    }
}
