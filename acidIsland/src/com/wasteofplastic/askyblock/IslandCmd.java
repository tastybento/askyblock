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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IslandCmd implements CommandExecutor {
    public boolean busyFlag = true;
    private Schematic island = null;
    public Location Islandlocation;
    private ASkyBlock plugin;
    // The island reset confirmation
    private HashMap<UUID,Boolean> confirm = new HashMap<UUID,Boolean>();
    // Last island
    Location last = new Location(ASkyBlock.getIslandWorld(), Settings.islandXOffset, Settings.island_level, Settings.islandZOffset);
    /**
     * Invite list - invited player name string (key), inviter name string (value)
     */
    private final HashMap<UUID, UUID> inviteList = new HashMap<UUID, UUID>();
    //private PlayerCache players;
    // The time a player has to wait until they can reset their island again
    private HashMap<UUID, Long> resetWaitTime = new HashMap<UUID, Long>();

    /**
     * Constructor
     * 
     * @param aSkyBlock
     * @param players 
     */
    public IslandCmd(ASkyBlock aSkyBlock) {
	// Plugin instance
	this.plugin = aSkyBlock;
	// Get the next island spot
	Location loc = getNextIsland();
	plugin.getLogger().info("Next free island spot is at " + loc.getBlockX() + "," + loc.getBlockZ());
	// Check if there is a schematic
	File schematicFile = new File(plugin.getDataFolder(), "island.schematic");
	if (!schematicFile.exists()) {
	    plugin.saveResource("island.schematic", false);
	    schematicFile = new File(plugin.getDataFolder(), "island.schematic");
	}
	if (schematicFile.exists()) {    
	    plugin.getLogger().info("Trying to load island schematic...");
	    try {
		island = Schematic.loadSchematic(schematicFile);
	    } catch (IOException e) {
		plugin.getLogger().severe("Could not load island schematic! Error in file.");
		e.printStackTrace();
	    }
	}
    }

    /*
     * PARTY SECTION!
     */

    /**
     * Adds a player to a team. The player and the teamleader MAY be the same
     * 
     * @param playerUUID
     * @param teamLeader
     * @return
     */
    public boolean addPlayertoTeam(final UUID playerUUID, final UUID teamLeader) {
	// Only add online players
	/*
	if (!plugin.getServer().getPlayer(playerUUID).isOnline() || !plugin.getServer().getPlayer(teamLeader).isOnline()) {
	    plugin.getLogger().info("Can only add player to a team if both player and leader are online.");
	    return false;
	}*/
	//plugin.getLogger().info("Adding player: " + playerUUID + " to team with leader: " + teamLeader);
	//plugin.getLogger().info("The team island location is: " + plugin.getPlayers().getIslandLocation(teamLeader));
	//plugin.getLogger().info("The leader's home location is: " + plugin.getPlayers().getHomeLocation(teamLeader) + " (may be different or null)");
	// Set the player's team giving the team leader's name and the team's island
	// location
	plugin.getPlayers().setJoinTeam(playerUUID, teamLeader, plugin.getPlayers().getIslandLocation(teamLeader));
	// If the player's name and the team leader are NOT the same when this
	// method is called then set the player's home location to the leader's
	// home location
	// if it exists, and if not set to the island location
	if (!playerUUID.equals(teamLeader)) {
	    if (plugin.getPlayers().getHomeLocation(teamLeader) != null) {
		plugin.getPlayers().setHomeLocation(playerUUID,plugin.getPlayers().getHomeLocation(teamLeader));
		plugin.getLogger().info("Setting player's home to the leader's home location");		
	    } else {
		//TODO - concerned this may be a bug
		plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getIslandLocation(teamLeader));
		plugin.getLogger().info("Setting player's home to the team island location");
	    }
	    // If the leader's member list does not contain player then add it
	    if (!plugin.getPlayers().getMembers(teamLeader).contains(playerUUID)) {
		plugin.getPlayers().addTeamMember(teamLeader,playerUUID);
	    }
	    // If the leader's member list does not contain their own name then
	    // add it
	    if (!plugin.getPlayers().getMembers(teamLeader).contains(teamLeader)) {
		plugin.getPlayers().addTeamMember(teamLeader, teamLeader);
	    }
	}
	return true;
    }

    /**
     * Removes a player from a team run by teamleader
     * 
     * @param player
     * @param teamleader
     */
    public void removePlayerFromTeam(final UUID player, final UUID teamleader) {
	// Remove player from the team
	plugin.getPlayers().removeMember(teamleader,player);
	// If player is online
	// If player is not the leader of their own team
	if (!player.equals(teamleader)) {
	    plugin.getPlayers().setLeaveTeam(player);
	    plugin.getPlayers().setHomeLocation(player, null);
	    plugin.getPlayers().setIslandLocation(player, null);
	} else {
	    // Ex-Leaders keeps their island, but the rest of the team items are removed
	    plugin.getPlayers().setLeaveTeam(player);	    
	}
    }

    /**
     * Makes an island
     * 
     * @param sender
     *            player who issued the island command
     * @return true if successful
     */
    private Location newIsland(final CommandSender sender) {
	// Player who is issuing the command
	final Player player = (Player) sender;
	final UUID playerUUID = player.getUniqueId();
	//final Players p = plugin.getPlayers().get(player.getName());
	Location next = getNextIsland();
	//plugin.getLogger().info("DEBUG: next location is " + next.toString());
	plugin.setNewIsland(true);
	Location cowSpot = generateIslandBlocks(next.getBlockX(), next.getBlockZ(), player, ASkyBlock.getIslandWorld());
	plugin.setNewIsland(false);
	//plugin.getLogger().info("DEBUG: player ID is: " + playerUUID.toString());
	plugin.getPlayers().setHasIsland(playerUUID,true);
	//plugin.getLogger().info("DEBUG: Set island to true - actually is " + plugin.getPlayers().hasIsland(playerUUID));

	plugin.getPlayers().setIslandLocation(playerUUID,next);
	// Store this island in the file system
	String islandName = next.getBlockX() + "," + next.getBlockZ() + ".yml";
	final File islandFolder = new File(plugin.getDataFolder() + File.separator + "islands");
	if (!islandFolder.exists()) {
	    islandFolder.mkdir();
	}
	final File islandFile = new File(plugin.getDataFolder() + File.separator + "islands" + File.separator + islandName);
	try {
	    if (!islandFile.createNewFile()) {
		plugin.getLogger().severe("Problem creating island file " + islandName + " - file exists, but should not!");
	    }
	} catch (IOException e) {
	    plugin.getLogger().severe("Problem creating island file " + islandName);
	}
	//plugin.getLogger().info("DEBUG: player island location is " + plugin.getPlayers().getIslandLocation(playerUUID).toString());
	// Teleport the player to a safe place
	//plugin.homeTeleport(player);
	plugin.getPlayers().save(playerUUID);
	// Remove any mobs if they just so happen to be around in the
	// vicinity
	/*
	final Iterator<Entity> ents = player.getNearbyEntities(50.0D, 250.0D, 50.0D).iterator();
	int numberOfCows = 0;
	while (ents.hasNext()) {
	    final Entity tempent = ents.next();
	    // Remove anything except for the player himself and the cow (!)
	    if (!(tempent instanceof Player) && !tempent.getType().equals(EntityType.COW)) {
		plugin.getLogger().warning("Removed an " + tempent.getType().toString() + " when creating island for " + player.getName());
		tempent.remove();
	    } else if (tempent.getType().equals(EntityType.COW)) {
		numberOfCows++;
		if (numberOfCows > 1) {
		    plugin.getLogger().warning("Removed an extra cow when creating island for " + player.getName());
		    tempent.remove();
		}
	    }
	}*/
	// Done
	return cowSpot;
    }

    /**
     * Get the location of next free island spot
     * @return Location of island spot
     */
    private Location getNextIsland() {
	// Find the next free spot
	Location next = last.clone();
	while (plugin.islandAtLocation(next)) {
	    next = nextGridLocation(next);
	}
	// Make the last next, last
	last = next.clone();
	return next;
    }

    private void resetMoney(Player player) {
	// Set player's balance in acid island to the starting balance
	try {
	    // plugin.getLogger().info("DEBUG: " + player.getName() + " " +
	    // Settings.general_worldName);
	    if (VaultHelper.econ == null) {
		//plugin.getLogger().warning("DEBUG: econ is null!");
		VaultHelper.setupEconomy();
	    }
	    Double playerBalance = VaultHelper.econ.getBalance(player, Settings.worldName);
	    // plugin.getLogger().info("DEBUG: playerbalance = " +
	    // playerBalance);
	    // Round the balance to 2 decimal places and slightly down to
	    // avoid issues when withdrawing the amount later
	    BigDecimal bd = new BigDecimal(playerBalance);
	    bd = bd.setScale(2, RoundingMode.HALF_DOWN);
	    playerBalance = bd.doubleValue();
	    // plugin.getLogger().info("DEBUG: playerbalance after rounding = "
	    // + playerBalance);
	    if (playerBalance != Settings.startingMoney)  {
		if (playerBalance > Settings.startingMoney) {
		    Double difference = playerBalance - Settings.startingMoney;
		    EconomyResponse response = VaultHelper.econ.withdrawPlayer(player, Settings.worldName, difference);
		    // plugin.getLogger().info("DEBUG: withdrawn");
		    if (response.transactionSuccess()) {
			plugin.getLogger().info(
				"FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to " + Settings.startingMoney);
		    } else {
			plugin.getLogger().warning(
				"Problem trying to withdraw " + playerBalance + " from " + player.getName() + "'s account when they typed /island!");
			plugin.getLogger().warning("Error from economy was: " + response.errorMessage);
		    }
		} else {
		    Double difference = Settings.startingMoney - playerBalance;
		    EconomyResponse response = VaultHelper.econ.depositPlayer(player, Settings.worldName, difference);
		    if (response.transactionSuccess()) {
			plugin.getLogger().info(
				"FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to " + Settings.startingMoney);
		    } else {
			plugin.getLogger().warning(
				"Problem trying to deposit " + playerBalance + " from " + player.getName() + "'s account when they typed /island!");
			plugin.getLogger().warning("Error from economy was: " + response.errorMessage);
		    }

		}
	    }
	} catch (final Exception e) {
	    plugin.getLogger().severe("Error trying to zero " + player.getName() + "'s account when they typed /island!");
	    plugin.getLogger().severe(e.getMessage());
	}

    }

    /**
     * Creates an island block by block
     * 
     * @param x
     * @param z
     * @param player
     * @param world
     */
    private Location generateIslandBlocks(final int x, final int z, final Player player, final World world) {
	Location cowSpot = null;
	Location islandLoc = new Location(world,x,Settings.island_level,z);
	cowSpot = Schematic.pasteSchematic(world, islandLoc, island, player);
	return cowSpot;
    }

    /**
     * Finds the next free island spot based off the last known island Uses
     * island_distance setting from the config file Builds up in a grid fashion
     * 
     * @param lastIsland
     * @return
     */
    private Location nextGridLocation(final Location lastIsland) {
	//plugin.getLogger().info("DEBUG nextIslandLocation");
	final int x = lastIsland.getBlockX();
	final int z = lastIsland.getBlockZ();
	final Location nextPos = lastIsland;
	if (x < z) {
	    if (-1 * x < z) {
		nextPos.setX(nextPos.getX() + Settings.islandDistance);
		return nextPos;
	    }
	    nextPos.setZ(nextPos.getZ() + Settings.islandDistance);
	    return nextPos;
	}
	if (x > z) {
	    if (-1 * x >= z) {
		nextPos.setX(nextPos.getX() - Settings.islandDistance);
		return nextPos;
	    }
	    nextPos.setZ(nextPos.getZ() - Settings.islandDistance);
	    return nextPos;
	}
	if (x <= 0) {
	    nextPos.setZ(nextPos.getZ() + Settings.islandDistance);
	    return nextPos;
	}
	nextPos.setZ(nextPos.getZ() - Settings.islandDistance);
	return nextPos;
    }

    /**
     * Calculates the island level
     * @param asker - Player object of player who is asking
     * @param targetPlayer - UUID of the player's island that is being requested
     * @return - true if successful.
     */
    public boolean calculateIslandLevel(final Player asker, final UUID targetPlayer) {
	if (!busyFlag) {
	    asker.sendMessage(ChatColor.RED + Locale.islanderrorLevelNotReady);
	    plugin.getLogger().info(asker.getName() + " tried to use /island info but someone else used it first!");
	    return false;
	}
	busyFlag = false;
	if (!plugin.getPlayers().hasIsland(targetPlayer) && !plugin.getPlayers().inTeam(targetPlayer)) {
	    asker.sendMessage(ChatColor.RED + Locale.islanderrorInvalidPlayer);
	    busyFlag = true;
	    return false;
	}

	plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
	    public void run() {
		plugin.getLogger().info("Calculating island level");
		int oldLevel = plugin.getPlayers().getIslandLevel(targetPlayer);
		try {
		    Location l;
		    if (plugin.getPlayers().inTeam(targetPlayer)) {
			l = plugin.getPlayers().getTeamIslandLocation(targetPlayer);
		    } else {
			l = plugin.getPlayers().getIslandLocation(targetPlayer);
		    }
		    int blockcount = 0;
		    // Copy the limits hashmap
		    HashMap<Material,Integer> limitCount = new HashMap<Material, Integer>();
		    for (Material m : Settings.blockLimits.keySet()) {
			limitCount.put(m, Settings.blockLimits.get(m));
			//plugin.getLogger().info("DEBUG:" + m.toString() + " x " + Settings.blockLimits.get(m));
		    }
		    if (asker.getUniqueId().equals(targetPlayer) || asker.isOp()) {
			final int px = l.getBlockX();
			final int py = l.getBlockY();
			final int pz = l.getBlockZ();
			for (int x = -(Settings.island_protectionRange / 2); x <= (Settings.island_protectionRange / 2); x++) {
			    for (int y = 0; y <= 255; y++) {
				for (int z = -(Settings.island_protectionRange / 2); z <= (Settings.island_protectionRange / 2); z++) {
				    final Block b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock();
				    final Material blockType = b.getType();
				    // Total up the values
				    if (Settings.blockValues.containsKey(blockType)) {
					if (limitCount.containsKey(blockType)) {
					    int count = limitCount.get(blockType);
					    //plugin.getLogger().info("DEBUG: Count for " + blockType + " is " + count);
					    if (count > 0) {
						limitCount.put(blockType, --count);
						blockcount += Settings.blockValues.get(blockType);
					    } 
					} else {
					    blockcount += Settings.blockValues.get(blockType);
					}
				    }
				}
			    }
			}
			plugin.getPlayers().setIslandLevel(targetPlayer, blockcount / 100);
			plugin.getPlayers().save(targetPlayer);
			//plugin.updateTopTen();
			// Tell offline team members the island level increased.
			if (plugin.getPlayers().getIslandLevel(targetPlayer) > oldLevel) {
			    plugin.tellOfflineTeam(targetPlayer, ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
			}
		    }
		} catch (final Exception e) {
		    plugin.getLogger().info("Error while calculating Island Level: " + e);
		    busyFlag = true;
		}

		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		    public void run() {
			busyFlag = true;
			plugin.updateTopTen();
			if (asker.isOnline()) {
			    if (asker.getUniqueId().equals(targetPlayer)) {
				asker.sendMessage(
					ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
			    } else {
				if (plugin.getPlayers().isAKnownPlayer(targetPlayer)) {
				    asker.sendMessage(ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
				} else {
				    asker.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
				}
			    }
			}
		    }
		}, 20L);
	    }
	});
	return true;
    }

    /**
     * One-to-one relationship, you can return the first matched key
     * @param map
     * @param value
     * @return
     */
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	for (Entry<T, E> entry : map.entrySet()) {
	    if (value.equals(entry.getValue())) {
		return entry.getKey();
	    }
	}
	return null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player) sender;
	// Basic permissions check to even use /island
	if (!VaultHelper.checkPerm(player, "askyblock.island.create")) {
	    player.sendMessage(ChatColor.RED + Locale.islanderrorYouDoNotHavePermission);
	    return true;
	}
	/*
	 * Grab data for this player - may be null or empty
	 * playerUUID is the unique ID of the player who issued the command
	 */
	final UUID playerUUID = player.getUniqueId();
	final UUID teamLeader = plugin.getPlayers().getTeamLeader(playerUUID);
	final List<UUID> teamMembers = plugin.getPlayers().getMembers(playerUUID);
	// The target player's UUID
	UUID targetPlayer = null;
	// Check if a player has an island or is in a team
	switch (split.length) {
	// /island command by itself
	case 0:
	    // New island
	    if (plugin.getPlayers().getIslandLocation(playerUUID) == null && !plugin.getPlayers().inTeam(playerUUID)) {
		// Create new island for player
		player.sendMessage(ChatColor.GREEN + Locale.islandnew);
		final Location cowSpot = newIsland(sender);
		plugin.homeTeleport(player);
		plugin.resetPlayer(player);
		if (Settings.resetMoney) {
		    resetMoney(player);
		}

		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable () {
		    @Override
		    public void run() {
			player.getWorld().spawnEntity(cowSpot, EntityType.COW);

		    }
		}, 40L);		    
		setResetWaitTime(player);
		return true;
	    } else {
		// Teleport home
		plugin.homeTeleport(player);
		if (Settings.islandRemoveMobs) {
		    plugin.removeMobs(player.getLocation());
		}
		return true;
	    }
	case 1:
	    if (split[0].equalsIgnoreCase("about")) {
		player.sendMessage(ChatColor.GOLD + "ASkyBlock (c) 2014 by TastyBento");
		//Spawn enderman
		//Enderman enderman = (Enderman) player.getWorld().spawnEntity(player.getLocation().add(new Vector(5,0,5)), EntityType.ENDERMAN);
		//enderman.setCustomName("TastyBento's Ghost");
		//enderman.setCarriedMaterial(new MaterialData(Material.GRASS));
		/*
		final Hologram h = new Hologram(plugin, ChatColor.GOLD + "" + ChatColor.BOLD + "ASkyBlock", "(c)2014 TastyBento");
		h.show(player.getLocation());
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

		    @Override
		    public void run() {
			h.destroy();
		    }}, 40L);
		 */

	    }

	    if (split[0].equalsIgnoreCase("controlpanel") || split[0].equalsIgnoreCase("cp")) {
		//if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		if (VaultHelper.checkPerm(player, "askyblock.island.controlpanel")) {
		    player.openInventory(ControlPanel.controlPanel.get(ControlPanel.getDefaultPanelName()));
		    return true;
		}
		//}
	    }

	    if (split[0].equalsIgnoreCase("minishop") || split[0].equalsIgnoreCase("ms")) {
		if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		    if (VaultHelper.checkPerm(player, "askyblock.island.minishop")) {
			player.openInventory(ControlPanel.miniShop);
			return true;
		    }
		}
	    }
	    // /island <command>
	    if (split[0].equalsIgnoreCase("warp")) {
		if (VaultHelper.checkPerm(player, "askyblock.island.warp")) {
		    player.sendMessage(ChatColor.YELLOW + "/island warp <player>: " + ChatColor.WHITE + Locale.islandhelpWarp);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("warps")) {
		if (VaultHelper.checkPerm(player, "askyblock.island.warp")) {
		    // Step through warp table
		    Set<UUID> warpList = plugin.listWarps();
		    if (warpList.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + Locale.warpserrorNoWarpsYet);
			if (VaultHelper.checkPerm(player, "askyblock.island.addwarp")) {
			    player.sendMessage(ChatColor.YELLOW + Locale.warpswarpTip);
			}
			return true;
		    } else {
			Boolean hasWarp = false;
			String wlist = "";
			for (UUID w : warpList) {
			    if (wlist.isEmpty()) {
				wlist = plugin.getPlayers().getName(w);
			    } else {
				wlist += ", " + plugin.getPlayers().getName(w);
			    }
			    if (w.equals(playerUUID)) {
				hasWarp = true;
			    }
			}
			player.sendMessage(ChatColor.YELLOW + Locale.warpswarpsAvailable + ": " + ChatColor.WHITE + wlist);
			if (!hasWarp && (VaultHelper.checkPerm(player, "askyblock.island.addwarp"))) {
			    player.sendMessage(ChatColor.YELLOW + Locale.warpswarpTip);
			}
			return true;
		    }
		}
	    } else if (split[0].equalsIgnoreCase("restart") || split[0].equalsIgnoreCase("reset")) {
		// Check this player has an island
		if (!plugin.getPlayers().hasIsland(playerUUID)) {
		    // No so just start and island
		    player.performCommand("as");
		    return true;
		}
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    if (!plugin.getPlayers().getTeamLeader(playerUUID).equals(playerUUID)) {
			player.sendMessage(ChatColor.RED
				+ Locale.islandresetOnlyOwner);
		    } else {
			player.sendMessage(ChatColor.YELLOW
				+ Locale.islandresetMustRemovePlayers);
		    }
		    return true;
		}
		// Check if the player has used up all their resets
		if (plugin.getPlayers().getResetsLeft(playerUUID) == 0) {
		    player.sendMessage(ChatColor.RED + Locale.islandResetNoMore);
		    return true;
		}
		if (plugin.getPlayers().getResetsLeft(playerUUID) > 0) {
		    player.sendMessage(ChatColor.RED + Locale.resetYouHave.replace("[number]", String.valueOf(plugin.getPlayers().getResetsLeft(playerUUID))));
		}
		if (!onRestartWaitTime(player) || Settings.resetWait == 0 || player.isOp()) {
		    // Kick off the confirmation
		    player.sendMessage(ChatColor.RED + Locale.islandresetConfirm);
		    if (!confirm.containsKey(playerUUID) || !confirm.get(playerUUID)) {
			confirm.put(playerUUID, true);
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable () {
			    @Override
			    public void run() {
				confirm.put(playerUUID,false);
			    }
			}, 200L);	
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.YELLOW + Locale.islandresetWait.replace("[time]",String.valueOf(getResetWaitTime(player))));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("confirm")) {
		// This is where the actual reset is done
		if (confirm.containsKey(playerUUID) && confirm.get(playerUUID)) {
		    // Actually RESET the island
		    player.sendMessage(ChatColor.YELLOW + Locale.islandresetPleaseWait);
		    plugin.getPlayers().setResetsLeft(playerUUID, plugin.getPlayers().getResetsLeft(playerUUID) -1);
		    if (plugin.getPlayers().getResetsLeft(playerUUID) == 0) {
			player.sendMessage(ChatColor.YELLOW + Locale.islandResetNoMore);
		    }
		    if (plugin.getPlayers().getResetsLeft(playerUUID) > 0) {
			player.sendMessage(ChatColor.YELLOW + Locale.resetYouHave.replace("[number]", String.valueOf(plugin.getPlayers().getResetsLeft(playerUUID))));
		    }
		    //plugin.getLogger().info("DEBUG Reset command issued!");
		    final Location oldIsland = plugin.getPlayers().getIslandLocation(playerUUID);
		    //plugin.unregisterEvents();		
		    final Location cowSpot = newIsland(sender);
		    plugin.getPlayers().setHomeLocation(player.getUniqueId(), null);
		    plugin.homeTeleport(player);
		    plugin.resetPlayer(player);
		    if (Settings.resetMoney) {
			resetMoney(player);
		    }

		    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable () {
			@Override
			public void run() {
			    //plugin.homeTeleport(player);
			    player.getWorld().spawnEntity(cowSpot, EntityType.COW);

			}
		    }, 40L);		    
		    //player.getWorld().spawnEntity(cowSpot, EntityType.COW);
		    setResetWaitTime(player);
		    plugin.removeWarp(playerUUID);
		    if (oldIsland != null) {
			plugin.removeIsland(oldIsland);
			DeleteIsland deleteIsland = new DeleteIsland(plugin,oldIsland);
			deleteIsland.runTaskTimer(plugin, 80L, 40L);
		    }
		    //plugin.restartEvents();
		} else {
		    player.sendMessage(ChatColor.YELLOW + "/island restart: " + ChatColor.WHITE + Locale.islandhelpRestart);
		}
	    } else if (split[0].equalsIgnoreCase("sethome")) {
		if (VaultHelper.checkPerm(player, "askyblock.island.sethome")) {
		    plugin.homeSet(player);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("help")) { 
		player.sendMessage(ChatColor.GREEN + "ASkyBlock " + plugin.getDescription().getVersion() + " help:");

		player.sendMessage(ChatColor.YELLOW + "/" + label + ": " + ChatColor.WHITE + Locale.islandhelpIsland);
		if (plugin.getSpawn().getSpawnLoc() != null) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " spawn: " + ChatColor.WHITE + Locale.islandhelpSpawn);
		}
		player.sendMessage(ChatColor.YELLOW + "/" + label + " controlpanel or cp: " + ChatColor.WHITE + Locale.islandhelpControlPanel);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " restart: " + ChatColor.WHITE + Locale.islandhelpRestart);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " sethome: " + ChatColor.WHITE + Locale.islandhelpSetHome);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " level: " + ChatColor.WHITE + Locale.islandhelpLevel);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " level <player>: " + ChatColor.WHITE + Locale.islandhelpLevelPlayer);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " top: " + ChatColor.WHITE + Locale.islandhelpTop);
		if (VaultHelper.checkPerm(player, "askyblock.island.minishop")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " minishop or ms: " + ChatColor.WHITE + Locale.islandhelpMiniShop);		    
		}
		if (VaultHelper.checkPerm(player, "askyblock.island.warp")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " warps: " + ChatColor.WHITE + Locale.islandhelpWarps);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " warp <player>: " + ChatColor.WHITE + Locale.islandhelpWarp);
		}
		if (VaultHelper.checkPerm(player, "askyblock.team.create")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " team: " + ChatColor.WHITE + Locale.islandhelpTeam);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " invite <player>: " + ChatColor.WHITE + Locale.islandhelpInvite);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " leave: " + ChatColor.WHITE + Locale.islandhelpLeave);
		}
		if (VaultHelper.checkPerm(player, "askyblock.team.kick")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " kick <player>: " + ChatColor.WHITE + Locale.islandhelpKick);
		}
		if (VaultHelper.checkPerm(player, "askyblock.team.join")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " <accept/reject>: " + ChatColor.WHITE + Locale.islandhelpAcceptReject);
		}
		if (VaultHelper.checkPerm(player, "askyblock.team.makeleader")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " makeleader <player>: " + ChatColor.WHITE + Locale.islandhelpMakeLeader);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("spawn") && plugin.getSpawn().getSpawnLoc() != null) {
		// go to spawn
		//plugin.getLogger().info("Debug: getSpawn" + plugin.getSpawn().toString() );
		//plugin.getLogger().info("Debug: getSpawn loc" + plugin.getSpawn().getSpawnLoc().toString() );
		player.teleport(plugin.getSpawn().getSpawnLoc());
		return true;
	    } else if (split[0].equalsIgnoreCase("top")) {
		if (VaultHelper.checkPerm(player, "askyblock.island.topten")) {
		    plugin.showTopTen(player);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("level")) {
		if (plugin.playerIsOnIsland(player)) {
		    if (!plugin.getPlayers().inTeam(playerUUID) && !plugin.getPlayers().hasIsland(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoIsland);
		    } else {
			calculateIslandLevel(player, playerUUID);
		    }
		    return true;
		}
		player.sendMessage(ChatColor.RED + Locale.challengeserrorNotOnIsland);
		return true;
	    } else if (split[0].equalsIgnoreCase("invite")) {
		// Invite label with no name, i.e., /island invite - tells the player how many more people they can invite
		if (VaultHelper.checkPerm(player, "askyblock.team.create")) {
		    player.sendMessage(ChatColor.YELLOW + "Use" + ChatColor.WHITE + " /" + label + " invite <playername> " + ChatColor.YELLOW
			    + Locale.islandhelpInvite);
		    // If the player who is doing the inviting has a team
		    if (plugin.getPlayers().inTeam(playerUUID)) {
			// Check to see if the player is the leader
			if (teamLeader.equals(playerUUID)) {
			    // Check to see if the team is already full
			    int maxSize = Settings.maxTeamSize;
			    if (VaultHelper.checkPerm(player, "askyblock.team.vip")) {
				maxSize = Settings.maxTeamSizeVIP;
			    }
			    if (VaultHelper.checkPerm(player, "askyblock.team.vip2")) {
				maxSize = Settings.maxTeamSizeVIP2;
			    }
			    if (teamMembers.size() < maxSize) {
				player.sendMessage(ChatColor.GREEN + Locale.inviteyouCanInvite.replace("[number]", String.valueOf(maxSize - teamMembers.size())));
			    } else {
				player.sendMessage(ChatColor.RED + Locale.inviteerrorYourIslandIsFull);
			    }
			    return true;
			}

			player.sendMessage(ChatColor.RED + Locale.inviteerrorYouMustHaveIslandToInvite);
			return true;
		    }

		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("accept")) {
		// Accept an invite command
		if (VaultHelper.checkPerm(player, "askyblock.team.join")) {
		    // If player is not in a team but has been invited to join one
		    if (!plugin.getPlayers().inTeam(playerUUID) && inviteList.containsKey(playerUUID)) {
			// If the invitee has an island of their own
			if (plugin.getPlayers().hasIsland(playerUUID)) {
			    plugin.getLogger().info(player.getName() + "'s island will be deleted because they joined a party.");
			    // Delete the island next tick
			    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
				@Override
				public void run() {
				    plugin.deletePlayerIsland(playerUUID);
				    plugin.getLogger().info("Island deleted.");
				}
			    });
			}
			// Add the player to the team
			addPlayertoTeam(playerUUID, inviteList.get(playerUUID));
			// If the leader who did the invite does not yet have a team (leader is not in a team yet)
			if (!plugin.getPlayers().inTeam(inviteList.get(playerUUID))) {
			    // Add the leader to their own team
			    addPlayertoTeam(inviteList.get(playerUUID), inviteList.get(playerUUID));
			} 
			setResetWaitTime(player);

			plugin.homeTeleport(player);
			plugin.resetPlayer(player);
			player.sendMessage(ChatColor.GREEN + Locale.inviteyouHaveJoinedAnIsland);
			if (Bukkit.getPlayer(inviteList.get(playerUUID)) != null) {
			    Bukkit.getPlayer(inviteList.get(playerUUID)).sendMessage(ChatColor.GREEN + Locale.invitehasJoinedYourIsland.replace("[name]", player.getName()));
			}
			// Remove the invite
			inviteList.remove(player.getUniqueId());
			return true;
		    }
		    player.sendMessage(ChatColor.RED + Locale.errorCommandNotReady);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("reject")) {
		// Reject /island reject
		if (inviteList.containsKey(player.getUniqueId())) {
		    player.sendMessage(ChatColor.YELLOW + Locale.rejectyouHaveRejectedInvitation);
		    // If the player is online still then tell them directly about the rejection
		    if (Bukkit.getPlayer(inviteList.get(player.getUniqueId())) != null) {
			Bukkit.getPlayer(inviteList.get(player.getUniqueId())).sendMessage(
				ChatColor.RED + Locale.rejectnameHasRejectedInvite.replace("[name]", player.getName()));
		    }
		    // Remove this player from the global invite list
		    inviteList.remove(player.getUniqueId());
		} else {
		    // Someone typed /island reject and had not been invited
		    player.sendMessage(ChatColor.RED + Locale.rejectyouHaveNotBeenInvited);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("leave")) {
		// Leave team command
		if (VaultHelper.checkPerm(player, "askyblock.team.join")) {
		    if (player.getWorld().getName().equalsIgnoreCase(ASkyBlock.getIslandWorld().getName())) {
			if (plugin.getPlayers().inTeam(playerUUID)) {
			    if (plugin.getPlayers().getTeamLeader(playerUUID).equals(playerUUID)) {
				player.sendMessage(ChatColor.YELLOW + Locale.leaveerrorYouAreTheLeader);
				return true;
			    }
			    plugin.resetPlayer(player);
			    if (!player.performCommand("spawn")) {
				player.teleport(player.getWorld().getSpawnLocation());
			    }
			    removePlayerFromTeam(playerUUID, teamLeader);
			    // Remove any warps
			    plugin.removeWarp(playerUUID);
			    player.sendMessage(ChatColor.YELLOW + Locale.leaveyouHaveLeftTheIsland);
			    // Tell the leader if they are online
			    if (plugin.getServer().getPlayer(teamLeader) != null) {
				plugin.getServer().getPlayer(teamLeader).sendMessage(ChatColor.RED + Locale.leavenameHasLeftYourIsland.replace("[name]",player.getName()));
			    } else {
				// Leave them a message
				plugin.setMessage(teamLeader, ChatColor.RED + Locale.leavenameHasLeftYourIsland.replace("[name]",player.getName()));
			    }
			    // Check if the size of the team is now 1
			    //teamMembers.remove(playerUUID);
			    if (teamMembers.size() < 3) {
				plugin.getLogger().info("Party is less than 2 - removing leader from team");
				removePlayerFromTeam(teamLeader, teamLeader);
			    }
			    return true;
			} else {
			    player.sendMessage(ChatColor.RED + Locale.leaveerrorYouCannotLeaveIsland);
			    return true;
			}
		    } else {
			player.sendMessage(ChatColor.RED + Locale.leaveerrorYouMustBeInWorld);
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("team")) {
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    if (teamLeader.equals(playerUUID)) {
			int maxSize = Settings.maxTeamSize;
			if (VaultHelper.checkPerm(player, "askyblock.team.vip")) {
			    maxSize = Settings.maxTeamSizeVIP;
			}
			if (VaultHelper.checkPerm(player, "askyblock.team.vip2")) {
			    maxSize = Settings.maxTeamSizeVIP2;
			}
			if (teamMembers.size() < maxSize) {
			    player.sendMessage(ChatColor.GREEN + Locale.inviteyouCanInvite.replace("[number]", String.valueOf(maxSize - teamMembers.size())));
			} else {
			    player.sendMessage(ChatColor.RED + Locale.inviteerrorYourIslandIsFull);
			}
		    }

		    player.sendMessage(ChatColor.YELLOW + Locale.teamlistingMembers + ":");
		    // Display members in the list
		    for (UUID m : plugin.getPlayers().getMembers(teamLeader)) {
			player.sendMessage(ChatColor.WHITE + plugin.getPlayers().getName(m));
		    }
		} else if (inviteList.containsKey(playerUUID)) {
		    // TODO: Worried about this next line...
		    player.sendMessage(ChatColor.YELLOW + Locale.invitenameHasInvitedYou.replace("[name]", plugin.getPlayers().getName(inviteList.get(playerUUID))));
		    player.sendMessage(ChatColor.WHITE + "/" + label + " [accept/reject]" + ChatColor.YELLOW + Locale.invitetoAcceptOrReject);
		} else {
		    player.sendMessage(ChatColor.RED + Locale.kickerrorNoTeam);
		}
		return true;
	    } else {
		// Incorrect syntax
		return false;
	    }

	case 2:
	    if (split[0].equalsIgnoreCase("warp")) {
		// Warp somewhere command
		if (VaultHelper.checkPerm(player, "askyblock.island.warp")) {
		    final Set<UUID> warpList = plugin.listWarps();
		    if (warpList.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + Locale.warpserrorNoWarpsYet);
			if (VaultHelper.checkPerm(player, "askyblock.island.addwarp")) {
			    player.sendMessage(ChatColor.YELLOW + Locale.warpswarpTip);
			}
			return true;
		    } else {
			// Check if this is part of a name
			UUID foundWarp = null;
			for (UUID warp : warpList) {
			    if (plugin.getPlayers().getName(warp).toLowerCase().startsWith(split[1].toLowerCase())) {
				foundWarp = warp;
				break;
			    }
			}
			if (foundWarp == null) {
			    player.sendMessage(ChatColor.RED + Locale.warpserrorDoesNotExist);
			    return true;
			} else {
			    // Warp exists!
			    final Location warpSpot = plugin.getWarp(foundWarp);
			    // Check if the warp spot is safe
			    if (warpSpot == null) {
				player.sendMessage(ChatColor.RED + Locale.warpserrorNotReadyYet);
				plugin.getLogger().warning("Null warp found, owned by " + plugin.getPlayers().getName(foundWarp));
				return true;
			    }
			    // Find out which direction the warp is facing
			    Block b = player.getWorld().getBlockAt(warpSpot);
			    if (b.getType().equals(Material.SIGN_POST)) {
				Sign sign = (Sign) b.getState();
				org.bukkit.material.Sign s = (org.bukkit.material.Sign)sign.getData();
				BlockFace directionFacing = s.getFacing();
				Location inFront = b.getRelative(directionFacing).getLocation();
				if ((ASkyBlock.isSafeLocation(inFront))) {
				    // convert blockface to angle
				    float yaw = ASkyBlock.blockFaceToFloat(directionFacing);
				    final Location actualWarp = new Location(inFront.getWorld(), inFront.getBlockX() + 0.5D, inFront.getBlockY(),
					    inFront.getBlockZ() + 0.5D, yaw, 30F);
				    player.teleport(actualWarp);
				    player.getWorld().playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1F, 1F);
				    return true;
				}
			    } else {
				// Warp has been removed
				player.sendMessage(ChatColor.RED + "Sorry, that warp is no longer active.");
				plugin.removeWarp(warpSpot);
				return true;
			    }
			    if (!(ASkyBlock.isSafeLocation(warpSpot))) {
				player.sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
				plugin.getLogger().warning("Unsafe warp found at " + warpSpot.toString() + " owned by " + plugin.getPlayers().getName(foundWarp));
				return true;
			    } else {
				final Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5D, warpSpot.getBlockY(),
					warpSpot.getBlockZ() + 0.5D);
				player.teleport(actualWarp);
				player.getWorld().playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1F, 1F);
				return true;
			    }
			}
		    }
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("level")) {
		// island level command
		if (VaultHelper.checkPerm(player, "askyblock.island.info")) {
		    if (!plugin.getPlayers().inTeam(playerUUID) && !plugin.getPlayers().hasIsland(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoIsland);
		    } else {
			// May return null if not known
			final UUID invitedPlayerUUID = plugin.getPlayers().getUUID(split[1]);
			// Invited player must be known
			if (invitedPlayerUUID == null) {
			    player.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			    return true;
			}
			calculateIslandLevel(player, plugin.getPlayers().getUUID(split[1]));
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("invite")) {
		// Team invite a player command
		if (VaultHelper.checkPerm(player, "askyblock.team.create")) {
		    // May return null if not known
		    final UUID invitedPlayerUUID = plugin.getPlayers().getUUID(split[1]);
		    // Invited player must be known
		    if (invitedPlayerUUID == null) {
			player.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    }
		    // Player must be online
		    // TODO: enable offline players to be invited
		    if (plugin.getServer().getPlayer(invitedPlayerUUID) == null) {
			player.sendMessage(ChatColor.RED + Locale.errorOfflinePlayer);
			return true;
		    }
		    // Player issuing the command must have an island
		    if (!plugin.getPlayers().hasIsland(player.getUniqueId())) {
			player.sendMessage(ChatColor.RED + Locale.inviteerrorYouMustHaveIslandToInvite);
			return true;
		    }
		    // Player cannot invite themselves
		    if (player.getName().equalsIgnoreCase(split[1])) {
			player.sendMessage(ChatColor.RED + Locale.inviteerrorYouCannotInviteYourself);
			return true;
		    }
		    // If the player already has a team then check that they are the leader, etc
		    if (plugin.getPlayers().inTeam(player.getUniqueId())) {
			// Leader?
			if (teamLeader.equals(player.getUniqueId())) {
			    // Invited player is free and not in a team
			    if (!plugin.getPlayers().inTeam(invitedPlayerUUID)) {
				// Player has space in their team
				int maxSize = Settings.maxTeamSize;
				if (VaultHelper.checkPerm(player, "askyblock.team.vip")) {
				    maxSize = Settings.maxTeamSizeVIP;
				}
				if (VaultHelper.checkPerm(player, "askyblock.team.vip2")) {
				    maxSize = Settings.maxTeamSizeVIP2;
				}
				if (teamMembers.size() < maxSize) {
				    // If that player already has an invite out then retract it.
				    // Players can only have one invite out at a time - interesting
				    if (inviteList.containsValue(playerUUID)) {
					inviteList.remove(getKeyByValue(inviteList, player.getUniqueId()));
					player.sendMessage(ChatColor.YELLOW + Locale.inviteremovingInvite);
				    }
				    // Put the invited player (key) onto the list with inviter (value)
				    // If someone else has invited a player, then this invite will overwrite the previous invite!
				    inviteList.put(invitedPlayerUUID, player.getUniqueId());
				    player.sendMessage(ChatColor.GREEN + Locale.inviteinviteSentTo.replace("[name]", split[1]));
				    // Send message to online player
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(Locale.invitenameHasInvitedYou.replace("[name]", player.getName()));
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(
					    ChatColor.WHITE + "/" + label + " [accept/reject]" + ChatColor.YELLOW + " " + Locale.invitetoAcceptOrReject);
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + Locale.invitewarningYouWillLoseIsland);
				} else {
				    player.sendMessage(ChatColor.RED + Locale.inviteerrorYourIslandIsFull);
				}
			    } else {
				player.sendMessage(ChatColor.RED + Locale.inviteerrorThatPlayerIsAlreadyInATeam);
			    }
			} else {
			    player.sendMessage(ChatColor.RED + Locale.inviteerrorYouMustHaveIslandToInvite);
			}
		    } else {
			// First-time invite player does not have a team
			// Check if invitee is in a team or not
			if (!plugin.getPlayers().inTeam(invitedPlayerUUID)) {
			    // If the inviter already has an invite out, remove it
			    if (inviteList.containsValue(playerUUID)) {
				inviteList.remove(getKeyByValue(inviteList, player.getUniqueId()));
				player.sendMessage(ChatColor.YELLOW + Locale.inviteremovingInvite);
			    }
			    // Place the player and invitee on the invite list
			    inviteList.put(invitedPlayerUUID, player.getUniqueId());

			    player.sendMessage(ChatColor.GREEN + Locale.inviteinviteSentTo.replace("[name]", split[1]));
			    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(Locale.invitenameHasInvitedYou.replace("[name]", player.getName()));
			    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(
				    ChatColor.WHITE + "/" + label + " [accept/reject]" + ChatColor.YELLOW + " " + Locale.invitetoAcceptOrReject);
			    // Check if the player has an island and warn accordingly
			    if (plugin.getPlayers().hasIsland(invitedPlayerUUID)) {
				Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + Locale.invitewarningYouWillLoseIsland);
			    }
			} else {
			    player.sendMessage(ChatColor.RED + Locale.inviteerrorThatPlayerIsAlreadyInATeam);
			}
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("kick") || split[0].equalsIgnoreCase("remove")) {
		// Island remove command with a player name, or island kick command
		if (VaultHelper.checkPerm(player, "askyblock.team.kick")) {
		    if (!plugin.getPlayers().inTeam(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.kickerrorNoTeam);
			return true;
		    }
		    // Only leaders can kick
		    if (teamLeader != null && !teamLeader.equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.kickerrorOnlyLeaderCan);
			return true;
		    }
		    // The main thing to do is check if the player name to kick is in the list of players in the team.
		    targetPlayer = null;
		    for (UUID member : teamMembers) {
			if (plugin.getPlayers().getName(member).equalsIgnoreCase(split[1])) {
			    targetPlayer = member;
			}
		    }
		    if (targetPlayer == null) {
			player.sendMessage(ChatColor.RED + Locale.kickerrorNotPartOfTeam);
			return true;
		    }
		    if (teamMembers.contains(targetPlayer)) {
			// If the player leader tries to kick or remove themselves
			if (player.getUniqueId().equals(targetPlayer)) {
			    player.sendMessage(ChatColor.RED + Locale.leaveerrorLeadersCannotLeave);
			    return true;
			}
			// If target is online
			Player target = plugin.getServer().getPlayer(targetPlayer);
			if (target != null) {
			    target.sendMessage(ChatColor.RED + Locale.kicknameRemovedYou.replace("[name]", player.getName()));

			    // Clear the player out and throw their stuff at the leader
			    if (target.getWorld().getName().equalsIgnoreCase(ASkyBlock.getIslandWorld().getName())) {
				for (ItemStack i : target.getInventory().getContents()) {
				    if (i != null) {
					try {
					    player.getWorld().dropItemNaturally(player.getLocation(), i);
					    target.getInventory().remove(i);
					} catch (Exception e) {}
				    }
				}
				for (ItemStack i : target.getEquipment().getArmorContents()) {
				    if (i != null) {
					try {
					    player.getWorld().dropItemNaturally(player.getLocation(), i);
					} catch (Exception e) {}
				    }
				}
				plugin.resetPlayer(target);
			    }
			    if (!target.performCommand("spawn")) {
				target.teleport(ASkyBlock.getIslandWorld().getSpawnLocation());
			    } 
			} else {
			    // Offline
			    // Tell offline player they were kicked
			    plugin.setMessage(targetPlayer, ChatColor.RED + Locale.kicknameRemovedYou.replace("[name]", player.getName()));
			}
			// Remove any warps
			plugin.removeWarp(targetPlayer);
			// Tell leader they removed the player
			sender.sendMessage(ChatColor.RED + Locale.kicknameRemoved.replace("[name]", split[1]));
			removePlayerFromTeam(targetPlayer, teamLeader);
			teamMembers.remove(targetPlayer);
			if (teamMembers.size() < 2) {
			    removePlayerFromTeam(player.getUniqueId(), teamLeader);
			}				
		    } else {
			plugin.getLogger().warning("Player " + player.getName() + " failed to remove " + plugin.getPlayers().getName(targetPlayer));
			player.sendMessage(ChatColor.RED + Locale.kickerrorNotPartOfTeam);
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("makeleader")) {
		if (VaultHelper.checkPerm(player, "askyblock.team.makeleader")) {
		    targetPlayer = plugin.getPlayers().getUUID(split[1]);
		    if (targetPlayer == null) {
			player.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    }
		    if (!plugin.getPlayers().inTeam(player.getUniqueId())) {
			player.sendMessage(ChatColor.RED + Locale.makeLeadererrorYouMustBeInTeam);
			return true;
		    }

		    if (plugin.getPlayers().getMembers(player.getUniqueId()).size() > 2) {
			player.sendMessage(ChatColor.RED + Locale.makeLeadererrorRemoveAllPlayersFirst);
			plugin.getLogger().info(player.getName() + " tried to transfer his island, but failed because >2 people in a team");
			return true;
		    }

		    if (plugin.getPlayers().inTeam(player.getUniqueId())) {
			if (teamLeader.equals(player.getUniqueId())) {
			    if (teamMembers.contains(targetPlayer)) {

				// Check if online
				if (plugin.getServer().getPlayer(targetPlayer) != null) {
				    plugin.getServer().getPlayer(targetPlayer).sendMessage(ChatColor.GREEN + Locale.makeLeaderyouAreNowTheOwner);
				} else {
				    plugin.setMessage(targetPlayer, Locale.makeLeaderyouAreNowTheOwner);
				    //.makeLeadererrorPlayerMustBeOnline
				}
				player.sendMessage(ChatColor.GREEN + Locale.makeLeadernameIsNowTheOwner.replace("[name]", plugin.getPlayers().getName(targetPlayer)));
				// targetPlayer is the new leader
				// Remove the target player from the team
				removePlayerFromTeam(targetPlayer, teamLeader);
				// Remove the leader from the team
				removePlayerFromTeam(teamLeader, teamLeader);
				// Transfer the data from the old leader to the new one
				plugin.transferIsland(player.getUniqueId(), targetPlayer);
				// Create a new team with 			
				addPlayertoTeam(player.getUniqueId(), targetPlayer);
				addPlayertoTeam(targetPlayer, targetPlayer);
				return true;
			    }
			    player.sendMessage(ChatColor.RED + Locale.makeLeadererrorThatPlayerIsNotInTeam);
			} else {
			    player.sendMessage(ChatColor.RED + Locale.makeLeadererrorNotYourIsland);
			}
		    } else {
			player.sendMessage(ChatColor.RED + Locale.makeLeadererrorGeneralError);
		    }
		    return true;
		}
	    } else {
		return false;
	    }
	}
	return false;
    }

    /**
     * Set time out for island restarting
     * @param player
     * @return
     */
    public boolean onRestartWaitTime(final Player player) {
	if (resetWaitTime.containsKey(player.getUniqueId())) {
	    if (resetWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
		return true;
	    }

	    return false;
	}

	return false;
    }
    /**
     * Sets a timeout for player into the Hashmap resetWaitTime
     * 
     * @param player
     */
    public void setResetWaitTime(final Player player) {
	resetWaitTime.put(player.getUniqueId(), Long.valueOf(Calendar.getInstance().getTimeInMillis() + Settings.resetWait * 1000));
    }

    /**
     * Returns how long the player must wait until they can restart their island in seconds
     * 
     * @param player
     * @return
     */
    public long getResetWaitTime(final Player player) {
	if (resetWaitTime.containsKey(player.getUniqueId())) {
	    if (resetWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
		return (resetWaitTime.get(player.getUniqueId()).longValue() - Calendar.getInstance().getTimeInMillis())/1000;
	    }

	    return 0L;
	}

	return 0L;
    }



}