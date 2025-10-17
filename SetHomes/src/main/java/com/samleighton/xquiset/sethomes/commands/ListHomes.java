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
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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
    private void checkForNamedHomes(Player viewer, String ownerUuid, String ownerName, boolean isOwnerView) {
        if (pl.hasNamedHomes(ownerUuid)) {
            Map<String, Home> homes = pl.getPlayersNamedHomes(ownerUuid);
            List<String> homeNames = new ArrayList<>(homes.keySet());
            homeNames.sort(String.CASE_INSENSITIVE_ORDER);

            String targetName = ownerName;
            boolean canRunCommand = isOwnerView || (targetName != null && !targetName.isEmpty());
            if (targetName == null || targetName.isEmpty()) {
                targetName = ownerUuid;
            }

            for (String id : homeNames) {
                Home home = homes.get(id);
                TextComponent.Builder builder = Component.text()
                        .append(Component.text("Name: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text(id, NamedTextColor.WHITE, TextDecoration.UNDERLINED))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("World: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text(home.getWorld(), NamedTextColor.WHITE));

                String desc = home.getDesc();
                if (desc != null && !desc.isEmpty()) {
                    builder.append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                            .append(Component.text("Desc: ", NamedTextColor.DARK_AQUA))
                            .append(Component.text(desc, NamedTextColor.WHITE));
                }

                if (canRunCommand) {
                    String command = isOwnerView ? "/home " + id : "/home-of " + targetName + " " + id;
                    Component hover = Component.text()
                            .append(Component.text("Click to teleport to ", NamedTextColor.GOLD))
                            .append(Component.text(isOwnerView ? "your home" : targetName + "'s home", NamedTextColor.WHITE))
                            .append(Component.newline())
                            .append(Component.text("World: ", NamedTextColor.GRAY))
                            .append(Component.text(home.getWorld(), NamedTextColor.WHITE));
                    builder.clickEvent(ClickEvent.runCommand(command));
                    builder.hoverEvent(HoverEvent.showText(hover));
                }

                viewer.sendMessage(builder.build());
            }
        }
        viewer.sendMessage(filler);
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
                TextComponent.Builder builder = Component.text()
                        .append(Component.text("Default Home", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("World: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text(defaultHome.getWorld().getName(), NamedTextColor.WHITE));

                Component hover = Component.text()
                        .append(Component.text("Click to teleport to your default home", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("World: ", NamedTextColor.GRAY))
                        .append(Component.text(defaultHome.getWorld().getName(), NamedTextColor.WHITE));

                builder.clickEvent(ClickEvent.runCommand("/home"));
                builder.hoverEvent(HoverEvent.showText(hover));
                p.sendMessage(builder.build());
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
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
        String ownerName = target.getName();
        String displayName = ownerName != null ? ownerName : uuid;

        sender.sendMessage(ChatColor.BOLD + "Homes currently set for the player - " + displayName);
        sender.sendMessage(filler);

        //Tell the player if they have a default home set or not
        if (pl.hasUnknownHomes(uuid)) {
            Location defaultHome = pl.getPlayersUnnamedHome(uuid);
            if (defaultHome != null && defaultHome.getWorld() != null) {
                TextComponent.Builder builder = Component.text()
                        .append(Component.text("Default Home", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("World: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text(defaultHome.getWorld().getName(), NamedTextColor.WHITE));

                if (ownerName != null && !ownerName.isEmpty()) {
                    Component hover = Component.text()
                            .append(Component.text("Click to teleport to ", NamedTextColor.GOLD))
                            .append(Component.text(ownerName + "'s default home", NamedTextColor.WHITE))
                            .append(Component.newline())
                            .append(Component.text("World: ", NamedTextColor.GRAY))
                            .append(Component.text(defaultHome.getWorld().getName(), NamedTextColor.WHITE));
                    builder.clickEvent(ClickEvent.runCommand("/home-of " + ownerName));
                    builder.hoverEvent(HoverEvent.showText(hover));
                }

                sender.sendMessage(builder.build());
            }
        }

        //Check to make sure the player has homes
        checkForNamedHomes(sender, uuid, ownerName, false);
    }

}
