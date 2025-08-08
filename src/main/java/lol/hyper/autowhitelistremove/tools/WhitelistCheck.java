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

package lol.hyper.autowhitelistremove.tools;

import lol.hyper.autowhitelistremove.AutoWhitelistRemove;
import lol.hyper.hyperlib.utils.FileUtils;
import net.ess3.api.IUser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhitelistCheck {

    final Pattern pattern = Pattern.compile("\\d+([wdm])", Pattern.CASE_INSENSITIVE);
    private final AutoWhitelistRemove autoWhitelistRemove;

    public WhitelistCheck(AutoWhitelistRemove autoWhitelistRemove) {
        this.autoWhitelistRemove = autoWhitelistRemove;
    }

    /**
     * Verify the time duration in the config is correct.
     *
     * @param timeInConfig String from the config.
     * @return True if valid, false if not.
     */
    public boolean verifyTimeDuration(String timeInConfig) {
        Matcher matcher = pattern.matcher(timeInConfig);
        return matcher.matches();
    }

    /**
     * Check the whitelist and remove players if they are too inactive.
     *
     * @return A set of players removed/to be removed.
     */
    public Set<String> checkWhitelist(boolean actuallyRemove) {
        Set<String> inactivePlayersName = new HashSet<>();
        Set<OfflinePlayer> inactivePlayers = new HashSet<>();

        String inactivePeriod = autoWhitelistRemove.config.getString("inactive-period");
        if (inactivePeriod == null) {
            autoWhitelistRemove.logger.warn("inactive-period is NOT SET!");
            return Collections.emptySet();
        }
        autoWhitelistRemove.logger.info("Checking for inactive players...");
        autoWhitelistRemove.logger.info("Current duration is set to {}", inactivePeriod);

        int inactivePlayersCounter = 0;
        // go through each player on the whitelist
        for (OfflinePlayer offlinePlayer : Bukkit.getWhitelistedPlayers()) {
            UUID uuid = offlinePlayer.getUniqueId();
            String playerUsername = offlinePlayer.getName();
            IUser user = autoWhitelistRemove.hookAPIEssentials().getUser(uuid);

            if (user != null) {
                // skip players that have not logged in
                if (!offlinePlayer.hasPlayedBefore() || offlinePlayer.getLastLogin() == 0) {
                    offlinePlayer.setWhitelisted(false);
                    autoWhitelistRemove.logger.info("Skipping player {} since they have not played yet.", playerUsername);
                    continue;
                }

                // skip if their UUID is on the ignored list
                if (autoWhitelistRemove.config.getStringList("ignored-players").contains(uuid.toString())) {
                    autoWhitelistRemove.logger.info("Skipping player {} because their UUID is on the ignored list.", playerUsername);
                    continue;
                }

                // skip if their name is on the ignored list
                if (autoWhitelistRemove.config.getStringList("ignored-players").contains(playerUsername)) {
                    autoWhitelistRemove.logger.info("Skipping player {} because their name is on the ignored list.", playerUsername);
                    continue;
                }

                boolean inactive = isPlayerInactive(offlinePlayer, inactivePeriod);
                if (inactive) {
                    inactivePlayersCounter++;
                    inactivePlayersName.add(offlinePlayer.getName());
                    inactivePlayers.add(offlinePlayer);
                }
            }

            if (actuallyRemove) {
                autoWhitelistRemove.logger.info("{} players are going to be removed.", inactivePlayersCounter);
                removePlayers(inactivePlayers);
            } else {
                autoWhitelistRemove.logger.info("{} players can be removed.", inactivePlayersCounter);
            }
        }
        return inactivePlayersName;
    }

    /**
     * Remove these players from the whitelist.
     *
     * @param playersToRemove List of players to remove.
     */
    private void removePlayers(Set<OfflinePlayer> playersToRemove) {
        List<String> commands = autoWhitelistRemove.config.getStringList("extra-commands");
        Bukkit.getGlobalRegionScheduler().run(autoWhitelistRemove, t -> {
            for (OfflinePlayer offlinePlayer : playersToRemove) {
                if (!commands.isEmpty()) {
                    for (String command : commands) {
                        String finalCommand = this.getCommand(offlinePlayer, command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    }
                }
                offlinePlayer.setWhitelisted(false);
            }
        });

        if (autoWhitelistRemove.config.getBoolean("save-whitelist-removals")) {
            exportPlayers(playersToRemove);
        }
    }

    private @NotNull String getCommand(OfflinePlayer offlinePlayer, String command) {
        String finalCommand = command;
        if (command.contains("%player%")) {
            final String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "UnknownPlayer";
            finalCommand = command.replace("%player%", playerName);
        }
        if (command.contains("%uuid%")) {
            finalCommand = command.replace("%uuid%", offlinePlayer.getUniqueId().toString());
        }
        return finalCommand;
    }

    /**
     * Get the total weeks between start and end date.
     *
     * @param d1 The first date (in the past or "older" date)
     * @param d2 The more recent date.
     * @return The total weeks that have passed.
     */
    public long getWeeksBetween(Date d1, Date d2) {
        return ChronoUnit.WEEKS.between(
                d1.toInstant().atZone(ZoneId.systemDefault()), d2.toInstant().atZone(ZoneId.systemDefault()));
    }

    /**
     * Get the total days between start and end date.
     *
     * @param d1 The first date (in the past or "older" date)
     * @param d2 The more recent date.
     * @return The total days that have passed.
     */
    public long getDaysBetween(Date d1, Date d2) {
        return ChronoUnit.DAYS.between(
                d1.toInstant().atZone(ZoneId.systemDefault()), d2.toInstant().atZone(ZoneId.systemDefault()));
    }

    /**
     * Get the total months between start and end date.
     *
     * @param d1 The first date (in the past or "older" date)
     * @param d2 The more recent date.
     * @return The total months that have passed.
     */
    public long getMonthsBetween(Date d1, Date d2) {
        return ChronoUnit.MONTHS.between(
                d1.toInstant().atZone(ZoneId.systemDefault()), d2.toInstant().atZone(ZoneId.systemDefault()));
    }

    /**
     * Check to see if a player has been inactive.
     *
     * @param playerToCheck  The player to check.
     * @param inactivePeriod The time duration of being inactive. 3d, 2m, 4w.
     * @return If the player is not active in the given duration.
     */
    private boolean isPlayerInactive(OfflinePlayer playerToCheck, String inactivePeriod) {
        String playerUsername = playerToCheck.getName();
        String timeType = inactivePeriod.substring(inactivePeriod.length() - 1);
        // get when they lasted played
        Date lastPlayed = new Date(playerToCheck.getLastLogin());
        // get how long they have to be offline
        int duration = Integer.parseInt(inactivePeriod.substring(0, inactivePeriod.length() - 1));

        // check if we are using weeks/days/months for the time period
        // there is probably a better way of doing this, but this is a safe way
        switch (timeType) {
            case "w": {
                // calc how many weeks they haven't played for
                long weeksBetween = getWeeksBetween(lastPlayed, new Date());
                // if they are too inactive, remove them
                if (weeksBetween >= duration) {
                    autoWhitelistRemove.logger.info("{} can be removed! They haven't played in over {} weeks! Last online: {} weeks ago.", playerUsername, duration, weeksBetween);
                    return true;
                }
                break;
            }
            case "d": {
                // calc how many days they haven't played for
                long daysBetween = getDaysBetween(lastPlayed, new Date());
                // if they are too inactive, remove them
                if (daysBetween >= duration) {
                    autoWhitelistRemove.logger.info("{} can be removed! They haven't played in over {} days! Last online: {} days ago.", playerUsername, duration, daysBetween);
                    return true;
                }
                break;
            }
            case "m": {
                // calc how many months they haven't played for
                long monthsBetween = getMonthsBetween(lastPlayed, new Date());
                // if they are too inactive, remove them
                if (monthsBetween >= duration) {
                    autoWhitelistRemove.logger.info("{} can be removed! They haven't played in over {} months! Last online: {} months ago.", playerUsername, duration, monthsBetween);
                    return true;
                }
                break;
            }
            default: {
                // if the config syntax is wrong, then this is the safe way of telling the user it's wrong
                autoWhitelistRemove.logger.warn("Invalid time duration {}! Please check your config!", timeType);
            }
        }
        return false;
    }

    /**
     * Export players to removals.json
     *
     * @param players Players to export.
     */
    private void exportPlayers(Set<OfflinePlayer> players) {
        String arrayRaw;
        if (autoWhitelistRemove.removalsFile.exists()) {
            arrayRaw = FileUtils.readFile(autoWhitelistRemove.removalsFile);
        } else {
            arrayRaw = "[]";
        }
        JSONArray array = new JSONArray(arrayRaw);
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String strDate = dateFormat.format(date);
        for (OfflinePlayer player : players) {
            JSONObject object = new JSONObject();
            object.put("name", player.getName());
            object.put("uuid", player.getUniqueId());
            object.put("date", strDate);
            array.put(object);
        }
        FileUtils.writeFile(array.toString(4), autoWhitelistRemove.removalsFile);
    }
}
