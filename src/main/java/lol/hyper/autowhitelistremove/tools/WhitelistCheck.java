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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
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
            autoWhitelistRemove.logger.warning("inactive-period is NOT SET!");
            return Collections.emptySet();
        }
        autoWhitelistRemove.logger.info("Checking for inactive players...");
        autoWhitelistRemove.logger.info("Current duration is set to " + inactivePeriod);

        int inactivePlayersCounter = 0;
        // go through each player on the whitelist
        for (OfflinePlayer offlinePlayer : Bukkit.getWhitelistedPlayers()) {
            UUID uuid = offlinePlayer.getUniqueId();
            String playerUsername = offlinePlayer.getName();

            // skip players that have not logged in
            if (!offlinePlayer.hasPlayedBefore() || offlinePlayer.getLastLogin() == 0) {
                autoWhitelistRemove.logger.info("Skipping player " + playerUsername + " since they have not played yet.");
                continue;
            }

            // skip if their UUID is on the ignored list
            if (autoWhitelistRemove.config.getStringList("ignored-players").contains(uuid.toString())) {
                autoWhitelistRemove.logger.info("Skipping player " + playerUsername + " because they are on the ignored list.");
                continue;
            }

            // skip if their name is on the ignored list
            if (autoWhitelistRemove.config.getStringList("ignored-players").contains(playerUsername)) {
                autoWhitelistRemove.logger.info("Skipping player " + playerUsername + " because they are on the ignored list.");
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
            autoWhitelistRemove.logger.info(inactivePlayersCounter + " players are going to be removed.");
            removePlayers(inactivePlayers);
        } else {
            autoWhitelistRemove.logger.info(inactivePlayersCounter + " players can be removed.");
        }

        return inactivePlayersName;
    }

    /**
     * Remove these players from the whitelist.
     *
     * @param playersToRemove List of players to remove.
     */
    private void removePlayers(Set<OfflinePlayer> playersToRemove) {
        for (OfflinePlayer offlinePlayer : playersToRemove) {
            offlinePlayer.setWhitelisted(false);
        }

        if (autoWhitelistRemove.config.getBoolean("save-whitelist-removals")) {
            exportPlayers(playersToRemove);
        }
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
                    autoWhitelistRemove.logger.info(playerUsername
                            + " can be removed! They haven't played in over " + duration
                            + " weeks! Last online: " + weeksBetween + " weeks ago.");
                    return true;
                }
                break;
            }
            case "d": {
                // calc how many days they haven't played for
                long daysBetween = getDaysBetween(lastPlayed, new Date());
                // if they are too inactive, remove them
                if (daysBetween >= duration) {
                    autoWhitelistRemove.logger.info(playerUsername
                            + " can be removed! They haven't played in over " + duration
                            + " days! Last online: " + daysBetween + " days ago.");
                    return true;
                }
                break;
            }
            case "m": {
                // calc how many months they haven't played for
                long monthsBetween = getMonthsBetween(lastPlayed, new Date());
                // if they are too inactive, remove them
                if (monthsBetween >= duration) {
                    autoWhitelistRemove.logger.info(playerUsername
                            + " can be removed! They haven't played in over " + duration
                            + " months! Last online: " + monthsBetween + " months ago.");
                    return true;
                }
                break;
            }
            default: {
                // if the config syntax is wrong, then this is the safe way of telling the user it's wrong
                autoWhitelistRemove.logger.warning(
                        "Invalid time duration " + timeType + "! Please check your config!");
            }
        }
        return false;
    }

    /**
     * Read contents of a file stored as a JSONArray.
     *
     * @param file File to read data from.
     * @return JSONArray from said file.
     */
    private JSONArray readFile(File file) {
        JSONArray object = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            object = new JSONArray(sb.toString());
            br.close();
        } catch (Exception e) {
            autoWhitelistRemove.logger.severe("Unable to read file " + file.getAbsolutePath());
            autoWhitelistRemove.logger.severe("This is bad, really bad.");
            e.printStackTrace();
        }
        return object;
    }

    /**
     * Write data to JSON file.
     *
     * @param file        File to write data to.
     * @param jsonToWrite Data to write to file. This much be a JSON string.
     */
    private void writeFile(File file, String jsonToWrite) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(jsonToWrite);
            writer.close();
        } catch (IOException e) {
            autoWhitelistRemove.logger.severe("Unable to write file " + file.getAbsolutePath());
            autoWhitelistRemove.logger.severe("This is bad, really bad.");
            e.printStackTrace();
        }
    }

    /**
     * Export players to removals.json
     *
     * @param players Players to export.
     */
    private void exportPlayers(Set<OfflinePlayer> players) {
        JSONArray array;
        if (autoWhitelistRemove.removalsFile.exists()) {
            array = readFile(autoWhitelistRemove.removalsFile);
        } else {
            array = new JSONArray();
        }
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
        writeFile(autoWhitelistRemove.removalsFile, array.toString(4));
    }
}
