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

package com.wasteofplastic.askyblock.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.CoopPlay;
import com.wasteofplastic.askyblock.DeleteIslandChunk;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.PlayerCache;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.TopTen;
import com.wasteofplastic.askyblock.WarpSigns;
import com.wasteofplastic.askyblock.panels.BiomesPanel;
import com.wasteofplastic.askyblock.panels.ControlPanel;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/*
 * New commands:
 * 
 * teamkick <player> - removes a player from any team they are on. Does not throw stuff or teleport them.
 * teamadd <player> <leader> - adds the player to the leader's team. If leader does not have a team, one is made.
 * teamdelete <leader> - removes the leader's team completely
 * 
 */

/**
 * This class handles admin commands
 * 
 */
public class AdminCmd implements CommandExecutor, TabCompleter {
    private ASkyBlock plugin;
    private List<UUID> removeList = new ArrayList<UUID>();
    private boolean purgeFlag = false;
    private boolean confirmReq = false;
    private boolean confirmOK = false;
    private int confirmTimer = 0;
    private boolean purgeUnownedConfirm = false;
    private HashMap<String, Island> unowned = new HashMap<String,Island>();
    private boolean asyncPending = false;
    private BukkitTask asyncCheck;


    public AdminCmd(ASkyBlock aSkyBlock) {
	this.plugin = aSkyBlock;
    }

