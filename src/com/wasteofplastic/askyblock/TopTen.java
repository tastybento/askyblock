package com.wasteofplastic.askyblock;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.util.MapUtil;
import com.wasteofplastic.askyblock.util.Util;

/**
 * Handles all Top Ten List functions
 * 
 * @author tastybento
 * 
 */
public class TopTen {
    private static ASkyBlock plugin = ASkyBlock.getPlugin();
    // Top ten list of players
    private static Map<UUID, Integer> topTenList = new HashMap<UUID, Integer>();

    /**
     * Adds a player to the top ten, if the level is good enough
     * 
     * @param ownerUUID
     * @param level
     */
    public static void topTenAddEntry(UUID ownerUUID, int level) {
	// Special case for removals. If a level of zero is given the player
	// needs to be removed from the list
	if (level < 1) {
	    if (topTenList.containsKey(ownerUUID)) {
		topTenList.remove(ownerUUID);
	    }
	    return;
	}
	// Only keep the top 20
	topTenList.put(ownerUUID, level);
	topTenList = MapUtil.sortByValue(topTenList);
	// plugin.getLogger().info("DEBUG: +" + level + ": " +
	// topTenList.values().toString());
    }

    /**
     * Removes ownerUUID from the top ten list
     * 
     * @param ownerUUID
     */
    public static void topTenRemoveEntry(UUID ownerUUID) {
	topTenList.remove(ownerUUID);
    }

    /**
     * Generates a sorted map of islands for the Top Ten list from all player
     * files
     */
    public static void topTenCreate() {
	// This map is a list of owner and island level
	YamlConfiguration player = new YamlConfiguration();
	int index = 1;
	for (final File f : plugin.getPlayersFolder().listFiles()) {
	    // Need to remove the .yml suffix
	    String fileName = f.getName();
	    if (fileName.endsWith(".yml")) {
		try {
		    String playerUUIDString = fileName.substring(0, fileName.length() - 4);
		    final UUID playerUUID = UUID.fromString(playerUUIDString);
		    if (playerUUID == null) {
			plugin.getLogger().warning("Player file contains erroneous UUID data.");
			plugin.getLogger().info("Looking at " + playerUUIDString);
		    }
		    player.load(f);
		    index++;
		    if (index % 1000 == 0) {
			plugin.getLogger().info("Processed " + index + " players");
		    }
		    // Players player = new Players(this, playerUUID);
		    int islandLevel = player.getInt("islandLevel", 0);
		    String teamLeaderUUID = player.getString("teamLeader", "");
		    if (islandLevel > 0) {
			if (!player.getBoolean("hasTeam")) {
			    topTenAddEntry(playerUUID, islandLevel);
			} else if (!teamLeaderUUID.isEmpty()) {
			    if (teamLeaderUUID.equals(playerUUIDString)) {
				topTenAddEntry(playerUUID, islandLevel);
			    }
			}
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}
	plugin.getLogger().info("Processed " + index + " players");
	// Save the top ten
	topTenSave();
    }

    public static void topTenSave() {
	if (topTenList == null) {
	    return;
	}
	plugin.getLogger().info("Saving top ten list");
	// Make file
	File topTenFile = new File(plugin.getDataFolder(), "topten.yml");
	// Make configuration
	YamlConfiguration config = new YamlConfiguration();
	// Save config

	int rank = 0;
	for (Map.Entry<UUID, Integer> m : topTenList.entrySet()) {
	    if (rank++ == 10) {
		break;
	    }
	    config.set("topten." + m.getKey().toString(), m.getValue());
	}
	try {
	    config.save(topTenFile);
	    plugin.getLogger().info("Saved top ten list");
	} catch (Exception e) {
	    plugin.getLogger().severe("Could not save top ten list!");
	    e.printStackTrace();
	}
    }

    /**
     * Loads the top ten from the file system topten.yml. If it does not exist
     * then the top ten is created
     */
    public static void topTenLoad() {
	topTenList.clear();
	// Check to see if the top ten list exists
	File topTenFile = new File(plugin.getDataFolder(), "topten.yml");
	if (!topTenFile.exists()) {
	    plugin.getLogger().warning("Top ten file does not exist - creating it. This could take some time with a large number of players");
	    topTenCreate();
	    plugin.getLogger().warning("Completed top ten creation.");
	} else {
	    // Load the top ten
	    YamlConfiguration topTenConfig = Util.loadYamlFile("topten.yml");
	    // Load the values
	    if (topTenConfig.isSet("topten")) {
		for (String playerUUID : topTenConfig.getConfigurationSection("topten").getKeys(false)) {
		    // getLogger().info(playerUUID);
		    try {
			UUID uuid = UUID.fromString(playerUUID);
			// getLogger().info(uuid.toString());
			int level = topTenConfig.getInt("topten." + playerUUID);
			// getLogger().info("Level = " + level);
			TopTen.topTenAddEntry(uuid, level);
		    } catch (Exception e) {
			e.printStackTrace();
			plugin.getLogger().severe("Problem loading top ten list - recreating - this may take some time");
			topTenCreate();
		    }
		}
	    }
	}
    }

    /**
     * Displays the Top Ten list if it exists in chat
     * 
     * @param player
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    public static boolean topTenShow(final Player player) {
	player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).topTenheader);
	if (topTenList == null) {
	    topTenCreate();
	    // player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).topTenerrorNotReady);
	    // return true;
	}
	int i = 1;
	// getLogger().info("DEBUG: " + topTenList.toString());
	// getLogger().info("DEBUG: " + topTenList.values());
	for (Map.Entry<UUID, Integer> m : topTenList.entrySet()) {
	    final UUID playerUUID = m.getKey();
	    if (plugin.getPlayers().inTeam(playerUUID)) {
		final List<UUID> pMembers = plugin.getPlayers().getMembers(playerUUID);
		String memberList = "";
		for (UUID members : pMembers) {
		    memberList += plugin.getPlayers().getName(members) + ", ";
		}
		if (memberList.length() > 2) {
		    memberList = memberList.substring(0, memberList.length() - 2);
		}
		player.sendMessage(ChatColor.AQUA + "#" + i + ": " + plugin.getPlayers().getName(playerUUID) + " (" + memberList + ") - "
			+ plugin.myLocale(player.getUniqueId()).levelislandLevel + " " + m.getValue());
	    } else {
		player.sendMessage(ChatColor.AQUA + "#" + i + ": " + plugin.getPlayers().getName(playerUUID) + " - " + plugin.myLocale(player.getUniqueId()).levelislandLevel + " "
			+ m.getValue());
	    }
	    if (i++ == 10) {
		break;
	    }
	}
	return true;
    }

    static void remove(UUID owner) {
	topTenList.remove(owner);
    }

    /**
     * Get a sorted descending map of the top players
     * @return the topTenList - may be more or less than ten
     */
    public static Map<UUID, Integer> getTopTenList() {
        return topTenList;
    }
}