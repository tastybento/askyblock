/**
 *
 */
package com.wasteofplastic.askyblock.util;

import com.wasteofplastic.askyblock.Settings;
import org.bukkit.Bukkit;

/**
 * General purpose logging with access to a debug level
 *
 * @author tastybento
 */
public class Logger {
    /**
     * General purpose logger to reduce console spam
     *
     * @param level
     * @param info
     */
    public static void logger(int level, String info) {
        if (level <= Settings.debug) {
            Bukkit.getLogger().info("DEBUG [" + level + "]:" + info);
        }
    }

}
