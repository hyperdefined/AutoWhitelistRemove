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

import com.earth2me.essentials.Essentials;
import lol.hyper.autowhitelistremove.command.CommandAWR;
import lol.hyper.autowhitelistremove.tools.WhitelistCheck;
import lol.hyper.hyperlib.HyperLib;
import lol.hyper.hyperlib.bstats.HyperStats;
import lol.hyper.hyperlib.releases.HyperUpdater;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class AutoWhitelistRemove extends JavaPlugin {

    public final ComponentLogger logger = this.getComponentLogger();
    public final File configFile = new File(this.getDataFolder(), "config.yml");
    public final File removalsFile = new File(this.getDataFolder(), "removals.json");
    public FileConfiguration config;
    public WhitelistCheck whitelistCheck;
    public CommandAWR commandAWR;
    private Essentials api;

    @Override
    public void onEnable() {
        HyperLib hyperLib = new HyperLib(this);
        hyperLib.setup();

        HyperStats stats = new HyperStats(hyperLib, 11684);
        stats.setup();

        whitelistCheck = new WhitelistCheck(this);
        commandAWR = new CommandAWR(this);
        api = (Essentials) this.getServer().getPluginManager().getPlugin("Essentials");
        this.getCommand("awr").setExecutor(commandAWR);
        loadConfig();
        if (config.getBoolean("autoremove-on-start")) {
            Bukkit.getGlobalRegionScheduler().runDelayed(this, scheduledTask -> whitelistCheck.checkWhitelist(true), 50);
        }

        HyperUpdater updater = new HyperUpdater(hyperLib);
        updater.setGitHub("hyperdefined", "AutoWhitelistRemove");
        updater.setModrinth("YLFgSRDy");
        updater.setHangar("AutoWhitelistRemove", "paper");
        updater.check();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            this.saveResource("config.yml", true);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        int CONFIG_VERSION = 3;
        if (config.getInt("config-version") != CONFIG_VERSION) {
            logger.warn("Your configuration is out of date! Some features may not work!");
        }
        boolean isCorrect = whitelistCheck.verifyTimeDuration(config.getString("inactive-period"));
        if (!isCorrect) {
            logger.warn("The time duration currently set is not valid! This will break everything!");
        }
    }

    public Essentials hookAPIEssentials() {
        return api;
    }
}
