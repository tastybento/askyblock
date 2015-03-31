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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.scheduler.BukkitTask;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.CoopPlay;
import com.wasteofplastic.askyblock.DeleteIslandChunk;
import com.wasteofplastic.askyblock.GridManager;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.LevelCalc;
import com.wasteofplastic.askyblock.Messages;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.TopTen;
import com.wasteofplastic.askyblock.WarpSigns;
import com.wasteofplastic.askyblock.listeners.IslandGuard;
import com.wasteofplastic.askyblock.panels.Biomes;
import com.wasteofplastic.askyblock.panels.ControlPanel;
import com.wasteofplastic.askyblock.panels.SettingsPanel;
import com.wasteofplastic.askyblock.schematics.Schematic;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class IslandCmd implements CommandExecutor {
    public boolean levelCalcFreeFlag = true;
    // private Schematic island = null;
    private HashMap<String, Schematic> schematics = new HashMap<String, Schematic>();
    // private Location Islandlocation;
    private ASkyBlock plugin;
    // The island reset confirmation
    private HashMap<UUID, Boolean> confirm = new HashMap<UUID, Boolean>();
    // Last island
    Location last = null;
    /**
     * Invite list - invited player name string (key), inviter name string
     * (value)
     */
    private final HashMap<UUID, UUID> inviteList = new HashMap<UUID, UUID>();
    // private PlayerCache players;
    // The time a player has to wait until they can reset their island again
    private HashMap<UUID, Long> resetWaitTime = new HashMap<UUID, Long>();
    // Level calc cool down
    private HashMap<UUID, Long> levelWaitTime = new HashMap<UUID, Long>();

    // Level calc checker
    BukkitTask checker = null;

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
	// Location loc = getNextIsland();
	// plugin.getLogger().info("Next free island spot is at " +
	// loc.getBlockX() + "," + loc.getBlockZ());
	// Check if there is a schematic
	File schematicFile = new File(plugin.getDataFolder(), "island.schematic");
	if (!schematicFile.exists()) {
	    // Load in the ASkyBlock one if required
	    if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
		plugin.saveResource("island.schematic", false);
		schematicFile = new File(plugin.getDataFolder(), "island.schematic");
	    }
	}
	if (schematicFile.exists()) {
	    plugin.getLogger().info("Trying to load island schematic...");
	    schematics.put("", new Schematic(schematicFile));
	    // island = Schematic.loadSchematic(schematicFile);
	}
	// Now add any permission-based schematics
	if (!Settings.schematics.isEmpty()) {
	    for (String perm : Settings.schematics.keySet()) {
		schematicFile = new File(plugin.getDataFolder(), Settings.schematics.get(perm));
		if (schematicFile.exists()) {
		    schematics.put(perm, new Schematic(schematicFile));
		    // island = Schematic.loadSchematic(schematicFile);
		} else {
		    plugin.getLogger().severe("Schematic file '" + Settings.schematics.get(perm) + "' does not exist!");
		}
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
	 * if (!plugin.getServer().getPlayer(playerUUID).isOnline() ||
	 * !plugin.getServer().getPlayer(teamLeader).isOnline()) {
	 * plugin.getLogger().info(
	 * "Can only add player to a team if both player and leader are online."
	 * );
	 * return false;
	 * }
	 */
	// plugin.getLogger().info("Adding player: " + playerUUID +
	// " to team with leader: " + teamLeader);
	// plugin.getLogger().info("The team island location is: " +
	// plugin.getPlayers().getIslandLocation(teamLeader));
	// plugin.getLogger().info("The leader's home location is: " +
	// plugin.getPlayers().getHomeLocation(teamLeader) +
	// " (may be different or null)");
	// Set the player's team giving the team leader's name and the team's
	// island
	// location
	plugin.getPlayers().setJoinTeam(playerUUID, teamLeader, plugin.getPlayers().getIslandLocation(teamLeader));
	// If the player's name and the team leader are NOT the same when this
	// method is called then set the player's home location to the leader's
	// home location
	// if it exists, and if not set to the island location
	if (!playerUUID.equals(teamLeader)) {
	    // Clear any old home locations
	    plugin.getPlayers().clearHomeLocations(playerUUID);
	    if (plugin.getPlayers().getHomeLocation(teamLeader,1) != null) {
		plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getHomeLocation(teamLeader));
		// plugin.getLogger().info("DEBUG: Setting player's home to the leader's home location");
	    } else {
		plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getIslandLocation(teamLeader));
		// plugin.getLogger().info("DEBUG: Setting player's home to the team island location");
	    }
	    // If the leader's member list does not contain player then add it
	    if (!plugin.getPlayers().getMembers(teamLeader).contains(playerUUID)) {
		plugin.getPlayers().addTeamMember(teamLeader, playerUUID);
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
	plugin.getPlayers().removeMember(teamleader, player);
	// If player is online
	// If player is not the leader of their own team
	if (!player.equals(teamleader)) {
	    plugin.getPlayers().setLeaveTeam(player);
	    plugin.getPlayers().setHomeLocation(player, null);
	    plugin.getPlayers().setIslandLocation(player, null);
	    plugin.getPlayers().setTeamIslandLocation(player, null);
	    runCommands(Settings.leaveCommands, player);
	} else {
	    // Ex-Leaders keeps their island, but the rest of the team items are
	    // removed
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
	// final Players p = plugin.getPlayers().get(player.getName());
	Location next = getNextIsland();
	// plugin.getLogger().info("DEBUG: next location is " +
	// next.toString());
	plugin.setNewIsland(true);
	Location cowSpot = generateIslandBlocks(next.getBlockX(), next.getBlockZ(), player, ASkyBlock.getIslandWorld());
	plugin.setNewIsland(false);
	// plugin.getLogger().info("DEBUG: player ID is: " +
	// playerUUID.toString());
	plugin.getPlayers().setHasIsland(playerUUID, true);
	// plugin.getLogger().info("DEBUG: Set island to true - actually is " +
	// plugin.getPlayers().hasIsland(playerUUID));

	plugin.getPlayers().setIslandLocation(playerUUID, next);
	// Add to the grid
	plugin.getGrid().addIsland(next.getBlockX(), next.getBlockZ(), playerUUID);
	// plugin.getLogger().info("DEBUG: player island location is " +
	// plugin.getPlayers().getIslandLocation(playerUUID).toString());
	// Teleport the player to a safe place
	// plugin.getGrid().homeTeleport(player);
	plugin.getPlayers().save(playerUUID);
	// Remove any mobs if they just so happen to be around in the
	// vicinity
	/*
	 * final Iterator<Entity> ents = player.getNearbyEntities(50.0D, 250.0D,
	 * 50.0D).iterator();
	 * int numberOfCows = 0;
	 * while (ents.hasNext()) {
	 * final Entity tempent = ents.next();
	 * // Remove anything except for the player himself and the cow (!)
	 * if (!(tempent instanceof Player) &&
	 * !tempent.getType().equals(EntityType.COW)) {
	 * plugin.getLogger().warning("Removed an " +
	 * tempent.getType().toString() + " when creating island for " +
	 * player.getName());
	 * tempent.remove();
	 * } else if (tempent.getType().equals(EntityType.COW)) {
	 * numberOfCows++;
	 * if (numberOfCows > 1) {
	 * plugin.getLogger().warning(
	 * "Removed an extra cow when creating island for " + player.getName());
	 * tempent.remove();
	 * }
	 * }
	 * }
	 */
	// Done
	return cowSpot;
    }

    /**
     * Get the location of next free island spot
     * 
     * @return Location of island spot
     */
    private Location getNextIsland() {
	// Find the next free spot
	if (last == null) {
	    last = new Location(ASkyBlock.getIslandWorld(), Settings.islandXOffset + Settings.islandStartX, Settings.island_level, Settings.islandZOffset + Settings.islandStartZ);
	}
	Location next = last.clone();

	while (plugin.getGrid().islandAtLocation(next)) {
	    next = nextGridLocation(next);
	}
	// Make the last next, last
	last = next.clone();
	return next;
    }

    /*
     * // Find the next free spot
     * if (last == null) {
     * last = new Location(ASkyBlock.getIslandWorld(), Settings.islandXOffset,
     * Settings.island_level, Settings.islandZOffset);
     * }
     * Location next = last.clone();
     * int x = next.getBlockX();
     * int y = next.getBlockY();
     * int z = next.getBlockZ();
     * // Check all 4 corners of the island
     * Location one = new Location(ASkyBlock.getIslandWorld(),x -
     * Settings.islandDistance/2,y,z - Settings.islandDistance/2);
     * Location two = new Location(ASkyBlock.getIslandWorld(),x +
     * Settings.islandDistance/2,y,z - Settings.islandDistance/2);
     * Location three = new Location(ASkyBlock.getIslandWorld(),x -
     * Settings.islandDistance/2,y,z + Settings.islandDistance/2);
     * Location four = new Location(ASkyBlock.getIslandWorld(),x +
     * Settings.islandDistance/2,y,z + Settings.islandDistance/2);
     * plugin.getLogger().info("DEBUG 1=" + one + " 2 = " + two
     * + " 3 = "+ three + " 4 = " + four);
     * plugin.getLogger().info("DEBUG 1=" + plugin.islandAtLocation(one) +
     * " 2 = " + plugin.islandAtLocation(two)
     * + " 3 = "+ plugin.islandAtLocation(three) + " 4 = " +
     * plugin.islandAtLocation(four));
     * //plugin.getLogger().info("next island starting point " +
     * last.toString());
     * while (plugin.islandAtLocation(next) || plugin.islandAtLocation(one) ||
     * plugin.islandAtLocation(two)
     * || plugin.islandAtLocation(three) || plugin.islandAtLocation(four)) {
     * next = nextGridLocation(next);
     * x = next.getBlockX();
     * y = next.getBlockY();
     * z = next.getBlockZ();
     * // Check all 4 corners of the island
     * one = new Location(ASkyBlock.getIslandWorld(),x -
     * Settings.islandDistance/2,y,z - Settings.islandDistance/2);
     * two = new Location(ASkyBlock.getIslandWorld(),x +
     * Settings.islandDistance/2,y,z - Settings.islandDistance/2);
     * three = new Location(ASkyBlock.getIslandWorld(),x -
     * Settings.islandDistance/2,y,z + Settings.islandDistance/2);
     * four = new Location(ASkyBlock.getIslandWorld(),x +
     * Settings.islandDistance/2,y,z + Settings.islandDistance/2);
     * plugin.getLogger().info("DEBUG 1=" + one + " 2 = " + two
     * + " 3 = "+ three + " 4 = " + four);
     * plugin.getLogger().info("DEBUG 1=" + plugin.islandAtLocation(one) +
     * " 2 = " + plugin.islandAtLocation(two)
     * + " 3 = "+ plugin.islandAtLocation(three) + " 4 = " +
     * plugin.islandAtLocation(four));
     * }
     * // Make the last next, last
     * last = next.clone();
     * return next;
     * }
     */

    private void resetMoney(Player player) {
	if (!Settings.useEconomy) {
	    return;
	}
	// Set player's balance in acid island to the starting balance
	try {
	    // plugin.getLogger().info("DEBUG: " + player.getName() + " " +
	    // Settings.general_worldName);
	    if (VaultHelper.econ == null) {
		// plugin.getLogger().warning("DEBUG: econ is null!");
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
	    if (playerBalance != Settings.startingMoney) {
		if (playerBalance > Settings.startingMoney) {
		    Double difference = playerBalance - Settings.startingMoney;
		    EconomyResponse response = VaultHelper.econ.withdrawPlayer(player, Settings.worldName, difference);
		    // plugin.getLogger().info("DEBUG: withdrawn");
		    if (response.transactionSuccess()) {
			plugin.getLogger().info(
				"FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to "
					+ Settings.startingMoney);
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
				"FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to "
					+ Settings.startingMoney);
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
    @SuppressWarnings("deprecation")
    private Location generateIslandBlocks(final int x, final int z, final Player player, final World world) {
	Location cowSpot = null;
	Location islandLoc = new Location(world, x, Settings.island_level, z);
	// What happens next depends on whether this is AcidIsland or ASkyBlock
	// Check to see if a baseline schematic is loaded
	if (!schematics.isEmpty()) {
	    // This is the same for AcidIsland or ASkyblock
	    // Find out what level of island this player will get
	    String mySchematic = "";
	    for (String perm : schematics.keySet()) {
		if (VaultHelper.checkPerm(player, perm)) {
		    mySchematic = perm;
		}
	    }
	    // plugin.getLogger().info("DEBUG: size of schematics = " +
	    // schematics.size());
	    // Paste the schematic
	    cowSpot = schematics.get(mySchematic).pasteSchematic(world, islandLoc, player);
	    if (cowSpot == null) {
		islandLoc.getBlock().setType(Material.BEDROCK);
		plugin.getLogger().severe("Schematic loading error - cannot load " + mySchematic);
		player.sendMessage(ChatColor.RED + "There was a massive problem pasting the new island. Please tell an admin!");
		return islandLoc;
	    }
	    return cowSpot;
	} else {
	    // No schematic loaded
	    if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
		islandLoc.getBlock().setType(Material.BEDROCK);
		player.sendMessage(ChatColor.RED + "No schematic!");
		return islandLoc;
	    } else {
		// AcidIsland
		// Build island layer by layer
		// Start from the base
		// half sandstone; half sand
		int y = 0;
		for (int x_space = x - 4; x_space <= x + 4; x_space++) {
		    for (int z_space = z - 4; z_space <= z + 4; z_space++) {
			final Block b = world.getBlockAt(x_space, y, z_space);
			b.setType(Material.BEDROCK);
		    }
		}
		for (y = 1; y < Settings.island_level + 5; y++) {
		    for (int x_space = x - 4; x_space <= x + 4; x_space++) {
			for (int z_space = z - 4; z_space <= z + 4; z_space++) {
			    final Block b = world.getBlockAt(x_space, y, z_space);
			    if (y < (Settings.island_level / 2)) {
				b.setType(Material.SANDSTONE);
			    } else {
				b.setType(Material.SAND);
				b.setData((byte) 0);
			    }
			}
		    }
		}
		// Then cut off the corners to make it round-ish
		for (y = 0; y < Settings.island_level + 5; y++) {
		    for (int x_space = x - 4; x_space <= x + 4; x_space += 8) {
			for (int z_space = z - 4; z_space <= z + 4; z_space += 8) {
			    final Block b = world.getBlockAt(x_space, y, z_space);
			    b.setType(Material.STATIONARY_WATER);
			}
		    }
		}
		// Add some grass
		for (y = Settings.island_level + 4; y < Settings.island_level + 5; y++) {
		    for (int x_space = x - 2; x_space <= x + 2; x_space++) {
			for (int z_space = z - 2; z_space <= z + 2; z_space++) {
			    final Block blockToChange = world.getBlockAt(x_space, y, z_space);
			    blockToChange.setType(Material.GRASS);
			}
		    }
		}
		// Place bedrock - MUST be there (ensures island are not
		// overwritten
		Block b = world.getBlockAt(x, Settings.island_level, z);
		b.setType(Material.BEDROCK);
		// Then add some more dirt in the classic shape
		y = Settings.island_level + 3;
		for (int x_space = x - 2; x_space <= x + 2; x_space++) {
		    for (int z_space = z - 2; z_space <= z + 2; z_space++) {
			b = world.getBlockAt(x_space, y, z_space);
			b.setType(Material.DIRT);
		    }
		}
		b = world.getBlockAt(x - 3, y, z);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x + 3, y, z);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x, y, z - 3);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x, y, z + 3);
		b.setType(Material.DIRT);
		y = Settings.island_level + 2;
		for (int x_space = x - 1; x_space <= x + 1; x_space++) {
		    for (int z_space = z - 1; z_space <= z + 1; z_space++) {
			b = world.getBlockAt(x_space, y, z_space);
			b.setType(Material.DIRT);
		    }
		}
		b = world.getBlockAt(x - 2, y, z);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x + 2, y, z);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x, y, z - 2);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x, y, z + 2);
		b.setType(Material.DIRT);
		y = Settings.island_level + 1;
		b = world.getBlockAt(x - 1, y, z);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x + 1, y, z);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x, y, z - 1);
		b.setType(Material.DIRT);
		b = world.getBlockAt(x, y, z + 1);
		b.setType(Material.DIRT);

		// Add island items
		y = Settings.island_level;
		// Add tree (natural)
		final Location treeLoc = new Location(world, x, y + 5D, z);
		world.generateTree(treeLoc, TreeType.ACACIA);
		// Place the cow
		cowSpot = new Location(world, x, (Settings.island_level + 5), z - 2);

		// Place a helpful sign in front of player
		Block blockToChange = world.getBlockAt(x, Settings.island_level + 5, z + 3);
		blockToChange.setType(Material.SIGN_POST);
		Sign sign = (Sign) blockToChange.getState();
		sign.setLine(0, plugin.myLocale(player.getUniqueId()).signLine1.replace("[player]", player.getName()));
		sign.setLine(1, plugin.myLocale(player.getUniqueId()).signLine2.replace("[player]", player.getName()));
		sign.setLine(2, plugin.myLocale(player.getUniqueId()).signLine3.replace("[player]", player.getName()));
		sign.setLine(3, plugin.myLocale(player.getUniqueId()).signLine4.replace("[player]", player.getName()));
		((org.bukkit.material.Sign) sign.getData()).setFacingDirection(BlockFace.NORTH);
		sign.update();
		// Place the chest - no need to use the safe spawn function
		// because we
		// know what this island looks like
		blockToChange = world.getBlockAt(x, Settings.island_level + 5, z + 1);
		blockToChange.setType(Material.CHEST);
		// Only set if the config has items in it
		if (Settings.chestItems.length > 0) {
		    final Chest chest = (Chest) blockToChange.getState();
		    final Inventory inventory = chest.getInventory();
		    inventory.clear();
		    inventory.setContents(Settings.chestItems);
		    chest.update();
		}
		// Fill the chest and orient it correctly (1.8 faces it north!
		DirectionalContainer dc = (DirectionalContainer) blockToChange.getState().getData();
		dc.setFacingDirection(BlockFace.SOUTH);
		blockToChange.setData(dc.getData(), true);
		return cowSpot;
	    }
	}
    }

    /**
     * Finds the next free island spot based off the last known island Uses
     * island_distance setting from the config file Builds up in a grid fashion
     * 
     * @param lastIsland
     * @return
     */
    private Location nextGridLocation(final Location lastIsland) {
	// plugin.getLogger().info("DEBUG nextIslandLocation");
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
     * 
     * @param asker
     *            - Player object of player who is asking
     * @param targetPlayer
     *            - UUID of the player's island that is being requested
     * @return - true if successful.
     */
    public boolean calculateIslandLevel(final Player asker, final UUID targetPlayer) {
	if (plugin.isCalculatingLevel()) {
	    asker.sendMessage(ChatColor.RED + plugin.myLocale(asker.getUniqueId()).islanderrorLevelNotReady);
	    return false;
	}
	// This flag is true if the command can be used
	plugin.setCalculatingLevel(true);
	if (asker.getUniqueId().equals(targetPlayer) || asker.isOp()) {
	    if (!onLevelWaitTime(asker) || Settings.levelWait <= 0 || asker.isOp()) {
		asker.sendMessage(ChatColor.GREEN + plugin.myLocale(asker.getUniqueId()).levelCalculating);
		LevelCalc levelCalc = new LevelCalc(plugin, targetPlayer, asker);
		levelCalc.runTaskTimer(plugin, 0L, 5L);
		setLevelWaitTime(asker);
	    } else {
		asker.sendMessage(ChatColor.YELLOW + plugin.myLocale(asker.getUniqueId()).islandresetWait.replace("[time]", String.valueOf(getLevelWaitTime(asker))));
		plugin.setCalculatingLevel(false);
	    }
	} else {
	    asker.sendMessage(ChatColor.GREEN + plugin.myLocale(asker.getUniqueId()).islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
	    plugin.setCalculatingLevel(false);
	}

	return true;
    }

    /**
     * One-to-one relationship, you can return the first matched key
     * 
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
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player) sender;
	// Basic permissions check to even use /island
	if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.create")) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islanderrorYouDoNotHavePermission);
	    return true;
	}
	/*
	 * Grab data for this player - may be null or empty
	 * playerUUID is the unique ID of the player who issued the command
	 */
	final UUID playerUUID = player.getUniqueId();
	final UUID teamLeader = plugin.getPlayers().getTeamLeader(playerUUID);
	List<UUID> teamMembers = new ArrayList<UUID>();
	if (teamLeader != null) {
	    teamMembers = plugin.getPlayers().getMembers(teamLeader);
	}
	// The target player's UUID
	UUID targetPlayer = null;
	// Check if a player has an island or is in a team
	switch (split.length) {
	// /island command by itself
	case 0:
	    // New island
	    if (plugin.getPlayers().getIslandLocation(playerUUID) == null && !plugin.getPlayers().inTeam(playerUUID)) {
		// Create new island for player
		player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).islandnew);
		final Location cowSpot = newIsland(sender);
		plugin.getGrid().homeTeleport(player);
		plugin.resetPlayer(player);
		if (Settings.resetMoney) {
		    resetMoney(player);
		}
		plugin.getBiomes().setIslandBiome(plugin.getPlayers().getIslandLocation(playerUUID), Settings.defaultBiome);
		// plugin.getLogger().info("Spawning cow at " +
		// cowSpot.toString());
		if (Settings.islandCompanion != null) {
		    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
			    spawnCompanion(player, cowSpot);
			}
		    }, 40L);
		}
		setResetWaitTime(player);
		if (!plugin.myLocale(player.getUniqueId()).islandSubTitle.isEmpty()) {
		    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
			    "title " + player.getName() + " subtitle {text:\"" + plugin.myLocale(player.getUniqueId()).islandSubTitle + "\", color:blue}");
		}
		if (!plugin.myLocale(player.getUniqueId()).islandTitle.isEmpty()) {
		    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
			    "title " + player.getName() + " title {text:\"" + plugin.myLocale(player.getUniqueId()).islandTitle + "\", color:gold}");
		}
		if (!plugin.myLocale(player.getUniqueId()).islandDonate.isEmpty() && !plugin.myLocale(player.getUniqueId()).islandURL.isEmpty()) {
		    plugin.getServer().dispatchCommand(
			    plugin.getServer().getConsoleSender(),
			    "tellraw " + player.getName() + " {text:\"" + plugin.myLocale(player.getUniqueId()).islandDonate + "\",color:aqua" + ",clickEvent:{action:open_url,value:\""
				    + plugin.myLocale(player.getUniqueId()).islandURL + "\"}}");
		}
		return true;
	    } else {
		if (Settings.useControlPanel) {
		    player.performCommand(Settings.ISLANDCOMMAND + " cp");
		} else {
		    if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName) || Settings.allowTeleportWhenFalling
			    || !IslandGuard.isFalling(playerUUID) || (player.isOp() && !Settings.damageOps)) {
			// Teleport home
			plugin.getGrid().homeTeleport(player);
			if (Settings.islandRemoveMobs) {
			    plugin.getGrid().removeMobs(player.getLocation());
			}
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
		    }
		}
		return true;
	    }
	case 1:
	    if (split[0].equalsIgnoreCase("lang")) {
		player.sendMessage("/" + label + " lang <locale>");
		player.sendMessage("English");
		player.sendMessage("Français");
		player.sendMessage("Deutsch");
		player.sendMessage("Español");
		player.sendMessage("Italiano");
		player.sendMessage("한국의 / Korean");
		player.sendMessage("Polski");
		player.sendMessage("Brasil");
		player.sendMessage("中国 / Chinese");
		return true;
	    } else if (split[0].equalsIgnoreCase("settings")) {
		// Show what the plugin settings are
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lock")) {
		    player.openInventory(SettingsPanel.islandGuardPanel());
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("lock")) {
		if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lock")) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return true;
		}
		// plugin.getLogger().info("DEBUG: perms ok");
		// Find out which island they want to lock
		Island island = plugin.getGrid().getIsland(playerUUID);
		if (island == null) {
		    // plugin.getLogger().info("DEBUG: player has no island in grid");
		    // Player has no island in the grid
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
		    return true;
		} else {
		    if (!island.isLocked()) {
			player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).lockLocking);
			Messages.tellOfflineTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerLocked.replace("[name]", player.getDisplayName()));
			Messages.tellTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerLocked.replace("[name]", player.getDisplayName()));
			island.setLocked(true);
		    } else {
			player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).lockUnlocking);
			Messages.tellOfflineTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerUnlocked.replace("[name]", player.getDisplayName()));
			Messages.tellTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerUnlocked.replace("[name]", player.getDisplayName()));
			island.setLocked(false);
		    }
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("go")) {
		if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
		    // Player has no island
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoIsland);
		    return true;
		}
		// Teleport home
		plugin.getGrid().homeTeleport(player);
		if (Settings.islandRemoveMobs) {
		    plugin.getGrid().removeMobs(player.getLocation());
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("about")) {
		player.sendMessage("");
		player.sendMessage(ChatColor.GOLD + "(c) 2014 - 2015 by tastybento");
		player.sendMessage(ChatColor.GOLD + "This plugin is free software: you can redistribute");
		player.sendMessage(ChatColor.GOLD + "it and/or modify it under the terms of the GNU");
		player.sendMessage(ChatColor.GOLD + "General Public License as published by the Free");
		player.sendMessage(ChatColor.GOLD + "Software Foundation, either version 3 of the License,");
		player.sendMessage(ChatColor.GOLD + "or (at your option) any later version.");
		player.sendMessage(ChatColor.GOLD + "This plugin is distributed in the hope that it");
		player.sendMessage(ChatColor.GOLD + "will be useful, but WITHOUT ANY WARRANTY; without");
		player.sendMessage(ChatColor.GOLD + "even the implied warranty of MERCHANTABILITY or");
		player.sendMessage(ChatColor.GOLD + "FITNESS FOR A PARTICULAR PURPOSE.  See the");
		player.sendMessage(ChatColor.GOLD + "GNU General Public License for more details.");
		player.sendMessage(ChatColor.GOLD + "You should have received a copy of the GNU");
		player.sendMessage(ChatColor.GOLD + "General Public License along with this plugin.");
		player.sendMessage(ChatColor.GOLD + "If not, see <http://www.gnu.org/licenses/>.");
		player.sendMessage(ChatColor.GOLD + "Souce code is available on GitHub.");
		return true;
		// Spawn enderman
		// Enderman enderman = (Enderman)
		// player.getWorld().spawnEntity(player.getLocation().add(new
		// Vector(5,0,5)), EntityType.ENDERMAN);
		// enderman.setCustomName("TastyBento's Ghost");
		// enderman.setCarriedMaterial(new
		// MaterialData(Material.GRASS));
		/*
		 * final Hologram h = new Hologram(plugin, ChatColor.GOLD + "" +
		 * ChatColor.BOLD + "ASkyBlock", "(c)2014 TastyBento");
		 * h.show(player.getLocation());
		 * plugin.getServer().getScheduler().runTaskLater(plugin, new
		 * Runnable() {
		 * @Override
		 * public void run() {
		 * h.destroy();
		 * }}, 40L);
		 */

	    }

	    if (split[0].equalsIgnoreCase("controlpanel") || split[0].equalsIgnoreCase("cp")) {
		// if
		// (player.getWorld().getName().equalsIgnoreCase(Settings.worldName))
		// {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.controlpanel")) {
		    player.openInventory(ControlPanel.controlPanel.get(ControlPanel.getDefaultPanelName()));
		    return true;
		}
		// }
	    }

	    if (split[0].equalsIgnoreCase("minishop") || split[0].equalsIgnoreCase("ms")) {
		if (Settings.useEconomy) {
		    if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {

			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.minishop")) {
			    player.openInventory(ControlPanel.miniShop);
			    return true;
			}
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
			return true;
		    }
		}
	    }
	    // /island <command>
	    if (split[0].equalsIgnoreCase("warp")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.warp")) {
		    player.sendMessage(ChatColor.YELLOW + "/island warp <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpWarp);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("warps")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.warp")) {
		    // Step through warp table
		    Set<UUID> warpList = WarpSigns.listWarps();
		    if (warpList.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).warpserrorNoWarpsYet);
			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp") && plugin.getGrid().playerIsOnIsland(player)) {
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
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
			player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).warpswarpsAvailable + ": " + ChatColor.WHITE + wlist);
			if (!hasWarp && (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp"))) {
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
			}
			return true;
		    }
		}
	    } else if (split[0].equalsIgnoreCase("restart") || split[0].equalsIgnoreCase("reset")) {
		// Check this player has an island
		if (!plugin.getPlayers().hasIsland(playerUUID)) {
		    // No so just start an island
		    player.performCommand(Settings.ISLANDCOMMAND);
		    return true;
		}
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    if (!plugin.getPlayers().getTeamLeader(playerUUID).equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandresetOnlyOwner);
		    } else {
			player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).islandresetMustRemovePlayers);
		    }
		    return true;
		}
		// Check if the player has used up all their resets
		if (plugin.getPlayers().getResetsLeft(playerUUID) == 0) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandResetNoMore);
		    return true;
		}
		if (plugin.getPlayers().getResetsLeft(playerUUID) > 0) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).resetYouHave.replace("[number]", String.valueOf(plugin.getPlayers().getResetsLeft(playerUUID))));
		}
		if (!onRestartWaitTime(player) || Settings.resetWait == 0 || player.isOp()) {
		    // Kick off the confirmation
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandresetConfirm.replace("[seconds]", String.valueOf(Settings.resetConfirmWait)));
		    if (!confirm.containsKey(playerUUID) || !confirm.get(playerUUID)) {
			confirm.put(playerUUID, true);
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			    @Override
			    public void run() {
				confirm.put(playerUUID, false);
			    }
			}, (Settings.resetConfirmWait * 20));
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).islandresetWait.replace("[time]", String.valueOf(getResetWaitTime(player))));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("confirm")) {
		// This is where the actual reset is done
		if (confirm.containsKey(playerUUID) && confirm.get(playerUUID)) {
		    // Actually RESET the island
		    player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).islandresetPleaseWait);
		    plugin.getPlayers().setResetsLeft(playerUUID, plugin.getPlayers().getResetsLeft(playerUUID) - 1);
		    if (plugin.getPlayers().getResetsLeft(playerUUID) == 0) {
			player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).islandResetNoMore);
		    }
		    if (plugin.getPlayers().getResetsLeft(playerUUID) > 0) {
			player.sendMessage(ChatColor.YELLOW
				+ plugin.myLocale(player.getUniqueId()).resetYouHave.replace("[number]", String.valueOf(plugin.getPlayers().getResetsLeft(playerUUID))));
		    }
		    // Clear any coop inventories
		    // CoopPlay.getInstance().returnAllInventories(player);
		    // Remove any coop invitees and grab their stuff
		    CoopPlay.getInstance().clearMyInvitedCoops(player);
		    CoopPlay.getInstance().clearMyCoops(player);
		    // plugin.getLogger().info("DEBUG Reset command issued!");
		    final Location oldIsland = plugin.getPlayers().getIslandLocation(playerUUID);
		    // plugin.unregisterEvents();
		    final Location cowSpot = newIsland(sender);
		    plugin.getPlayers().setHomeLocation(playerUUID, null);
		    plugin.getGrid().homeTeleport(player);
		    plugin.resetPlayer(player);
		    if (Settings.resetMoney) {
			resetMoney(player);
		    }
		    plugin.getBiomes().setIslandBiome(plugin.getPlayers().getIslandLocation(playerUUID), Settings.defaultBiome);
		    if (Settings.islandCompanion != null) {
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			    @Override
			    public void run() {
				spawnCompanion(player, cowSpot);
			    }
			}, 40L);
		    }
		    setResetWaitTime(player);
		    WarpSigns.removeWarp(playerUUID);
		    if (oldIsland != null) {
			// Remove any coops
			CoopPlay.getInstance().clearAllIslandCoops(oldIsland);
			// Delete the island itself
			new DeleteIslandChunk(plugin, oldIsland);
			// Delete the new nether island too (if it exists)
			if (Settings.createNether && Settings.newNether) {
			    new DeleteIslandChunk(plugin, oldIsland.toVector().toLocation(ASkyBlock.getNetherWorld()));
			}
		    }
		    // plugin.restartEvents();
		    runCommands(Settings.resetCommands, player.getUniqueId());
		    return true;
		} else {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/island restart: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpRestart);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("sethome")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    plugin.getGrid().homeSet(player);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("help")) {
		player.sendMessage(ChatColor.GREEN + plugin.getName() + " " + plugin.getDescription().getVersion() + " help:");
		if (Settings.useControlPanel) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + ": " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpControlPanel);
		} else {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + ": " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpIsland);
		}
		if (Settings.maxHomes > 1 && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " go <1 - " + Settings.maxHomes + ">: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpTeleport);
		} else {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " go: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpTeleport);
		}
		if (plugin.getGrid() != null && plugin.getGrid().getSpawn() != null) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " spawn: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpSpawn);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.controlpanel")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " controlpanel or cp: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpControlPanel);
		}
		player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " restart: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpRestart);
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    if (Settings.maxHomes > 1) {
			player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " sethome <1 - " + Settings.maxHomes + ">: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpSetHome);
		    } else {
			player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " sethome: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpSetHome);
		    }
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.info")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " level: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpLevel);
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " level <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpLevelPlayer);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.topten")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " top: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpTop);
		}
		if (Settings.useEconomy && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.minishop")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " minishop or ms: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpMiniShop);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.warp")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " warps: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpWarps);
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " warp <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpWarp);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.create")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " team: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpTeam);
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " invite <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpInvite);
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " leave: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpLeave);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.kick")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " kick <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpKick);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.join")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " <accept/reject>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpAcceptReject);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.makeleader")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " makeleader <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpMakeLeader);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.biomes")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " biomes: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpBiome);
		}
		// if (!Settings.allowPvP) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.expel")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " expel <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpExpel);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "coop")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " coop: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpCoop);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lock")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " lock: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandHelpLock);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.settings")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " settings: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandHelpSettings);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.challenges")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + plugin.myLocale(player.getUniqueId()).islandHelpChallenges);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lang")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " lang <locale> - select language");
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("biomes")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.biomes")) {
		    // Only the team leader can do this
		    if (teamLeader != null && !teamLeader.equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).levelerrornotYourIsland);
			return true;
		    }
		    if (!plugin.getPlayers().hasIsland(playerUUID)) {
			// Player has no island
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
			return true;
		    }
		    if (!plugin.getGrid().playerIsOnIsland(player)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotOnIsland);
			return true;
		    }
		    // player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "[Biomes]");
		    Inventory inv = Biomes.getBiomePanel(player);
		    if (inv != null) {
			player.openInventory(inv);
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("spawn") && plugin.getGrid().getSpawn() != null) {
		// go to spawn
		// plugin.getLogger().info("Debug: getSpawn" +
		// plugin.getSpawn().toString() );
		// plugin.getLogger().info("Debug: getSpawn loc" +
		// plugin.getSpawn().getSpawnLoc().toString() );
		player.teleport(ASkyBlock.getIslandWorld().getSpawnLocation());
		/*
		 * player.sendBlockChange(plugin.getSpawn().getSpawnLoc()
		 * ,plugin.getSpawn().getSpawnLoc().getBlock().getType()
		 * ,plugin.getSpawn().getSpawnLoc().getBlock().getData());
		 * player.sendBlockChange(plugin.getSpawn().getSpawnLoc().getBlock
		 * ().getRelative(BlockFace.DOWN).getLocation()
		 * ,plugin.getSpawn().getSpawnLoc().getBlock().getRelative(BlockFace
		 * .DOWN).getType()
		 * ,plugin.getSpawn().getSpawnLoc().getBlock().getRelative(BlockFace
		 * .DOWN).getData());
		 */
		return true;
	    } else if (split[0].equalsIgnoreCase("top")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.topten")) {
		    TopTen.topTenShow(player);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("level")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.info")) {
		    if (plugin.getGrid().playerIsOnIsland(player)) {
			if (!plugin.getPlayers().inTeam(playerUUID) && !plugin.getPlayers().hasIsland(playerUUID)) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
			} else {
			    calculateIslandLevel(player, playerUUID);
			}
			return true;
		    }
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotOnIsland);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("invite")) {
		// Invite label with no name, i.e., /island invite - tells the
		// player how many more people they can invite
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.create")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "Use" + ChatColor.WHITE + " /" + label + " invite <playername> " + plugin.myLocale(player.getUniqueId()).helpColor
			    + plugin.myLocale(player.getUniqueId()).islandhelpInvite);
		    // If the player who is doing the inviting has a team
		    if (plugin.getPlayers().inTeam(playerUUID)) {
			// Check to see if the player is the leader
			if (teamLeader.equals(playerUUID)) {
			    // Check to see if the team is already full
			    int maxSize = Settings.maxTeamSize;
			    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip")) {
				maxSize = Settings.maxTeamSizeVIP;
			    }
			    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip2")) {
				maxSize = Settings.maxTeamSizeVIP2;
			    }
			    if (teamMembers.size() < maxSize) {
				player.sendMessage(ChatColor.GREEN
					+ plugin.myLocale(player.getUniqueId()).inviteyouCanInvite.replace("[number]", String.valueOf(maxSize - teamMembers.size())));
			    } else {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYourIslandIsFull);
			    }
			    return true;
			}

			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYouMustHaveIslandToInvite);
			return true;
		    }

		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("accept")) {
		// Accept an invite command
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.join")) {
		    // If player is not in a team but has been invited to join
		    // one
		    if (!plugin.getPlayers().inTeam(playerUUID) && inviteList.containsKey(playerUUID)) {
			// If the invitee has an island of their own
			if (plugin.getPlayers().hasIsland(playerUUID)) {
			    plugin.getLogger().info(player.getName() + "'s island will be deleted because they joined a party.");
			    plugin.deletePlayerIsland(playerUUID, true);
			    plugin.getLogger().info("Island deleted.");
			}
			// Add the player to the team
			addPlayertoTeam(playerUUID, inviteList.get(playerUUID));
			// If the leader who did the invite does not yet have a
			// team (leader is not in a team yet)
			if (!plugin.getPlayers().inTeam(inviteList.get(playerUUID))) {
			    // Add the leader to their own team
			    addPlayertoTeam(inviteList.get(playerUUID), inviteList.get(playerUUID));
			}
			setResetWaitTime(player);

			plugin.getGrid().homeTeleport(player);
			plugin.resetPlayer(player);
			player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).inviteyouHaveJoinedAnIsland);
			if (Bukkit.getPlayer(inviteList.get(playerUUID)) != null) {
			    Bukkit.getPlayer(inviteList.get(playerUUID)).sendMessage(
				    ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).invitehasJoinedYourIsland.replace("[name]", player.getName()));
			}
			// Remove the invite
			inviteList.remove(player.getUniqueId());
			return true;
		    }
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("reject")) {
		// Reject /island reject
		if (inviteList.containsKey(player.getUniqueId())) {
		    player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).rejectyouHaveRejectedInvitation);
		    // If the player is online still then tell them directly
		    // about the rejection
		    if (Bukkit.getPlayer(inviteList.get(player.getUniqueId())) != null) {
			Bukkit.getPlayer(inviteList.get(player.getUniqueId())).sendMessage(
				ChatColor.RED + plugin.myLocale(player.getUniqueId()).rejectnameHasRejectedInvite.replace("[name]", player.getName()));
		    }
		    // Remove this player from the global invite list
		    inviteList.remove(player.getUniqueId());
		} else {
		    // Someone typed /island reject and had not been invited
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).rejectyouHaveNotBeenInvited);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("leave")) {
		// Leave team command
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.join")) {
		    if (player.getWorld().getName().equalsIgnoreCase(ASkyBlock.getIslandWorld().getName())) {
			if (plugin.getPlayers().inTeam(playerUUID)) {
			    if (plugin.getPlayers().getTeamLeader(playerUUID).equals(playerUUID)) {
				player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).leaveerrorYouAreTheLeader);
				return true;
			    }
			    // Clear any coop inventories
			    // CoopPlay.getInstance().returnAllInventories(player);
			    // Remove any of the target's coop invitees and grab
			    // their stuff
			    CoopPlay.getInstance().clearMyInvitedCoops(player);
			    CoopPlay.getInstance().clearMyCoops(player);
			    plugin.resetPlayer(player);
			    if (!player.performCommand(Settings.SPAWNCOMMAND)) {
				player.teleport(player.getWorld().getSpawnLocation());
			    }
			    // Log the location that this player left so they
			    // cannot join again before the cool down ends
			    plugin.getPlayers().startInviteCoolDownTimer(playerUUID, plugin.getPlayers().getTeamIslandLocation(teamLeader));
			    // Remove from team
			    removePlayerFromTeam(playerUUID, teamLeader);
			    // Remove any warps
			    WarpSigns.removeWarp(playerUUID);
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).leaveyouHaveLeftTheIsland);
			    // Tell the leader if they are online
			    if (plugin.getServer().getPlayer(teamLeader) != null) {
				plugin.getServer().getPlayer(teamLeader)
				.sendMessage(ChatColor.RED + plugin.myLocale(teamLeader).leavenameHasLeftYourIsland.replace("[name]", player.getName()));
			    } else {
				// Leave them a message
				Messages.setMessage(teamLeader, ChatColor.RED + plugin.myLocale(teamLeader).leavenameHasLeftYourIsland.replace("[name]", player.getName()));
			    }
			    // Check if the size of the team is now 1
			    // teamMembers.remove(playerUUID);
			    if (teamMembers.size() < 2) {
				// plugin.getLogger().info("DEBUG: Party is less than 2 - removing leader from team");
				removePlayerFromTeam(teamLeader, teamLeader);
			    }
			    return true;
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).leaveerrorYouCannotLeaveIsland);
			    return true;
			}
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).leaveerrorYouMustBeInWorld);
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("team")) {
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    if (teamLeader.equals(playerUUID)) {
			int maxSize = Settings.maxTeamSize;
			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip")) {
			    maxSize = Settings.maxTeamSizeVIP;
			}
			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip2")) {
			    maxSize = Settings.maxTeamSizeVIP2;
			}
			if (teamMembers.size() < maxSize) {
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).inviteyouCanInvite.replace("[number]", String.valueOf(maxSize - teamMembers.size())));
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYourIslandIsFull);
			}
		    }

		    player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).teamlistingMembers + ":");
		    // Display members in the list
		    for (UUID m : plugin.getPlayers().getMembers(teamLeader)) {
			player.sendMessage(ChatColor.WHITE + plugin.getPlayers().getName(m));
		    }
		} else if (inviteList.containsKey(playerUUID)) {
		    player.sendMessage(ChatColor.YELLOW
			    + plugin.myLocale(player.getUniqueId()).invitenameHasInvitedYou.replace("[name]", plugin.getPlayers().getName(inviteList.get(playerUUID))));
		    player.sendMessage(ChatColor.WHITE + "/" + label + " [accept/reject]" + ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).invitetoAcceptOrReject);
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).kickerrorNoTeam);
		}
		return true;
	    } else {
		// Incorrect syntax
		return false;
	    }
	    /*
	     * Commands that have two parameters
	     */
	case 2:
	    if (split[0].equalsIgnoreCase("lang")) {
		if (split[1].equalsIgnoreCase("english")) {
		    plugin.getPlayers().setLocale(playerUUID, "en-US"); 
		} else if (split[1].equalsIgnoreCase("Français") || split[1].equalsIgnoreCase("Francais")) {
		    plugin.getPlayers().setLocale(playerUUID, "fr-FR"); 
		} else if (split[1].equalsIgnoreCase("Deutsch")) {
		    plugin.getPlayers().setLocale(playerUUID, "de-DE");  
		} else if (split[1].equalsIgnoreCase("español") || split[1].equalsIgnoreCase("espanol")) {
		    plugin.getPlayers().setLocale(playerUUID, "es-ES");  
		} else if (split[1].equalsIgnoreCase("italiano")) {
		    plugin.getPlayers().setLocale(playerUUID, "it-IT");  
		} else if (split[1].equalsIgnoreCase("Korean") || split[1].equalsIgnoreCase("한국의")) {
		    plugin.getPlayers().setLocale(playerUUID, "ko-KR");  
		} else if (split[1].equalsIgnoreCase("polski")) {
		    plugin.getPlayers().setLocale(playerUUID, "pl-PL");  
		} else if (split[1].equalsIgnoreCase("Brasil")) {
		    plugin.getPlayers().setLocale(playerUUID, "pt-BR");  
		} else if (split[1].equalsIgnoreCase("Chinese") || split[1].equalsIgnoreCase("中国")) {
		    plugin.getPlayers().setLocale(playerUUID, "zh-CN");  
		} else {
		    // Typed it in wrong
		    player.sendMessage("/" + label + " lang <locale>");
		    player.sendMessage("English");
		    player.sendMessage("Français");
		    player.sendMessage("Deutsch");
		    player.sendMessage("Español");
		    player.sendMessage("Italiano");
		    player.sendMessage("한국의 / Korean");
		    player.sendMessage("Polski");
		    player.sendMessage("Brasil");
		    player.sendMessage("中国 / Chinese");
		    return true;
		}
		player.sendMessage("OK!");
		return true;
	    }
	    // Multi home
	    if (split[0].equalsIgnoreCase("go")) {
		if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
		    // Player has no island
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
		    return true;
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    int number = 1;
		    try {
			number = Integer.valueOf(split[1]);
			if (number < 1) {
			    plugin.getGrid().homeTeleport(player,1);
			}
			if (number > Settings.maxHomes) {
			    if (Settings.maxHomes > 1) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNumHomes.replace("[max]",String.valueOf(Settings.maxHomes)));
			    } else {
				plugin.getGrid().homeTeleport(player,1);
			    }
			} else {
			    // Teleport home
			    plugin.getGrid().homeTeleport(player,number);
			}
		    } catch (Exception e) {
			// Teleport home
			plugin.getGrid().homeTeleport(player,1);
		    }
		    if (Settings.islandRemoveMobs) {
			plugin.getGrid().removeMobs(player.getLocation());
		    }
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission); 
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("sethome")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    if (Settings.maxHomes > 1) {
			// Check the number given is a number
			int number = 0;
			try {
			    number = Integer.valueOf(split[1]);
			    if (number < 0 || number > Settings.maxHomes) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNumHomes.replace("[max]",String.valueOf(Settings.maxHomes)));
			    } else {
				plugin.getGrid().homeSet(player, number);
			    }
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNumHomes.replace("[max]",String.valueOf(Settings.maxHomes)));
			}
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    }
		    return true;
		}
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		return true;
	    } else if (split[0].equalsIgnoreCase("warp")) {
		// Warp somewhere command
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.warp")) {
		    final Set<UUID> warpList = WarpSigns.listWarps();
		    if (warpList.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).warpserrorNoWarpsYet);
			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp")) {
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
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
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorDoesNotExist);
			    return true;
			} else {
			    // Warp exists!
			    final Location warpSpot = WarpSigns.getWarp(foundWarp);
			    // Check if the warp spot is safe
			    if (warpSpot == null) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNotReadyYet);
				plugin.getLogger().warning("Null warp found, owned by " + plugin.getPlayers().getName(foundWarp));
				return true;
			    }
			    // Find out which direction the warp is facing
			    Block b = warpSpot.getBlock();
			    if (b.getType().equals(Material.SIGN_POST)) {
				Sign sign = (Sign) b.getState();
				org.bukkit.material.Sign s = (org.bukkit.material.Sign) sign.getData();
				BlockFace directionFacing = s.getFacing();
				Location inFront = b.getRelative(directionFacing).getLocation();
				if ((GridManager.isSafeLocation(inFront))) {
				    // convert blockface to angle
				    float yaw = Util.blockFaceToFloat(directionFacing);
				    final Location actualWarp = new Location(inFront.getWorld(), inFront.getBlockX() + 0.5D, inFront.getBlockY(),
					    inFront.getBlockZ() + 0.5D, yaw, 30F);
				    player.teleport(actualWarp);
				    player.getWorld().playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1F, 1F);
				    Player warpOwner = plugin.getServer().getPlayer(foundWarp);
				    if (warpOwner != null) {
					warpOwner.sendMessage(plugin.myLocale(foundWarp).warpsPlayerWarped.replace("[name]", player.getDisplayName()));
				    }
				    return true;
				}
			    } else {
				// Warp has been removed
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorDoesNotExist);
				WarpSigns.removeWarp(warpSpot);
				return true;
			    }
			    if (!(GridManager.isSafeLocation(warpSpot))) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNotSafe);
				plugin.getLogger().warning(
					"Unsafe warp found at " + warpSpot.toString() + " owned by " + plugin.getPlayers().getName(foundWarp));
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
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return false;
		}
	    } else if (split[0].equalsIgnoreCase("level")) {
		// island level <name> command
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.info")) {
		    // Find out if the target has an island
		    final UUID targetPlayerUUID = plugin.getPlayers().getUUID(split[1]);
		    // Invited player must be known
		    if (targetPlayerUUID == null) {
			// plugin.getLogger().info("DEBUG: unknown player");
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
			return true;
		    }
		    // Check if this player has an island or not
		    if (plugin.getPlayers().hasIsland(targetPlayerUUID) || plugin.getPlayers().inTeam(targetPlayerUUID)) {
			calculateIslandLevel(player, targetPlayerUUID);
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIslandOther);
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("invite")) {
		// Team invite a player command
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.create")) {
		    // May return null if not known
		    final UUID invitedPlayerUUID = plugin.getPlayers().getUUID(split[1]);
		    // Invited player must be known
		    if (invitedPlayerUUID == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
			return true;
		    }
		    // Player must be online
		    if (plugin.getServer().getPlayer(invitedPlayerUUID) == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorOfflinePlayer);
			return true;
		    }
		    // Player issuing the command must have an island
		    if (!plugin.getPlayers().hasIsland(player.getUniqueId())) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYouMustHaveIslandToInvite);
			return true;
		    }
		    // Player cannot invite themselves
		    if (player.getName().equalsIgnoreCase(split[1])) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYouCannotInviteYourself);
			return true;
		    }
		    // Check if this player can be invited to this island, or
		    // whether they are still on cooldown
		    long time = plugin.getPlayers().getInviteCoolDownTime(invitedPlayerUUID, plugin.getPlayers().getIslandLocation(playerUUID));
		    if (time > 0 && !player.isOp()) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorCoolDown.replace("[time]", String.valueOf(time)));
			return true;
		    }
		    // If the player already has a team then check that they are
		    // the leader, etc
		    if (plugin.getPlayers().inTeam(player.getUniqueId())) {
			// Leader?
			if (teamLeader.equals(player.getUniqueId())) {
			    // Invited player is free and not in a team
			    if (!plugin.getPlayers().inTeam(invitedPlayerUUID)) {
				// Player has space in their team
				int maxSize = Settings.maxTeamSize;
				if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip")) {
				    maxSize = Settings.maxTeamSizeVIP;
				}
				if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip2")) {
				    maxSize = Settings.maxTeamSizeVIP2;
				}
				if (teamMembers.size() < maxSize) {
				    // If that player already has an invite out
				    // then retract it.
				    // Players can only have one invite out at a
				    // time - interesting
				    if (inviteList.containsValue(playerUUID)) {
					inviteList.remove(getKeyByValue(inviteList, player.getUniqueId()));
					player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).inviteremovingInvite);
				    }
				    // Put the invited player (key) onto the
				    // list with inviter (value)
				    // If someone else has invited a player,
				    // then this invite will overwrite the
				    // previous invite!
				    inviteList.put(invitedPlayerUUID, player.getUniqueId());
				    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).inviteinviteSentTo.replace("[name]", split[1]));
				    // Send message to online player
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(plugin.myLocale(invitedPlayerUUID).invitenameHasInvitedYou.replace("[name]", player.getName()));
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(
					    ChatColor.WHITE + "/" + label + " [accept/reject]" + ChatColor.YELLOW + " " + plugin.myLocale(invitedPlayerUUID).invitetoAcceptOrReject);
				    if (plugin.getPlayers().hasIsland(invitedPlayerUUID)) {
					Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + plugin.myLocale(invitedPlayerUUID).invitewarningYouWillLoseIsland);
				    }
				} else {
				    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYourIslandIsFull);
				}
			    } else {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorThatPlayerIsAlreadyInATeam);
			    }
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYouMustHaveIslandToInvite);
			}
		    } else {
			// First-time invite player does not have a team
			// Check if invitee is in a team or not
			if (!plugin.getPlayers().inTeam(invitedPlayerUUID)) {
			    // If the inviter already has an invite out, remove
			    // it
			    if (inviteList.containsValue(playerUUID)) {
				inviteList.remove(getKeyByValue(inviteList, player.getUniqueId()));
				player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).inviteremovingInvite);
			    }
			    // Place the player and invitee on the invite list
			    inviteList.put(invitedPlayerUUID, player.getUniqueId());

			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).inviteinviteSentTo.replace("[name]", split[1]));
			    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(plugin.myLocale(invitedPlayerUUID).invitenameHasInvitedYou.replace("[name]", player.getName()));
			    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(
				    ChatColor.WHITE + "/" + label + " [accept/reject]" + ChatColor.YELLOW + " " + plugin.myLocale(invitedPlayerUUID).invitetoAcceptOrReject);
			    // Check if the player has an island and warn
			    // accordingly
			    // plugin.getLogger().info("DEBUG: invited player = "
			    // + invitedPlayerUUID.toString());
			    if (plugin.getPlayers().hasIsland(invitedPlayerUUID)) {
				// plugin.getLogger().info("DEBUG: invited player has island");
				Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + plugin.myLocale(invitedPlayerUUID).invitewarningYouWillLoseIsland);
			    }
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorThatPlayerIsAlreadyInATeam);
			}
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return false;
		}
	    } else if (split[0].equalsIgnoreCase("coop")) {
		// Give a player coop privileges
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "coop")) {
		    // May return null if not known
		    final UUID invitedPlayerUUID = plugin.getPlayers().getUUID(split[1]);
		    // Invited player must be known
		    if (invitedPlayerUUID == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
			return true;
		    }
		    // Player must be online
		    Player newPlayer = plugin.getServer().getPlayer(invitedPlayerUUID);
		    if (newPlayer == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorOfflinePlayer);
			return true;
		    }
		    // Player issuing the command must have an island
		    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYouMustHaveIslandToInvite);
			return true;
		    }
		    // Player cannot invite themselves
		    if (player.getName().equalsIgnoreCase(split[1])) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).inviteerrorYouCannotInviteYourself);
			return true;
		    }
		    // If target player is already on the team ignore
		    if (plugin.getPlayers().getMembers(playerUUID).contains(invitedPlayerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).coopOnYourTeam);
			return true;
		    }
		    // Target has to have an island
		    if (!plugin.getPlayers().inTeam(invitedPlayerUUID)) {
			if (!plugin.getPlayers().hasIsland(invitedPlayerUUID)) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIslandOther);
			    return true;
			}
		    }
		    // Add target to coop list
		    CoopPlay.getInstance().addCoopPlayer(player, newPlayer);
		    // Tell everyone what happened
		    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).coopSuccess.replace("[name]", newPlayer.getDisplayName()));
		    newPlayer.sendMessage(ChatColor.GREEN + plugin.myLocale(newPlayer.getUniqueId()).coopMadeYouCoop.replace("[name]", player.getDisplayName()));
		    return true;

		}
	    } else if (split[0].equalsIgnoreCase("expel")) {
		if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.expel")) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return true;
		}
		// Find out who they want to expel
		final UUID targetPlayerUUID = plugin.getPlayers().getUUID(split[1]);
		// Player must be known
		if (targetPlayerUUID == null) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
		    return true;
		}
		// Target should not be themselves
		if (targetPlayerUUID.equals(playerUUID)) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).expelNotYourself);
		    return true;
		}
		// Target must be online
		Player target = plugin.getServer().getPlayer(targetPlayerUUID);
		if (target == null) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorOfflinePlayer);
		    return true;
		}
		// Target cannot be op
		if (target.isOp() || VaultHelper.checkPerm(target, Settings.PERMPREFIX + "mod.bypassprotect")) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).expelFail.replace("[name]", target.getDisplayName()));
		    return true;
		}
		/*
		 * // Find out if the target is in a coop area
		 * Location coopLocation =
		 * plugin.locationIsOnIsland(CoopPlay.getInstance
		 * ().getCoopIslands(target),target.getLocation());
		 * // Get the expeller's island
		 * Location expellersIsland = null;
		 * if (plugin.getPlayers().inTeam(player.getUniqueId())) {
		 * expellersIsland =
		 * plugin.getPlayers().getTeamIslandLocation(player
		 * .getUniqueId());
		 * } else {
		 * expellersIsland =
		 * plugin.getPlayers().getIslandLocation(player.getUniqueId());
		 * }
		 * // Return this island inventory to the owner
		 * CoopPlay.getInstance().returnInventory(target,
		 * expellersIsland);
		 * // Mark them as no longer on a coop island
		 * CoopPlay.getInstance().setOnCoopIsland(targetPlayerUUID,
		 * null);
		 * // Find out if this location the same as this player's island
		 * if (coopLocation != null &&
		 * coopLocation.equals(expellersIsland)) {
		 * // They were on the island so return their home inventory
		 * if (plugin.getPlayers().inTeam(targetPlayerUUID)) {
		 * InventorySave.getInstance().loadPlayerInventory(player,
		 * plugin.getPlayers().getTeamIslandLocation(targetPlayerUUID));
		 * } else {
		 * InventorySave.getInstance().loadPlayerInventory(player,
		 * plugin.getPlayers().getIslandLocation(targetPlayerUUID));
		 * }
		 * }
		 */
		// Remove them from the coop list
		boolean coop = CoopPlay.getInstance().removeCoopPlayer(player, target);
		if (coop) {
		    target.sendMessage(ChatColor.RED + plugin.myLocale(target.getUniqueId()).coopRemoved.replace("[name]", player.getDisplayName()));
		    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).coopRemoveSuccess.replace("[name]", target.getDisplayName()));
		}
		// See if target is on this player's island
		if (plugin.getGrid().isOnIsland(player, target)) {
		    // Check to see if this player has an island or is just
		    // helping out
		    if (plugin.getPlayers().inTeam(targetPlayerUUID) || plugin.getPlayers().hasIsland(targetPlayerUUID)) {
			plugin.getGrid().homeTeleport(target);
		    } else {
			// Just move target to spawn
			if (!target.performCommand(Settings.SPAWNCOMMAND)) {
			    target.teleport(player.getWorld().getSpawnLocation());
			    /*
			     * target.sendBlockChange(target.getWorld().
			     * getSpawnLocation()
			     * ,target.getWorld().getSpawnLocation().getBlock().
			     * getType()
			     * ,target.getWorld().getSpawnLocation().getBlock().
			     * getData());
			     */
			}
		    }
		    target.sendMessage(ChatColor.RED + plugin.myLocale(target.getUniqueId()).expelExpelled);
		    plugin.getLogger().info(player.getName() + " expelled " + target.getName() + " from their island.");
		    // Yes they are
		    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).expelSuccess.replace("[name]", target.getDisplayName()));
		} else if (!coop) {
		    // No they're not
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).expelNotOnIsland);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("kick") || split[0].equalsIgnoreCase("remove")) {
		// PlayerIsland remove command with a player name, or island kick
		// command
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.kick")) {
		    if (!plugin.getPlayers().inTeam(playerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).kickerrorNoTeam);
			return true;
		    }
		    // Only leaders can kick
		    if (teamLeader != null && !teamLeader.equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).kickerrorOnlyLeaderCan);
			return true;
		    }
		    // The main thing to do is check if the player name to kick
		    // is in the list of players in the team.
		    targetPlayer = null;
		    for (UUID member : teamMembers) {
			if (plugin.getPlayers().getName(member).equalsIgnoreCase(split[1])) {
			    targetPlayer = member;
			}
		    }
		    if (targetPlayer == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).kickerrorNotPartOfTeam);
			return true;
		    }
		    if (teamMembers.contains(targetPlayer)) {
			// If the player leader tries to kick or remove
			// themselves
			if (player.getUniqueId().equals(targetPlayer)) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).leaveerrorLeadersCannotLeave);
			    return true;
			}
			// If target is online
			Player target = plugin.getServer().getPlayer(targetPlayer);
			if (target != null) {
			    // plugin.getLogger().info("DEBUG: player is online");
			    target.sendMessage(ChatColor.RED + plugin.myLocale(targetPlayer).kicknameRemovedYou.replace("[name]", player.getName()));
			    // Log the location that this player left so they
			    // cannot join again before the cool down ends
			    plugin.getPlayers().startInviteCoolDownTimer(targetPlayer, plugin.getPlayers().getIslandLocation(playerUUID));
			    // Clear any coop inventories
			    // CoopPlay.getInstance().returnAllInventories(target);
			    // Remove any of the target's coop invitees and
			    // anyone they invited
			    CoopPlay.getInstance().clearMyInvitedCoops(target);
			    CoopPlay.getInstance().clearMyCoops(target);
			    // Clear the player out and throw their stuff at the
			    // leader
			    if (target.getWorld().getName().equalsIgnoreCase(ASkyBlock.getIslandWorld().getName())) {
				for (ItemStack i : target.getInventory().getContents()) {
				    if (i != null) {
					try {
					    player.getWorld().dropItemNaturally(player.getLocation(), i);
					} catch (Exception e) {
					}
				    }
				}
				for (ItemStack i : target.getEquipment().getArmorContents()) {
				    if (i != null) {
					try {
					    player.getWorld().dropItemNaturally(player.getLocation(), i);
					} catch (Exception e) {
					}
				    }
				}
				// plugin.resetPlayer(target); <- no good if
				// reset inventory is false
				// Clear their inventory and equipment and set
				// them as survival
				target.getInventory().clear(); // Javadocs are
				// wrong - this
				// does not
				// clear armor slots! So...
				// plugin.getLogger().info("DEBUG: Clearing kicked player's inventory");
				target.getInventory().setArmorContents(null);
				target.getInventory().setHelmet(null);
				target.getInventory().setChestplate(null);
				target.getInventory().setLeggings(null);
				target.getInventory().setBoots(null);
				target.getEquipment().clear();
				if (Settings.resetChallenges) {
				    // Reset the player's challenge status
				    plugin.getPlayers().resetAllChallenges(target.getUniqueId());
				}
				// Reset the island level
				plugin.getPlayers().setIslandLevel(target.getUniqueId(), 0);
				plugin.getPlayers().save(target.getUniqueId());
				TopTen.topTenAddEntry(playerUUID, 0);
				// Update the inventory
				target.updateInventory();
			    }
			    if (!target.performCommand(Settings.SPAWNCOMMAND)) {
				target.teleport(ASkyBlock.getIslandWorld().getSpawnLocation());
			    }
			} else {
			    // Offline
			    // plugin.getLogger().info("DEBUG: player is offline "
			    // + targetPlayer.toString());
			    // Tell offline player they were kicked
			    Messages.setMessage(targetPlayer, ChatColor.RED + plugin.myLocale(player.getUniqueId()).kicknameRemovedYou.replace("[name]", player.getName()));
			}
			// Remove any warps
			WarpSigns.removeWarp(targetPlayer);
			// Tell leader they removed the player
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).kicknameRemoved.replace("[name]", split[1]));
			removePlayerFromTeam(targetPlayer, teamLeader);
			teamMembers.remove(targetPlayer);
			if (teamMembers.size() < 2) {
			    removePlayerFromTeam(player.getUniqueId(), teamLeader);
			}
			plugin.getPlayers().save(targetPlayer);
		    } else {
			plugin.getLogger().warning("Player " + player.getName() + " failed to remove " + plugin.getPlayers().getName(targetPlayer));
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).kickerrorNotPartOfTeam);
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return false;
		}
	    } else if (split[0].equalsIgnoreCase("makeleader")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.makeleader")) {
		    targetPlayer = plugin.getPlayers().getUUID(split[1]);
		    if (targetPlayer == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
			return true;
		    }
		    if (targetPlayer.equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).makeLeadererrorGeneralError);
			return true;
		    }
		    if (!plugin.getPlayers().inTeam(player.getUniqueId())) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).makeLeadererrorYouMustBeInTeam);
			return true;
		    }

		    if (plugin.getPlayers().getMembers(player.getUniqueId()).size() > 2) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).makeLeadererrorRemoveAllPlayersFirst);
			plugin.getLogger().info(player.getName() + " tried to transfer his island, but failed because >2 people in a team");
			return true;
		    }

		    if (plugin.getPlayers().inTeam(player.getUniqueId())) {
			if (teamLeader.equals(player.getUniqueId())) {
			    if (teamMembers.contains(targetPlayer)) {

				// Check if online
				if (plugin.getServer().getPlayer(targetPlayer) != null) {
				    plugin.getServer().getPlayer(targetPlayer).sendMessage(ChatColor.GREEN + plugin.myLocale(targetPlayer).makeLeaderyouAreNowTheOwner);
				} else {
				    Messages.setMessage(targetPlayer, plugin.myLocale(player.getUniqueId()).makeLeaderyouAreNowTheOwner);
				    // .makeLeadererrorPlayerMustBeOnline
				}
				player.sendMessage(ChatColor.GREEN
					+ plugin.myLocale(player.getUniqueId()).makeLeadernameIsNowTheOwner.replace("[name]", plugin.getPlayers().getName(targetPlayer)));
				// targetPlayer is the new leader
				// plugin.getLogger().info("DEBUG: " +
				// plugin.getPlayers().getIslandLevel(teamLeader));
				// Remove the target player from the team
				removePlayerFromTeam(targetPlayer, teamLeader);
				// Remove the leader from the team
				removePlayerFromTeam(teamLeader, teamLeader);
				// plugin.getLogger().info("DEBUG: " +
				// plugin.getPlayers().getIslandLevel(teamLeader));
				// Transfer the data from the old leader to the
				// new one
				plugin.getGrid().transferIsland(player.getUniqueId(), targetPlayer);
				// Create a new team with
				addPlayertoTeam(player.getUniqueId(), targetPlayer);
				addPlayertoTeam(targetPlayer, targetPlayer);
				return true;
			    }
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).makeLeadererrorThatPlayerIsNotInTeam);
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).makeLeadererrorNotYourIsland);
			}
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).makeLeadererrorGeneralError);
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    return false;
		}
	    } else {
		return false;
	    }
	}
	return false;
    }

    /**
     * Spawns a companion for the player at the location given
     * @param player
     * @param cowSpot
     */
    protected void spawnCompanion(Player player, Location cowSpot) {
	// Older versions of the server require custom names to only apply to Living Entities
	LivingEntity companion = (LivingEntity) player.getWorld().spawnEntity(cowSpot, Settings.islandCompanion);  
	if (!Settings.companionNames.isEmpty()) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt(Settings.companionNames.size());
	    String name = Settings.companionNames.get(randomNum).replace("[player]", player.getDisplayName());
	    //plugin.getLogger().info("DEBUG: name is " + name);
	    companion.setCustomName(name);
	    companion.setCustomNameVisible(true);
	} 
    }

    /**
     * Runs commands when a player resets or leaves a team, etc.
     * 
     * @param commands
     * @param player
     */
    private void runCommands(List<String> commands, UUID player) {
	// Run any reset commands
	for (String cmd : commands) {
	    // Substitute in any references to player
	    try {
		if (!plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("[player]", plugin.getPlayers().getName(player)))) {
		    plugin.getLogger().severe("Problem executing island reset commands - skipping!");
		    plugin.getLogger().severe("Command was : " + cmd);
		}
	    } catch (Exception e) {
		plugin.getLogger().severe("Problem executing island reset commands - skipping!");
		plugin.getLogger().severe("Command was : " + cmd);
		plugin.getLogger().severe("Error was: " + e.getMessage());
		e.printStackTrace();
	    }
	}

    }

    /**
     * Set time out for island restarting
     * 
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

    public boolean onLevelWaitTime(final Player player) {
	if (levelWaitTime.containsKey(player.getUniqueId())) {
	    if (levelWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
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
    private void setResetWaitTime(final Player player) {
	resetWaitTime.put(player.getUniqueId(), Long.valueOf(Calendar.getInstance().getTimeInMillis() + Settings.resetWait * 1000));
    }

    /**
     * Sets cool down for the level command
     * 
     * @param player
     */
    private void setLevelWaitTime(final Player player) {
	levelWaitTime.put(player.getUniqueId(), Long.valueOf(Calendar.getInstance().getTimeInMillis() + Settings.levelWait * 1000));
    }

    /**
     * Returns how long the player must wait until they can restart their island
     * in seconds
     * 
     * @param player
     * @return
     */
    private long getResetWaitTime(final Player player) {
	if (resetWaitTime.containsKey(player.getUniqueId())) {
	    if (resetWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
		return (resetWaitTime.get(player.getUniqueId()).longValue() - Calendar.getInstance().getTimeInMillis()) / 1000;
	    }

	    return 0L;
	}

	return 0L;
    }

    private long getLevelWaitTime(final Player player) {
	if (levelWaitTime.containsKey(player.getUniqueId())) {
	    if (levelWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
		return (levelWaitTime.get(player.getUniqueId()).longValue() - Calendar.getInstance().getTimeInMillis()) / 1000;
	    }

	    return 0L;
	}

	return 0L;
    }

}