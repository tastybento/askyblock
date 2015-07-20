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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.CoopPlay;
import com.wasteofplastic.askyblock.DeleteIslandChunk;
import com.wasteofplastic.askyblock.GridManager;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.PlayerCache;
import com.wasteofplastic.askyblock.SafeSpotTeleport;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.Settings.GameType;
import com.wasteofplastic.askyblock.TopTen;
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

    public AdminCmd(ASkyBlock aSkyBlock) {
	this.plugin = aSkyBlock;
    }

    private void help(CommandSender sender, String label) {
	if (!(sender instanceof Player)) {
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " clearreset <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpclearReset);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " completechallenge <challengename> <player>:" + ChatColor.WHITE + " "
		    + plugin.myLocale().adminHelpcompleteChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpdelete);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info challenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfoIsland);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " lock <player>: " + ChatColor.WHITE + " " + plugin.myLocale().adminHelplock);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelppurge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpreload);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " resetallchallenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpresetAllChallenges);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " resetchallenge <challengename> <player>:" + ChatColor.WHITE + " "
		    + plugin.myLocale().adminHelpresetChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " resethome <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpResetHome);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetBiome);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " team add <player> <leader>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpadd);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " team kick <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpkick);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " topbreeders: " + ChatColor.WHITE + " " + plugin.myLocale().adminHelptopBreeders);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " topten:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelptopTen);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " unregister <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpunregister);

	} else {
	    // Only give help if the player has permissions
	    // Permissions are split into admin permissions and mod permissions
	    // Listed in alphabetical order
	    Player player = (Player) sender;
	    player.sendMessage(plugin.myLocale(player.getUniqueId()).adminHelpHelp);
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " clearreset <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpclearReset);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " completechallenge <challengename> <player>:" + ChatColor.WHITE + " "
			+ plugin.myLocale(player.getUniqueId()).adminHelpcompleteChallenge);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " delete <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpdelete);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.deleteisland") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " deleteisland confirm:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpdelete);
	    }

	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfoIsland);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfo);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info challenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfo);

	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.lock") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " lock <player>: " + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelplock);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.purge") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurge);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge unowned:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurgeUnowned);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge allow/disallow:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurgeAllowDisallow);
	    }

	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reload") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " reload:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpreload);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.register") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " register <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpregister);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.resethome") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resethome <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpResetHome);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetchallenge <challengename> <player>:" + ChatColor.WHITE + " "
			+ plugin.myLocale(player.getUniqueId()).adminHelpresetChallenge);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetallchallenges <player>:" + ChatColor.WHITE + " "
			+ plugin.myLocale(player.getUniqueId()).adminHelpresetAllChallenges);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetsign:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpResetSign);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetsign <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpResetSign);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp()) {
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpsetBiome);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.resethome") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " sethome <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetHome);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setspawn") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " setspawn:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetSpawn);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setrange") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " setrange:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetRange);
	    }
	    if (Settings.teamChat && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.teamchatspy") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " spy:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpTeamChatSpy);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp()) {
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " team kick <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpkick);
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " team add <player> <leader>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpadd);
		// sender.sendMessage(ChatColor.YELLOW + "/" + label +
		// " team delete <leader>:" + ChatColor.WHITE +
		// " Removes the leader's team compeletely.");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topten") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " topten:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptopTen);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topbreeders") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " topbreeders: " + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptopBreeders);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " tp <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptp);
	    }
	    if (Settings.createNether && Settings.newNether && (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tpnether") || player.isOp())) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " tpnether <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptpNether);
	    }

	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " unregister <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpunregister);
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
	    if (Settings.teamChat && split[0].equalsIgnoreCase("spy")) {
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
		    return true;
		}
		player = (Player) sender;
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.teamchatspy") || player.isOp()) {
		    if (plugin.getChatListener().toggleSpy(player.getUniqueId())) {
			sender.sendMessage(ChatColor.GREEN + plugin.myLocale().teamChatStatusOn);
		    } else {
			sender.sendMessage(ChatColor.GREEN + plugin.myLocale().teamChatStatusOff);
		    }
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("lock")) {
		// Just /asadmin lock
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
		    return true;
		}
		player = (Player) sender;
		Island island = plugin.getGrid().getIslandAt(player.getLocation());
		// Check if island exists
		if (island == null) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNotOnIsland);
		    return true;
		} else {
		    Player owner = plugin.getServer().getPlayer(island.getOwner());
		    if (island.isLocked()) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().lockUnlocking);
			island.setLocked(false);
			if (owner != null) {
			    owner.sendMessage(plugin.myLocale(owner.getUniqueId()).adminLockadminUnlockedIsland);
			} else {
			    plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminUnlockedIsland);
			}
		    } else {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().lockLocking);
			island.setLocked(true);
			if (owner != null) {
			    owner.sendMessage(plugin.myLocale(owner.getUniqueId()).adminLockadminLockedIsland);
			} else {
			    plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminLockedIsland);
			}
		    }
		    return true;
		}
	    } else
		// Find farms
		if (split[0].equalsIgnoreCase("topbreeders")) {
		    // Go through each island and find how many farms there are
		    sender.sendMessage(plugin.myLocale().adminTopBreedersFinding);
		    //TreeMap<Integer, List<UUID>> topEntityIslands = new TreeMap<Integer, List<UUID>>();
		    // Generate the stats
		    sender.sendMessage(plugin.myLocale().adminTopBreedersChecking.replace("[number]",String.valueOf(plugin.getGrid().getOwnershipMap().size())));
		    // Try just finding every entity
		    final List<Entity> allEntities = ASkyBlock.getIslandWorld().getEntities();
		    final World islandWorld = ASkyBlock.getIslandWorld();
		    final World netherWorld = ASkyBlock.getNetherWorld();
		    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
			    Map<UUID, Multiset<EntityType>> result = new HashMap<UUID, Multiset<EntityType>>();
			    // Find out where the entities are
			    for (Entity entity: allEntities) {
				//System.out.println("DEBUG " + entity.getType().toString());
				if (entity.getLocation().getWorld().equals(islandWorld) || entity.getLocation().getWorld().equals(netherWorld)) {
				    //System.out.println("DEBUG in world");
				    if (entity instanceof Creature && !(entity instanceof Player)) {
					//System.out.println("DEBUG creature");
					// Find out where it is
					Island island = plugin.getGrid().getIslandAt(entity.getLocation());
					if (island != null && !island.isSpawn()) {
					    //System.out.println("DEBUG on island");
					    // Add to result
					    UUID owner = island.getOwner();
					    Multiset<EntityType> count = result.get(owner);
					    if (count == null) {
						// New entry for owner
						//System.out.println("DEBUG new entry for owner");
						count = HashMultiset.create();
					    }
					    count.add(entity.getType());
					    result.put(owner, count);
					}
				    }
				}
			    }
			    // Sort by the number of entities on each island
			    TreeMap<Integer, List<UUID>> topEntityIslands = new TreeMap<Integer, List<UUID>>();
			    for (Entry<UUID, Multiset<EntityType>> entry : result.entrySet()) {
				int numOfEntities = entry.getValue().size();
				List<UUID> players = topEntityIslands.get(numOfEntities);
				if (players == null) {
				    players = new ArrayList<UUID>();
				} 
				players.add(entry.getKey());
				topEntityIslands.put(numOfEntities, players);
			    }
			    final TreeMap<Integer, List<UUID>> topBreeders = topEntityIslands;
			    final Map<UUID, Multiset<EntityType>> finalResult = result;
			    // Now display results in sync thread
			    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

				@Override
				public void run() {
				    if (topBreeders.isEmpty()) {
					sender.sendMessage(plugin.myLocale().adminTopBreedersNothing);
					return;
				    }
				    int rank = 1;
				    // Display, largest first
				    for (int numOfEntities : topBreeders.descendingKeySet()) {
					// Only bother if there's more that 5 animals
					if (numOfEntities > 5) {
					    // There can be multiple owners in the same position
					    List<UUID> owners = topBreeders.get(numOfEntities);
					    // Go through the owners one by one
					    for (UUID owner : owners) {
						sender.sendMessage("#" + rank + " " + plugin.getPlayers().getName(owner) + " = " + numOfEntities);
						String content = "";
						Multiset<EntityType> entityCount = finalResult.get(owner);
						for (EntityType entity: entityCount.elementSet()) {
						    int num = entityCount.count(entity);
						    String color = ChatColor.GREEN.toString();
						    if (num > 10 && num <= 20) {
							color = ChatColor.YELLOW.toString();
						    } else if (num > 20 && num <= 40) {
							color = ChatColor.GOLD.toString();
						    } else if (num > 40) {
							color = ChatColor.RED.toString();
						    }
						    content += Util.prettifyText(entity.toString()) + " x " + color + num + ChatColor.WHITE + ", ";
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
				    // If we didn't show anything say so
				    if (rank == 1) {
					sender.sendMessage(plugin.myLocale().adminTopBreedersNothing);
				    }

				}});

			}});
		    return true;
		}
	    // Delete island
	    if (split[0].equalsIgnoreCase("deleteisland")) {
		sender.sendMessage(ChatColor.RED + plugin.myLocale().adminDeleteIslandError);
		return true;
	    }
	    // Set spawn
	    if (split[0].equalsIgnoreCase("setspawn")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUseInGame);
		    return true;
		}
		player = (Player) sender;
		// Island spawn must be in the island world
		if (!player.getLocation().getWorld().getName().equals(Settings.worldName)) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
		    return true;
		}
		// The island location is calculated based on the grid
		Location closestIsland = getClosestIsland(player.getLocation());
		Island oldSpawn = plugin.getGrid().getSpawn();
		Island newSpawn = plugin.getGrid().getIslandAt(closestIsland);
		if (newSpawn != null && newSpawn.isSpawn()) {
		    // Already spawn, so just set the world spawn coords
		    plugin.getGrid().setSpawnPoint(player.getLocation());
		    //ASkyBlock.getIslandWorld().setSpawnLocation(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminSetSpawnset);
		    return true;
		}
		// Space otherwise occupied - find if anyone owns it
		if (newSpawn != null && newSpawn.getOwner() != null) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminSetSpawnownedBy.replace("[name]",plugin.getPlayers().getName(newSpawn.getOwner())));
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminSetSpawnmove);
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
		plugin.getGrid().setSpawnPoint(player.getLocation());
		//ASkyBlock.getIslandWorld().setSpawnLocation(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		player.sendMessage(ChatColor.GREEN + plugin.myLocale().adminSetSpawnsetting.replace("[location]", player.getLocation().getBlockX() + "," + player.getLocation().getBlockZ()));
		player.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", newSpawn.getCenter().getBlockX() + "," + newSpawn.getCenter().getBlockZ()));
		player.sendMessage(ChatColor.YELLOW + (plugin.myLocale().adminSetSpawnlimits.replace("[min]", newSpawn.getMinX() + "," + newSpawn.getMinZ())).replace("[max]",
			(newSpawn.getMinX() + newSpawn.getIslandDistance() - 1) + "," + (newSpawn.getMinZ() + newSpawn.getIslandDistance() - 1)));
		player.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(newSpawn.getProtectionSize())));
		player.sendMessage(ChatColor.YELLOW + (plugin.myLocale().adminSetSpawncoords.replace("[min]",  newSpawn.getMinProtectedX() + ", " + newSpawn.getMinProtectedZ())).replace("[max]",
			+ (newSpawn.getMinProtectedX() + newSpawn.getProtectionSize() - 1) + ", "
				+ (newSpawn.getMinProtectedZ() + newSpawn.getProtectionSize() - 1)));
		if (newSpawn.isLocked()) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale().adminSetSpawnlocked);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info") || split[0].equalsIgnoreCase("setrange")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUseInGame);
		    return true;
		}
		Location closestIsland = getClosestIsland(((Player) sender).getLocation());
		if (closestIsland == null) {
		    sender.sendMessage(ChatColor.RED + "Sorry, could not find an island. Move closer?");
		    return true;
		}
		Island island = plugin.getGrid().getIslandAt(closestIsland);
		if (island != null && island.isSpawn()) {
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminInfotitle);
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
		    sender.sendMessage(ChatColor.YELLOW + (plugin.myLocale().adminSetSpawnlimits.replace("[min]", island.getMinX() + "," + island.getMinZ())).replace("[max]",
			    (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1)));
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(island.getProtectionSize())));
		    sender.sendMessage(ChatColor.YELLOW + (plugin.myLocale().adminSetSpawncoords.replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ())).replace("[max]",
			    + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
				    + (island.getMinProtectedZ() + island.getProtectionSize() - 1)));
		    if (island.isLocked()) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminSetSpawnlocked);
		    }
		    return true;
		}
		if (island == null) {
		    plugin.getLogger().info("Get island at was null" + closestIsland);
		}
		UUID target = plugin.getPlayers().getPlayerFromIslandLocation(closestIsland);
		if (target == null) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminInfounowned);
		    return true;
		}
		showInfo(target, sender);
		return true;
	    } else if (split[0].equalsIgnoreCase("resetsign")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUseInGame);
		    return true;
		}
		player = (Player) sender;
		if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") && !player.isOp()) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return true;
		}
		// Find out whether the player is looking at a warp sign
		// Look at what the player was looking at
		BlockIterator iter = new BlockIterator(player, 10);
		Block lastBlock = iter.next();
		while (iter.hasNext()) {
		    lastBlock = iter.next();
		    if (lastBlock.getType() == Material.AIR)
			continue;
		    break;
		}
		if (!lastBlock.getType().equals(Material.SIGN_POST)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminResetSignNoSign);
		    return true;
		}
		// Check if it is a warp sign
		Sign sign = (Sign) lastBlock.getState();
		sender.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminResetSignFound);
		// Find out whose island this is
		// plugin.getLogger().info("DEBUG: closest bedrock: " +
		// closestBedRock.toString());
		UUID target = plugin.getPlayers().getPlayerFromIslandLocation(player.getLocation());
		if (target == null) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminInfounowned);
		    return true;
		}
		if (plugin.getWarpSignsListener().addWarp(target, lastBlock.getLocation())) {
		    // Change sign color to green
		    sign.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
		    sign.update();
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminResetSignRescued.replace("[name]", plugin.getPlayers().getName(target)));
		    return true;
		}
		// Warp already exists
		sender.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminResetSignErrorExists.replace("[name]", plugin.getWarpSignsListener().getWarpOwner(lastBlock.getLocation())));
		return true;

	    } else if (split[0].equalsIgnoreCase("reload")) {
		plugin.reloadConfig();
		plugin.loadPluginConfig();
		plugin.getChallenges().reloadChallengeConfig();
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
		plugin.getIslandCmd().loadSchematics();
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
	    // Resetsign <player> - makes a warp sign for player
	    if (split[0].equalsIgnoreCase("resetsign")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUseInGame);
		    return true;
		}
		Player p = (Player) sender;
		if (!VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.signadmin") && !p.isOp()) {
		    p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorNoPermission);
		    return true;
		}
		// Convert target name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		} else {
		    // Check if this player has an island
		    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
			// No island
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoIslandOther);
			return true;
		    }
		    // Has an island
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
		    // Check if it is a sign
		    if (!lastBlock.getType().equals(Material.SIGN_POST)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).adminResetSignNoSign);
			return true;
		    }
		    Sign sign = (Sign) lastBlock.getState();
		    // Check if the sign is within the right island boundary
		    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
		    if (!plugin.getGrid().getIslandAt(islandLoc).inIslandSpace(sign.getLocation())) {
			p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).adminSetHomeNotOnPlayersIsland);
		    } else {
			sender.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminResetSignFound);
			// Find out if this player is allowed to have a sign on this island
			if (plugin.getWarpSignsListener().addWarp(playerUUID, lastBlock.getLocation())) {
			    // Change sign color to green
			    sign.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
			    sign.update();
			    p.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminResetSignRescued.replace("[name]", plugin.getPlayers().getName(playerUUID)));
			    return true;
			}
			// Warp already exists
			sender.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).adminResetSignErrorExists.replace("[name]", plugin.getWarpSignsListener().getWarpOwner(lastBlock.getLocation())));
		    }
		}
		return true;
	    }
	    // Delete the island you are on
	    else if (split[0].equalsIgnoreCase("deleteisland")) {
		if (!split[1].equalsIgnoreCase("confirm")) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminDeleteIslandError);
		    return true;
		}
		// Get the island I am on
		Island island = plugin.getGrid().getIslandAt(((Player) sender).getLocation());
		if (island == null) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminDeleteIslandnoid);
		    return true;
		}
		// Try to get the owner of this island
		UUID owner = island.getOwner();
		String name = "unknown";
		if (owner != null) {
		    name = plugin.getPlayers().getName(owner);
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminSetSpawnownedBy.replace("[name]", name));
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminDeleteIslanduse.replace("[name]",name));
		    return true;
		} else {
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().deleteremoving.replace("[name]", name));
		    deleteIslands(island, sender);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("resethome")) { 
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		} else {
		    // Check if this player has an island
		    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
			// No island
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoIslandOther);
			return true;
		    }
		    // Has an island
		    Location safeHome = plugin.getGrid().getSafeHomeLocation(playerUUID, 1);
		    if (safeHome == null) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminSetHomeNoneFound);
		    } else {
			plugin.getPlayers().setHomeLocation(playerUUID, safeHome);
			sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminSetHomeHomeSet.replace("[location]", safeHome.getBlockX() + ", " + safeHome.getBlockY() + "," + safeHome.getBlockZ()));
		    }
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("sethome")) { 
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
		    return true;
		}
		player = (Player)sender;
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		} else {
		    // Check if this player has an island
		    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
			// No island
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIslandOther);
			return true;
		    }
		    // Has an island
		    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
		    // Check the player is within the island boundaries
		    if (!plugin.getGrid().getIslandAt(islandLoc).inIslandSpace(player.getLocation())) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminSetHomeNotOnPlayersIsland);
		    } else {
			// Check that the location is safe
			if (!GridManager.isSafeLocation(player.getLocation())) {
			    // Not safe
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminSetHomeNoneFound);
			} else {
			    // Success
			    plugin.getPlayers().setHomeLocation(playerUUID, player.getLocation());
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminSetHomeHomeSet.replace("[location]", player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ()));
			}
		    }
		}
		return true;
	    } else
		// Set protection for the island the player is on
		if (split[0].equalsIgnoreCase("setrange")) {
		    if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
			return true;
		    }
		    player = (Player)sender;
		    UUID playerUUID = player.getUniqueId();
		    Island island = plugin.getGrid().getIslandAt(player.getLocation());
		    // Check if island exists
		    if (island == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale().errorNotOnIsland);
			return true;
		    } else {
			int newRange = 10;
			int maxRange = Settings.islandDistance;
			// If spawn do something different
			if (island.isSpawn()) {
			    try {
				newRange = Integer.valueOf(split[1]);
			    } catch (Exception e) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid);
				return true;
			    }
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
			    if (newRange > maxRange) {
				player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(playerUUID).adminSetRangeWarning.replace("[max]",String.valueOf(maxRange)));
				player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeWarning2);
			    }
			    island.setProtectionSize(newRange);
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawnlimits.replace("[min]", island.getMinX() + "," + island.getMinZ()).replace("[max]",
				    (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1)));
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(island.getProtectionSize())));
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawncoords.replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ()).replace("[max]",
				    + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
					    + (island.getMinProtectedZ() + island.getProtectionSize() - 1)));
			    if (island.isLocked()) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().adminSetSpawnlocked);
			    }
			} else {
			    if (!plugin.getConfig().getBoolean("island.overridelimit")) {
				maxRange -= 16;
			    }
			    try {
				newRange = Integer.valueOf(split[1]);
			    } catch (Exception e) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid + " "  + plugin.myLocale(playerUUID).adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));
				return true;
			    }
			    if (newRange < 10 || newRange > maxRange) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid + " "  + plugin.myLocale(playerUUID).adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));
				return true;
			    }
			    island.setProtectionSize(newRange);
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
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
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
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
		// Check who has not been online since the time
		for (Entry<UUID, Island> entry: plugin.getGrid().getOwnershipMap().entrySet()) {
		    //plugin.getLogger().info("UUID = " + entry.getKey());
		    // Only do this if it isn't protected
		    if (entry.getKey() != null && !entry.getValue().isPurgeProtected()) {
			if (Bukkit.getOfflinePlayer(entry.getKey()).hasPlayedBefore()) {
			    long offlineTime = Bukkit.getOfflinePlayer(entry.getKey()).getLastPlayed();
			    offlineTime = (System.currentTimeMillis() - offlineTime) / 3600000L;
			    if (offlineTime > time) {
				//if (plugin.getPlayers().getIslandLevel(entry.getKey()) < Settings.abandonedIslandLevel) {
				// Check level later
				removeList.add(entry.getKey());
				//}
			    }
			} else {
			    removeList.add(entry.getKey());
			}
		    }
		}
		if (removeList.isEmpty()) {
		    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgenoneFound);
		    purgeFlag = false;
		    return true;
		}
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().purgethisWillRemove.replace("[number]", String.valueOf(removeList.size())).replace("[level]", String.valueOf(Settings.abandonedIslandLevel)));
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
					// Check the level
					if (plugin.getPlayers().getIslandLevel(removeList.get(0)) < Settings.abandonedIslandLevel) {
					    sender.sendMessage(ChatColor.YELLOW + "[" + (total - removeList.size() + 1) + "/" + total + "] "
						    + plugin.myLocale().purgeremovingName.replace("[name]", plugin.getPlayers().getName(removeList.get(0))));
					    plugin.deletePlayerIsland(removeList.get(0), true);
					} 
					removeList.remove(0);
				    }
				    //sender.sendMessage("Now waiting...");
				}
			    }.runTaskTimer(plugin, 0L, 20L);
			    confirmReq = false;
			    confirmOK = false;
			    this.cancel();
			}
		    }
		}.runTaskTimer(plugin, 0L, 40L);
		return true;
	    } else if (split[0].equalsIgnoreCase("lock")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		} else {
		    Island island = plugin.getGrid().getIsland(playerUUID);
		    if (island != null) {
			Player owner = plugin.getServer().getPlayer(island.getOwner());
			if (island.isLocked()) {
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().lockUnlocking);
			    island.setLocked(false);
			    if (owner != null) {
				owner.sendMessage(plugin.myLocale(owner.getUniqueId()).adminLockadminUnlockedIsland);
			    } else {
				plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminUnlockedIsland);
			    }
			} else {
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().lockLocking);
			    island.setLocked(true);
			    if (owner != null) {
				owner.sendMessage(plugin.myLocale(owner.getUniqueId()).adminLockadminLockedIsland);
			    } else {
				plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminLockedIsland);
			    }
			}
		    } else {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoIslandOther);
		    }
		    return true;
		}
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
		player = (Player)sender;
		// Convert name to a UUID
		final UUID targetUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(targetUUID)) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
		    return true;
		} else {
		    if (plugin.getPlayers().hasIsland(targetUUID) || plugin.getPlayers().inTeam(targetUUID)) {
			// Teleport to the over world
			Location warpSpot = plugin.getPlayers().getIslandLocation(targetUUID).toVector().toLocation(ASkyBlock.getIslandWorld());
			String failureMessage = ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminTpManualWarp.replace("[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
				+ warpSpot.getBlockZ());
			new SafeSpotTeleport(plugin, player, warpSpot, failureMessage);
			return true;
		    }
		    sender.sendMessage(plugin.myLocale().errorNoIslandOther);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("tpnether")) {
		if (!Settings.createNether || !Settings.newNether) {
		    return false;
		}
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		    return true;
		}
		player = (Player)sender;
		// Convert name to a UUID
		final UUID targetUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(targetUUID)) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
		    return true;
		} else {
		    if (plugin.getPlayers().hasIsland(targetUUID) || plugin.getPlayers().inTeam(targetUUID)) {
			// Teleport to the nether
			Location warpSpot = plugin.getPlayers().getIslandLocation(targetUUID).toVector().toLocation(ASkyBlock.getNetherWorld());
			String failureMessage = ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminTpManualWarp.replace("[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
				+ warpSpot.getBlockZ());
			new SafeSpotTeleport(plugin, player, warpSpot, failureMessage);
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
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminUnregisterOnTeam);
			return true;
		    }
		    Location island = plugin.getPlayers().getIslandLocation(playerUUID);
		    if (island == null) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoIslandOther);
			return true;
		    }
		    // Delete player, but keep blocks
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminUnregisterKeepBlocks.replace("[location]",
			    + plugin.getPlayers().getIslandLocation(playerUUID).getBlockX() + ","
				    + plugin.getPlayers().getIslandLocation(playerUUID).getBlockZ()));
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
		UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		// Check if player exists
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		}
		// Check if the target is in a team and if so, the leader needs to be adjusted
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    playerUUID = plugin.getPlayers().getTeamLeader(playerUUID);
		}
		// Get the range that this player has now
		Island island = plugin.getGrid().getIsland(playerUUID);
		if (island == null) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoIslandOther);
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
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminSetRangeInvalid + " "  + plugin.myLocale().adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));

			return true;
		    }
		    if (newRange < 10 || newRange > maxRange) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminSetRangeInvalid + " "  + plugin.myLocale().adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));

			return true;
		    }
		    island.setProtectionSize(newRange);
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
		    showInfo(playerUUID, sender);
		    return true;
		}
	    }
	    // Change biomes
	    if (split[0].equalsIgnoreCase("setbiome")) {
		// Convert name to a UUID
		UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		// Check if player exists
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
		    return true;
		}
		// Check if the target is in a team and if so, the leader
		if (plugin.getPlayers().inTeam(playerUUID)) {   
		    playerUUID = plugin.getPlayers().getTeamLeader(playerUUID);
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
			if (teamLeader == null) {
			    // Player is apparently in a team, but there is no team leader
			    // Remove their team status
			    // Clear the player of all team-related items
			    plugin.getPlayers().setLeaveTeam(playerUUID);
			    plugin.getPlayers().setHomeLocation(playerUUID, null);
			    plugin.getPlayers().setIslandLocation(playerUUID, null);
			    // Remove any warps
			    plugin.getWarpSignsListener().removeWarp(playerUUID);
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().kicknameRemoved.replace("[name]", split[2]));
			    return true;
			}
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
			    plugin.getWarpSignsListener().removeWarp(playerUUID);
			    sender.sendMessage(ChatColor.RED + plugin.myLocale().kicknameRemoved.replace("[name]", split[2]));
			    return true;
			} else {
			    sender.sendMessage(ChatColor.RED + (plugin.myLocale().adminTeamKickLeader.replace("[label]",label)).replace("[name]",split[2]));
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
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminTeamAddLeaderToOwn);
		    return true;
		}
		// See if leader has an island
		if (!plugin.getPlayers().hasIsland(teamLeader)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminTeamAddLeaderNoIsland);
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
		    sender.sendMessage(ChatColor.GOLD + plugin.myLocale().adminTeamAddedLeader);
		}
		// This is a hack to clear any pending invitations
		if (targetPlayer != null) {
		    targetPlayer.performCommand(Settings.ISLANDCOMMAND + " decline");
		}
		// If the invitee has an island of their own
		if (plugin.getPlayers().hasIsland(playerUUID)) {
		    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
		    if (islandLoc != null) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale().adminTeamNowUnowned.replace("[name]", plugin.getPlayers().getName(playerUUID)).replace("[location]", islandLoc.getBlockX() + " "
				+ islandLoc.getBlockZ()));
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
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminTeamSettingHome);
		} else {
		    plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getIslandLocation(teamLeader));
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminTeamSettingHome);
		}
		// If the leader's member list does not contain player then add
		// it
		if (!plugin.getPlayers().getMembers(teamLeader).contains(playerUUID)) {
		    plugin.getPlayers().addTeamMember(teamLeader, playerUUID);
		    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminTeamAddingPlayer);
		} else {
		    sender.sendMessage(ChatColor.GOLD + plugin.myLocale().adminTeamAlreadyOnTeam);
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
	plugin.getGrid().removePlayersFromIsland(island,null);
	// Reset the biome
	plugin.getBiomes().setIslandBiome(island.getCenter(), Settings.defaultBiome);
	new DeleteIslandChunk(plugin, island);
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

		    sender.sendMessage(ChatColor.YELLOW + "[" + (total - unowned.size() + 1) + "/" + total + "] " + plugin.myLocale().purgeRemovingAt.replace("[location]", 
			    entry.getValue().getCenter().getWorld().getName() + " " + entry.getValue().getCenter().getBlockX()
			    + "," + entry.getValue().getCenter().getBlockZ()));
		    deleteIslands(entry.getValue(),sender);
		    // Remove from the list
		    it.remove();
		}
		sender.sendMessage(plugin.myLocale().purgeNowWaiting);
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
	    sender.sendMessage(plugin.myLocale().purgeCountingUnowned);
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
				    break;
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
			sender.sendMessage(plugin.myLocale().purgeStillChecking);
		    } else {
			// Done
			if (unowned.size() > 0) {
			    if (Settings.GAMETYPE.equals(GameType.ASKYBLOCK)) {
				sender.sendMessage(plugin.myLocale().purgeSkyBlockFound.replace("[number]", String.valueOf(unowned.size())));
			    } else {
				sender.sendMessage(plugin.myLocale().purgeAcidFound.replace("[number]", String.valueOf(unowned.size())));
			    }
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
	sender.sendMessage(plugin.myLocale().adminInfoPlayer + ": " + ChatColor.GREEN + plugin.getPlayers().getName(playerUUID));
	sender.sendMessage(ChatColor.WHITE + "UUID: " + playerUUID.toString());
	// Display island level
	sender.sendMessage(ChatColor.GREEN + plugin.myLocale().levelislandLevel + ": " + plugin.getPlayers().getIslandLevel(playerUUID));
	// Last login
	try {
	    Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed());
	    sender.sendMessage(ChatColor.GOLD + plugin.myLocale().adminInfoLastLogin + ": " + d.toString());
	} catch (Exception e) {
	}
	Location islandLoc = null;
	// Teams
	if (plugin.getPlayers().inTeam(playerUUID)) {
	    final UUID leader = plugin.getPlayers().getTeamLeader(playerUUID);
	    final List<UUID> pList = plugin.getPlayers().getMembers(leader);
	    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminInfoTeamLeader + ": " + plugin.getPlayers().getName(leader));
	    sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminInfoTeamMembers + ":");
	    for (UUID member : pList) {
		if (!member.equals(leader)) {
		    sender.sendMessage(ChatColor.WHITE + " - " + plugin.getPlayers().getName(member));
		}
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
	    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
	    sender.sendMessage(ChatColor.YELLOW + (plugin.myLocale().adminSetSpawnlimits.replace("[min]", island.getMinX() + "," + island.getMinZ())).replace("[max]",
		    (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1)));
	    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(island.getProtectionSize())));
	    sender.sendMessage(ChatColor.YELLOW + (plugin.myLocale().adminSetSpawncoords.replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ())).replace("[max]",
		    + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
			    + (island.getMinProtectedZ() + island.getProtectionSize() - 1)));
	    if (island.isSpawn()) {
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminInfoIsSpawn);
	    }
	    if (island.isLocked()) {
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminInfoIsLocked);
	    } else {
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminInfoIsUnlocked);
	    }
	    if (island.isPurgeProtected()) {
		sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminInfoIsProtected);
	    } else {
		sender.sendMessage(ChatColor.GREEN + plugin.myLocale().adminInfoIsUnprotected);
	    }
	    List<UUID> banList = plugin.getPlayers().getBanList(playerUUID);
	    if (!banList.isEmpty()) {
		sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminInfoBannedPlayers + ":");
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
	    // Number of hoppers
	    sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().adminInfoHoppers.replace("[number]", String.valueOf(island.getHopperCount())));
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
	sender.sendMessage(ChatColor.WHITE + plugin.myLocale().challengesguiTitle + ":");
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
     * Assigns player to an island
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
		sender.sendMessage(ChatColor.RED + plugin.myLocale().adminRegisterNotSpawn);
		return false;
	    }
	    UUID oldOwner = island.getOwner();
	    if (oldOwner != null) {
		if (plugin.getPlayers().inTeam(oldOwner)) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminRegisterLeadsTeam.replace("[name]", plugin.getPlayers().getName(oldOwner)));	   
		    return false;
		}
		sender.sendMessage(ChatColor.RED + plugin.myLocale().adminRegisterTaking.replace("[name]", plugin.getPlayers().getName(oldOwner)));
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
		sender.sendMessage(ChatColor.RED + (plugin.myLocale().adminRegisterHadIsland.replace("[name]", plugin.getPlayers().getName(playersIsland.getOwner())).replace("[location]",
			playersIsland.getCenter().getBlockX() + "," + playersIsland.getCenter().getBlockZ())));
		plugin.getGrid().setIslandOwner(playersIsland, null);
	    }

	    plugin.getPlayers().setHomeLocation(newOwner, island.getCenter());
	    plugin.getPlayers().setHasIsland(newOwner, true);
	    plugin.getPlayers().setIslandLocation(newOwner, island.getCenter());
	    // Change the grid
	    plugin.getGrid().setIslandOwner(island, newOwner);
	    return true;
	} else {
	    sender.sendMessage(ChatColor.RED + plugin.myLocale().adminRegisterNoIsland);
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
			"clearreset", "setbiome", "topbreeders", "team"));
		break;
	    case 2:
		if (args[0].equalsIgnoreCase("lock")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("resetsign")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("unregister")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("delete")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("completechallenge")
			|| args[0].equalsIgnoreCase("resetchallenge")) {
		    options.addAll(plugin.getChallenges().getAllChallenges());
		}
		if (args[0].equalsIgnoreCase("resetallchallenges")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("info")) {
		    options.add("challenges");

		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("clearreset")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("setbiome")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("team")) {
		    options.add("add");
		    options.add("kick");
		}
		break;
	    case 3: 
		if (args[0].equalsIgnoreCase("completechallenge")
			|| args[0].equalsIgnoreCase("resetchallenge")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (args[0].equalsIgnoreCase("info")
			&& args[1].equalsIgnoreCase("challenges")) {
		    options.addAll(Util.getOnlinePlayerList());
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
		    options.addAll(Util.getOnlinePlayerList());
		}
		break;
	    case 4:
		if (args[0].equalsIgnoreCase("team") && args[1].equalsIgnoreCase("add")) {
		    options.addAll(Util.getOnlinePlayerList());
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
		if (Settings.createNether && Settings.newNether && (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tpnether") || player.isOp())) {
		    options.add("tpnether");
		}

		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp()) {
		    options.add("setbiome");
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp()) {
		    options.add("team");
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.lock") || player.isOp()) {
		    options.add("lock");
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") || player.isOp()) {
		    options.add("resetsign");
		}
		if (Settings.teamChat && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.teamchatspy") || player.isOp()) {
		    options.add("spy");
		}
		break;
	    case 2:
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.lock") || player.isOp()) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") || player.isOp()) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp())
			&& args[0].equalsIgnoreCase("unregister")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp())
			&& args[0].equalsIgnoreCase("delete")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
			&& (args[0].equalsIgnoreCase("completechallenge") || args[0].equalsIgnoreCase("resetchallenge"))) {
		    options.addAll(plugin.getChallenges().getAllChallenges());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
			&& args[0].equalsIgnoreCase("resetallchallenges")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp())
			&& args[0].equalsIgnoreCase("info")) {
		    options.add("challenges");

		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp())
			&& args[0].equalsIgnoreCase("clearreset")) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp())
			&& (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("tpnether"))) {
		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp())
			&& args[0].equalsIgnoreCase("setbiome")) {
		    options.addAll(Util.getOnlinePlayerList());
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
		    options.addAll(Util.getOnlinePlayerList());
		}
		if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp())
			&& args[0].equalsIgnoreCase("info")
			&& args[1].equalsIgnoreCase("challenges")) {
		    options.addAll(Util.getOnlinePlayerList());
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
		    options.addAll(Util.getOnlinePlayerList());
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

	return Util.tabLimit(options, lastArg);
    }
}