package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.events.IslandLevelEvent;

/**
 * A class that calculates the level of an island. Runs through all the island
 * blocks
 * Designed to run repeatedly until the calculation is done.
 * 
 * @author tastybento
 * 
 */
public class LevelCalc extends BukkitRunnable {
    private ASkyBlock plugin;
    private Location l;
    private int counter;
    private int px;
    private int pz;
    private int slice;
    private HashMap<MaterialData, Integer> limitCount = new HashMap<MaterialData, Integer>();
    private int blockCount;
    private int underWaterBlockCount;
    private int oldLevel;
    private UUID targetPlayer;
    private Player asker;
    private int range = Settings.island_protectionRange;
    private boolean silent = false;

    public LevelCalc(ASkyBlock plugin, UUID targetPlayer, Player asker) {
	this(plugin, targetPlayer, asker, false);
    }

    /**
     * Calculates the level of an island
     * 
     * @param plugin
     * @param targetPlayer
     * @param asker
     */
    public LevelCalc(ASkyBlock plugin, UUID targetPlayer, Player asker, boolean silent) {
	this.silent = silent;
	this.plugin = plugin;
	// plugin.getLogger().info("DEBUG: running level calc " + silent);
	this.targetPlayer = targetPlayer;
	this.asker = asker;
	this.counter = 255;
	if (plugin.getPlayers().inTeam(targetPlayer)) {
	    this.l = plugin.getPlayers().getTeamIslandLocation(targetPlayer);
	} else {
	    this.l = plugin.getPlayers().getIslandLocation(targetPlayer);
	}
	this.px = l.getBlockX();
	this.pz = l.getBlockZ();
	// Get player's island
	Island island = plugin.getGrid().getIsland(targetPlayer);
	if (island != null) {
	    range = island.getProtectionSize();
	} else {
	    range = Settings.island_protectionRange;
	}
	// Calculated based on the size of the protection area
	double ratio = (double) counter * 12100 / (double) (range*range);
	// plugin.getLogger().info("DEBUG: ratio = " + ratio +
	// " protection range = " + range);
	this.slice = (int) ratio;
	if (this.slice < 1) {
	    this.slice = 1;
	}
	// Copy the limits hashmap
	for (MaterialData m : Settings.blockLimits.keySet()) {
	    limitCount.put(m, Settings.blockLimits.get(m));
	    //plugin.getLogger().info("DEBUG:" + m.toString() + " x " + Settings.blockLimits.get(m));
	}
	this.blockCount = 0;
	this.underWaterBlockCount = 0;
	oldLevel = plugin.getPlayers().getIslandLevel(targetPlayer);
    }

