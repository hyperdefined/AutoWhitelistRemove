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
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
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
    private final BukkitAudiences audiences;

    public CommandAWR(AutoWhitelistRemove autoWhitelistRemove) {
        this.autoWhitelistRemove = autoWhitelistRemove;
        this.whitelistCheck = autoWhitelistRemove.whitelistCheck;
        this.audiences = autoWhitelistRemove.getAdventure();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("autowhitelistremove.command")) {
            audiences.sender(sender).sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
            return true;
        }
        switch (args.length) {
            case 1: {
                switch (args[0]) {
                    case "check": {
                        if (!sender.hasPermission("autowhitelistremove.check")) {
                            audiences.sender(sender).sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
                        } else {
                            Set<String> removedPlayers = whitelistCheck.checkWhitelist(false);
                            audiences.sender(sender).sendMessage(Component.text("--------------------AWR---------------------").color(NamedTextColor.GOLD));
                            if (removedPlayers.isEmpty()) {
                                audiences.sender(sender).sendMessage(Component.text("No players to remove.").color(NamedTextColor.YELLOW));
                            } else {
                                audiences.sender(sender).sendMessage(Component.text(removedPlayers.size() + " players will be removed. Type \"/awr check confirm\" to confirm the removal.").color(NamedTextColor.YELLOW));
                                audiences.sender(sender).sendMessage(Component.text(String.join(", ", removedPlayers)).color(NamedTextColor.YELLOW));
                            }
                            audiences.sender(sender).sendMessage(Component.text("--------------------------------------------").color(NamedTextColor.GOLD));
                        }
                        return true;
                    }
                    case "reload": {
                        if (!sender.hasPermission("autowhitelistremove.reload")) {
                            audiences.sender(sender).sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
                        } else {
                            autoWhitelistRemove.loadConfig();
                            audiences.sender(sender).sendMessage(Component.text("Config reloaded!").color(NamedTextColor.GREEN));
                        }
                        return true;
                    }
                    default: {
                        audiences.sender(sender).sendMessage(Component.text("--------------------AWR---------------------").color(NamedTextColor.GOLD));
                        audiences.sender(sender).sendMessage(Component.text("/awr help ").color(NamedTextColor.GOLD).append(Component.text("- Shows this menu.").color(NamedTextColor.YELLOW)));
                        audiences.sender(sender).sendMessage(Component.text("/awr check ").color(NamedTextColor.GOLD).append(Component.text("- Check inactive players and remove them.").color(NamedTextColor.YELLOW)));
                        audiences.sender(sender).sendMessage(Component.text("/awr help ").color(NamedTextColor.GOLD).append(Component.text("- Reload the config.").color(NamedTextColor.YELLOW)));
                        audiences.sender(sender).sendMessage(Component.text("--------------------------------------------").color(NamedTextColor.GOLD));
                        return true;
                    }
                }
            }
            case 2: {
                if ("confirm".equalsIgnoreCase(args[1])) {
                    if (sender.hasPermission("autowhitelistremove.check.confirm")) {
                        Set<String> removedPlayers = whitelistCheck.checkWhitelist(true);
                        audiences.sender(sender).sendMessage(Component.text("--------------------AWR---------------------").color(NamedTextColor.GOLD));
                        if (removedPlayers.isEmpty()) {
                            audiences.sender(sender).sendMessage(Component.text("No players to remove.").color(NamedTextColor.YELLOW));
                        } else {
                            audiences.sender(sender).sendMessage(Component.text(removedPlayers.size() + " players have been removed.").color(NamedTextColor.YELLOW));
                            audiences.sender(sender).sendMessage(Component.text(String.join(", ", removedPlayers)).color(NamedTextColor.YELLOW));
                        }
                        audiences.sender(sender).sendMessage(Component.text("--------------------------------------------").color(NamedTextColor.GOLD));
                    } else {
                        audiences.sender(sender).sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
                    }
                    return true;
                }
                audiences.sender(sender).sendMessage(Component.text("--------------------AWR---------------------").color(NamedTextColor.GOLD));
                audiences.sender(sender).sendMessage(Component.text("/awr help ").color(NamedTextColor.GOLD).append(Component.text("- Shows this menu.").color(NamedTextColor.YELLOW)));
                audiences.sender(sender).sendMessage(Component.text("/awr check ").color(NamedTextColor.GOLD).append(Component.text("- Check inactive players and remove them.").color(NamedTextColor.YELLOW)));
                audiences.sender(sender).sendMessage(Component.text("/awr help ").color(NamedTextColor.GOLD).append(Component.text("- Reload the config.").color(NamedTextColor.YELLOW)));
                audiences.sender(sender).sendMessage(Component.text("--------------------------------------------").color(NamedTextColor.GOLD));
                return true;
            }
            default: {
                audiences.sender(sender).sendMessage(Component.text("AutoWhitelistRemove version " + autoWhitelistRemove.getDescription().getVersion() + ". Created by hyperdefined.").color(NamedTextColor.GREEN));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("check")) {
                return Collections.singletonList("confirm");
            }
        }
        return Arrays.asList("check", "help", "reload");
    }
}
