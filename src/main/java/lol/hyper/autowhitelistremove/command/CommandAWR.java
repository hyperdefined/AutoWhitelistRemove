/*
 * This file is part of AutoWhitelistRemove.
 *
 * AutoWhitelistRemove is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AutoWhitelistRemove is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AutoWhitelistRemove.  If not, see <https://www.gnu.org/licenses/>.
 */

package lol.hyper.autowhitelistremove.command;

import lol.hyper.autowhitelistremove.AutoWhitelistRemove;
import lol.hyper.autowhitelistremove.tools.WhitelistCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class CommandAWR implements TabExecutor {

    private final AutoWhitelistRemove autoWhitelistRemove;
    private final WhitelistCheck whitelistCheck;

    public CommandAWR(AutoWhitelistRemove autoWhitelistRemove) {
        this.autoWhitelistRemove = autoWhitelistRemove;
        this.whitelistCheck = autoWhitelistRemove.whitelistCheck;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("autowhitelistremove.command")) {
            sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("AutoWhitelistRemove version " + autoWhitelistRemove.getPluginMeta().getVersion() + ". Created by hyperdefined.", NamedTextColor.GREEN));
            return true;
        }


        switch (args[0]) {
            case "check": {
                if (!sender.hasPermission("autowhitelistremove.check")) {
                    sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
                    return true;
                }

                if (args.length == 2) {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        sender.sendMessage(Component.text("Invalid usage. See /awr help.", NamedTextColor.RED));
                        return true;
                    }

                    if (!sender.hasPermission("autowhitelistremove.check.confirm")) {
                        sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
                        return true;
                    }

                    Set<String> removedPlayers = whitelistCheck.checkWhitelist(true);
                    sender.sendMessage(Component.text("--------------------AWR---------------------", NamedTextColor.GOLD));
                    if (removedPlayers.isEmpty()) {
                        sender.sendMessage(Component.text("No players to remove.", NamedTextColor.YELLOW));
                    } else {
                        sender.sendMessage(Component.text(removedPlayers.size() + " players have been removed.", NamedTextColor.YELLOW));
                        sender.sendMessage(Component.text(String.join(", ", removedPlayers), NamedTextColor.YELLOW));
                    }
                    sender.sendMessage(Component.text("--------------------------------------------", NamedTextColor.GOLD));
                    return true;
                }

                Set<String> removedPlayers = whitelistCheck.checkWhitelist(false);
                sender.sendMessage(Component.text("--------------------AWR---------------------", NamedTextColor.GOLD));
                if (removedPlayers.isEmpty()) {
                    sender.sendMessage(Component.text("No players to remove.", NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text(removedPlayers.size() + " players will be removed. Type \"/awr check confirm\" to confirm the removal.", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text(String.join(", ", removedPlayers), NamedTextColor.YELLOW));
                }
                sender.sendMessage(Component.text("--------------------------------------------", NamedTextColor.GOLD));
                break;
            }
            case "ignore": {
                if (!sender.hasPermission("autowhitelistremove.ignore")) {
                    sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
                    return true;
                }
                autoWhitelistRemove.logger.info(String.valueOf(args.length));
                if (args.length == 1) {
                    sender.sendMessage(Component.text("You must specify an action, 'add' or 'remove'.", NamedTextColor.RED));
                    return true;
                }
                if (args[1].equalsIgnoreCase("add")) {
                    if (args.length == 2) {
                        sender.sendMessage(Component.text("You must specify a UUID to add.", NamedTextColor.RED));
                        return true;
                    }
                    String toAdd = args[2];
                    try {
                        UUID temp = UUID.fromString(toAdd);
                        toAdd = temp.toString();
                    } catch (IllegalArgumentException ignored) {
                        // if the input is not a UUID, assume it's a player
                        Player player = Bukkit.getPlayerExact(toAdd);
                        toAdd = player.getUniqueId().toString();
                    }

                    List<String> ignoredPlayers = autoWhitelistRemove.config.getStringList("ignored-players");
                    if (ignoredPlayers.contains(toAdd)) {
                        sender.sendMessage(Component.text("This player is already ignored.", NamedTextColor.RED));
                        return true;
                    }

                    ignoredPlayers.add(toAdd);
                    autoWhitelistRemove.config.set("ignored-players", ignoredPlayers);
                    try {
                        autoWhitelistRemove.config.save(autoWhitelistRemove.configFile);
                        sender.sendMessage(Component.text("Added UUID " + toAdd + " to whitelist.", NamedTextColor.GREEN));
                    } catch (IOException exception) {
                        autoWhitelistRemove.logger.severe("Unable to save config!");
                        exception.printStackTrace();
                    }
                    return true;
                }
                if (args[1].equalsIgnoreCase("remove")) {
                    if (args.length == 2) {
                        sender.sendMessage(Component.text("You must specify a UUID to remove.", NamedTextColor.RED));
                        return true;
                    }
                    String toRemove = args[2];
                    try {
                        UUID temp = UUID.fromString(toRemove);
                        toRemove = temp.toString();
                    } catch (IllegalArgumentException ignored) {
                        // if the input is not a UUID, assume it's a player
                        Player player = Bukkit.getPlayerExact(toRemove);
                        toRemove = player.getUniqueId().toString();
                    }

                    List<String> ignoredPlayers = autoWhitelistRemove.config.getStringList("ignored-players");
                    if (!ignoredPlayers.contains(toRemove)) {
                        sender.sendMessage(Component.text("This player is not ignored, can't remove", NamedTextColor.RED));
                        return true;
                    }

                    ignoredPlayers.remove(toRemove);
                    autoWhitelistRemove.config.set("ignored-players", ignoredPlayers);
                    try {
                        autoWhitelistRemove.config.save(autoWhitelistRemove.configFile);
                        sender.sendMessage(Component.text("Removed UUID " + toRemove + " from whitelist.", NamedTextColor.GREEN));
                    } catch (IOException exception) {
                        autoWhitelistRemove.logger.severe("Unable to save config!");
                        exception.printStackTrace();
                    }
                    return true;
                }
                sender.sendMessage(Component.text("This is not a valid action for 'ignore'.", NamedTextColor.RED));
                break;
            }
            case "reload": {
                if (!sender.hasPermission("autowhitelistremove.reload")) {
                    sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
                    return true;
                }
                autoWhitelistRemove.loadConfig();
                sender.sendMessage(Component.text("Config reloaded!", NamedTextColor.GREEN));
                break;
            }
            case "help":
            default: {
                sender.sendMessage(Component.text("--------------------AWR---------------------").color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text("/awr help ", NamedTextColor.GOLD).append(Component.text("- Shows this menu.", NamedTextColor.YELLOW)));
                sender.sendMessage(Component.text("/awr check ", NamedTextColor.GOLD).append(Component.text("- Check inactive players and remove them.", NamedTextColor.YELLOW)));
                sender.sendMessage(Component.text("/awr help ", NamedTextColor.GOLD).append(Component.text("- Reload the config.", NamedTextColor.YELLOW)));
                sender.sendMessage(Component.text("--------------------------------------------").color(NamedTextColor.GOLD));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("check") && sender.hasPermission("autowhitelistremove.check.confirm")) {
                return Collections.singletonList("confirm");
            }
        }
        return Arrays.asList("check", "help", "reload");
    }
}
