package com.samleighton.xquiset.sethomes.commands;

import com.samleighton.xquiset.sethomes.Home;
import com.samleighton.xquiset.sethomes.SetHomes;
import com.samleighton.xquiset.sethomes.utils.ChatUtils;
import org.apache.commons.lang.StringUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class ListHomes implements CommandExecutor {

    private final SetHomes pl;
    private final String filler = StringUtils.repeat("-", 53);

    public ListHomes(SetHomes plugin) {
        this.pl = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        //Make sure the sender of the command is a player
        if (!(sender instanceof Player)) {
            //Sends message to sender of command that they're not a player
            ChatUtils.notPlayerError(sender);
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("homes")) {
            Player p = (Player) sender;

            if (args.length == 1) {
                if (p.hasPermission("homes.gethomes")) {
                    //Create a offline player for the name they passed
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                    //Check to make sure the player has actually joined the server
                    if (offlinePlayer.hasPlayedBefore()) {
                        UUID uuid = offlinePlayer.getUniqueId();
                        listHomes(uuid, p);
                    } else {
                        ChatUtils.sendError(p, "That user has never player here before!");
                        return true;
                    }
                } else {
                    //Send player message because they didn't have the proper permissions
                    ChatUtils.permissionError(p);
                }
                return true;
            } else if (args.length == 0) {
                //List the homes for the player who sent the command
                listHomes(p);
                return true;
            } else {
                //Tell the player if they've sent to many arguments with the command
                ChatUtils.tooManyArgs(p);
                return false;
            }
        }
        return false;
    }

    /**
     * Used to check if a player has named homes, and will also send the
     * homes list if there are any homes.
     *
     * @param p,    the player object to check homes for
     * @param uuid, the uuid of the player object as a string
     */
    private void checkForNamedHomes(Player p, String uuid, String targetName, boolean isSelf) {
        if (!pl.hasNamedHomes(uuid)) {
            p.sendMessage(filler);
            return;
        }

        Map<String, Home> homes = new TreeMap<>(pl.getPlayersNamedHomes(uuid));

        for (Map.Entry<String, Home> entry : homes.entrySet()) {
            String name = entry.getKey();
            Home home = entry.getValue();
            String world = home.getWorld();
            String desc = home.getDesc();

            String baseMessage = ChatColor.DARK_AQUA + "Name: " + ChatColor.WHITE + name + ChatColor.DARK_GRAY + " | " +
                    ChatColor.DARK_AQUA + "World: " + ChatColor.WHITE + world;
            if (desc != null && !desc.isEmpty()) {
                baseMessage += ChatColor.DARK_GRAY + " | " + ChatColor.DARK_AQUA + "Desc: " + ChatColor.WHITE + desc;
            }

            TextComponent component = new TextComponent(baseMessage);
            String command = isSelf ? "/home " + name : "/home-of " + targetName + " " + name;
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

            String hoverText = ChatColor.GOLD + "Click to teleport";
            hoverText += ChatColor.GRAY + " (" + command + ")";
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(hoverText).create()));

            p.spigot().sendMessage(component);
        }

        p.sendMessage(filler);
    }

    /**
     * Used to generate a list of homes for the commend sender.
     * Will also print the list to the player in a nice format
     *
     * @param p, the player sending the command
     */
    private void listHomes(Player p) {
        //The uuid string of the player p
        String uuid = p.getUniqueId().toString();

        //Begin listing homes for the player
        p.sendMessage(ChatColor.BOLD + "Your Currently Set Homes");
        p.sendMessage(filler);

        //Tell the player if they have a default home set or not
        if (pl.hasUnknownHomes(uuid)) {
            Location defaultHome = pl.getPlayersUnnamedHome(uuid);
            if (defaultHome != null && defaultHome.getWorld() != null) {
                String world = defaultHome.getWorld().getName();
                sendDefaultHomeComponent(p, world, true, null);
            }
        }

        //Check to make sure the player has homes
        checkForNamedHomes(p, uuid, p.getName(), true);
    }

    /**
     * Used to generate a list of homes of another player
     * for the command sender. Typically used by admins
     *
     * @param playerUUID, the UUID of the player who's homes we're trying to retrieve
     * @param sender,     the sender of the command to print the list to
     */
    private void listHomes(UUID playerUUID, Player sender) {
        String uuid = playerUUID.toString();

        String targetName = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (targetName == null) {
            targetName = playerUUID.toString();
        }

        sender.sendMessage(ChatColor.BOLD + "Homes currently set for the player - " + targetName);
        sender.sendMessage(filler);

        //Tell the player if they have a default home set or not
        if (pl.hasUnknownHomes(uuid)) {
            //Gets the name of the world the home has been set in
            Location defaultHome = pl.getPlayersUnnamedHome(uuid);
            if (defaultHome != null && defaultHome.getWorld() != null) {
                String world = defaultHome.getWorld().getName();
                sendDefaultHomeComponent(sender, world, false, targetName);
            }
        }

        //Check to make sure the player has homes
        checkForNamedHomes(sender, uuid, targetName, false);
    }

    private void sendDefaultHomeComponent(Player viewer, String world, boolean isSelf, String targetName) {
        String label = ChatColor.GOLD + "Default Home";
        String message = label + ChatColor.DARK_GRAY + " | " + ChatColor.DARK_AQUA + "World: " + ChatColor.WHITE + world;
        TextComponent component = new TextComponent(message);
        String command = isSelf ? "/home" : "/home-of " + targetName;
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        String hoverText = ChatColor.GOLD + "Click to teleport" + ChatColor.GRAY + " (" + command + ")";
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText).create()));
        viewer.spigot().sendMessage(component);
    }

}
