package com.samleighton.xquiset.sethomes.commands;

import com.samleighton.xquiset.sethomes.Home;
import com.samleighton.xquiset.sethomes.SetHomes;
import com.samleighton.xquiset.sethomes.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class ListHomes implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

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
    private void sendNamedHomes(Player viewer, String uuid, Function<String, String> commandBuilder, Function<String, String> hoverBuilder) {
        if (pl.hasNamedHomes(uuid)) {
            Map<String, Home> homes = pl.getPlayersNamedHomes(uuid);

            for (Map.Entry<String, Home> entry : homes.entrySet()) {
                String homeName = entry.getKey();
                Home home = entry.getValue();
                String world = home.getWorld();
                String desc = home.getDesc();

                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append(ChatColor.DARK_AQUA).append("Name: ")
                        .append(ChatColor.WHITE).append(homeName)
                        .append(ChatColor.DARK_GRAY).append(" | ")
                        .append(ChatColor.DARK_AQUA).append("World: ")
                        .append(ChatColor.WHITE).append(world);

                if (desc != null) {
                    messageBuilder.append(ChatColor.DARK_GRAY).append(" | ")
                            .append(ChatColor.DARK_AQUA).append("Desc: ")
                            .append(ChatColor.WHITE).append(desc);
                }

                sendClickableLine(viewer, messageBuilder.toString(), commandBuilder.apply(homeName), hoverBuilder.apply(homeName));
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
                //Gets the name of the world the home has been set in
                String world = defaultHome.getWorld().getName();
                sendClickableLine(
                        p,
                        ChatColor.GOLD + "Default Home" + ChatColor.DARK_GRAY + " | " + ChatColor.DARK_AQUA + "World: " + ChatColor.WHITE + world,
                        "/home",
                        ChatColor.YELLOW + "Click to teleport to your default home."
                );
            } else {
                ChatUtils.sendError(p, "Your default home could not be listed because its world is unavailable.");
            }
        }

        //Check to make sure the player has homes
        sendNamedHomes(
                p,
                uuid,
                homeName -> "/home " + homeName,
                homeName -> ChatColor.YELLOW + "Click to teleport to home '" + homeName + "'."
        );
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
        String targetName = Objects.requireNonNullElse(Bukkit.getOfflinePlayer(playerUUID).getName(), "Unknown");

        sender.sendMessage(ChatColor.BOLD + "Homes currently set for the player - " + targetName);
        sender.sendMessage(filler);

        //Tell the player if they have a default home set or not
        if (pl.hasUnknownHomes(uuid)) {
            Location defaultHome = pl.getPlayersUnnamedHome(uuid);

            if (defaultHome != null && defaultHome.getWorld() != null) {
                //Gets the name of the world the home has been set in
                String world = defaultHome.getWorld().getName();
                sendClickableLine(
                        sender,
                        ChatColor.GOLD + "Default Home - World: " + world,
                        "/home-of " + targetName,
                        ChatColor.YELLOW + "Click to teleport to " + targetName + "'s default home."
                );
            } else {
                ChatUtils.sendError(sender, targetName + "'s default home could not be listed because its world is unavailable.");
            }
        }

        //Check to make sure the player has homes
        sendNamedHomes(
                sender,
                uuid,
                homeName -> "/home-of " + targetName + " " + homeName,
                homeName -> ChatColor.YELLOW + "Click to teleport to " + targetName + "'s home '" + homeName + "'."
        );
    }

    private void sendClickableLine(Player player, String legacyMessage, String command, String hoverMessage) {
        Component base = LEGACY_SERIALIZER.deserialize(legacyMessage);
        Component hover = LEGACY_SERIALIZER.deserialize(hoverMessage);

        player.sendMessage(base
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(hover)));
    }

}
