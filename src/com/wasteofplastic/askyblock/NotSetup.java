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

    /**
     * Handles plugin operation if a critical setup parameter is missing
     * 
     * @param reason
     */
    public NotSetup(Reason reason) {
        this.reason = reason;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
        Util.sendMessage(sender, ChatColor.RED + ASkyBlock.getPlugin().myLocale().notSetupHeader);
        switch (reason) {
        case DISTANCE:
            Util.sendMessage(sender, ChatColor.RED + ASkyBlock.getPlugin().myLocale().notSetupDistance);
            break;
        case GENERATOR:
            Util.sendMessage(sender, ChatColor.RED + ASkyBlock.getPlugin().myLocale().notSetupGenerator);
            if (Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
                Util.sendMessage(sender, ChatColor.RED + ASkyBlock.getPlugin().myLocale().notSetupGeneratorMultiverse);
            }
            break;
        case WORLD_NAME:
            Util.sendMessage(sender, ChatColor.RED + ASkyBlock.getPlugin().myLocale().notSetupWorldname);
            break;
        case CONFIG_OUTDATED:
        	Util.sendMessage(sender, ChatColor.RED + ASkyBlock.getPlugin().myLocale().notSetupConfigOutdated);
        	break;
        default:
            break;
        }
        return true;
    }

}