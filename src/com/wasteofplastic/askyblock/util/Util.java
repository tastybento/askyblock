package com.wasteofplastic.askyblock.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.PlayerCache;

/**
 * A set of utility methods
 * 
 * @author tastybento
 * 
 */
public class Util {
    private static ASkyBlock plugin = ASkyBlock.getPlugin();

    /**
     * Loads a YAML file and if it does not exist it is looked for in the JAR
     * 
     * @param file
     * @return
     */
    public static YamlConfiguration loadYamlFile(String file) {
	File dataFolder = plugin.getDataFolder();
	File yamlFile = new File(dataFolder, file);

	YamlConfiguration config = null;
	if (yamlFile.exists()) {
	    try {
		config = new YamlConfiguration();
		config.load(yamlFile);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    // Create the missing file
	    config = new YamlConfiguration();
	    plugin.getLogger().info("No " + file + " found. Creating it...");
	    try {
		if (plugin.getResource(file) != null) {
		    plugin.getLogger().info("Using default found in jar file.");
		    plugin.saveResource(file, false);
		    config = new YamlConfiguration();
		    config.load(yamlFile);
		} else {
		    config.save(yamlFile);
		}
	    } catch (Exception e) {
		plugin.getLogger().severe("Could not create the " + file + " file!");
	    }
	}
	return config;
    }

    /**
     * Saves a YAML file
     * 
     * @param yamlFile
     * @param fileLocation
     */
    public static void saveYamlFile(YamlConfiguration yamlFile, String fileLocation) {
	File dataFolder = plugin.getDataFolder();
	File file = new File(dataFolder, fileLocation);

	try {
	    yamlFile.save(file);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Cuts up a string into multiple lines with the same color code at the
     * start of each line
     * 
     * @param color
     * @param longLine
     * @param length
     * @return List containing the colored lines
     */
    public static List<String> chop(ChatColor color, String longLine, int length) {
	List<String> result = new ArrayList<String>();
	// int multiples = longLine.length() / length;
	int i = 0;
	for (i = 0; i < longLine.length(); i += length) {
	    // for (int i = 0; i< (multiples*length); i += length) {
	    int endIndex = Math.min(i + length, longLine.length());
	    String line = longLine.substring(i, endIndex);
	    // Do the following only if i+length is not the end of the string
	    if (endIndex < longLine.length()) {
		// Check if last character in this string is not a space
		if (!line.substring(line.length() - 1).equals(" ")) {
		    // If it is not a space, check to see if the next character
		    // in long line is a space.
		    if (!longLine.substring(endIndex, endIndex + 1).equals(" ")) {
			// If it is not, then we are cutting a word in two and
			// need to backtrack to the last space if possible
			int lastSpace = line.lastIndexOf(" ");
			// Only do this if there is a space in the line to
			// backtrack to...
			if (lastSpace != -1 && lastSpace < line.length()) {
			    line = line.substring(0, lastSpace);
			    i -= (length - lastSpace - 1);
			}
		    }
		}
	    }
	    // }
	    result.add(color + line);
	}
	// result.add(color + longLine.substring(i, longLine.length()));
	return result;
    }

    /**
     * Converts block face direction to radial degrees. Returns 0 if block face
     * is not radial.
     * 
     * @param face
     * @return degrees
     */
    public static float blockFaceToFloat(BlockFace face) {
	switch (face) {
	case EAST:
	    return 90F;
	case EAST_NORTH_EAST:
	    return 67.5F;
	case EAST_SOUTH_EAST:
	    return 0F;
	case NORTH:
	    return 0F;
	case NORTH_EAST:
	    return 45F;
	case NORTH_NORTH_EAST:
	    return 22.5F;
	case NORTH_NORTH_WEST:
	    return 337.5F;
	case NORTH_WEST:
	    return 315F;
	case SOUTH:
	    return 180F;
	case SOUTH_EAST:
	    return 135F;
	case SOUTH_SOUTH_EAST:
	    return 157.5F;
	case SOUTH_SOUTH_WEST:
	    return 202.5F;
	case SOUTH_WEST:
	    return 225F;
	case WEST:
	    return 270F;
	case WEST_NORTH_WEST:
	    return 292.5F;
	case WEST_SOUTH_WEST:
	    return 247.5F;
	default:
	    return 0F;
	}
    }

    /**
     * Converts a name like IRON_INGOT into Iron Ingot to improve readability
     * 
     * @param ugly
     *            The string such as IRON_INGOT
     * @return A nicer version, such as Iron Ingot
     * 
     *         Credits to mikenon on GitHub!
     */
    public static String prettifyText(String ugly) {
	if (!ugly.contains("_") && (!ugly.equals(ugly.toUpperCase())))
	    return ugly;
	String fin = "";
	ugly = ugly.toLowerCase();
	if (ugly.contains("_")) {
	    String[] splt = ugly.split("_");
	    int i = 0;
	    for (String s : splt) {
		i += 1;
		fin += Character.toUpperCase(s.charAt(0)) + s.substring(1);
		if (i < splt.length)
		    fin += " ";
	    }
	} else {
	    fin += Character.toUpperCase(ugly.charAt(0)) + ugly.substring(1);
	}
	return fin;
    }

    /**
     * Converts a serialized location to a Location. Returns null if string is
     * empty
     * 
     * @param s
     *            - serialized location in format "world:x:y:z"
     * @return Location
     */
    static public Location getLocationString(final String s) {
	if (s == null || s.trim() == "") {
	    return null;
	}
	final String[] parts = s.split(":");
	if (parts.length == 4) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    if (w == null) {
		return null;
	    }
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	} else if (parts.length == 6) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    if (w == null) {
		return null;
	    }
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
	    final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
	    return new Location(w, x, y, z, yaw, pitch);
	}
	return null;
    }

    /**
     * Converts a location to a simple string representation
     * If location is null, returns empty string
     * 
     * @param l
     * @return
     */
    static public String getStringLocation(final Location l) {
	if (l == null || l.getWorld() == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" + Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
    }

    /**
	 * Returns all of the items that begin with the given start, 
	 * ignoring case.  Intended for tabcompletion. 
	 * 
	 * @param list
	 * @param start
	 * @return
	 */
	public static List<String> tabLimit(final List<String> list, final String start) {
	final List<String> returned = new ArrayList<String>();
	for (String s : list) {
	if (s.toLowerCase().startsWith(start.toLowerCase())) {
		returned.add(s);
	}
	}
	
	return returned;
	}
	
	/**
	 * Gets a list of all players who are currently online.
	 * 
	 * @return
	 */
	public static List<String> getOnlinePlayerList() {
	final List<String> returned = new ArrayList<String>();
	final List<Player> players = PlayerCache.getOnlinePlayers();
	for (Player p : players) {
		returned.add(p.getName());
	}
	return returned;
	}
}