    @Override
    public void run() {
	// Only run if the flag is set
	if (!plugin.isCalculatingLevel()) {
	    this.cancel();
	}
	//slice = 256;
	//plugin.getLogger().info("DEBUG: slice = " + slice);
	//long lastPoll = System.currentTimeMillis();
	calculateSlice(counter, (counter - slice));
	//plugin.getLogger().info("DEBUG: timer = " + (System.currentTimeMillis()- lastPoll) + "ms");
	counter = counter - slice - 1;
	if (counter <= 0) {
	    // Calculations are complete
	    // Add in the under water count
	    blockCount += (int)((double)underWaterBlockCount * Math.max(Settings.underWaterMultiplier,1D));
	    // Add a multiplier based on the island rating 50 = normal, 100 = max hard, 1 = max easy
	    // TODO: Removing this functionality for now as it will be too confusing for players
	    // Need to get the rating for the LEADER of the island, not the target player
	    // int multiplier = plugin.getPlayers().getStartIslandRating(leader);
	    // If not zero then use it.
	    //plugin.getLogger().info("DEBUG: block count = " + blockCount);
	    // blockCount = (blockCount * multiplier) / 5000;
	    blockCount /= Settings.levelCost;
	    // plugin.getLogger().info("DEBUG: updating player");
	    // Update player and team mates
	    plugin.getPlayers().setIslandLevel(targetPlayer, blockCount);
	    // plugin.getLogger().info("DEBUG: set island level, now trying to save player");
	    plugin.getPlayers().save(targetPlayer);
	    // plugin.getLogger().info("DEBUG: save player, now looking at team members");
	    // Update any team members too
	    if (plugin.getPlayers().inTeam(targetPlayer)) {
		// plugin.getLogger().info("DEBUG: player is in team");
		for (UUID member : plugin.getPlayers().getMembers(targetPlayer)) {
		    // plugin.getLogger().info("DEBUG: updating team member level too");
		    plugin.getPlayers().setIslandLevel(member, blockCount);
		    plugin.getPlayers().save(member);
		}
	    }
	    // plugin.getLogger().info("DEBUG: finished team member saving");
	    // plugin.updateTopTen();
	    if (!this.silent) {
		// Tell offline team members the island level increased.
		if (plugin.getPlayers().getIslandLevel(targetPlayer) > oldLevel) {
		    // plugin.getLogger().info("DEBUG: telling offline players");
		    plugin.getMessages().tellOfflineTeam(targetPlayer, ChatColor.GREEN + plugin.myLocale(targetPlayer).islandislandLevelis + " " + ChatColor.WHITE
			    + plugin.getPlayers().getIslandLevel(targetPlayer));
		}
		if (asker.isOnline()) {
		    // plugin.getLogger().info("DEBUG: updating player GUI");
		    asker.sendMessage(ChatColor.GREEN + plugin.myLocale(asker.getUniqueId()).islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
		}
	    }
	    this.silent = false;
	    // plugin.getLogger().info("DEBUG: updating top ten");
	    if (plugin.getPlayers().inTeam(targetPlayer)) {
		UUID leader = plugin.getPlayers().getTeamLeader(targetPlayer);
		if (leader != null) {
		    TopTen.topTenAddEntry(leader, blockCount);
		}
	    } else {
		TopTen.topTenAddEntry(targetPlayer, blockCount);
	    }
	    // Fire the level event
	    Island island = plugin.getGrid().getIsland(targetPlayer);
	    final IslandLevelEvent event = new IslandLevelEvent(targetPlayer, island, blockCount);
	    plugin.getServer().getPluginManager().callEvent(event);
	    // plugin.getLogger().info("DEBUG: finished updating top ten");

	    // plugin.getLogger().info("DEBUG: clearing flag");
	    // Clear flag
	    plugin.setCalculatingLevel(false);
	    // Cancel this task
	    // plugin.getLogger().info("DEBUG: canceling task");
	    this.cancel();
	    // plugin.getLogger().info("DEBUG: cancelled");
	}
    }

    /**
     * This calculates the value of a horizontal slice of island space.
     * It may be 255 to 0, or it may be smaller. The larger the island area, the smaller the slice.
     * This keeps the per-tick time low enough not to cause lag.
     * @param top
     * @param bottom
     */
    private void calculateSlice(int top, int bottom) {
	//plugin.getLogger().info("DEBUG: calculating top = " + top + " bottom = "+ bottom);
	if (bottom < 0) {
	    bottom = 0;
	}
	int r = range /2;
	//plugin.getLogger().info("DEBUG: range = " + r);
	for (int y = top; y >= bottom; y--) {
	    // plugin.getLogger().info("DEBUG: y = " + y);
	    //plugin.getLogger().info("DEBUG: blockcount = " + blockCount);
	    //plugin.getLogger().info("DEBUG: underwater blockcount = " + underWaterBlockCount);
	    for (int x = px - r; x <= px + r; x++) {
		for (int z = pz - r; z <= pz + r; z++) {
		    Material blockType = l.getWorld().getBlockAt(x, y, z).getType();
		    byte data = l.getWorld().getBlockAt(x, y, z).getData();
		    MaterialData md = new MaterialData(blockType);
		    md.setData(data);
		    MaterialData generic = new MaterialData(blockType);
		    if (blockType != Material.AIR) {
			// Total up the values
			if (limitCount.containsKey(md) && Settings.blockValues.containsKey(md)) {
			    int count = limitCount.get(md);
			    //plugin.getLogger().info("DEBUG: Count for non-generic " + md + " is " + count);
			    if (count > 0) {
				limitCount.put(md, --count);
				if (y<Settings.sea_level) {
				    underWaterBlockCount += Settings.blockValues.get(md);
				} else {
				    blockCount += Settings.blockValues.get(md);
				}
			    }
			} else if (limitCount.containsKey(generic) && Settings.blockValues.containsKey(generic)) {
			    int count = limitCount.get(generic);
			    //plugin.getLogger().info("DEBUG: Count for generic " + generic + " is " + count);
			    if (count > 0) {  
				limitCount.put(md, --count);
				if (y<Settings.sea_level) {
				    underWaterBlockCount += Settings.blockValues.get(generic);
				} else {
				    blockCount += Settings.blockValues.get(generic);
				}
			    }
			} else if (Settings.blockValues.containsKey(md)) {
			    //plugin.getLogger().info("DEBUG: Adding " + md + " = " + Settings.blockValues.get(md));
			    if (y<Settings.sea_level) {
				underWaterBlockCount += Settings.blockValues.get(md);
			    } else {
				blockCount += Settings.blockValues.get(md);
			    }
			} else if (Settings.blockValues.containsKey(generic)) {
			    //plugin.getLogger().info("DEBUG: Adding " + generic + " = " + Settings.blockValues.get(generic));
			    if (y<Settings.sea_level) {
				underWaterBlockCount += Settings.blockValues.get(generic);
			    } else {
				blockCount += Settings.blockValues.get(generic);
			    }
			}

		    }
		}
	    }
	}
    }

}