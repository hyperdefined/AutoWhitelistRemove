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

package lol.hyper.autowhitelistremove;

import lol.hyper.autowhitelistremove.command.CommandAWR;
import lol.hyper.autowhitelistremove.tools.WhitelistCheck;
import lol.hyper.githubreleaseapi.GitHubRelease;
import lol.hyper.githubreleaseapi.GitHubReleaseAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class AutoWhitelistRemove extends JavaPlugin {

    public final Logger logger = this.getLogger();
    public final File configFile = new File(this.getDataFolder(), "config.yml");
    public final File removalsFile = new File(this.getDataFolder(), "removals.json");
    public FileConfiguration config;
    public WhitelistCheck whitelistCheck;
    public CommandAWR commandAWR;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        adventure = BukkitAudiences.create(this);
        whitelistCheck = new WhitelistCheck(this);
        commandAWR = new CommandAWR(this);
        this.getCommand("awr").setExecutor(commandAWR);
        loadConfig();
        if (config.getBoolean("autoremove-on-start")) {
            Bukkit.getAsyncScheduler().runDelayed(this, t -> whitelistCheck.checkWhitelist(true), 50L, TimeUnit.MILLISECONDS);
        }

        new Metrics(this, 11684);

        Bukkit.getAsyncScheduler().runNow(this, t -> this.checkForUpdates());
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            this.saveResource("config.yml", true);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        int CONFIG_VERSION = 2;
        if (config.getInt("config-version") != CONFIG_VERSION) {
            logger.warning("Your configuration is out of date! Some features may not work!");
        }
        boolean isCorrect = whitelistCheck.verifyTimeDuration(config.getString("inactive-period"));
        if (!isCorrect) {
            logger.warning("The time duration currently set is not valid! This will break everything!");
        }
    }

    public void checkForUpdates() {
        GitHubReleaseAPI api;
        try {
            api = new GitHubReleaseAPI("AutoWhitelistRemove", "hyperdefined");
        } catch (IOException e) {
            logger.warning("Unable to check updates!");
            e.printStackTrace();
            return;
        }
        GitHubRelease current = api.getReleaseByTag(this.getDescription().getVersion());
        GitHubRelease latest = api.getLatestVersion();
        if (current == null) {
            logger.warning("You are running a version that does not exist on GitHub. If you are in a dev environment, you can ignore this. Otherwise, this is a bug!");
            return;
        }
        int buildsBehind = api.getBuildsBehind(current);
        if (buildsBehind == 0) {
            logger.info("You are running the latest version.");
        } else {
            logger.warning("A new version is available (" + latest.getTagVersion() + ")! You are running version " + current.getTagVersion() + ". You are " + buildsBehind + " version(s) behind.");
        }
    }

    public BukkitAudiences getAdventure() {
        if(this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }
}