    private void help(CommandSender sender, String label) {
	if (!(sender instanceof Player)) {
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpreload);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " topten:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelptopTen);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " unregister <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpunregister);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpdelete);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " completechallenge <challengename> <player>:" + ChatColor.WHITE + " "
		    + plugin.myLocale().adminHelpcompleteChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " resetchallenge <challengename> <player>:" + ChatColor.WHITE + " "
		    + plugin.myLocale().adminHelpresetChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " resetallchallenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpresetAllChallenges);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelppurge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info challenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfoIsland);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " clearreset <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpclearReset);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " Sets leader's island biome.");
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " topbreeders: " + ChatColor.WHITE + " Lists most populated islands current loaded");
	    sender.sendMessage(ChatColor.GREEN + "== Team Editing Commands ==");
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " team kick <player>:" + ChatColor.WHITE + " Removes player from any team.");
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " team add <player> <leader>:" + ChatColor.WHITE + " Adds player to leader's team.");
	} else {
	    // Only give help if the player has permissions
	    // Permissions are split into admin permissions and mod permissions
	    Player player = (Player) sender;
	    player.sendMessage(plugin.myLocale(player.getUniqueId()).adminHelpHelp);
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reload") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " reload:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpreload);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.register") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " register <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpregister);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " unregister <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpunregister);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " delete <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpdelete);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.deleteisland") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " deleteisland confirm:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpdelete);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.purge") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurge);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge unowned:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurgeUnowned);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge allow/disallow:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurgeAllowDisallow);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topten") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " topten:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptopTen);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topbreeders") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " topbreeders: " + ChatColor.WHITE + " Lists most populated islands current loaded");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " completechallenge <challengename> <player>:" + ChatColor.WHITE + " "
			+ plugin.myLocale(player.getUniqueId()).adminHelpcompleteChallenge);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetchallenge <challengename> <player>:" + ChatColor.WHITE + " "
			+ plugin.myLocale(player.getUniqueId()).adminHelpresetChallenge);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetallchallenges <player>:" + ChatColor.WHITE + " "
			+ plugin.myLocale(player.getUniqueId()).adminHelpresetAllChallenges);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info:" + ChatColor.WHITE + " Info on nearest island.");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfo);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info challenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfo);

	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " clearreset <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpclearReset);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setspawn") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " setspawn:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetSpawn);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setrange") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " setrange:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetRange);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " tp <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptp);
	    }
	    if (Settings.newNether && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tpnether") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " tpnether <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptp + "(Nether)");
	    }

	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp()) {
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " Sets leader's island biome.");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp()) {
		sender.sendMessage(ChatColor.GREEN + "== Team Editing Commands ==");
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " team kick <player>:" + ChatColor.WHITE + " Removes player from any team.");
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " team add <player> <leader>:" + ChatColor.WHITE + " Adds player to leader's team.");
		// sender.sendMessage(ChatColor.YELLOW + "/" + label +
		// " team delete <leader>:" + ChatColor.WHITE +
		// " Removes the leader's team compeletely.");
	    }

	}
    }

    /*
     * (non-Javadoc)
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	// Console commands
	Player player;
	if (sender instanceof Player) {
	    player = (Player) sender;
	    if (split.length > 0) {
		// Admin-only commands : reload, register, delete and purge
		if (split[0].equalsIgnoreCase("reload") || split[0].equalsIgnoreCase("register") || split[0].equalsIgnoreCase("delete")
			|| split[0].equalsIgnoreCase("purge") || split[0].equalsIgnoreCase("confirm") || split[0].equalsIgnoreCase("setspawn")
			|| split[0].equalsIgnoreCase("deleteisland") || split[0].equalsIgnoreCase("setrange")
			|| split[0].equalsIgnoreCase("unregister")) {
		    if (!checkAdminPerms(player, split)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
			return true;
		    }
		} else {
		    // Mod commands
		    if (!checkModPerms(player, split)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
			return true;
		    }
		}
	    }
	}
	// Check for zero parameters e.g., /asadmin
	switch (split.length) {
	case 0:
	    help(sender, label);
	    return true;
	case 1:
	    // Find farms
	    if (split[0].equalsIgnoreCase("topbreeders")) {
		// Go through each island and find how many farms there are
		sender.sendMessage("Finding top breeders...");
		TreeMap<Integer, List<UUID>> topEntityIslands = new TreeMap<Integer, List<UUID>>();
		// Generate the stats
		sender.sendMessage("Checking " + plugin.getGrid().getOwnershipMap().size() + " islands...");
		for (Island island : plugin.getGrid().getOwnershipMap().values()) {
		    if (!island.isSpawn()) {
			Location islandLoc = new Location(island.getCenter().getWorld(), island.getCenter().getBlockX(), 128, island.getCenter().getBlockZ());
			Entity snowball = islandLoc.getWorld().spawnEntity(islandLoc, EntityType.SNOWBALL);
			if (snowball == null) {
			    sender.sendMessage("Problem checking island " + island.getCenter().toString());
			} else {
			    // Clear stats
			    island.clearStats();
			    // All of the island space is checked
			    List<Entity> islandEntities = snowball.getNearbyEntities(Settings.islandDistance / 2, 128, Settings.islandDistance / 2);
			    snowball.remove();
			    if (islandEntities.size() > 2) {
				int numOfEntities = 0;
				for (Entity entity : islandEntities) {
				    if (entity instanceof Creature && !(entity instanceof Player)) {
					numOfEntities++;
					island.addEntity(entity.getType());
				    }
				}
				// Store the gross number
				List<UUID> players = new ArrayList<UUID>();
				if (topEntityIslands.containsKey(numOfEntities)) {
				    // Get the previous values
				    players = topEntityIslands.get(numOfEntities);
				}
				players.add(island.getOwner());
				topEntityIslands.put(numOfEntities, players);
			    }
			}
		    }
		}
		// sender.sendMessage("Done");
		// Display the stats
		int rank = 1;
		for (int numOfEntities : topEntityIslands.descendingKeySet()) {
		    if (numOfEntities > 0) {
			List<UUID> owners = topEntityIslands.get(numOfEntities);
			for (UUID owner : owners) {
			    sender.sendMessage("#" + rank + " " + plugin.getPlayers().getName(owner) + " total " + numOfEntities);
			    String content = "";
			    for (Entry<EntityType, Integer> entry : plugin.getGrid().getIsland(owner).getEntities().entrySet()) {
				int num = entry.getValue();
				String color = ChatColor.GREEN.toString();
				if (num > 10 && num <= 20) {
				    color = ChatColor.YELLOW.toString();
				} else if (num > 20 && num <= 40) {
				    color = ChatColor.GOLD.toString();
				} else if (num > 40) {
				    color = ChatColor.RED.toString();
				}
				content += Util.prettifyText(entry.getKey().toString()) + " x " + color + entry.getValue() + ChatColor.WHITE + ", ";
			    }
			    int lastComma = content.lastIndexOf(",");
			    // plugin.getLogger().info("DEBUG: last comma " +
			    // lastComma);
			    if (lastComma > 0) {
				content = content.substring(0, lastComma);
			    }
			    sender.sendMessage("  " + content);
			}
			rank++;
			if (rank > 10) {
			    break;
			}
		    }
		}
		return true;
	    }
	    // Delete island
	    if (split[0].equalsIgnoreCase("deleteisland")) {
		sender.sendMessage(ChatColor.RED + "Use " + ChatColor.BOLD + "deleteisland confirm" + ChatColor.RESET + "" + ChatColor.RED
			+ " to delete the island you are on.");
		return true;
	    }
	    // Set spawn
	    if (split[0].equalsIgnoreCase("setspawn")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
		    return true;
		}
		Player p = (Player) sender;
		// Island spawn must be in the island world
		if (!p.getLocation().getWorld().getName().equals(Settings.worldName)) {
		    p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorWrongWorld);
		    return true;
		}
		// The island location is calculated based on the grid
		Location closestIsland = getClosestIsland(p.getLocation());
		Island oldSpawn = plugin.getGrid().getSpawn();
		Island newSpawn = plugin.getGrid().getIslandAt(closestIsland);
		if (newSpawn != null && newSpawn.isSpawn()) {
		    // Already spawn, so just set the world spawn coords
		    ASkyBlock.getIslandWorld().setSpawnLocation(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
		    sender.sendMessage(ChatColor.GREEN + "Set island spawn to your location.");
		    return true;
		}
		// Space otherwise occupied - find if anyone owns it
		if (newSpawn != null && newSpawn.getOwner() != null) {
		    sender.sendMessage(ChatColor.RED + "This island space is owned by " + plugin.getPlayers().getName(newSpawn.getOwner()));
		    sender.sendMessage(ChatColor.RED + "Move further away or unregister the owner.");
		    return true;
		}
		if (oldSpawn != null) {
		    sender.sendMessage(ChatColor.GOLD + "Changing spawn island location. Warning: old spawn island location at "
			    + oldSpawn.getCenter().getBlockX() + "," + oldSpawn.getCenter().getBlockZ()
			    + " will be at risk of being overwritten with new islands. Recommend to clear that old area.");
		    plugin.getGrid().deleteSpawn();
		}
		// New spawn site is free, so make it official
		if (newSpawn == null) {
		    // Make the new spawn
		    newSpawn = plugin.getGrid().addIsland(closestIsland.getBlockX(), closestIsland.getBlockZ());
		}
		plugin.getGrid().setSpawn(newSpawn);
		ASkyBlock.getIslandWorld().setSpawnLocation(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
		sender.sendMessage(ChatColor.GREEN + "Set island spawn to your location " + p.getLocation().getBlockX() + "," + p.getLocation().getBlockZ());
		sender.sendMessage(ChatColor.YELLOW + "Spawn island center " + newSpawn.getCenter().getBlockX() + "," + newSpawn.getCenter().getBlockZ());
		sender.sendMessage(ChatColor.YELLOW + "Spawn island limits " + newSpawn.getMinX() + "," + newSpawn.getMinZ() + " to "
			+ (newSpawn.getMinX() + newSpawn.getIslandDistance() - 1) + "," + (newSpawn.getMinZ() + newSpawn.getIslandDistance() - 1));
		sender.sendMessage(ChatColor.YELLOW + "Spawn protection range = " + newSpawn.getProtectionSize());
		sender.sendMessage(ChatColor.YELLOW + "Spawn protection coords " + newSpawn.getMinProtectedX() + ", " + newSpawn.getMinProtectedZ() + " to "
			+ (newSpawn.getMinProtectedX() + newSpawn.getProtectionSize() - 1) + ", "
			+ (newSpawn.getMinProtectedZ() + newSpawn.getProtectionSize() - 1));
		if (newSpawn.isLocked()) {
		    sender.sendMessage(ChatColor.RED + "Spawn is locked!");
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info") || split[0].equalsIgnoreCase("setrange")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
		    return true;
		}
		Location closestIsland = getClosestIsland(((Player) sender).getLocation());
		if (closestIsland == null) {
		    sender.sendMessage(ChatColor.RED + "Sorry, could not find an island. Move closer?");
		    return true;
		}
		/*
		 * PlayerIsland island = plugin.getGrid().getIslandAt(closestIsland);
		 * plugin.getLogger().info("DEBUG: minimums " + island.getMinX()
		 * + ", " + island.getMinZ());
		 * plugin.getLogger().info("DEBUG: min protection " +
		 * island.getMinProtectedX() + ", " +
		 * island.getMinProtectedZ());
		 * plugin.getLogger().info("DEBUG: max protection " +
		 * (island.getMinProtectedX() + island.getProtectionSize() -1)
		 * + ", " + (island.getMinProtectedZ() +
		 * island.getProtectionSize() -1));
		 * plugin.getLogger().info("DEBUG: center = " +
		 * island.getCenter());
		 * plugin.getLogger().info("DEBUG: protection range = " +
		 * island.getProtectionSize());
		 * plugin.getLogger().info("DEBUG: island dist = " +
		 * island.getIslandDistance());
		 */
		// plugin.getLogger().info("DEBUG: location = " +
		// closestIsland.toString());
		// Find out whose island this is
		// plugin.getLogger().info("DEBUG: closest bedrock: " +
		// closestBedRock.toString());
		Island island = plugin.getGrid().getIslandAt(closestIsland);
		if (island != null && island.isSpawn()) {
		    sender.sendMessage(ChatColor.GREEN + "This is spawn island");
		    sender.sendMessage(ChatColor.YELLOW + "Spawn max coords " + island.getMinX() + "," + island.getMinZ() + " to "
			    + (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1));
		    sender.sendMessage(ChatColor.YELLOW + "Spawn protection range = " + island.getProtectionSize());
		    sender.sendMessage(ChatColor.YELLOW + "Spawn protection coords " + island.getMinProtectedX() + ", " + island.getMinProtectedZ() + " to "
			    + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
			    + (island.getMinProtectedZ() + island.getProtectionSize() - 1));
		    if (island.isLocked()) {
			sender.sendMessage(ChatColor.RED + "Spawn is locked!");
		    }
		    return true;
		}
		if (island == null) {
		    plugin.getLogger().info("Get island at was null" + closestIsland);
		}
		UUID target = plugin.getPlayers().getPlayerFromIslandLocation(closestIsland);
		if (target == null) {
		    sender.sendMessage(ChatColor.RED + "This island is not owned by anyone right now.");
		    return true;
		}
		showInfo(target, sender);
		return true;
	    } else if (split[0].equalsIgnoreCase("resetsign")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
		    return true;
		}
		Player p = (Player) sender;

		// Find out whether the player is looking at a warp sign
		// Look at what the player was looking at
		BlockIterator iter = new BlockIterator(p, 10);
		Block lastBlock = iter.next();
		while (iter.hasNext()) {
		    lastBlock = iter.next();
		    if (lastBlock.getType() == Material.AIR)
			continue;
		    break;
		}
		if (!lastBlock.getType().equals(Material.SIGN_POST)) {
		    sender.sendMessage(ChatColor.RED + "You must be looking at a Warp Sign to run this command. (not a sign post)");
		    return true;
		}
		// Check if it is a warp sign
		Sign sign = (Sign) lastBlock.getState();
		try {
		    if (!sign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)
			    && !sign.getLine(0).equalsIgnoreCase(ChatColor.RED + plugin.myLocale().warpswelcomeLine)) {
			sender.sendMessage(ChatColor.RED + "You must be looking at a Warp Sign to run this command. (wrong line)");
			return true;
		    }
		} catch (Exception e) {
		    sender.sendMessage(ChatColor.RED + "You must be looking at a Warp Sign to run this command. (exception)");
		    return true;
		}
		sender.sendMessage(ChatColor.GREEN + "Warp sign found!");
		// Find out whose island this is
		// plugin.getLogger().info("DEBUG: closest bedrock: " +
		// closestBedRock.toString());
		UUID target = plugin.getPlayers().getPlayerFromIslandLocation(p.getLocation());
		if (target == null) {
		    sender.sendMessage(ChatColor.RED + "This island is not owned by anyone right now - recommend that sign is removed.");
		    return true;
		}
		if (WarpSigns.addWarp(target, lastBlock.getLocation())) {
		    // Change sign color to green
		    sign.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
		    sign.update();
		    sender.sendMessage(ChatColor.GREEN + "Warp rescued and assigned to " + plugin.getPlayers().getName(target));
		    return true;
		}
		// Warp already exists
		sender.sendMessage(ChatColor.RED + "That warp sign is already active and owned by " + WarpSigns.getWarpOwner(lastBlock.getLocation()));
		return true;

	    } else if (split[0].equalsIgnoreCase("reload")) {
		plugin.reloadConfig();
		plugin.loadPluginConfig();
		Challenges.reloadChallengeConfig();
		plugin.getChallenges();
		if (Settings.useEconomy && VaultHelper.setupEconomy()) {
		    ControlPanel.loadShop();
		} else {
		    Settings.useEconomy = false;
		}
		ControlPanel.loadControlPanel();
		if (Settings.updateCheck) {
		    plugin.checkUpdates();
		} else {
		    plugin.setUpdateCheck(null);
		}
		IslandCmd.loadSchematics();
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().reloadconfigReloaded);
		return true;
	    } else if (split[0].equalsIgnoreCase("topten")) {
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminTopTengenerating);
		TopTen.topTenCreate();
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminTopTenfinished);
		return true;
	    } else if (split[0].equalsIgnoreCase("purge")) {
		if (purgeFlag) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().purgealreadyRunning);
		    return true;
		}
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgeusage.replace("[label]", label));
		return true;
	    } else if (split[0].equalsIgnoreCase("confirm")) {
		if (!confirmReq) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().confirmerrorTimeLimitExpired);
		    return true;
		} else {
		    // Tell purge routine to go
		    confirmOK = true;
		    confirmReq = false;
		}
		return true;
	    } else {
		sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		return false;
	    }
	case 2:
	    // Delete the island you are on
	    if (split[0].equalsIgnoreCase("deleteisland")) {
		if (!split[1].equalsIgnoreCase("confirm")) {
		    sender.sendMessage(ChatColor.RED + "Use " + ChatColor.BOLD + "deleteisland confirm" + ChatColor.RESET + "" + ChatColor.RED
			    + " to delete the island you are on.");
		    return true;
		}
		// Get the island I am on
		Island island = plugin.getGrid().getIslandAt(((Player) sender).getLocation());
		if (island == null) {
		    sender.sendMessage(ChatColor.RED + "Cannot identify island.");
		    return true;
		}
		// Try to get the owner of this island
		UUID owner = island.getOwner();
		String name = "unknown";
		if (owner != null) {
		    name = plugin.getPlayers().getName(owner);
		    sender.sendMessage(ChatColor.RED + "This island is owned by " + name);
		    sender.sendMessage(ChatColor.RED + "Use " + ChatColor.BOLD + "delete " + name + ChatColor.RESET + "" + ChatColor.RED
			    + " to delete the player instead.");
		    return true;
		} else {
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().deleteremoving.replace("[name]", name));
		    /*
		    new DeleteIslandChunk(plugin, island.getCenter());
		    // Delete the new nether island too (if it exists)
		    if (Settings.createNether && Settings.newNether) {
			new DeleteIslandChunk(plugin, island.getCenter().toVector().toLocation(ASkyBlock.getNetherWorld()));
		    }*/
		    deleteIslands(island, sender);
		    return true;
		}
	    }
	    // Set protection for the island the player is on
	    if (split[0].equalsIgnoreCase("setrange")) {
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + "Must use command in-game while on an island!");
		    return true;
		}
		Island island = plugin.getGrid().getIslandAt(((Player) sender).getLocation());
		// Check if island exists
		if (island == null) {
		    sender.sendMessage(ChatColor.RED + "You are not in an island space!");
		    return true;
		} else {
		    int newRange = 10;
		    int maxRange = Settings.islandDistance;
		    // If spawn do something different
		    if (island.isSpawn()) {
			try {
			    newRange = Integer.valueOf(split[1]);
			} catch (Exception e) {
			    sender.sendMessage(ChatColor.RED + "Invalid range!");
			    return true;
			}
			sender.sendMessage(ChatColor.GREEN + "This is spawn island - setting new range to " + newRange);
			if (newRange > maxRange) {
			    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Warning - range is greater than island range " + maxRange);
			    sender.sendMessage(ChatColor.RED + "Overlapped islands will act like spawn!");
			}
			island.setProtectionSize(newRange);
			sender.sendMessage(ChatColor.YELLOW + "Spawn max coords " + island.getMinX() + "," + island.getMinZ() + " to "
				+ (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1));
			sender.sendMessage(ChatColor.YELLOW + "Spawn protection range = " + island.getProtectionSize());
			sender.sendMessage(ChatColor.YELLOW + "Spawn protection coords " + island.getMinProtectedX() + ", " + island.getMinProtectedZ()
				+ " to " + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
				+ (island.getMinProtectedZ() + island.getProtectionSize() - 1));
			if (island.isLocked()) {
			    sender.sendMessage(ChatColor.RED + "Spawn is locked!");
			}
		    } else {
			if (!plugin.getConfig().getBoolean("island.overridelimit")) {
			    maxRange -= 16;
			}
			try {
			    newRange = Integer.valueOf(split[1]);
			} catch (Exception e) {
			    sender.sendMessage(ChatColor.RED + "Invalid range use 10 to " + maxRange);
			    return true;
			}
			if (newRange < 10 || newRange > maxRange) {
			    sender.sendMessage(ChatColor.RED + "Invalid range use 10 to " + maxRange);
			    return true;
			}
			island.setProtectionSize(newRange);
			sender.sendMessage(ChatColor.GREEN + "Set new range to " + ChatColor.WHITE + newRange);
			showInfo(island.getOwner(), sender);
		    }
		    return true;
		}
	    }
	    if (split[0].equalsIgnoreCase("purge")) {
		// PURGE Command
		// Check for "allow" or "disallow" flags
		// Protect island from purging
		if (split[1].equalsIgnoreCase("allow") || split[1].equalsIgnoreCase("disallow")) {
		    // Find the closest island
		    if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
			return true;
		    }
		    Player p = (Player) sender;
		    // Island spawn must be in the island world
		    if (!p.getLocation().getWorld().equals(ASkyBlock.getIslandWorld()) && !p.getLocation().getWorld().equals(ASkyBlock.getNetherWorld())) {
			p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorWrongWorld);
			return true;
		    }
		    Island island = plugin.getGrid().getIslandAt(p.getLocation());
		    if (island == null) {
			p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorNoIslandOther);
			return true;
		    }
		    if (split[1].equalsIgnoreCase("allow")) {
			island.setPurgeProtected(true);
		    } else {
			island.setPurgeProtected(false);
		    }
		    if (island.isPurgeProtected()) {
			p.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminAllowPurge);
		    } else {
			p.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminPreventPurge);  
		    }
		    return true;
		}

		// Purge runs in the background so if one is already running
		// this flag stops a repeat
		if (purgeFlag) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().purgealreadyRunning);
		    return true;
		}

		if (split[1].equalsIgnoreCase("unowned")) {
		    countUnowned(sender);
		    return true;
		}
		// Set the flag
		purgeFlag = true;
		// See if this purge unowned

		// Convert days to hours - no other limit checking?
		final int time;
		try {
		    time = Integer.parseInt(split[1]) * 24;
		} catch (Exception e) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().purgeusage.replace("[label]", label));
		    purgeFlag = false;
		    return true;
		}
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgecalculating.replace("[time]", split[1]));
		// Kick off task
		plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
		    public void run() {
			final File directoryPlayers = new File(plugin.getDataFolder() + File.separator + "players");

			long offlineTime = 0L;
			// Go through the player directory and build the purge
			// list of filenames
			for (final File playerFile : directoryPlayers.listFiles()) {
			    if (playerFile.getName().endsWith(".yml")) {
				final UUID playerUUID = UUID.fromString(playerFile.getName().substring(0, playerFile.getName().length() - 4));
				// Only bother if the player is offline (by
				// definition)
				if (Bukkit.getPlayer(playerUUID) == null) {
				    final OfflinePlayer oplayer = Bukkit.getOfflinePlayer(playerUUID);
				    offlineTime = oplayer.getLastPlayed();
				    // Calculate the number of hours the player
				    // has
				    // been offline
				    offlineTime = (System.currentTimeMillis() - offlineTime) / 3600000L;
				    // plugin.getLogger().info(plugin.getPlayers().getName(playerUUID)
				    // + " has been offline " + offlineTime +
				    // " hours. Required = " + time);
				    if (offlineTime > time) {
					// plugin.getLogger().info(plugin.getPlayers().getName(playerUUID)
					// +
					// " has not logged on recently enough");
					// Do the rest without loading the
					// player file
					YamlConfiguration oldPlayer = new YamlConfiguration();
					try {
					    oldPlayer.load(playerFile);
					    // Check if this player has an
					    // island - if not skip
					    if (oldPlayer.getBoolean("hasIsland", false)) {
						// If the player is in a team
						// then ignore
						if (!oldPlayer.getBoolean("hasTeam", false)) {
						    // plugin.getLogger().info("and is a lone player");
						    if (oldPlayer.getInt("islandLevel", 0) < Settings.abandonedIslandLevel) {
							// Check if the island is purge protected
							Island island = plugin.getGrid().getIsland(playerUUID);
							if (island == null || (island != null && !island.isPurgeProtected())) {
							    removeList.add(playerUUID);
							}
						    } 
						} 
					    }
					} catch (Exception e) {
					    // Just skip it
					    plugin.getLogger().severe("Error trying to load player file " + playerFile.getName() + " skipping...");
					}
				    }
				}
			    }
			}
			if (removeList.isEmpty()) {
			    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgenoneFound);
			    purgeFlag = false;
			    return;
			}
			sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgethisWillRemove.replace("[number]", String.valueOf(removeList.size())));
			sender.sendMessage(ChatColor.RED + plugin.myLocale().purgewarning);
			sender.sendMessage(ChatColor.RED + plugin.myLocale().purgetypeConfirm.replace("[label]", label));
			confirmReq = true;
			confirmOK = false;
			confirmTimer = 0;
			new BukkitRunnable() {
			    @Override
			    public void run() {
				// This waits for 10 seconds and if no
				// confirmation received, then it
				// cancels
				if (confirmTimer++ > 10) {
				    // Ten seconds is up!
				    confirmReq = false;
				    confirmOK = false;
				    purgeFlag = false;
				    removeList.clear();
				    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgepurgeCancelled);
				    this.cancel();
				} else if (confirmOK) {
				    // Set up a repeating task to run every 2
				    // seconds to remove
				    // islands one by one and then cancel when
				    // done
				    final int total = removeList.size();
				    new BukkitRunnable() {
					@Override
					public void run() {
					    if (removeList.isEmpty() && purgeFlag) {
						purgeFlag = false;
						sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgefinished);
						this.cancel();
					    }

					    if (removeList.size() > 0 && purgeFlag) {
						plugin.deletePlayerIsland(removeList.get(0), true);
						sender.sendMessage(ChatColor.YELLOW + "[" + removeList.size() + "/" + total + "] "
							+ plugin.myLocale().purgeremovingName.replace("[name]", plugin.getPlayers().getName(removeList.get(0))));
						removeList.remove(0);
					    }
					    sender.sendMessage("Now waiting...");
					}
				    }.runTaskTimer(plugin, 0L, 20L);
				    confirmReq = false;
				    confirmOK = false;
				    this.cancel();
				}
			    }
			}.runTaskTimer(plugin, 0L, 40L);
		    }
		});
		return true;
	    } else if (split[0].equalsIgnoreCase("clearreset")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		} else {
		    plugin.getPlayers().setResetsLeft(playerUUID, Settings.resetLimit);
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().clearedResetLimit + " [" + Settings.resetLimit + "]");
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("tp")) {
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		    return true;
		}
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		} else {
		    if (plugin.getPlayers().getIslandLocation(playerUUID) != null) {
			Location safeSpot = plugin.getGrid().getSafeHomeLocation(playerUUID,1);
			if (safeSpot != null) {
			    // This next line should help players with long ping
			    // times
			    ((Player) sender).teleport(safeSpot);
			    // ((Player)sender).sendBlockChange(safeSpot,safeSpot.getBlock().getType(),safeSpot.getBlock().getData());
			} else {
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNotSafe);
			    Location warpSpot = plugin.getPlayers().getIslandLocation(playerUUID);
			    sender.sendMessage(ChatColor.RED + "Manually warp to somewhere near " + warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
				    + warpSpot.getBlockZ());
			}
			return true;
		    }
		    sender.sendMessage(plugin.myLocale().errorNoIslandOther);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("tpnether")) {
		if (!Settings.newNether) {
		    return false;
		}
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		    return true;
		}
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		} else {
		    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
		    if (islandLoc != null) {
			Location safeSpot = null;
			if (islandLoc.getWorld().equals(ASkyBlock.getNetherWorld())) {
			    safeSpot = plugin.getGrid().getSafeHomeLocation(playerUUID,1);
			} else {
			    safeSpot = plugin.getGrid().bigScan(islandLoc.toVector().toLocation(ASkyBlock.getNetherWorld()), 30);
			}
			if (safeSpot != null) {
			    // This next line should help players with long ping
			    // times
			    ((Player) sender).teleport(safeSpot);
			    // ((Player)sender).sendBlockChange(safeSpot,safeSpot.getBlock().getType(),safeSpot.getBlock().getData());
			} else {
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNotSafe);
			    Location warpSpot = plugin.getPlayers().getIslandLocation(playerUUID);
			    sender.sendMessage(ChatColor.RED + "Manually warp to somewhere near " + warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
				    + warpSpot.getBlockZ());
			}
			return true;
		    }
		    sender.sendMessage(plugin.myLocale().errorNoIslandOther);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("delete")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		} else {
		    // This now deletes the player and cleans them up even if
		    // they don't have an island
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().deleteremoving.replace("[name]", split[1]));
		    // If they are online and in ASkyBlock then delete their
		    // stuff too
		    Player target = plugin.getServer().getPlayer(playerUUID);
		    if (target != null) {
			// Clear any coop inventories
			// CoopPlay.getInstance().returnAllInventories(target);
			// Remove any of the target's coop invitees and grab
			// their stuff
			CoopPlay.getInstance().clearMyInvitedCoops(target);
			CoopPlay.getInstance().clearMyCoops(target);
			plugin.resetPlayer(target);
		    }
		    // plugin.getLogger().info("DEBUG: deleting player");
		    plugin.deletePlayerIsland(playerUUID, true);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("register")) {
		if (sender instanceof Player) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
			return true;
		    } else {
			if (adminSetPlayerIsland(sender, ((Player) sender).getLocation(), playerUUID)) {
			    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().registersettingIsland.replace("[name]", split[1]));
			} else {
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().registererrorBedrockNotFound);
			}
			return true;
		    }
		} else {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("unregister")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		} else {
		    if (plugin.getPlayers().inTeam(playerUUID)) {
			sender.sendMessage(ChatColor.RED + "Player is in a team - disband it first.");
			return true;
		    }
		    Location island = plugin.getPlayers().getIslandLocation(playerUUID);
		    if (island == null) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoIslandOther);
			return true;
		    }
		    // Delete player, but keep blocks
		    sender.sendMessage(ChatColor.GREEN + "Removing player from world, but keeping island at "
			    + plugin.getPlayers().getIslandLocation(playerUUID).getBlockX() + ","
			    + plugin.getPlayers().getIslandLocation(playerUUID).getBlockZ());
		    plugin.deletePlayerIsland(playerUUID, false);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("info")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		// plugin.getLogger().info("DEBUG: console player info UUID = "
		// + playerUUID);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		} else {
		    showInfo(playerUUID, sender);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("resetallchallenges")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		}
		plugin.getPlayers().resetAllChallenges(playerUUID);
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().resetChallengessuccess.replace("[name]", split[1]));
		return true;
	    } else {
		return false;
	    }
	case 3:
	    // Confirm purge unowned
	    if (split[0].equalsIgnoreCase("purge")) {
		if (purgeFlag) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().purgealreadyRunning);
		    return true;
		}
		// Check if this is purge unowned
		if (split[1].equalsIgnoreCase("unowned") && split[2].equalsIgnoreCase("confirm")) {
		    if (!purgeUnownedConfirm) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().confirmerrorTimeLimitExpired);
			return true;
		    } else {
			purgeUnownedConfirm = false;
			// Purge the unowned islands
			purgeUnownedIslands(sender);
			return true;
		    }
		}

	    }
	    // Set protection
	    if (split[0].equalsIgnoreCase("setrange")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		// Check if player exists
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		}
		// Check if the target is in a team and if so, the leader
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    if (!plugin.getPlayers().getTeamLeader(playerUUID).equals(playerUUID)) {
			sender.sendMessage(ChatColor.RED + "That player is not the leader of the team. Leader is "
				+ plugin.getPlayers().getName(plugin.getPlayers().getTeamLeader(playerUUID)));
			return true;
		    }
		}
		// Get the range that this player has now
		Island island = plugin.getGrid().getIsland(playerUUID);
		if (island == null) {
		    sender.sendMessage(ChatColor.RED + "That player does not have an island...");
		    return true;
		} else {
		    int newRange = 0;
		    int maxRange = Settings.islandDistance;
		    if (!plugin.getConfig().getBoolean("island.overridelimit")) {
			maxRange -= 16;
		    }
		    try {
			newRange = Integer.valueOf(split[2]);
		    } catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid range use 10 to " + maxRange);
			return true;
		    }
		    if (newRange < 10 || newRange > maxRange) {
			sender.sendMessage(ChatColor.RED + "Invalid range use 10 to " + maxRange);
			return true;
		    }
		    island.setProtectionSize(newRange);
		    sender.sendMessage(ChatColor.GREEN + "Set new range to " + ChatColor.WHITE + newRange);
		    showInfo(playerUUID, sender);
		    return true;
		}
	    }
	    // Change biomes
	    if (split[0].equalsIgnoreCase("setbiome")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		// Check if player exists
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		}
		// Check if the target is in a team and if so, the leader
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    if (!plugin.getPlayers().getTeamLeader(playerUUID).equals(playerUUID)) {
			sender.sendMessage(ChatColor.RED + "That player is not the leader of the team. Leader is "
				+ plugin.getPlayers().getName(plugin.getPlayers().getTeamLeader(playerUUID)));
			return true;
		    }
		}
		// Check if biome is valid
		Biome biome = null;
		String biomeName = split[2].toUpperCase();
		try {
		    biome = Biome.valueOf(biomeName);
		    biomeName = biome.name();
		    if (!plugin.getConfig().contains("biomes." + biomeName)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().biomeUnknown);
			// Doing it this way ensures that only valid biomes are
			// shown
			for (Biome b : Biome.values()) {
			    if (plugin.getConfig().contains("biomes." + b.name())) {
				sender.sendMessage(b.name());
			    }
			}
			return true;
		    }
		    // Get friendly name
		    biomeName = plugin.getConfig().getString("biomes." + biomeName + ".friendlyname", Util.prettifyText(biomeName));

		} catch (Exception e) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().biomeUnknown);
		    for (Biome b : Biome.values()) {
			if (plugin.getConfig().contains("biomes." + b.name())) {
			    sender.sendMessage(b.name());
			}
		    }
		    return true;
		}
		// Okay clear to set biome
		// Actually set the biome
		if (plugin.getPlayers().inTeam(playerUUID) && plugin.getPlayers().getTeamIslandLocation(playerUUID) != null) {
		    plugin.getBiomes().setIslandBiome(plugin.getPlayers().getTeamIslandLocation(playerUUID), biome);
		} else {
		    plugin.getBiomes().setIslandBiome(plugin.getPlayers().getIslandLocation(playerUUID), biome);
		}
		sender.sendMessage(ChatColor.GREEN + plugin.myLocale().biomeSet.replace("[biome]", biomeName));
		Player targetPlayer = plugin.getServer().getPlayer(playerUUID);
		if (targetPlayer != null) {
		    // Online
		    targetPlayer.sendMessage("[Admin] " + ChatColor.GREEN + plugin.myLocale(playerUUID).biomeSet.replace("[biome]", biomeName));
		} else {
		    plugin.getMessages().setMessage(playerUUID, "[Admin] " + ChatColor.GREEN + plugin.myLocale(playerUUID).biomeSet.replace("[biome]", biomeName));
		}
		return true;
	    } else
		// team kick <player> and team delete <leader>
		if (split[0].equalsIgnoreCase("team")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
			return true;
		    }
		    if (split[1].equalsIgnoreCase("kick")) {
			// Remove player from team
			if (!plugin.getPlayers().inTeam(playerUUID)) {
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoTeam);
			    return true;
			}
			UUID teamLeader = plugin.getPlayers().getTeamLeader(playerUUID);
			// Payer is not a team leader
			if (!teamLeader.equals(playerUUID)) {
			    // Clear the player of all team-related items
			    plugin.getPlayers().setLeaveTeam(playerUUID);
			    plugin.getPlayers().setHomeLocation(playerUUID, null);
			    plugin.getPlayers().setIslandLocation(playerUUID, null);
			    // Clear the leader of this player and if they now have
			    // no team, remove the team
			    plugin.getPlayers().removeMember(teamLeader, playerUUID);
			    if (plugin.getPlayers().getMembers(teamLeader).size() < 2) {
				plugin.getPlayers().setLeaveTeam(teamLeader);
			    }
			    // Remove any warps
			    WarpSigns.removeWarp(playerUUID);
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().kicknameRemoved.replace("[name]", split[2]));
			    // If target is online -- do not tell target
			    /*
			     * Player target =
			     * plugin.getServer().getPlayer(playerUUID);
			     * if (target != null) {
			     * target.sendMessage(ChatColor.RED +
			     * plugin.myLocale().kicknameRemovedYou.replace("[name]",
			     * sender.getName()));
			     * } else {
			     * plugin.setMessage(playerUUID,ChatColor.RED +
			     * plugin.myLocale().kicknameRemovedYou.replace("[name]",
			     * sender.getName()));
			     * }
			     */
			    return true;
			} else {
			    sender.sendMessage(ChatColor.RED + "That player is a team leader. Remove team members first. Use '/" + label + " info " + split[2]
				    + "' to find team members.");
			    return true;
			}
		    } else {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
			return false;
		    }
		} else if (split[0].equalsIgnoreCase("completechallenge")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
			return true;
		    }
		    if (plugin.getPlayers().checkChallenge(playerUUID, split[1].toLowerCase())
			    || !plugin.getPlayers().get(playerUUID).challengeExists(split[1].toLowerCase())) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().completeChallengeerrorChallengeDoesNotExist);
			return true;
		    }
		    plugin.getPlayers().get(playerUUID).completeChallenge(split[1].toLowerCase());
		    sender.sendMessage(ChatColor.YELLOW
			    + plugin.myLocale().completeChallengechallangeCompleted.replace("[challengename]", split[1].toLowerCase()).replace("[name]", split[2]));
		    return true;
		} else if (split[0].equalsIgnoreCase("resetchallenge")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
			return true;
		    }
		    if (!plugin.getPlayers().checkChallenge(playerUUID, split[1].toLowerCase())
			    || !plugin.getPlayers().get(playerUUID).challengeExists(split[1].toLowerCase())) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().resetChallengeerrorChallengeDoesNotExist);
			return true;
		    }
		    plugin.getPlayers().resetChallenge(playerUUID, split[1].toLowerCase());
		    sender.sendMessage(ChatColor.YELLOW
			    + plugin.myLocale().resetChallengechallengeReset.replace("[challengename]", split[1].toLowerCase()).replace("[name]", split[2]));
		    return true;
		} else if (split[0].equalsIgnoreCase("info") && split[1].equalsIgnoreCase("challenges")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    // plugin.getLogger().info("DEBUG: console player info UUID = "
		    // + playerUUID);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
			return true;
		    } else {
			showInfoChallenges(playerUUID, sender);
			return true;
		    }
		}
	    return false;
	case 4:
	    // Team add <player> <leader>
	    if (split[0].equalsIgnoreCase("team") && split[1].equalsIgnoreCase("add")) {
		// Convert names to UUIDs
		final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		final Player targetPlayer = plugin.getServer().getPlayer(playerUUID);
		final UUID teamLeader = plugin.getPlayers().getUUID(split[3]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID) || !plugin.getPlayers().isAKnownPlayer(teamLeader)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		}
		if (playerUUID.equals(teamLeader)) {
		    sender.sendMessage(ChatColor.RED + "Cannot add a leader to their own team.");
		    return true;
		}
		// See if leader has an island
		if (!plugin.getPlayers().hasIsland(teamLeader)) {
		    sender.sendMessage(ChatColor.RED + "Team leader does not have their own island so cannot have a team!");
		    return true;
		}
		// Check to see if this player is already in a team
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().inviteerrorThatPlayerIsAlreadyInATeam);
		    return true;
		}
		// If the leader's member list does not contain their own name
		// then
		// add it
		if (!plugin.getPlayers().getMembers(teamLeader).contains(teamLeader)) {
		    // Set up the team leader
		    plugin.getPlayers().setJoinTeam(teamLeader, teamLeader, plugin.getPlayers().getIslandLocation(teamLeader));
		    plugin.getPlayers().addTeamMember(teamLeader, teamLeader);
		    sender.sendMessage(ChatColor.GOLD + "Added the leader to this team!");
		}
		// This is a hack to clear any pending invitations
		if (targetPlayer != null) {
		    targetPlayer.performCommand(Settings.ISLANDCOMMAND + " decline");
		}
		// If the invitee has an island of their own
		if (plugin.getPlayers().hasIsland(playerUUID)) {
		    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
		    if (islandLoc != null) {
			sender.sendMessage(ChatColor.RED + plugin.getPlayers().getName(playerUUID) + " had an island at " + islandLoc.getBlockX() + " "
				+ islandLoc.getBlockZ() + " that will become unowned now. You may want to delete it manually.");
		    }
		}
		// Remove their old island affiliation - do not delete the
		// island just in case
		plugin.deletePlayerIsland(playerUUID, false);
		// Join the team and set the team island location and leader
		plugin.getPlayers().setJoinTeam(playerUUID, teamLeader, plugin.getPlayers().getIslandLocation(teamLeader));
		// Configure the best home location for this player
		if (plugin.getPlayers().getHomeLocation(teamLeader) != null) {
		    plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getHomeLocation(teamLeader));
		    sender.sendMessage(ChatColor.GREEN + "Setting player's home to the leader's home location");
		} else {
		    plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getIslandLocation(teamLeader));
		    sender.sendMessage(ChatColor.GREEN + "Setting player's home to the leader's island location");
		}
		// If the leader's member list does not contain player then add
		// it
		if (!plugin.getPlayers().getMembers(teamLeader).contains(playerUUID)) {
		    plugin.getPlayers().addTeamMember(teamLeader, playerUUID);
		    sender.sendMessage(ChatColor.GREEN + "Adding player to team.");
		} else {
		    sender.sendMessage(ChatColor.GOLD + "Player was already on this team!");
		}
		// Teleport the player if they are online
		if (targetPlayer != null) {
		    plugin.getGrid().homeTeleport(targetPlayer);
		}
		return true;
	    } else {
		sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		return false;
	    }
	default:
	    return false;
	}
    }

    /**
     * Deletes the overworld and nether islands together
     * @param island
     * @param sender
     */
    private void deleteIslands(Island island, CommandSender sender) {
	if (island.getCenter().getWorld().equals(ASkyBlock.getIslandWorld())) {
	    // Over World start
	    plugin.getGrid().removeMobsFromIsland(island.getCenter());
	    // Reset the biome
	    BiomesPanel.setIslandBiome(island.getCenter(), Settings.defaultBiome);
	    new DeleteIslandChunk(plugin, island.getCenter());
	    // Delete the new nether island too (if it exists)
	    if (Settings.createNether && Settings.newNether) {
		Location otherIsland = island.getCenter().toVector().toLocation(ASkyBlock.getNetherWorld());
		plugin.getGrid().removeMobsFromIsland(otherIsland);
		// Delete island
		new DeleteIslandChunk(plugin, otherIsland);  
	    }
	} else if (Settings.createNether && Settings.newNether && island.getCenter().getWorld().equals(ASkyBlock.getNetherWorld())) {
	    // Nether World Start
	    plugin.getGrid().removeMobsFromIsland(island.getCenter());
	    new DeleteIslandChunk(plugin, island.getCenter());
	    // Delete the overworld island too
	    Location otherIsland = island.getCenter().toVector().toLocation(ASkyBlock.getIslandWorld());
	    plugin.getGrid().removeMobsFromIsland(otherIsland);
	    // Reset the biome
	    BiomesPanel.setIslandBiome(island.getCenter(), Settings.defaultBiome);
	    // Delete island
	    new DeleteIslandChunk(plugin, otherIsland);  
	} else {
	    sender.sendMessage(ChatColor.RED + "Cannot delete island at location " + island.getCenter().toString() + " because it is not in the official island world");
	} 
    }

    /**
     * Purges the unowned islands upon direction from sender
     * @param sender
     */
    private void purgeUnownedIslands(final CommandSender sender) {
	purgeFlag = true;
	final int total = unowned.size();
	new BukkitRunnable() {
	    @Override
	    public void run() {
		if (unowned.isEmpty()) {
		    purgeFlag = false;
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgefinished);
		    this.cancel();
		}
		if (unowned.size() > 0) {
		    Iterator<Entry<String, Island>> it = unowned.entrySet().iterator();
		    Entry<String,Island> entry = it.next();

		    sender.sendMessage(ChatColor.YELLOW + "[" + (total - unowned.size() + 1) + "/" + total + "] Removing island at location "
			    + entry.getValue().getCenter().getWorld().getName() + " " + entry.getValue().getCenter().getBlockX()
			    + "," + entry.getValue().getCenter().getBlockZ());
		    deleteIslands(entry.getValue(),sender);
		    // Remove from the list
		    it.remove();
		}
		sender.sendMessage("Now waiting...");
	    }
	}.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Counts unowned islands
     * @param sender
     */
    private void countUnowned(final CommandSender sender) {
	unowned = plugin.getGrid().getUnownedIslands();
	if (!unowned.isEmpty()) {
	    purgeFlag = true;
	    sender.sendMessage("Counting unowned islands and checking player files. This could take some time...");
	    // Prepare for the async check - make final
	    final File playerFolder = plugin.getPlayersFolder();
	    // Set the pending flag
	    asyncPending = true;
	    // Check against player files
	    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

		@Override
		public void run() {
		    //System.out.println("DEBUG: Running async task");
		    // Check files against potentialUnowned
		    FilenameFilter ymlFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
			    String lowercaseName = name.toLowerCase();
			    if (lowercaseName.endsWith(".yml")) {
				return true;
			    } else {
				return false;
			    }
			}
		    };
		    for (File file: playerFolder.listFiles(ymlFilter)) {
			try {
			    Scanner scanner = new Scanner(file);
			    while (scanner.hasNextLine()) {
				final String lineFromFile = scanner.nextLine();
				if (lineFromFile.contains("islandLocation:")) { 
				    // Check against potentialUnowned list
				    String loc = lineFromFile.substring(lineFromFile.indexOf(' ')).trim();
				    //System.out.println("DEBUG: Location in player file is " + loc);
				    if (unowned.containsKey(loc)) {
					//System.out.println("DEBUG: Location found in player file - do not delete");
					unowned.remove(loc);
				    }
				}
			    }
			    scanner.close();
			} catch (FileNotFoundException e) {
			    e.printStackTrace();
			}
		    }
		    //System.out.println("DEBUG: scanning done");
		    asyncPending = false;
		}});
	    // Create a repeating task to check if the async task has completed

	    new BukkitRunnable() {

		@Override
		public void run() {
		    if (asyncPending) {
			// Still waiting
			sender.sendMessage("Still checking player files...");
		    } else {
			// Done
			if (unowned.size() > 0) {
			    sender.sendMessage("There are " + unowned.size() + " unowned islands. Do 'purge unowned confirm' to delete them within 20 seconds.");
			    purgeUnownedConfirm = true;
			    purgeFlag = false;
			    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

				@Override
				public void run() {
				    if (purgeUnownedConfirm) {
					purgeUnownedConfirm = false;
					sender.sendMessage(plugin.myLocale().purgepurgeCancelled);
				    }
				}}, 400L);
			} else {
			    sender.sendMessage(plugin.myLocale().purgenoneFound);
			    purgeFlag = false;
			}
			this.cancel();
		    }

		}
	    }.runTaskTimer(plugin,20L,20L);
	} else {
	    sender.sendMessage(plugin.myLocale().purgenoneFound);
	}
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     * 
     * @param location
     * @return
     */
    public static Location getClosestIsland(Location location) {
	long x = Math.round((double) location.getBlockX() / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
	long z = Math.round((double) location.getBlockZ() / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;
	long y = Settings.island_level;
	return new Location(location.getWorld(), x, y, z);
    }

    /**
     * Shows info on a player
     * 
     * @param playerUUID
     * @param sender
     */
    private void showInfo(UUID playerUUID, CommandSender sender) {
	sender.sendMessage("Owner: " + ChatColor.GREEN + plugin.getPlayers().getName(playerUUID));
	sender.sendMessage(ChatColor.WHITE + "UUID: " + playerUUID.toString());
	// Display island level
	sender.sendMessage(ChatColor.GREEN + plugin.myLocale().levelislandLevel + ": " + plugin.getPlayers().getIslandLevel(playerUUID));
	// Last login
	try {
	    Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed());
	    sender.sendMessage(ChatColor.GOLD + "Last login: " + d.toString());
	} catch (Exception e) {
	}
	Location islandLoc = null;
	// Teams
	if (plugin.getPlayers().inTeam(playerUUID)) {
	    final UUID leader = plugin.getPlayers().getTeamLeader(playerUUID);
	    final List<UUID> pList = plugin.getPlayers().getMembers(leader);
	    sender.sendMessage(ChatColor.GREEN + plugin.getPlayers().getName(leader));
	    for (UUID member : pList) {
		sender.sendMessage(ChatColor.WHITE + " - " + plugin.getPlayers().getName(member));
	    }
	    islandLoc = plugin.getPlayers().getTeamIslandLocation(playerUUID);
	} else {
	    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().errorNoTeam);
	    if (plugin.getPlayers().hasIsland(playerUUID)) {
		islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
	    }
	    if (!(plugin.getPlayers().getTeamLeader(playerUUID) == null)) {
		sender.sendMessage(ChatColor.RED + plugin.myLocale().adminInfoerrorNullTeamLeader);
	    }
	    if (!plugin.getPlayers().getMembers(playerUUID).isEmpty()) {
		sender.sendMessage(ChatColor.RED + plugin.myLocale().adminInfoerrorTeamMembersExist);
	    }
	}
	if (islandLoc != null) {
	    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminInfoislandLocation + ":" + ChatColor.WHITE + " (" + islandLoc.getBlockX() + ","
		    + islandLoc.getBlockY() + "," + islandLoc.getBlockZ() + ")");
	    Island island = plugin.getGrid().getIslandAt(islandLoc);
	    if (island == null) {
		plugin.getLogger().warning("Player has an island, but it is not in the grid. Adding it now...");
		island = plugin.getGrid().addIsland(islandLoc.getBlockX(), islandLoc.getBlockZ(), playerUUID);
	    }
	    sender.sendMessage(ChatColor.YELLOW + "Island max size (distance) = " + island.getIslandDistance());
	    sender.sendMessage(ChatColor.YELLOW + "Island maximums " + island.getMinX() + "," + island.getMinZ() + " to "
		    + (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1));
	    sender.sendMessage(ChatColor.YELLOW + "Island protection range = " + island.getProtectionSize());
	    sender.sendMessage(ChatColor.YELLOW + "Island protection " + island.getMinProtectedX() + ", " + island.getMinProtectedZ() + " to "
		    + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", " + (island.getMinProtectedZ() + island.getProtectionSize() - 1));
	    if (island.isSpawn()) {
		sender.sendMessage(ChatColor.YELLOW + "Island is spawn");
	    }
	    if (island.isLocked()) {
		sender.sendMessage(ChatColor.YELLOW + "Island is locked");
	    } else {
		sender.sendMessage(ChatColor.YELLOW + "Island is unlocked");
	    }
	    if (island.isPurgeProtected()) {
		sender.sendMessage(ChatColor.GREEN + "Island is purge protected");
	    } else {
		sender.sendMessage(ChatColor.GREEN + "Island is not purge protected");
	    }
	    List<UUID> banList = plugin.getPlayers().getBanList(playerUUID);
	    if (!banList.isEmpty()) {
		sender.sendMessage(ChatColor.YELLOW + "Banned players:");
		String list = "";
		for (UUID uuid : banList) {
		    Player target = plugin.getServer().getPlayer(uuid);
		    if (target != null) {
			//online
			list += target.getDisplayName() + ", ";
		    } else {
			list += plugin.getPlayers().getName(uuid) + ", ";
		    }
		}
		if (!list.isEmpty()) {
		    sender.sendMessage(ChatColor.RED + list.substring(0, list.length()-2));
		}
	    }	
	} else {
	    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoIslandOther);
	}
    }

    /**
     * Shows info on the challenge situation for player
     * 
     * @param playerUUID
     * @param sender
     */
    private void showInfoChallenges(UUID playerUUID, CommandSender sender) {
	sender.sendMessage("Name:" + ChatColor.GREEN + plugin.getPlayers().getName(playerUUID));
	sender.sendMessage(ChatColor.WHITE + "UUID: " + playerUUID.toString());
	// Completed challenges
	sender.sendMessage(ChatColor.WHITE + "Challenges:");
	HashMap<String, Boolean> challenges = plugin.getPlayers().getChallengeStatus(playerUUID);
	HashMap<String, Integer> challengeTimes = plugin.getPlayers().getChallengeTimes(playerUUID);
	for (String c : challenges.keySet()) {
	    if (challengeTimes.containsKey(c)) {
		sender.sendMessage(c + ": "
			+ ((challenges.get(c)) ? ChatColor.GREEN + plugin.myLocale().challengescomplete : ChatColor.AQUA + plugin.myLocale().challengesincomplete) + "("
			+ plugin.getPlayers().checkChallengeTimes(playerUUID, c) + ")");

	    } else {
		sender.sendMessage(c + ": "
			+ ((challenges.get(c)) ? ChatColor.GREEN + plugin.myLocale().challengescomplete : ChatColor.AQUA + plugin.myLocale().challengesincomplete));
	    }
	}
    }

    private boolean checkAdminPerms(Player player2, String[] split) {
	// Check perms quickly for this command
	if (player2.isOp()) {
	    return true;
	}
	String check = split[0];
	if (check.equalsIgnoreCase("confirm"))
	    check = "purge";
	if (VaultHelper.checkPerm(player2, Settings.PERMPREFIX + "admin." + split[0].toLowerCase())) {
	    return true;
	}
	return false;
    }

    private boolean checkModPerms(Player player2, String[] split) {
	// Check perms quickly for this command
	if (player2.isOp()) {
	    return true;
	}
	String check = split[0];
	// Check special cases
	if (check.contains("challenge".toLowerCase())) {
	    check = "challenges";
	}
	if (VaultHelper.checkPerm(player2, Settings.PERMPREFIX + "mod." + split[0].toLowerCase())) {
	    return true;
	}
	return false;
    }

    /**
     * Searches for bedrock around a location (20x20x20) and then assigns the
     * player to that island
     * 
     * @param sender
     *            - the player requesting the assignment
     * @param l
     *            - the location of sender
     * @param newOwner
     *            - the assignee
     * @return - true if successful, false if not
     */
    public boolean adminSetPlayerIsland(final CommandSender sender, final Location l, final UUID newOwner) {
	// Location island = getClosestIsland(l);
	// Check what the grid thinks
	Island island = plugin.getGrid().getIslandAt(l);
	if (island != null) {
	    if (island.isSpawn()) {
		sender.sendMessage(ChatColor.RED + "You cannot take ownership of spawn!");
		return false;
	    }
	    UUID oldOwner = island.getOwner();
	    if (oldOwner != null) {
		if (plugin.getPlayers().inTeam(oldOwner)) {
		    sender.sendMessage(ChatColor.RED + plugin.getPlayers().getName(oldOwner) + " leads a team. Kick players from it first.");
		    return false;
		}
		sender.sendMessage(ChatColor.RED + "Taking ownership away from " + plugin.getPlayers().getName(oldOwner));
		plugin.getPlayers().setIslandLevel(newOwner, plugin.getPlayers().getIslandLevel(oldOwner));
		plugin.getPlayers().setTeamIslandLocation(oldOwner, null);
		plugin.getPlayers().setHasIsland(oldOwner, false);
		plugin.getPlayers().setIslandLocation(oldOwner, null);
		plugin.getPlayers().setIslandLevel(oldOwner, 0);
		plugin.getPlayers().setTeamIslandLocation(oldOwner, null);
		// plugin.topTenChangeOwner(oldOwner, newOwner);
	    }
	    // Check if the assigned player already has an island
	    Island playersIsland = plugin.getGrid().getIsland(newOwner);
	    if (playersIsland != null) {
		sender.sendMessage(ChatColor.RED + plugin.getPlayers().getName(playersIsland.getOwner()) + " had an island at ("
			+ playersIsland.getCenter().getBlockX() + "," + playersIsland.getCenter().getBlockZ() + ")");
		plugin.getGrid().setIslandOwner(playersIsland, null);
	    }

	    plugin.getPlayers().setHomeLocation(newOwner, island.getCenter());
	    plugin.getPlayers().setHasIsland(newOwner, true);
	    plugin.getPlayers().setIslandLocation(newOwner, island.getCenter());
	    // Change the grid
	    plugin.getGrid().setIslandOwner(island, newOwner);
	    return true;
	} else {
	    sender.sendMessage(ChatColor.RED + "There is no known island in this area!");
	    return false;
	}
    }
    
    @Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
    final List<String> options = new ArrayList<String>();
	String lastArg = (args.length != 0 ? args[args.length - 1] : "");
    
	if (!(sender instanceof Player)) {
	//Server console or something else; doesn't have
	//permission checking.
	
	switch (args.length) {
	case 0: 
	case 1:
		options.addAll(Arrays.asList("reload", "topten", "unregister",
				"delete", "completechallenge", "resetchallenge",
				"resetallchallenges", "purge", "info", "info", "info",
				"clearreset", "setbiome", "topbreeders", "team", "team"));
		break;
	case 2:
		if (args[0].equalsIgnoreCase("unregister")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("delete")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("completechallenge")
				|| args[0].equalsIgnoreCase("resetchallenge")) {
			options.addAll(plugin.getChallenges().getAllChallenges());
		}
		if (args[0].equalsIgnoreCase("resetallchallenges")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("info")) {
			options.add("challenges");
			
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("clearreset")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("setbiome")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("team")) {
			options.add("add");
			options.add("kick");
		}
		break;
	case 3: 
		if (args[0].equalsIgnoreCase("completechallenge")
				|| args[0].equalsIgnoreCase("resetchallenge")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("info")
				&& args[1].equalsIgnoreCase("challenges")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if (args[0].equalsIgnoreCase("setbiome")) {
			final Biome[] biomes = Biome.values();
			for (Biome b : biomes) {
				if (plugin.getConfig().contains("biomes." + b.name())) {
				options.add(b.name());
				}
			}
		}
		if (args[0].equalsIgnoreCase("team")
				&& (args[1].equalsIgnoreCase("add")
				|| args[1].equalsIgnoreCase("kick"))) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		break;
	case 4:
		if (args[0].equalsIgnoreCase("team") && args[1].equalsIgnoreCase("add")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
	}
	} else {
	final Player player = (Player) sender;

	switch (args.length) {
	case 0: 
	case 1: 
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reload") || player.isOp()) {
	    	options.add("reload");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.register") || player.isOp()) {
	    	options.add("register");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp()) {
	    	options.add("unregister");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp()) {
	    	options.add("delete");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.deleteisland") || player.isOp()) {
	    	options.add("deleteisland");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.purge") || player.isOp()) {
	    	options.add("purge");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topten") || player.isOp()) {
	    	options.add("topten");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topbreeders") || player.isOp()) {
	    	options.add("topbreeders");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
	    	options.add("completechallenge");
			options.add("resetchallenge");
			options.add("resetallchallenges");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp()) {
	    	options.add("info");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp()) {
	    	options.add("clearreset");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setspawn") || player.isOp()) {
	    	options.add("setspawn");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setrange") || player.isOp()) {
	    	options.add("setrange");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp()) {
	    	options.add("tp");
	    }
	    if (Settings.newNether && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tpnether") || player.isOp()) {
	    	options.add("tpnether");
	    }

	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp()) {
	    	options.add("setbiome");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp()) {
	    	options.add("team");
	    }
		break;
	case 2:
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp())
				&& args[0].equalsIgnoreCase("unregister")) {
		//TODO this is really repetitive -- move the players online code to a shared function.
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp())
				&& args[0].equalsIgnoreCase("delete")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
				&& (args[0].equalsIgnoreCase("completechallenge") || args[0].equalsIgnoreCase("resetchallenge"))) {
			options.addAll(plugin.getChallenges().getAllChallenges());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
				&& args[0].equalsIgnoreCase("resetallchallenges")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp())
				&& args[0].equalsIgnoreCase("info")) {
			options.add("challenges");
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp())
				&& args[0].equalsIgnoreCase("clearreset")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp())
				&& (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("tpnether"))) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp())
				&& args[0].equalsIgnoreCase("setbiome")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp())
				&& args[0].equalsIgnoreCase("team")) {
	    	options.add("kick");
	    	options.add("add");
	    }
		break;
	case 3: 
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
				&& (args[0].equalsIgnoreCase("completechallenge") || args[0].equalsIgnoreCase("resetchallenge"))) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp())
				&& args[0].equalsIgnoreCase("info")
				&& args[1].equalsIgnoreCase("challenges")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp())
				&& args[0].equalsIgnoreCase("setbiome")) {
			final Biome[] biomes = Biome.values();
			for (Biome b : biomes) {
				if (plugin.getConfig().contains("biomes." + b.name())) {
				options.add(b.name());
				}
			}
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp())
				&& args[0].equalsIgnoreCase("team")
					&& (args[1].equalsIgnoreCase("add")
					|| args[1].equalsIgnoreCase("kick"))) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
		break;
	case 4:
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp())
				&& args[0].equalsIgnoreCase("team")
				&& args[1].equalsIgnoreCase("add")) {
			final List<Player> players = PlayerCache.getOnlinePlayers();
			for (Player p : players) {
				options.add(p.getName());
			}
		}
	}
	}

	return tabLimit(options, lastArg);
	}
    
    /**
	 * Returns all of the items that begin with the given start, 
	 * ignoring case. 
	 * 
	 * TODO: May want to move this to a more centralized class, 
	 * as it is probably going to be re-used a lot.
	 * 
	 * @param list
	 * @param start
	 * @return
	 */
	private List<String> tabLimit(final List<String> list, final String start) {
	List<String> returned = new ArrayList<String>();
	for (String s : list) {
	if (s.toLowerCase().startsWith(start.toLowerCase())) { //Case-insensitive startsWith.
		returned.add(s);
	}
	}
	
	return returned;
	}
}