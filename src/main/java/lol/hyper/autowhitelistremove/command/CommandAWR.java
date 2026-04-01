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

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lol.hyper.autowhitelistremove.AutoWhitelistRemove;
import lol.hyper.autowhitelistremove.tools.WhitelistCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.*;

public class CommandAWR implements BasicCommand {

    private final AutoWhitelistRemove autoWhitelistRemove;
    private final WhitelistCheck whitelistCheck;

    public CommandAWR(AutoWhitelistRemove autoWhitelistRemove) {
        this.autoWhitelistRemove = autoWhitelistRemove;
        this.whitelistCheck = autoWhitelistRemove.whitelistCheck;
    }

    @Override
    public void execute(CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();
        if (!sender.hasPermission("autowhitelistremove.command")) {
            sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("AutoWhitelistRemove version " + autoWhitelistRemove.getPluginMeta().getVersion() + ". Created by hyperdefined.", NamedTextColor.GREEN));
            return;
        }

        switch (args[0]) {
            case "check": {
                if (!sender.hasPermission("autowhitelistremove.check")) {
                    sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
                    return;
                }

                if (args.length == 2) {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        sender.sendMessage(Component.text("Invalid usage. See /awr help.", NamedTextColor.RED));
                        return;
                    }

                    if (!sender.hasPermission("autowhitelistremove.check.confirm")) {
                        sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
                        return;
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
                    return;
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
                    return;
                }
                if (args.length == 1) {
                    sender.sendMessage(Component.text("You must specify an action, 'add' or 'remove'.", NamedTextColor.RED));
                    return;
                }
                if (args[1].equalsIgnoreCase("add")) {
                    if (args.length == 2) {
                        sender.sendMessage(Component.text("You must specify a UUID to add.", NamedTextColor.RED));
                        return;
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
                        return;
                    }

                    ignoredPlayers.add(toAdd);
                    autoWhitelistRemove.config.set("ignored-players", ignoredPlayers);
                    try {
                        autoWhitelistRemove.config.save(autoWhitelistRemove.configFile);
                        sender.sendMessage(Component.text("Added UUID " + toAdd + " to whitelist.", NamedTextColor.GREEN));
                    } catch (IOException exception) {
                        autoWhitelistRemove.logger.error("Unable to save config!", exception);
                    }
                    return;
                }
                if (args[1].equalsIgnoreCase("remove")) {
                    if (args.length == 2) {
                        sender.sendMessage(Component.text("You must specify a UUID to remove.", NamedTextColor.RED));
                        return;
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
                        return;
                    }

                    ignoredPlayers.remove(toRemove);
                    autoWhitelistRemove.config.set("ignored-players", ignoredPlayers);
                    try {
                        autoWhitelistRemove.config.save(autoWhitelistRemove.configFile);
                        sender.sendMessage(Component.text("Removed UUID " + toRemove + " from whitelist.", NamedTextColor.GREEN));
                    } catch (IOException exception) {
                        autoWhitelistRemove.logger.error("Unable to save config!", exception);
                    }
                    return;
                }
                sender.sendMessage(Component.text("This is not a valid action for 'ignore'.", NamedTextColor.RED));
                break;
            }
            case "reload": {
                if (!sender.hasPermission("autowhitelistremove.reload")) {
                    sender.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
                    return;
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
    }

    @Override
    public String permission() {
        return "autowhitelistremove.command";
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            List<String> suggestions = new ArrayList<>();
            if (sender.hasPermission("autowhitelistremove.reload")) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("autowhitelistremove.ignore")) {
                suggestions.add("ignore");
            }
            suggestions.add("help"); // make this always there
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("check") && sender.hasPermission("autowhitelistremove.check.confirm")) {
            return Collections.singletonList("confirm");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("ignore") && sender.hasPermission("autowhitelistremove.ignore")) {
            return Arrays.asList("add", "remove");
        }
        return Collections.emptyList();
    }
}
