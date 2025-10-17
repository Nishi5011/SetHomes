package com.samleighton.xquiset.sethomes.commands;

import com.samleighton.xquiset.sethomes.Home;
import com.samleighton.xquiset.sethomes.SetHomes;
import com.samleighton.xquiset.sethomes.utils.ChatUtils;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SetHome implements CommandExecutor {
    private final SetHomes pl;
    private final HashMap<String, Integer> maxHomesList;
    private final Permission vaultPerms;
    private final LuckPerms luckPerms;
    private final boolean permissions;

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

            //No home name provided
            if (args.length < 1) {
                //Save the home
                pl.saveUnknownHome(uuid, playersHome);

                ChatUtils.sendSuccess(p, "You have set a default home!");
                //They have provided a home name and possibly description too
            } else {
                if (p.hasPermission("homes.sethome")) {
                    boolean confirmFlag = args.length > 1 && args[args.length - 1].equalsIgnoreCase("confirm");

                    // Cleanse the input argument of any non alphanumeric characters
                    String homeName = args[0].replaceAll("[^a-zA-Z0-9]", "");

                    // Ensure that after cleansing the homename still has a value
                    if (homeName.length() > 0) {
                        // Set the home name to the given name
                        playersHome.setHomeName(homeName);
                    } else {
                        ChatUtils.sendError(p, "Please use a valid home name! Only a-z & 0-9 characters are allowed.");
                        return true;
                    }

                    //Build the description as a combination of all other arguments passed excluding confirm
                    StringBuilder descBuilder = new StringBuilder();
                    int descEndIndex = confirmFlag ? args.length - 2 : args.length - 1;
                    for (int i = 1; i <= descEndIndex; i++) {
                        descBuilder.append(args[i]).append(" ");
                    }

                    String desc = descBuilder.toString().trim();
                    if (!desc.isEmpty()) {
                        playersHome.setDesc(desc);
                    }

                    boolean hasNamedHomes = pl.hasNamedHomes(uuid);
                    Map<String, Home> existingHomes = hasNamedHomes ? pl.getPlayersNamedHomes(uuid) : new HashMap<>();
                    boolean hasHomeWithName = existingHomes.containsKey(homeName);

                    if (!confirmFlag) {
                        if (hasNamedHomes) {
                            int maxHomes = getMaxHomesAllowed(p);
                            if ((existingHomes.size() >= maxHomes && maxHomes != 0) && !p.hasPermission("homes.config_bypass")) {
                                ChatUtils.sendInfo(p, pl.config.getString("max-homes-msg"));
                                return true;
                            }
                        }

                        if (hasHomeWithName) {
                            sendConfirmMessage(p, homeName);
                            return true;
                        }

                        pl.saveNamedHome(uuid, playersHome);
                        ChatUtils.sendSuccess(p, "Your home '" + playersHome.getHomeName() + "' has been set!");
                        return true;
                    }

                    if (hasHomeWithName) {
                        pl.deleteNamedHome(uuid, homeName);
                        Home existingHome = existingHomes.get(homeName);
                        if ((playersHome.getDesc() == null || playersHome.getDesc().isEmpty()) && existingHome != null) {
                            playersHome.setDesc(existingHome.getDesc());
                        }
                        ChatUtils.sendSuccess(p, "Your home '" + homeName + "' has been updated!");
                    } else {
                        if (hasNamedHomes) {
                            int maxHomes = getMaxHomesAllowed(p);
                            if ((existingHomes.size() >= maxHomes && maxHomes != 0) && !p.hasPermission("homes.config_bypass")) {
                                ChatUtils.sendInfo(p, pl.config.getString("max-homes-msg"));
                                return true;
                            }
                        }
                        ChatUtils.sendSuccess(p, "Your home '" + homeName + "' has been set!");
                    }

                    pl.saveNamedHome(uuid, playersHome);
                    return true;
                }
                //Send player message because they didn't have the proper permissions
                ChatUtils.permissionError(p);
            }
            return true;
        }
        return false;
    }

    private void sendConfirmMessage(Player p, String homeName) {
        ChatUtils.sendError(p, "You already have a home with that name. Confirm to overwrite it.");
        TextComponent confirm = new TextComponent(ChatColor.YELLOW + "Click here to confirm overwrite of '" + homeName + "'");
        String command = "/sethome " + homeName + " confirm";
        confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.RED + "This will overwrite the existing home." + ChatColor.GRAY + " (" + command + ")")
                        .create()));
        p.spigot().sendMessage(confirm);
        ChatUtils.sendInfo(p, "Or run " + ChatColor.AQUA + command + ChatColor.GOLD + " to confirm manually.");
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
}
