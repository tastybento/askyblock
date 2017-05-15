/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.wasteofplastic.askyblock;

import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.wasteofplastic.askyblock.util.Util;

/**
 * This class runs when the config file is not set up enough, or is unsafe
 * It provides useful information to the admin on what is wrong.
 * 
 * @author tastybento
 * 
 */
public class NotSetup implements CommandExecutor {

    public enum Reason {
        DISTANCE, GENERATOR, WORLD_NAME, CONFIG_OUTDATED;
    };

    private Reason reason;
    private ASkyBlock plugin;

    /**
     * Handles plugin operation if a critical setup parameter is missing
     * 
     * @param reason
     */
    public NotSetup(ASkyBlock plugin, Reason reason) {
        this.plugin = plugin;
        this.reason = reason;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
        // Get the default language
        Settings.defaultLanguage = plugin.getConfig().getString("general.defaultlanguage", "en-US");

        // Load languages
        HashMap<String,ASLocale> availableLocales = new HashMap<String,ASLocale>();
        FileLister fl = new FileLister(plugin);
        try {
            int index = 1;
            for (String code: fl.list()) {
                //plugin.getLogger().info("DEBUG: lang file = " + code);
                availableLocales.put(code, new ASLocale(plugin, code, index++));
            }
        } catch (IOException e1) {
            plugin.getLogger().severe("Could not add locales!");
        }
        if (!availableLocales.containsKey(Settings.defaultLanguage)) {
            plugin.getLogger().severe("'" + Settings.defaultLanguage + ".yml' not found in /locale folder. Using /locale/en-US.yml");
            Settings.defaultLanguage = "en-US";
            availableLocales.put(Settings.defaultLanguage, new ASLocale(plugin, Settings.defaultLanguage, 0));
        }
        plugin.setAvailableLocales(availableLocales);
        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().notSetupHeader);
        switch (reason) {
        case DISTANCE:
            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().notSetupDistance);
            break;
        case GENERATOR:
            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().notSetupGenerator);
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().notSetupGeneratorMultiverse);
            }
            break;
        case WORLD_NAME:
            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().notSetupWorldname);
            break;
        case CONFIG_OUTDATED:
        	Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().notSetupConfigOutdated);
        	break;
        default:
            break;
        }
        return true;
    }

}