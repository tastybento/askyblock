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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

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
public class AdminCmd implements CommandExecutor {
    private ASkyBlock plugin;
    private List<UUID> removeList = new ArrayList<UUID>();
    private boolean purgeFlag = false;
    private boolean confirmReq = false;
    private boolean confirmOK = false;
    private int confirmTimer = 0;

    protected AdminCmd(ASkyBlock aSkyBlock) {
	this.plugin = aSkyBlock;
    }

    private void help(CommandSender sender, String label) {
	if (!(sender instanceof Player)) {
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " topten:" + ChatColor.WHITE + " " + Locale.adminHelptopTen);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <player>:" + ChatColor.WHITE + " " + Locale.adminHelpdelete);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " completechallenge <challengename> <player>:" + ChatColor.WHITE
		    + " " + Locale.adminHelpcompleteChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " resetchallenge <challengename> <player>:" + ChatColor.WHITE
		    + " " + Locale.adminHelpresetChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " resetallchallenges <player>:" + ChatColor.WHITE + " " + Locale.adminHelpresetAllChallenges);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + Locale.adminHelppurge);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info challenges <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " info:" + ChatColor.WHITE + " " + Locale.adminHelpinfoIsland);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " clearreset <player>:" + ChatColor.WHITE + " " + Locale.adminHelpclearReset);
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " Sets leader's island biome.");
	    sender.sendMessage(ChatColor.GREEN + "== Team Editing Commands ==");
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " team kick <player>:" + ChatColor.WHITE + " Removes player from any team.");
	    sender.sendMessage(ChatColor.YELLOW + "/" + label + " team add <player> <leader>:" + ChatColor.WHITE + " Adds player to leader's team.");	    
	} else {
	    // Only give help if the player has permissions
	    // Permissions are split into admin permissions and mod permissions
	    Player player = (Player)sender;
	    player.sendMessage(Locale.adminHelpHelp);
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reload") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.register") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " register <player>:" + ChatColor.WHITE + " " + Locale.adminHelpregister);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " delete <player>:" + ChatColor.WHITE + " " + Locale.adminHelpdelete);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.purge") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + Locale.adminHelppurge);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " purge holes:" + ChatColor.WHITE + " " + Locale.adminHelppurgeholes);

	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topten") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " topten:" + ChatColor.WHITE + " " + Locale.adminHelptopTen);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " completechallenge <challengename> <player>:" + ChatColor.WHITE
			+ " " + Locale.adminHelpcompleteChallenge);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetchallenge <challengename> <player>:" + ChatColor.WHITE
			+ " " + Locale.adminHelpresetChallenge);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " resetallchallenges <player>:" + ChatColor.WHITE + " " + Locale.adminHelpresetAllChallenges);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info:" + ChatColor.WHITE + " Info on nearest island.");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info challenges <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);

	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " clearreset <player>:" + ChatColor.WHITE + " " + Locale.adminHelpclearReset);
	    }	    
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setspawn") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " setspawn:" + ChatColor.WHITE + " " + Locale.adminHelpSetSpawn);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/" + label + " tp <player>:" + ChatColor.WHITE + " " + Locale.adminHelptp);
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp()) {
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " Sets leader's island biome.");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp()) {
		sender.sendMessage(ChatColor.GREEN + "== Team Editing Commands ==");
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " team kick <player>:" + ChatColor.WHITE + " Removes player from any team.");
		sender.sendMessage(ChatColor.YELLOW + "/" + label + " team add <player> <leader>:" + ChatColor.WHITE + " Adds player to leader's team.");
		//sender.sendMessage(ChatColor.YELLOW + "/" + label + " team delete <leader>:" + ChatColor.WHITE + " Removes the leader's team compeletely.");
	    }

	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	// Console commands
	Player player;
	if (sender instanceof Player) {
	    player = (Player)sender;
	    if (split.length > 0) {
		// Admin : reload, register, delete and purge
		if (split[0].equalsIgnoreCase("reload") || split[0].equalsIgnoreCase("register")
			|| split[0].equalsIgnoreCase("delete") || split[0].equalsIgnoreCase("purge")
			|| split[0].equalsIgnoreCase("confirm") || split[0].equalsIgnoreCase("setspawn")) {
		    if (!checkAdminPerms(player, split)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoPermission);
			return true;
		    }
		} else {
		    // Mod commands
		    if (!checkModPerms(player, split)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoPermission);
			return true;
		    }		    
		}
	    }
	}
	// Check for zero parameters e.g., /asadmin
	switch (split.length) {
	case 0:
	    help(sender,label);
	    return true;
	case 1:
	    if (split[0].equalsIgnoreCase("setspawn")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
		    return true;
		}
		Player p = (Player)sender;
		// TODO: The island location needs to be calculated based on the grid, NOT the bedrock location


		Location closestBedRock = null;
		double distance = 0;
		for (int x = -Settings.islandDistance; x< Settings.islandDistance; x++) {
		    for (int z = -Settings.islandDistance; z< Settings.islandDistance; z++) {

			Location blockLoc = new Location(p.getWorld(),x + p.getLocation().getBlockX(),Settings.island_level,z + p.getLocation().getBlockZ());
			if (blockLoc.getBlock().getType().equals(Material.BEDROCK)) {
			    if (closestBedRock == null) {
				closestBedRock = blockLoc.clone();
				distance = closestBedRock.distanceSquared(p.getLocation());
			    } else {
				// Find out if this is closer to the player

				double newDist = blockLoc.distanceSquared(p.getLocation());
				if (distance > newDist) {
				    closestBedRock = blockLoc.clone();
				    distance = newDist;
				}
			    }
			}
		    }
		}

		// TODO: find closest island locus and check file syste,

		if (closestBedRock == null) {
		    sender.sendMessage(ChatColor.RED + "Sorry, could not find an island. Move closer?");
		    return true;
		}
		// Find out whose island this is
		//plugin.getLogger().info("DEBUG: closest bedrock: " + closestBedRock.toString());
		UUID target = plugin.getPlayers().getPlayerFromIslandLocation(closestBedRock);
		if (target == null) {
		    sender.sendMessage(ChatColor.GREEN + "This island is not owned by anyone right now.");
		} else {
		    sender.sendMessage(ChatColor.GREEN + "This island was owned by " + plugin.getPlayers().getName(target));
		    if (plugin.getPlayers().inTeam(target)) {
			sender.sendMessage(ChatColor.RED + "That is a team island. Remove the team members first!");
			return true;
		    }
		}
		// Set the spawn
		plugin.getSpawn().setSpawnLoc(closestBedRock,((Player)sender).getLocation());
		// Set it for the world too
		closestBedRock.getWorld().setSpawnLocation(((Player)sender).getLocation().getBlockX(),closestBedRock.getBlockY(),closestBedRock.getBlockZ());
		// Remove player's ownership and set them to having no island
		if (target != null) {
		    plugin.getPlayers().setIslandLevel(target, 0);
		    plugin.getPlayers().setHasIsland(target, false);
		    plugin.getPlayers().setHomeLocation(target, null);
		    plugin.getPlayers().setIslandLocation(target, null);
		    plugin.getPlayers().save(target);
		}
		sender.sendMessage(ChatColor.GREEN + "Converted island to spawn. (to undo, use " + label + " register <playername>");
		sender.sendMessage(ChatColor.GREEN + "Settings are in spawn.yml");
		sender.sendMessage(ChatColor.GREEN + "Set spawn location.");
		plugin.getSpawn().save();
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
		    return true;
		}
		Location closestIsland = getClosestIsland(((Player)sender).getLocation());
		if (closestIsland == null) {
		    sender.sendMessage(ChatColor.RED + "Sorry, could not find an island. Move closer?");
		    return true;
		}
		//plugin.getLogger().info("DEBUG: location = " + closestIsland.toString());
		// Find out whose island this is
		//plugin.getLogger().info("DEBUG: closest bedrock: " + closestBedRock.toString());
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
		Player p = (Player)sender;

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
		Sign sign = (Sign)lastBlock.getState();
		try {
		    if (!sign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + Locale.warpswelcomeLine) &&
			    !sign.getLine(0).equalsIgnoreCase(ChatColor.RED + Locale.warpswelcomeLine)) {
			sender.sendMessage(ChatColor.RED + "You must be looking at a Warp Sign to run this command. (wrong line)");
			return true;  		    
		    }
		} catch (Exception e) {
		    sender.sendMessage(ChatColor.RED + "You must be looking at a Warp Sign to run this command. (exception)");
		    return true;  		    
		}
		sender.sendMessage(ChatColor.GREEN + "Warp sign found!");
		Location closestBedRock = getClosestIsland(((Player)sender).getLocation());
		if (closestBedRock == null) {
		    sender.sendMessage(ChatColor.RED + "Sorry, could not find island bedrock. Move closer?");
		    return true;
		}
		// Find out whose island this is
		//plugin.getLogger().info("DEBUG: closest bedrock: " + closestBedRock.toString());
		UUID target = plugin.getPlayers().getPlayerFromIslandLocation(closestBedRock);
		if (target == null) {
		    sender.sendMessage(ChatColor.RED + "This island is not owned by anyone right now - recommend that sign is removed.");
		    return true;
		}
		if (plugin.addWarp(target, lastBlock.getLocation())) {
		    sender.sendMessage(ChatColor.GREEN + "Warp rescued and assigned to " + plugin.getPlayers().getName(target));
		    return true;	    
		}
		// Warp already exists
		sender.sendMessage(ChatColor.RED + "That warp sign is already active and owned by " + plugin.getWarpOwner(lastBlock.getLocation()));
		return true;


	    } else if (split[0].equalsIgnoreCase("reload")) {
		plugin.reloadConfig();
		plugin.loadPluginConfig();
		plugin.reloadChallengeConfig();
		if (Settings.useEconomy) {
		    ControlPanel.loadShop();
		}
		ControlPanel.loadControlPanel();
		plugin.getSpawn().reload();
		sender.sendMessage(ChatColor.YELLOW + Locale.reloadconfigReloaded);
		return true;
	    } else if (split[0].equalsIgnoreCase("topten")) {
		sender.sendMessage(ChatColor.YELLOW + Locale.adminTopTengenerating);
		plugin.updateTopTen();
		sender.sendMessage(ChatColor.YELLOW + Locale.adminTopTenfinished);
		return true;
	    } else if (split[0].equalsIgnoreCase("purge")) {
		if (purgeFlag) {
		    sender.sendMessage(ChatColor.RED + Locale.purgealreadyRunning);
		    return true;
		}
		sender.sendMessage(ChatColor.YELLOW + Locale.purgeusage.replace("[label]", label));
		return true;
	    } else if (split[0].equalsIgnoreCase("confirm")) { 
		if (!confirmReq) {
		    sender.sendMessage(ChatColor.RED + Locale.confirmerrorTimeLimitExpired);
		    return true;
		} else {
		    // Tell purge routine to go
		    confirmOK = true;
		    confirmReq = false;
		}
		return true;
	    } else {
		sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
		return false;
	    }
	case 2:
	    if (split[0].equalsIgnoreCase("purge")) {
		// PURGE Command
		// Purge runs in the background so if one is already running this flag stops a repeat
		if (purgeFlag) {
		    sender.sendMessage(ChatColor.RED + Locale.purgealreadyRunning);
		    return true;
		}
		// Set the flag
		purgeFlag = true;
		// See if this purge holes
		if (split[1].equalsIgnoreCase("holes")) {
		    purgeHoles(sender);
		    return true;
		}

		// Convert days to hours - no other limit checking?
		final int time = Integer.parseInt(split[1]) * 24;

		sender.sendMessage(ChatColor.YELLOW + Locale.purgecalculating.replace("[time]", split[1]));
		// Kick off task
		plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
		    public void run() {
			final File directoryPlayers = new File(plugin.getDataFolder() + File.separator + "players");

			long offlineTime = 0L;
			// Go through the player directory and build the purge list of filenames
			for (final File playerFile : directoryPlayers.listFiles()) {
			    if (playerFile.getName().endsWith(".yml")) {
				final UUID playerUUID = UUID.fromString(playerFile.getName().substring(0, playerFile.getName().length()-4));
				// Only bother if the layer is offline (by definition)
				if (Bukkit.getOfflinePlayer(playerUUID) != null && Bukkit.getPlayer(playerUUID) == null) {
				    final OfflinePlayer oplayer = Bukkit.getOfflinePlayer(playerUUID);
				    offlineTime = oplayer.getLastPlayed();
				    // Calculate the number of hours the player has
				    // been offline
				    offlineTime = (System.currentTimeMillis() - offlineTime) / 3600000L;
				    //plugin.getLogger().info(plugin.getPlayers().getName(playerUUID) + " has been offline " + offlineTime + " hours. Required = " + time);
				    if (offlineTime > time) {
					//plugin.getLogger().info(plugin.getPlayers().getName(playerUUID) + " has not logged on recently enough");
					if (plugin.getPlayers().hasIsland(playerUUID)) {
					    // If the player is in a team then ignore
					    if (!plugin.getPlayers().inTeam(playerUUID)) {
						//plugin.getLogger().info("and is a lone player");
						if (plugin.getPlayers().getIslandLevel(playerUUID) < Settings.abandonedIslandLevel) {
						    //plugin.getLogger().info("and their island will be removed!");
						    //player.sendMessage("Island level for " + plugin.getPlayers().getName(playerUUID) + " is " + plugin.getPlayers().getIslandLevel(playerUUID));
						    removeList.add(playerUUID);
						} else {
						    //plugin.getLogger().info("but thei island level is > " + Settings.abandonedIslandLevel + " so not deleting");
						}
					    } else {
						//plugin.getLogger().info("but is in a team");
					    }
					} else {
					    //plugin.getLogger().info("but does not have an island.");
					}
				    }
				}
			    }
			}
			if (removeList.isEmpty()) {
			    sender.sendMessage(ChatColor.YELLOW + Locale.purgenoneFound);
			    purgeFlag = false;
			    return;
			}
			sender.sendMessage(ChatColor.YELLOW + Locale.purgethisWillRemove.replace("[number]",String.valueOf(removeList.size())));
			sender.sendMessage(ChatColor.RED + Locale.purgewarning);
			sender.sendMessage(ChatColor.RED + Locale.purgetypeConfirm.replace("[label]", label));
			confirmReq = true;
			confirmOK = false;
			confirmTimer = 0;
			new BukkitRunnable() {
			    @Override
			    public void run() {
				// This waits for 10 seconds and if no confirmation received, then it
				// cancels
				if (confirmTimer++ > 10) {
				    // Ten seconds is up!
				    confirmReq = false;
				    confirmOK = false;
				    purgeFlag = false;
				    removeList.clear();
				    sender.sendMessage(ChatColor.YELLOW + Locale.purgepurgeCancelled);
				    this.cancel();
				} else if (confirmOK) {
				    // Set up a repeating task to run every 5 seconds to remove
				    // islands one by one and then cancel when done
				    final int total = removeList.size();
				    new BukkitRunnable() {
					@Override
					public void run() {
					    if (removeList.isEmpty() && purgeFlag) {
						purgeFlag = false;
						sender.sendMessage(ChatColor.YELLOW + Locale.purgefinished);
						this.cancel();
					    } 

					    if (removeList.size() > 0 && purgeFlag) {
						plugin.deletePlayerIsland(removeList.get(0));
						sender.sendMessage(ChatColor.YELLOW + "[" + removeList.size() + "/" + total + "] " 
							+ Locale.purgeremovingName.replace("[name]", plugin.getPlayers().getName(removeList.get(0))));
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
			}.runTaskTimer(plugin, 0L,40L);
		    }
		});
		return true;
	    } else if (split[0].equalsIgnoreCase("clearreset")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {
		    plugin.getPlayers().setResetsLeft(playerUUID, Settings.resetLimit);
		    sender.sendMessage(ChatColor.YELLOW + Locale.clearedResetLimit + " [" + Settings.resetLimit + "]");
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("tp")) {
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
		    return true;
		}
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {
		    if (plugin.getPlayers().getIslandLocation(playerUUID) != null) {
			Location safeSpot = plugin.getSafeHomeLocation(playerUUID);
			if (safeSpot != null) {
			    // This next line should help players with long ping times
			    ((Player)sender).sendBlockChange(safeSpot,safeSpot.getBlock().getType(),safeSpot.getBlock().getData());
			    ((Player)sender).teleport(safeSpot);
			} else {
			    sender.sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
			    Location warpSpot = plugin.getPlayers().getIslandLocation(playerUUID);
			    sender.sendMessage(ChatColor.RED + "Manually warp to somewhere near " + warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " " + warpSpot.getBlockZ());
			}
			return true;
		    }
		    sender.sendMessage(Locale.errorNoIslandOther);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("delete")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {
		    // This now deletes the player and cleans them up even if they don't have an island
		    sender.sendMessage(ChatColor.YELLOW + Locale.deleteremoving.replace("[name]", split[1]));
		    // If they are online and in ASkyBlock then delete their stuff too
		    Player target = plugin.getServer().getPlayer(playerUUID);
		    if (target != null) {
			// Clear any coop inventories
			//CoopPlay.getInstance().returnAllInventories(target);
			// Remove any of the target's coop invitees and grab their stuff
			CoopPlay.getInstance().clearMyInvitedCoops(target);
			CoopPlay.getInstance().clearMyCoops(target);
			plugin.resetPlayer(target);
		    }
		    plugin.deletePlayerIsland(playerUUID);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("register")) {
		if (sender instanceof Player) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    } else {
			if (adminSetPlayerIsland(sender, ((Player) sender).getLocation(), playerUUID)) {
			    sender.sendMessage(ChatColor.GREEN + Locale.registersettingIsland.replace("[name]",split[1]));
			} else {
			    sender.sendMessage(ChatColor.RED + Locale.registererrorBedrockNotFound);
			}
			return true;
		    }
		} else {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		//plugin.getLogger().info("DEBUG: console player info UUID = " + playerUUID);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {
		    showInfo(playerUUID, sender);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("resetallchallenges")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		}
		plugin.getPlayers().resetAllChallenges(playerUUID);
		sender.sendMessage(ChatColor.YELLOW + Locale.resetChallengessuccess.replace("[name]", split[1]));
		return true;
	    }  else {
		return false;
	    }
	case 3:
	    // Change biomes
	    if (split[0].equalsIgnoreCase("setbiome")) {
		// Convert name to a UUID
		final UUID playerUUID = plugin.getPlayers().getUUID(split[1]);
		// Check if player exists
		if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
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
		    if (!plugin.getConfig().contains("biomes."+biomeName)) {
			sender.sendMessage(ChatColor.RED + Locale.biomeUnknown);
			// Doing it this way ensures that only valid biomes are shown
			for (Biome b: Biome.values()) {
			    if (plugin.getConfig().contains("biomes."+b.name())) {
				sender.sendMessage(b.name());
			    }
			}
			return true;
		    }
		    // Get friendly name
		    biomeName = plugin.getConfig().getString("biomes." + biomeName + ".friendlyname", ASkyBlock.prettifyText(biomeName));

		} catch (Exception e) {
		    sender.sendMessage(ChatColor.RED + Locale.biomeUnknown);
		    for (Biome b: Biome.values()) {
			if (plugin.getConfig().contains("biomes."+b.name())) {
			    sender.sendMessage(b.name());
			}
		    }
		    return true;
		}
		// Okay clear to set biome
		// Actually set the biome
		if (plugin.getPlayers().inTeam(playerUUID) && plugin.getPlayers().getTeamIslandLocation(playerUUID) != null) {
		    plugin.setIslandBiome(plugin.getPlayers().getTeamIslandLocation(playerUUID),biome);
		} else {
		    plugin.setIslandBiome(plugin.getPlayers().getIslandLocation(playerUUID), biome);
		}
		sender.sendMessage(ChatColor.GREEN + Locale.biomeSet.replace("[biome]", biomeName));
		Player targetPlayer = plugin.getServer().getPlayer(playerUUID);
		if (targetPlayer != null) {
		    // Online
		    targetPlayer.sendMessage("[Admin] " + ChatColor.GREEN + Locale.biomeSet.replace("[biome]", biomeName));
		} else {
		    plugin.setMessage(playerUUID, "[Admin] " + ChatColor.GREEN + Locale.biomeSet.replace("[biome]", biomeName));
		}
		return true;
	    } else 
		// team kick <player> and team delete <leader>
		if (split[0].equalsIgnoreCase("team")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    }
		    if (split[1].equalsIgnoreCase("kick")) {
			// Remove player from team
			if (!plugin.getPlayers().inTeam(playerUUID)) {
			    sender.sendMessage(ChatColor.RED + Locale.errorNoTeam);
			    return true;
			}
			UUID teamLeader= plugin.getPlayers().getTeamLeader(playerUUID);
			// Payer is not a team leader
			if (!teamLeader.equals(playerUUID)) {
			    // Clear the player of all team-related items
			    plugin.getPlayers().setLeaveTeam(playerUUID);
			    plugin.getPlayers().setHomeLocation(playerUUID, null);
			    plugin.getPlayers().setIslandLocation(playerUUID, null);
			    //Clear the leader of this player and if they now have no team, remove the team 
			    plugin.getPlayers().removeMember(teamLeader, playerUUID);
			    if (plugin.getPlayers().getMembers(teamLeader).size() < 2) {
				plugin.getPlayers().setLeaveTeam(teamLeader);
			    }				
			    // Remove any warps
			    plugin.removeWarp(playerUUID);
			    sender.sendMessage(ChatColor.RED + Locale.kicknameRemoved.replace("[name]", split[2]));
			    // If target is online -- do not tell target
			    /*
			    Player target = plugin.getServer().getPlayer(playerUUID);
			    if (target != null) {
				target.sendMessage(ChatColor.RED + Locale.kicknameRemovedYou.replace("[name]", sender.getName()));
			    } else {
				plugin.setMessage(playerUUID,ChatColor.RED + Locale.kicknameRemovedYou.replace("[name]", sender.getName()));
			    }*/
			    return true;
			} else {
			    sender.sendMessage(ChatColor.RED + "That player is a team leader. Remove team members first. Use '/" + label + " info " + split[2] + "' to find team members.");
			    return true;
			}		    
		    } else {
			sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
			return false;
		    }
		}  else if (split[0].equalsIgnoreCase("completechallenge")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    }
		    if (plugin.getPlayers().checkChallenge(playerUUID,split[1].toLowerCase()) || !plugin.getPlayers().get(playerUUID).challengeExists(split[1].toLowerCase())) {
			sender.sendMessage(ChatColor.RED + Locale.completeChallengeerrorChallengeDoesNotExist);
			return true;
		    }
		    plugin.getPlayers().get(playerUUID).completeChallenge(split[1].toLowerCase());
		    sender.sendMessage(ChatColor.YELLOW + Locale.completeChallengechallangeCompleted.replace("[challengename]", split[1].toLowerCase()).replace("[name]", split[2]));
		    return true;
		} else if (split[0].equalsIgnoreCase("resetchallenge")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    }
		    if (!plugin.getPlayers().checkChallenge(playerUUID,split[1].toLowerCase())
			    || !plugin.getPlayers().get(playerUUID).challengeExists(split[1].toLowerCase())) {
			sender.sendMessage(ChatColor.RED + Locale.resetChallengeerrorChallengeDoesNotExist);
			return true;
		    }
		    plugin.getPlayers().resetChallenge(playerUUID,split[1].toLowerCase());
		    sender.sendMessage(ChatColor.YELLOW +  Locale.resetChallengechallengeReset.replace("[challengename]", split[1].toLowerCase()).replace("[name]",split[2]));
		    return true;
		} else if (split[0].equalsIgnoreCase("info") && split[1].equalsIgnoreCase("challenges")) {
		    // Convert name to a UUID
		    final UUID playerUUID = plugin.getPlayers().getUUID(split[2]);
		    //plugin.getLogger().info("DEBUG: console player info UUID = " + playerUUID);
		    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
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
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
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
		    sender.sendMessage(ChatColor.RED + Locale.inviteerrorThatPlayerIsAlreadyInATeam);
		    return true;
		}
		// If the leader's member list does not contain their own name then
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
			sender.sendMessage(ChatColor.RED + plugin.getPlayers().getName(playerUUID) + " had an island at " + islandLoc.getBlockX() + " " + islandLoc.getBlockZ() 
				+ " that will become unowned now. You may want to delete it manually.");
		    }
		}
		// Remove their old island affiliation - do not delete the island just in case
		plugin.getPlayers().setIslandLocation(playerUUID, null);
		plugin.getPlayers().setHasIsland(playerUUID, false);
		// Join the team and set the team island location and leader
		plugin.getPlayers().setJoinTeam(playerUUID, teamLeader, plugin.getPlayers().getIslandLocation(teamLeader));
		// Configure the best home location for this player
		if (plugin.getPlayers().getHomeLocation(teamLeader) != null) {
		    plugin.getPlayers().setHomeLocation(playerUUID,plugin.getPlayers().getHomeLocation(teamLeader));
		    sender.sendMessage(ChatColor.GREEN + "Setting player's home to the leader's home location");		
		} else {
		    plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getIslandLocation(teamLeader));
		    sender.sendMessage(ChatColor.GREEN + "Setting player's home to the leader's island location");
		}
		// If the leader's member list does not contain player then add it
		if (!plugin.getPlayers().getMembers(teamLeader).contains(playerUUID)) {
		    plugin.getPlayers().addTeamMember(teamLeader,playerUUID);
		    sender.sendMessage(ChatColor.GREEN + "Adding player to team.");
		} else {
		    sender.sendMessage(ChatColor.GOLD + "Player was already on this team!");
		}
		// Teleport the player if they are online
		if (targetPlayer != null) {
		    plugin.homeTeleport(targetPlayer);
		} 
		return true;
	    } else {
		sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
		return false;
	    }
	default:
	    return false;
	}
    }

    /**
     * Tries to free up holes in the map for reuse
     * @param sender 
     */
    private void purgeHoles(final CommandSender sender) {
	// Get a list of all the files in the island directory
	final File islandFileDir = new File(plugin.getDataFolder() + File.separator + "islands");
	if (!islandFileDir.exists()) {
	    sender.sendMessage(ChatColor.GREEN + Locale.purgenoneFound);
	    return;
	}
	// Make a list of islands to check
	List <Location> islands = new ArrayList<Location>();
	File[] filenames = islandFileDir.listFiles();
	for (File file : filenames) {
	    try {
		String name = file.getName();
		if (name.endsWith(".yml")) {
		    // Get the coordinates of this file
		    name = name.substring(0, name.length() - 4);
		    String[] split = name.split(",");
		    int x = Integer.valueOf(split[0]);
		    int z = Integer.valueOf(split[1]);
		    int y = Settings.island_level;

		    // Quick check
		    Location loc = new Location(ASkyBlock.getIslandWorld(), x,y,z);
		    if (!loc.getBlock().getType().equals(Material.BEDROCK)) {
			// Check distance from spawn
			if (plugin.getSpawn().getBedrock() != null) {
			    // Spawn exists
			    Vector spawn = plugin.getSpawn().getBedrock().toVector().multiply(new Vector(1,0,1));
			    Vector locVector = loc.toVector().multiply(new Vector(1,0,1));
			    if (spawn.distanceSquared(locVector) > (double)((double)plugin.getSpawn().getRange()) * plugin.getSpawn().getRange()) {
				// Far enough away from spawn
				islands.add(loc);
			    }
			} else {
			    // No spawn
			    islands.add(loc);
			}
		    }
		}
	    } catch (Exception e) {
		plugin.getLogger().warning("Could not read filename " + file.getName() + " skipping...");
	    }
	}
	sender.sendMessage(ChatColor.GREEN + "Found " + islands.size() + " possible island holes. Now confirming...");
	// Set up a repeating task to run every 5 seconds to remove
	// holes one by one and then cancel when done
	final List<Location> removeList = islands;
	new BukkitRunnable() {
	    @Override
	    public void run() {
		if (removeList.isEmpty() && purgeFlag) {
		    purgeFlag = false;
		    sender.sendMessage(ChatColor.YELLOW + Locale.purgefinished);
		    this.cancel();
		} 
		if (removeList.size() > 0 && purgeFlag) {
		    Location loc = removeList.get(0);
		    boolean islandFound = false;
		    // Look around
		    final int px = loc.getBlockX();
		    final int pz = loc.getBlockZ();

		    for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
			    for (int y = 10; y <= 255; y++) {
				if (loc.getWorld().getBlockAt(x + px, y, z + pz).getType().equals(Material.BEDROCK)) {
				    //plugin.getLogger().info("Bedrock found during long search at " + (x + px) + " " + y + " " + (z + pz));
				    islandFound = true;
				    break;
				}
			    }
			}
		    }
		    if (!islandFound) {
			// Remove the file
			String filename = px + "," + pz + ".yml";
			sender.sendMessage(ChatColor.GREEN + "Removing " + filename);
			final File islandFile = new File(plugin.getDataFolder() + File.separator + "islands" + File.separator + filename);
			if (islandFile.exists()) {
			    if (!islandFile.delete()) {
				sender.sendMessage(ChatColor.RED + "Could not delete " + islandFile.getName() + "!");
			    }
			    return;
			}
		    }
		    removeList.remove(0);
		}
	    }
	}.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     * @param location
     * @return
     */
    protected static Location getClosestIsland(Location location) {
	long x = Math.round((double)location.getBlockX() / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
	long z = Math.round((double)location.getBlockZ() / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;
	long y = Settings.island_level;
	return new Location(location.getWorld(),x,y,z);
	/*
	for (int x = -Settings.islandDistance; x< Settings.islandDistance; x++) {
	    for (int z = -Settings.islandDistance; z< Settings.islandDistance; z++) {
		Location blockLoc = new Location(location.getWorld(),x + location.getBlockX(),Settings.island_level,z + location.getBlockZ());
		if (blockLoc.getBlock().getType().equals(Material.BEDROCK)) {
		    if (closestBedRock == null) {
			closestBedRock = blockLoc.clone();
			distance = closestBedRock.distanceSquared(location);
		    } else {
			// Find out if this is closer to the player

			double newDist = blockLoc.distanceSquared(location);
			if (distance > newDist) {
			    closestBedRock = blockLoc.clone();
			    distance = newDist;
			}
		    }
		}
	    }
	}
	// TODO Auto-generated method stub
	return closestBedRock;*/
    }

    /**
     * Shows info on a player
     * @param playerUUID
     * @param sender
     */
    private void showInfo(UUID playerUUID, CommandSender sender) {
	sender.sendMessage("Owner:" + ChatColor.GREEN + plugin.getPlayers().getName(playerUUID));
	sender.sendMessage(ChatColor.WHITE + "UUID: " + playerUUID.toString());
	// Display island level
	sender.sendMessage(ChatColor.GREEN + Locale.levelislandLevel + ": " + plugin.getPlayers().getIslandLevel(playerUUID));
	// Last login
	try {
	    Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed());
	    sender.sendMessage(ChatColor.GOLD + "Last login: " + d.toString());
	} catch (Exception e) {}

	// Teams
	if (plugin.getPlayers().inTeam(playerUUID)) {
	    final UUID leader = plugin.getPlayers().getTeamLeader(playerUUID);
	    final List<UUID> pList = plugin.getPlayers().getMembers(leader);
	    sender.sendMessage(ChatColor.GREEN + plugin.getPlayers().getName(leader) );
	    for (UUID member: pList) {
		sender.sendMessage(ChatColor.WHITE + " - " + plugin.getPlayers().getName(member));
	    }
	    sender.sendMessage(ChatColor.YELLOW + Locale.adminInfoislandLocation + ":" + ChatColor.WHITE + " (" + plugin.getPlayers().getTeamIslandLocation(playerUUID).getBlockX() + ","
		    + plugin.getPlayers().getTeamIslandLocation(playerUUID).getBlockY() + "," + plugin.getPlayers().getTeamIslandLocation(playerUUID).getBlockZ() + ")");
	} else {
	    sender.sendMessage(ChatColor.YELLOW + Locale.errorNoTeam);
	    if (plugin.getPlayers().hasIsland(playerUUID)) {
		sender.sendMessage(ChatColor.YELLOW + Locale.adminInfoislandLocation + ":" + ChatColor.WHITE + " (" + plugin.getPlayers().getIslandLocation(playerUUID).getBlockX() + ","
			+ plugin.getPlayers().getIslandLocation(playerUUID).getBlockY() + "," + plugin.getPlayers().getIslandLocation(playerUUID).getBlockZ() + ")");
	    }
	    if (!(plugin.getPlayers().getTeamLeader(playerUUID) == null)) {
		sender.sendMessage(ChatColor.RED + Locale.adminInfoerrorNullTeamLeader);
	    }
	    if (!plugin.getPlayers().getMembers(playerUUID).isEmpty()) {
		sender.sendMessage(ChatColor.RED + Locale.adminInfoerrorTeamMembersExist);
	    }
	}

    }

    /**
     * Shows info on the challenge situation for player
     * @param playerUUID
     * @param sender
     */
    private void showInfoChallenges(UUID playerUUID, CommandSender sender) {
	sender.sendMessage("Name:" + ChatColor.GREEN + plugin.getPlayers().getName(playerUUID));
	sender.sendMessage(ChatColor.WHITE + "UUID: " + playerUUID.toString());
	// Completed challenges
	sender.sendMessage(ChatColor.WHITE + "Challenges:");
	HashMap<String,Boolean> challenges = plugin.getPlayers().getChallengeStatus(playerUUID);
	for (String c: challenges.keySet()) {
	    sender.sendMessage(c + ": " + ((challenges.get(c)) ? ChatColor.GREEN + Locale.challengescomplete :ChatColor.AQUA + Locale.challengesincomplete));
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
     * @param player
     *            - the assignee
     * @return - true if successful, false if not
     */
    protected boolean adminSetPlayerIsland(final CommandSender sender, final Location l, final UUID player) {
	// TODO switch to file system
	// If the player is not online
	Location island = getClosestIsland(l);
	if (island != null) {
	    plugin.getPlayers().setHomeLocation(player, island);
	    plugin.getPlayers().setHasIsland(player,true);
	    plugin.getPlayers().setIslandLocation(player, island);
	    return true;
	}
	return false;
	/*
	final int px = l.getBlockX();
	final int py = l.getBlockY();
	final int pz = l.getBlockZ();
	for (int x = -10; x <= 10; x++) {
	    for (int y = -10; y <= 10; y++) {
		for (int z = -10; z <= 10; z++) {
		    final Block b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock();
		    if (b.getType().equals(Material.BEDROCK)) {
			plugin.getPlayers().setHomeLocation(player,new Location(l.getWorld(), px + x, py + y + 3, pz + z));
			plugin.getPlayers().setHasIsland(player,true);
			plugin.getPlayers().setIslandLocation(player, b.getLocation());
			return true;
		    }
		}
	    }
	}
	return false;
	 */
    }
}