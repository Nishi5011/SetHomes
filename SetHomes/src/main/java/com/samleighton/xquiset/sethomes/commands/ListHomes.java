package com.samleighton.xquiset.sethomes.commands;

import com.samleighton.xquiset.sethomes.Home;
import com.samleighton.xquiset.sethomes.SetHomes;
import com.samleighton.xquiset.sethomes.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
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
                        String ownerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : args[0];
                        listHomes(uuid, p, ownerName);
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

    private Component fillerLine() {
        return Component.text(filler, NamedTextColor.DARK_GRAY);
    }

    private Component buildDefaultHomeComponent(String world, String command, boolean selfListing, String ownerName) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("Default Home", NamedTextColor.GOLD));
        builder.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        builder.append(Component.text("World: ", NamedTextColor.DARK_AQUA));
        builder.append(Component.text(world, NamedTextColor.WHITE));

        String hoverOwner = selfListing ? "your" : ownerName == null ? "the" : ownerName + "'s";
        builder.hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + hoverOwner + " default home", NamedTextColor.GOLD)));
        builder.clickEvent(ClickEvent.runCommand(command));

        return builder.build();
    }

    private Component buildNamedHomeComponent(String homeName, Home home, String command, boolean selfListing, String ownerName) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("Name: ", NamedTextColor.DARK_AQUA));
        builder.append(Component.text(homeName, NamedTextColor.WHITE));
        builder.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        builder.append(Component.text("World: ", NamedTextColor.DARK_AQUA));
        builder.append(Component.text(home.getWorld(), NamedTextColor.WHITE));

        String desc = home.getDesc();
        if (desc != null && !desc.isEmpty()) {
            builder.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
            builder.append(Component.text("Desc: ", NamedTextColor.DARK_AQUA));
            builder.append(LegacyComponentSerializer.legacySection().deserialize(desc).colorIfAbsent(NamedTextColor.WHITE));
        }

        String hoverOwner = selfListing ? "your" : ownerName == null ? "the" : ownerName + "'s";
        builder.hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + hoverOwner + " '" + homeName + "' home", NamedTextColor.GOLD)));
        builder.clickEvent(ClickEvent.runCommand(command));

        return builder.build();
    }

    private void sendNamedHomes(Player viewer, String uuid, String commandPrefix, boolean selfListing, String ownerName) {
        if (!pl.hasNamedHomes(uuid)) {
            viewer.sendMessage(fillerLine());
            return;
        }

        for (Map.Entry<String, Home> entry : pl.getPlayersNamedHomes(uuid).entrySet()) {
            String homeName = entry.getKey();
            Home home = entry.getValue();
            viewer.sendMessage(buildNamedHomeComponent(homeName, home, commandPrefix + homeName, selfListing, ownerName));
        }

        viewer.sendMessage(fillerLine());
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
        p.sendMessage(Component.text("Your Currently Set Homes", NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        p.sendMessage(fillerLine());

        //Tell the player if they have a default home set or not
        if (pl.hasUnknownHomes(uuid)) {
            //Gets the name of the world the home has been set in
            org.bukkit.Location homeLocation = pl.getPlayersUnnamedHome(uuid);
            String world = homeLocation != null && homeLocation.getWorld() != null ? homeLocation.getWorld().getName() : "Unknown";
            p.sendMessage(buildDefaultHomeComponent(world, "/home", true, p.getName()));
        }

        //Check to make sure the player has homes
        sendNamedHomes(p, uuid, "/home ", true, p.getName());
    }

    /**
     * Used to generate a list of homes of another player
     * for the command sender. Typically used by admins
     *
     * @param playerUUID, the UUID of the player who's homes we're trying to retrieve
     * @param sender,     the sender of the command to print the list to
     */
    private void listHomes(UUID playerUUID, Player sender, String ownerName) {
        String uuid = playerUUID.toString();
        String displayName = ownerName != null ? ownerName : Bukkit.getOfflinePlayer(playerUUID).getName();
        if (displayName == null) {
            displayName = playerUUID.toString();
        }

        sender.sendMessage(Component.text("Homes currently set for the player - " + displayName, NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        sender.sendMessage(fillerLine());

        //Tell the player if they have a default home set or not
        if (pl.hasUnknownHomes(uuid)) {
            //Gets the name of the world the home has been set in
            org.bukkit.Location homeLocation = pl.getPlayersUnnamedHome(uuid);
            String world = homeLocation != null && homeLocation.getWorld() != null ? homeLocation.getWorld().getName() : "Unknown";
            sender.sendMessage(buildDefaultHomeComponent(world, "/home-of " + displayName, false, displayName));
        }

        //Check to make sure the player has homes
        sendNamedHomes(sender, uuid, "/home-of " + displayName + " ", false, displayName);
    }

}
