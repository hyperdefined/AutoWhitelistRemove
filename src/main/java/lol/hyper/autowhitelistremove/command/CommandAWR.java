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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CommandAWR implements TabExecutor {

    private final AutoWhitelistRemove autoWhitelistRemove;
    private final WhitelistCheck whitelistCheck;

    public CommandAWR(AutoWhitelistRemove autoWhitelistRemove) {
        this.autoWhitelistRemove = autoWhitelistRemove;
        this.whitelistCheck = autoWhitelistRemove.whitelistCheck;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
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
