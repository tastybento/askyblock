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

package com.wasteofplastic.askyblock.util;

import org.bukkit.Bukkit;

import com.wasteofplastic.askyblock.Settings;

/**
 * General purpose logging with access to a debug level
 * @author tastybento
 *
 */
public class Logger {
    /**
     * General purpose logger to reduce console spam
     * @param level
     * @param info
     */
    public static void logger(int level, String info) {
        if (level <= Settings.debug) {
            Bukkit.getLogger().info("DEBUG ["+level+"]:"+info);
        }
    }

}
