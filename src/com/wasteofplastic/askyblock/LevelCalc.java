package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class LevelCalc extends BukkitRunnable {
    private ASkyBlock plugin;
    private Location l;
    private int counter;
    private int px;
    private int pz;
    private int slice;
    private HashMap<Material,Integer> limitCount = new HashMap<Material, Integer>();
    private int blockcount;
    private int oldLevel;
    private UUID targetPlayer;
    private Player asker;
    private int range = Settings.island_protectionRange;

    /**
     * Calculates the level of an island
     * @param plugin
     * @param targetPlayer
     * @param asker
     */
    public LevelCalc(ASkyBlock plugin, UUID targetPlayer, Player asker) {
	this.plugin = plugin;
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
	double ratio = (double)counter * 10000 / (double)(range * range);
	// plugin.getLogger().info("DEBUG: ratio = " + ratio + " protection range = " + range);
	this.slice = (int)ratio;
	if (this.slice < 1) {
	    this.slice = 1;
	}
	// Copy the limits hashmap	
	for (Material m : Settings.blockLimits.keySet()) {
	    limitCount.put(m, Settings.blockLimits.get(m));
	    // plugin.getLogger().info("DEBUG:" + m.toString() + " x " + Settings.blockLimits.get(m));
	}
	this.blockcount = 0;
	oldLevel = plugin.getPlayers().getIslandLevel(targetPlayer);
    }

    @Override
    public void run() {
	// Only run if the flag is set
	if (!plugin.isCalculatingLevel()) {
	    this.cancel();
	}
	// plugin.getLogger().info("DEBUG: slice = " + slice);
	calculateSlice(counter, (counter-slice));
	counter = counter - slice - 1;
	if (counter <=0) {
	    // plugin.getLogger().info("DEBUG: updating player");
	    // Update player and team mates
	    plugin.getPlayers().setIslandLevel(targetPlayer, blockcount / 100);
	    // plugin.getLogger().info("DEBUG: set island level, now trying to save player");
	    plugin.getPlayers().save(targetPlayer);
	    // plugin.getLogger().info("DEBUG: save player, now looking at team members");
	    // Update any team members too
	    if (plugin.getPlayers().inTeam(targetPlayer)) {
		// plugin.getLogger().info("DEBUG: player is in team");
		for (UUID member: plugin.getPlayers().getMembers(targetPlayer)) {
		    // plugin.getLogger().info("DEBUG: updating team member level too");
		    plugin.getPlayers().setIslandLevel(member, blockcount / 100);
		    plugin.getPlayers().save(member);
		}
	    }
	    // plugin.getLogger().info("DEBUG: finished team member saving");
	    //plugin.updateTopTen();
	    // Tell offline team members the island level increased.
	    if (plugin.getPlayers().getIslandLevel(targetPlayer) > oldLevel) {
		// plugin.getLogger().info("DEBUG: telling offline players");
		plugin.tellOfflineTeam(targetPlayer, ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
	    }
	    // plugin.getLogger().info("DEBUG: updating top ten");
	    plugin.updateTopTen(targetPlayer, blockcount / 100);
	    // plugin.getLogger().info("DEBUG: finished updating top ten");
	    if (asker.isOnline()) {
		// plugin.getLogger().info("DEBUG: updating player GUI");
		asker.sendMessage(
			ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
	    }
	    // plugin.getLogger().info("DEBUG: clearing flag");
	    // Clear flag
	    plugin.setCalculatingLevel(false);
	    // Cancel this task
	    // plugin.getLogger().info("DEBUG: cancelling task");
	    this.cancel();
	    // plugin.getLogger().info("DEBUG: cancelled");
	}

    }

    private void calculateSlice(int top, int bottom) {
	// plugin.getLogger().info("DEBUG: calculating top = " + top + " bottom = "+ bottom);
	if (bottom <0) {
	    bottom = 0;
	}
	for (int y = top; y >= bottom; y--) {
	    // plugin.getLogger().info("DEBUG: y = " + y);
	    for (int x = range / 2 * -1; x <= range / 2; x++) {
		for (int z = range / 2 * -1; z <= range / 2; z++) {
		    final Block b = new Location(l.getWorld(), px + x, y, pz + z).getBlock();
		    final Material blockType = b.getType();
		    if (blockType != Material.AIR) {
			// Total up the values
			if (Settings.blockValues.containsKey(blockType)) {
			    if (limitCount.containsKey(blockType)) {
				int count = limitCount.get(blockType);
				// plugin.getLogger().info("DEBUG: Count for " + blockType + " is " + count);
				if (count > 0) {
				    limitCount.put(blockType, --count);
				    blockcount += Settings.blockValues.get(blockType);
				} 
			    } else {
				// plugin.getLogger().info("DEBUG: Adding " + blockType);
				blockcount += Settings.blockValues.get(blockType);
			    }
			} 
		    }
		}

	    }
	}
    }

}
