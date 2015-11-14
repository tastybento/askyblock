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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.CoopPlay;
import com.wasteofplastic.askyblock.DeleteIslandChunk;
import com.wasteofplastic.askyblock.GridManager;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.Flags;
import com.wasteofplastic.askyblock.LevelCalc;
import com.wasteofplastic.askyblock.LevelCalcByChunk;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.TopTen;
import com.wasteofplastic.askyblock.events.IslandJoinEvent;
import com.wasteofplastic.askyblock.events.IslandLeaveEvent;
import com.wasteofplastic.askyblock.events.IslandNewEvent;
import com.wasteofplastic.askyblock.events.IslandResetEvent;
import com.wasteofplastic.askyblock.listeners.PlayerEvents;
import com.wasteofplastic.askyblock.panels.ControlPanel;
import com.wasteofplastic.askyblock.schematics.Schematic;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class IslandCmd implements CommandExecutor, TabCompleter {
    public boolean levelCalcFreeFlag = true;
    private static HashMap<String, Schematic> schematics = new HashMap<String, Schematic>();
    private ASkyBlock plugin;
    // The island reset confirmation
    private HashMap<UUID, Boolean> confirm = new HashMap<UUID, Boolean>();
    // Last island
    Location last = null;
    // List of players in the middle of choosing an island schematic
    private Set<UUID> pendingNewIslandSelection = new HashSet<UUID>();
    private Set<UUID> resettingIsland = new HashSet<UUID>();
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
    // To choose an island randomly
    private final Random random = new Random();

    /**
     * Constructor
     * 
     * @param aSkyBlock
     * @param players
     */
    public IslandCmd(ASkyBlock aSkyBlock) {
	// Plugin instance
	this.plugin = aSkyBlock;
	// Load schematics
	loadSchematics();
    }

    /**
     * Loads schematics from the config.yml file. If the default
     * island is not included, it will be made up
     */
    public void loadSchematics() {
	// Check if there is a schematic folder and make it if it does not exist
	File schematicFolder = new File(plugin.getDataFolder(), "schematics");
	if (!schematicFolder.exists()) {
	    schematicFolder.mkdir();
	}
	// Clear the schematic list that is kept in memory
	schematics.clear();
	// Load the default schematic if it exists
	// Set up the default schematic
	File schematicFile = new File(schematicFolder, "island.schematic");
	File netherFile = new File(schematicFolder, "nether.schematic");
	if (!schematicFile.exists()) {
	    //plugin.getLogger().info("Default schematic does not exist...");
	    // Only copy if the default exists
	    if (plugin.getResource("schematics/island.schematic") != null) {
		plugin.getLogger().info("Default schematic does not exist, saving it...");
		plugin.saveResource("schematics/island.schematic", false);
		// Add it to schematics
		try {
		    schematics.put("default",new Schematic(plugin, schematicFile));
		} catch (IOException e) {
		    plugin.getLogger().severe("Could not load default schematic!");
		    e.printStackTrace();
		}
		// If this is repeated later due to the schematic config, fine, it will only add info
	    } else {
		// No islands.schematic in the jar, so just make the default using 
		// built-in island generation
		schematics.put("default",new Schematic(plugin));
	    }
	} else {
	    // It exists, so load it
	    try {
		schematics.put("default",new Schematic(plugin, schematicFile));
	    } catch (IOException e) {
		plugin.getLogger().severe("Could not load default schematic!");
		e.printStackTrace();
	    }
	}
	// Add the nether default too
	if (!netherFile.exists()) {
	    if (plugin.getResource("schematics/nether.schematic") != null) {
		plugin.saveResource("schematics/nether.schematic", false);

		// Add it to schematics
		try {
		    Schematic netherIsland = new Schematic(plugin, netherFile);
		    netherIsland.setVisible(false);
		    schematics.put("nether", netherIsland);
		} catch (IOException e) {
		    plugin.getLogger().severe("Could not load default nether schematic!");
		    e.printStackTrace();
		}  
	    } else {
		plugin.getLogger().severe("Could not find default nether schematic!");
	    }
	} else {
	    // It exists, so load it
	    try {
		Schematic netherIsland = new Schematic(plugin, netherFile);
		netherIsland.setVisible(false);
		schematics.put("nether", netherIsland);
	    } catch (IOException e) {
		plugin.getLogger().severe("Could not load default nether schematic!");
		e.printStackTrace();
	    }
	}
	// Set up some basic settings just in case the schematics section is missing
	if (schematics.containsKey("default")) {
	    schematics.get("default").setName("Island");
	    schematics.get("default").setDescription("");
	    schematics.get("default").setPartnerName("nether");
	    schematics.get("default").setBiome(Settings.defaultBiome);
	    schematics.get("default").setIcon(Material.GRASS);
	    if (Settings.chestItems.length == 0) {
		schematics.get("default").setUseDefaultChest(false);
	    }
	    schematics.get("default").setOrder(0);
	}
	if (schematics.containsKey("nether")) {
	    schematics.get("nether").setName("NetherBlock Island");
	    schematics.get("nether").setDescription("Nether Island");
	    schematics.get("nether").setPartnerName("default");
	    schematics.get("nether").setBiome(Biome.HELL);
	    schematics.get("nether").setIcon(Material.NETHERRACK);
	    schematics.get("nether").setVisible(false);
	    schematics.get("nether").setPasteEntities(true);
	    if (Settings.chestItems.length == 0) {
		schematics.get("nether").setUseDefaultChest(false);
	    }
	}

	// Load the schematics from config.yml
	ConfigurationSection schemSection = plugin.getConfig().getConfigurationSection("schematicsection");
	if (plugin.getConfig().contains("general.schematics")) {
	    tip();
	    // Load the schematics in this section 
	    int count = 1;
	    for (String perms: plugin.getConfig().getConfigurationSection("general.schematics").getKeys(true)) {
		// See if the file exists
		String fileName = plugin.getConfig().getString("general.schematics." + perms);
		File schem = new File(plugin.getDataFolder(), fileName);
		if (schem.exists()) {
		    plugin.getLogger().info("Loading schematic " + fileName + " for permission " + perms);
		    Schematic schematic;
		    try {
			schematic = new Schematic(plugin, schem);
			schematic.setPerm(perms);
			schematic.setHeading(perms);
			schematic.setName("#" + count++);
			if (!schematic.isVisible()) {
			    plugin.getLogger().info("Schematic " + fileName + " will not be shown on the GUI");  
			}
			schematics.put(perms, schematic);
		    } catch (IOException e) {
			plugin.getLogger().severe("Could not load schematic " + fileName + " due to error. Skipping...");
		    }
		} // Cannot declare a not-found because get keys gets some additional non-wanted strings
	    }
	} else if (plugin.getConfig().contains("schematicsection")) {
	    Settings.useSchematicPanel = schemSection.getBoolean("useschematicspanel", false);
	    Settings.chooseIslandRandomly = schemSection.getBoolean("chooseislandrandomly", false);
	    // Section exists, so go through the various sections
	    for (String key : schemSection.getConfigurationSection("schematics").getKeys(false)) {
		try {
		    Schematic newSchem = null;
		    // Check the file exists
		    //plugin.getLogger().info("DEBUG: schematics." + key + ".filename" );
		    String filename = schemSection.getString("schematics." + key + ".filename","");
		    if (!filename.isEmpty()) {
			//plugin.getLogger().info("DEBUG: filename = " + filename);
			// Check if this file exists or if it is in the jar
			schematicFile = new File(schematicFolder, filename);
			// See if the file exists
			if (schematicFile.exists()) {
			    newSchem = new Schematic(plugin, schematicFile);
			} else if (plugin.getResource("schematics/"+filename) != null) {
			    plugin.saveResource("schematics/"+filename, false);
			    newSchem = new Schematic(plugin, schematicFile);
			}
		    } else {
			//plugin.getLogger().info("DEBUG: filename is empty");
			if (key.equalsIgnoreCase("default")) {
			    //Øplugin.getLogger().info("DEBUG: key is default, so use this one");
			    newSchem = schematics.get("default");
			} else {
			    plugin.getLogger().severe("Schematic " + key + " does not have a filename. Skipping!");
			}
		    }
		    if (newSchem != null) {   
			// Set the heading
			newSchem.setHeading(key);
			// Order
			newSchem.setOrder(schemSection.getInt("schematics." + key + ".order", 0));
			// Load the rest of the settings
			// Icon
			try { 
			    Material icon;
			    String iconString = schemSection.getString("schematics." + key + ".icon","MAP").toUpperCase();
			    if (StringUtils.isNumeric(iconString)) {
				icon = Material.getMaterial(Integer.parseInt(iconString));
			    } else {
				icon = Material.valueOf(iconString);
			    }
			    newSchem.setIcon(icon);
			} catch (Exception e) {
			    newSchem.setIcon(Material.MAP); 
			}
			// Friendly name
			String name = ChatColor.translateAlternateColorCodes('&', schemSection.getString("schematics." + key + ".name",""));
			newSchem.setName(name);
			// Rating - Rating is not used right now
			int rating = schemSection.getInt("schematics." + key + ".rating",50);
			if (rating <1) {
			    rating = 1;
			} else if (rating > 100) {
			    rating = 100;
			}
			newSchem.setRating(rating);
			// Description
			String description = ChatColor.translateAlternateColorCodes('&', schemSection.getString("schematics." + key + ".description",""));
			description = description.replace("[rating]",String.valueOf(rating));
			newSchem.setDescription(description);
			// Permission
			String perm = schemSection.getString("schematics." + key + ".permission","");
			newSchem.setPerm(perm);
			// Use default chest
			newSchem.setUseDefaultChest(schemSection.getBoolean("schematics." + key + ".useDefaultChest", true));
			// Biomes - overrides default if it exists
			String biomeString = schemSection.getString("schematics." + key + ".biome",Settings.defaultBiome.toString());
			Biome biome = null;
			try {
			    biome = Biome.valueOf(biomeString);
			    newSchem.setBiome(biome);
			} catch (Exception e) {
			    plugin.getLogger().severe("Could not parse biome " + biomeString + " using default instead.");
			}
			// Use physics - overrides default if it exists
			newSchem.setUsePhysics(schemSection.getBoolean("schematics." + key + ".usephysics",Settings.usePhysics));	    
			// Paste Entities or not
			newSchem.setPasteEntities(schemSection.getBoolean("schematics." + key + ".pasteentities",false));
			// Paste air or not. Default is false - huge performance savings!
			//newSchem.setPasteAir(schemSection.getBoolean("schematics." + key + ".pasteair",false));	    
			// Visible in GUI or not
			newSchem.setVisible(schemSection.getBoolean("schematics." + key + ".show",true));
			// Partner schematic
			if (biome != null && biome.equals(Biome.HELL)) {
			    // Default for nether biomes is the default overworld island
			    newSchem.setPartnerName(schemSection.getString("schematics." + key + ".partnerSchematic","default"));
			} else {
			    // Default for overworld biomes is nether island
			    newSchem.setPartnerName(schemSection.getString("schematics." + key + ".partnerSchematic","nether"));
			}
			// Island companion
			List<String> companion = schemSection.getStringList("schematics." + key + ".companion");
			List<EntityType> companionTypes = new ArrayList<EntityType>();
			if (!companion.isEmpty()) {
			    for (String companionType : companion) {
				companionType = companionType.toUpperCase();
				if (companionType.equalsIgnoreCase("NOTHING")) {
				    companionTypes.add(null);
				} else {
				    try {
					EntityType type = EntityType.valueOf(companionType);
					// Limit types
					switch (type) {
					case BAT:
					case CHICKEN:
					case COW:
					case HORSE:
					case IRON_GOLEM:
					case MUSHROOM_COW:
					case OCELOT:
					case PIG:
					case RABBIT:
					case SHEEP:
					case SNOWMAN:
					case VILLAGER:
					case WOLF:
					    companionTypes.add(type);
					    break;
					default:
					    plugin.getLogger()
					    .warning(
						    "Island companion is not recognized in schematic '" + name + "'. Pick from COW, PIG, SHEEP, CHICKEN, VILLAGER, HORSE, IRON_GOLEM, OCELOT, RABBIT, WOLF, SNOWMAN, BAT, MUSHROOM_COW");
					    break;
					}
				    } catch (Exception e) {
					plugin.getLogger()
					.warning(
						"Island companion is not recognized in schematic '" + name + "'. Pick from COW, PIG, SHEEP, CHICKEN, VILLAGER, HORSE, IRON_GOLEM, OCELOT, RABBIT, WOLF, BAT, MUSHROOM_COW, SNOWMAN");
				    }
				}
			    }
			    newSchem.setIslandCompanion(companionTypes);
			}
			// Companion names
			List<String> companionNames = schemSection.getStringList("schematics." + key + ".companionnames");
			if (!companionNames.isEmpty()) {
			    List<String> names = new ArrayList<String>();
			    for (String companionName : companionNames) {
				names.add(ChatColor.translateAlternateColorCodes('&', companionName));
			    }
			    newSchem.setCompanionNames(names);
			}
			// Get chest items
			final List<String> chestItems = schemSection.getStringList("schematics." + key + ".chestItems");
			if (!chestItems.isEmpty()) {
			    ItemStack[] tempChest = new ItemStack[chestItems.size()];
			    int i = 0;
			    for (String chestItemString : chestItems) {
				//plugin.getLogger().info("DEBUG: chest item = " + chestItemString);
				try {
				    String[] amountdata = chestItemString.split(":");
				    if (amountdata[0].equals("POTION")) {
					if (amountdata.length == 3) {
					    Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1]));
					    tempChest[i++] = chestPotion.toItemStack(Integer.parseInt(amountdata[2]));
					} else if (amountdata.length == 4) {
					    // Extended or splash potions
					    if (amountdata[2].equals("EXTENDED")) {
						Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).extend();
						tempChest[i++] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
					    } else if (amountdata[2].equals("SPLASH")) {
						Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).splash();
						tempChest[i++] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
					    } else if (amountdata[2].equals("EXTENDEDSPLASH")) {
						Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).extend().splash();
						tempChest[i++] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
					    }
					}
				    } else {
					Material mat;
					if (StringUtils.isNumeric(amountdata[0])) {
					    mat = Material.getMaterial(Integer.parseInt(amountdata[0]));
					} else {
					    mat = Material.getMaterial(amountdata[0].toUpperCase());
					}
					if (amountdata.length == 2) {
					    tempChest[i++] = new ItemStack(mat, Integer.parseInt(amountdata[1]));
					} else if (amountdata.length == 3) {
					    tempChest[i++] = new ItemStack(mat, Integer.parseInt(amountdata[2]), Short.parseShort(amountdata[1]));
					}
				    }
				} catch (java.lang.IllegalArgumentException ex) {
				    plugin.getLogger().severe("Problem loading chest item for schematic '" + name + "' so skipping it: " + chestItemString);
				    plugin.getLogger().severe("Error is : " + ex.getMessage());
				    plugin.getLogger().info("Potential potion types are: ");
				    for (PotionType c : PotionType.values())
					plugin.getLogger().info(c.name());
				} catch (Exception e) {
				    plugin.getLogger().severe("Problem loading chest item for schematic '" + name + "' so skipping it: " + chestItemString);
				    plugin.getLogger().info("Potential material types are: ");
				    for (Material c : Material.values())
					plugin.getLogger().info(c.name());
				    // e.printStackTrace();
				}
			    }

			    // Store it
			    newSchem.setDefaultChestItems(tempChest);
			}
			// Player spawn block
			String spawnBlock = schemSection.getString("schematics." + key + ".spawnblock");
			if (spawnBlock != null) {
			    // Check to see if this block is a valid material
			    try {
				Material playerSpawnBlock;
				if (StringUtils.isNumeric(spawnBlock)) {
				    playerSpawnBlock = Material.getMaterial(Integer.parseInt(spawnBlock));
				} else {
				    playerSpawnBlock = Material.valueOf(spawnBlock.toUpperCase());
				}
				if (newSchem.setPlayerSpawnBlock(playerSpawnBlock)) {
				    plugin.getLogger().info("Player will spawn at the " + playerSpawnBlock.toString());
				} else {
				    plugin.getLogger().severe("Problem with schematic '" + name + "'. Spawn block '" + spawnBlock + "' not found in schematic or there is more than one. Skipping...");
				}
			    } catch (Exception e) {
				plugin.getLogger().severe("Problem with schematic '" + name + "'. Spawn block '" + spawnBlock + "' is unknown. Skipping...");
			    }
			} else {
			    // plugin.getLogger().info("No spawn block found");
			}
			// Store it
			schematics.put(key, newSchem);
			if (perm.isEmpty()) {
			    perm = "all players";
			} else {
			    perm = "player with " + perm + " permission";
			}
			plugin.getLogger().info("Loading schematic " + name + " (" + filename + ") for " + perm + ", order " + newSchem.getOrder());
		    } else {
			plugin.getLogger().warning("Could not find " + filename + " in the schematics folder! Skipping...");
		    }
		} catch (IOException e) {
		    plugin.getLogger().info("Error loading schematic in section " + key + ". Skipping...");
		}
	    }
	    if (schematics.isEmpty()) {
		tip();
	    }
	} 
    }

    private void tip() {
	// There is no section in config.yml. Save the default schematic anyway
	plugin.getLogger().warning("***************************************************************");
	plugin.getLogger().warning("* 'schematics' section in config.yml has been deprecated.     *");
	plugin.getLogger().warning("* See 'schematicsection' in config.new.yml for replacement.   *");
	plugin.getLogger().warning("***************************************************************");
    }

    /**
     * Adds a player to a team. The player and the teamleader MAY be the same
     * 
     * @param playerUUID
     * @param teamLeader
     * @return
     */
    public boolean addPlayertoTeam(final UUID playerUUID, final UUID teamLeader) {
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
	    // Fire event
	    final Island island = plugin.getGrid().getIsland(teamLeader);
	    final IslandJoinEvent event = new IslandJoinEvent(playerUUID, island);
	    plugin.getServer().getPluginManager().callEvent(event);
	}
	return true;
    }

    /**
     * Removes a player from a team run by teamleader
     * 
     * @param playerUUID
     * @param teamLeader
     */
    public void removePlayerFromTeam(final UUID playerUUID, final UUID teamLeader) {
	// Remove player from the team
	plugin.getPlayers().removeMember(teamLeader, playerUUID);
	// If player is online
	// If player is not the leader of their own team
	if (!playerUUID.equals(teamLeader)) {
	    plugin.getPlayers().setLeaveTeam(playerUUID);
	    //plugin.getPlayers().setHomeLocation(player, null);
	    plugin.getPlayers().clearHomeLocations(playerUUID);
	    plugin.getPlayers().setIslandLocation(playerUUID, null);
	    plugin.getPlayers().setTeamIslandLocation(playerUUID, null);
	    OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerUUID);
	    if (offlinePlayer.isOnline()) {
		// Check perms
		if (!((Player)offlinePlayer).hasPermission(Settings.PERMPREFIX + "command.leaveexempt")) {
		    runCommands(Settings.leaveCommands, offlinePlayer);
		}
	    } else {
		// If offline, all commands are run, sorry
		runCommands(Settings.leaveCommands, offlinePlayer);
	    }
	    // Fire event
	    final Island island = plugin.getGrid().getIsland(teamLeader);
	    final IslandLeaveEvent event = new IslandLeaveEvent(playerUUID, island);
	    plugin.getServer().getPluginManager().callEvent(event);
	} else {
	    // Ex-Leaders keeps their island, but the rest of the team items are
	    // removed
	    plugin.getPlayers().setLeaveTeam(playerUUID);
	}

    }

    /**
     * List schematics this player can access. If @param ignoreNoPermission is true, then only
     * schematics with a specific permission set will be checked. I.e., no common schematics will
     * be returned (including the default one).
     * @param player
     * @param ignoreNoPermission
     * @return List of schematics this player can use based on their permission level
     */
    public List<Schematic> getSchematics(Player player, boolean ignoreNoPermission) {
	List<Schematic> result = new ArrayList<Schematic>();
	// Find out what schematics this player can choose from
	//Bukkit.getLogger().info("DEBUG: Checking schematics for " + player.getName());
	for (Schematic schematic : schematics.values()) {
	    //Bukkit.getLogger().info("DEBUG: schematic name is '"+ schematic.getName() + "'");
	    //Bukkit.getLogger().info("DEBUG: perm is " + schematic.getPerm());
	    if ((!ignoreNoPermission && schematic.getPerm().isEmpty()) || VaultHelper.checkPerm(player, schematic.getPerm())) {
		//Bukkit.getLogger().info("DEBUG: player can use this schematic");
		// Only add if it's visible
		if (schematic.isVisible()) {
		    // Check if it's a nether island, but the nether is not enables
		    if (schematic.getBiome().equals(Biome.HELL)) {
			if (Settings.createNether && Settings.newNether) {
			    result.add(schematic);
			}
		    } else {
			result.add(schematic);
		    }
		}

	    }
	}
	// Sort according to order
	Collections.sort(result, new Comparator<Schematic>() {

	    @Override
	    public int compare(Schematic o1, Schematic o2) {
		return ((o2.getOrder() < o1.getOrder()) ? 1 : -1);
	    }

	});
	return result;
    }

    /**
     * @return the schematics
     */
    public static HashMap<String, Schematic> getSchematics() {
	return schematics;
    }

    /**
     * Makes the default island for the player
     * @param player
     */
    public void newIsland(final Player player) {
	//plugin.getLogger().info("DEBUG: Making default island");
	newIsland(player, schematics.get("default"));
    }

    /**
     * Makes an island using schematic. No permission checks are made. They have to be decided
     * before this method is called.
     * @param player
     * @param schematic
     */
    public void newIsland(final Player player, final Schematic schematic) {
	//long time = System.nanoTime();
	final UUID playerUUID = player.getUniqueId();
	boolean firstTime = false;
	if (!plugin.getPlayers().hasIsland(playerUUID)) {
	    firstTime = true;
	}
	//plugin.getLogger().info("DEBUG: finding island location");
	Location next = getNextIsland();
	//plugin.getLogger().info("DEBUG: found " + next);
	// Set the player's parameters to this island
	plugin.getPlayers().setHasIsland(playerUUID, true);
	// Clear any old home locations (they should be clear, but just in case)
	plugin.getPlayers().clearHomeLocations(playerUUID);
	// Set the player's island location to this new spot
	plugin.getPlayers().setIslandLocation(playerUUID, next);

	// Set the biome
	//BiomesPanel.setIslandBiome(next, schematic.getBiome());
	// Teleport to the new home
	if (schematic.isPlayerSpawn()) {
	    // Set home and teleport
	    plugin.getPlayers().setHomeLocation(playerUUID, schematic.getPlayerSpawn(next), 1);
	}

	// Sets a flag to temporarily disable cleanstone generation
	plugin.setNewIsland(true);
	//plugin.getBiomes();

	// Create island based on schematic
	if (schematic != null) {
	    //plugin.getLogger().info("DEBUG: pasting schematic " + schematic.getName() + " " + schematic.getPerm());
	    // Paste the starting island. If it is a HELL biome, then we start in the Nether
	    if (Settings.createNether && schematic.isInNether() && Settings.newNether) {
		// Nether start
		// Paste the overworld if it exists
		if (!schematic.getPartnerName().isEmpty() && schematics.containsKey(schematic.getPartnerName())) {
		    // A partner schematic is available
		    pastePartner(schematics.get(schematic.getPartnerName()),next, player);
		}
		// Switch home location to the Nether
		next = next.toVector().toLocation(ASkyBlock.getNetherWorld());
		// Set the player's island location to this new spot
		plugin.getPlayers().setIslandLocation(playerUUID, next);
		// TODO: work through the implications of this!
		schematic.pasteSchematic(next, player, true);
	    } else {
		// Over world start
		//plugin.getLogger().info("DEBUG: pasting");
		//long timer = System.nanoTime();
		// Paste the island and teleport the player home
		schematic.pasteSchematic(next, player, true);
		//double diff = (System.nanoTime() - timer)/1000000;
		//plugin.getLogger().info("DEBUG: nano time = " + diff + " ms");
		//plugin.getLogger().info("DEBUG: pasted overworld");
		if (Settings.createNether && Settings.newNether) {
		    // Paste the other world schematic
		    final Location netherLoc = next.toVector().toLocation(ASkyBlock.getNetherWorld());
		    if (schematic.getPartnerName().isEmpty()) {
			// This will paste the over world schematic again
			//plugin.getLogger().info("DEBUG: pasting nether");
			pastePartner(schematic, netherLoc, player);
			//plugin.getLogger().info("DEBUG: pasted nether");
		    } else {
			if (schematics.containsKey(schematic.getPartnerName())) {
			    //plugin.getLogger().info("DEBUG: pasting partner");
			    // A partner schematic is available
			    pastePartner(schematics.get(schematic.getPartnerName()),netherLoc, player);
			} else {
			    plugin.getLogger().severe("Partner schematic heading '" + schematic.getPartnerName() + "' does not exist");
			}
		    }
		}
	    }
	    // Record the rating of this schematic - not used for anything right now
	    plugin.getPlayers().setStartIslandRating(playerUUID, schematic.getRating());
	} 
	// Clear the cleanstone flag so events can happen again
	plugin.setNewIsland(false);
	// Add to the grid
	Island myIsland = plugin.getGrid().addIsland(next.getBlockX(), next.getBlockZ(), playerUUID);

	// Save the player so that if the server is reset weird things won't happen
	plugin.getPlayers().save(playerUUID);
	/*
	if (firstTime) {
	    plugin.getLogger().info("First time teleport");
	    plugin.getGrid().homeTeleport(player);
	}*/

	// Delayed teleport so that the island pasting can be completed.
	/*
	plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

	    @Override
	    public void run() {
		// New island teleport
		plugin.getGrid().homeTeleport(player);

	    }}, 40L);
	 */
	// Reset any inventory, etc. This is done AFTER the teleport because other plugins may switch out inventory based on world
	plugin.resetPlayer(player);
	// Reset money if required
	if (Settings.resetMoney) {
	    resetMoney(player);
	}
	// Start the reset cooldown
	if (!firstTime) {
	    setResetWaitTime(player);
	}
	// Set the custom protection range if appropriate
	// Dynamic home sizes with permissions
	int range = Settings.island_protectionRange;
	for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
	    if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.range.")) {
		range = Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "island.range.")[1]);
		// Do some sanity checking
		if (range % 2 != 0) {
		    range--;
		    plugin.getLogger().warning("Protection range must be even, using " + range + " for " + player.getName());
		}
		if (range > Settings.islandDistance) {
		    if (!plugin.getConfig().getBoolean("island.overridelimit", false)) {
			if (range > (Settings.islandDistance - 16)) {
			    range = Settings.islandDistance - 16;
			    plugin.getLogger().warning(
				    "Island protection range must be " + (Settings.islandDistance - 16) + " or less, (island range -16). Setting to: "
					    + range);
			}
		    } else {
			range = Settings.islandDistance;
		    }
		}
		if (range < 0) {
		    range = 0;
		}
	    }
	}
	myIsland.setProtectionSize(range);
	// Show fancy titles!
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
	// Run any commands that need to be run at the start
	if (firstTime) {
	    //plugin.getLogger().info("DEBUG: First time");
	    if (!player.hasPermission(Settings.PERMPREFIX + "command.newislandexempt")) {
		//plugin.getLogger().info("DEBUG: Executing new island commands");
		runCommands(Settings.startCommands, player);
	    }
	}
	// Save grid just in case there's a crash
	plugin.getGrid().saveGrid();
	// Done - fire event
	final IslandNewEvent event = new IslandNewEvent(player,schematic, myIsland);
	plugin.getServer().getPluginManager().callEvent(event);
	//plugin.getLogger().info("DEBUG: Done! " + (System.nanoTime()- time) * 0.000001);
    }

    /**
     * Does a delayed pasting of the partner island
     * @param schematic
     * @param player
     */
    private void pastePartner(final Schematic schematic, final Location loc, final Player player) {
	plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

	    @Override
	    public void run() {
		schematic.pasteSchematic(loc, player, false);

	    }}, 60L);

    }

    /**
     * Pastes a schematic at a location for the player
     * @param schematic
     * @param loc
     * @param player
     */
    public void pasteSchematic(final Schematic schematic, final Location loc, final Player player) {
	schematic.pasteSchematic(loc, player, false);
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
	// Player asking for their own island calc
	if (asker.getUniqueId().equals(targetPlayer) || asker.isOp() || VaultHelper.checkPerm(asker, Settings.PERMPREFIX + "mod.info")) {
	    // Newer better system - uses chunks
	    if (Settings.fastLevelCalc) {
		if (!onLevelWaitTime(asker) || Settings.levelWait <= 0 || asker.isOp() || VaultHelper.checkPerm(asker, Settings.PERMPREFIX + "mod.info")) {
		    asker.sendMessage(ChatColor.GREEN + plugin.myLocale(asker.getUniqueId()).levelCalculating);
		    setLevelWaitTime(asker);
		    new LevelCalcByChunk(plugin, targetPlayer, asker);
		} else {
		    asker.sendMessage(ChatColor.YELLOW + plugin.myLocale(asker.getUniqueId()).islandresetWait.replace("[time]", String.valueOf(getLevelWaitTime(asker))));
		}
	    } else {
		// Legacy support - maybe some people still want the old way (shrug)
		plugin.setCalculatingLevel(true);
		if (!onLevelWaitTime(asker) || Settings.levelWait <= 0 || asker.isOp() || VaultHelper.checkPerm(asker, Settings.PERMPREFIX + "mod.info")) {
		    asker.sendMessage(ChatColor.GREEN + plugin.myLocale(asker.getUniqueId()).levelCalculating);
		    LevelCalc levelCalc = new LevelCalc(plugin, targetPlayer, asker);
		    levelCalc.runTaskTimer(plugin, 0L, 5L);
		    setLevelWaitTime(asker);
		} else {
		    asker.sendMessage(ChatColor.YELLOW + plugin.myLocale(asker.getUniqueId()).islandresetWait.replace("[time]", String.valueOf(getLevelWaitTime(asker))));
		    plugin.setCalculatingLevel(false);
		}
	    }
	} else {
	    // Asking for the level of another player
	    asker.sendMessage(ChatColor.GREEN + plugin.myLocale(asker.getUniqueId()).islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
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
	    sender.sendMessage(plugin.myLocale().errorUseInGame);
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
		// Check if the max number of islands is made already
		if (Settings.maxIslands > 0 && plugin.getGrid().getIslandCount() > Settings.maxIslands) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorMaxIslands);
		    return true;
		}
		// Create new island for player
		player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).islandnew);
		chooseIsland(player);
		return true;
	    } else {
		if (plugin.getPlayers().getControlPanel(playerUUID)) {
		    player.performCommand(Settings.ISLANDCOMMAND + " cp");
		} else {
		    if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName) || Settings.allowTeleportWhenFalling
			    || !PlayerEvents.isFalling(playerUUID) || (player.isOp() && !Settings.damageOps)) {
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
	    if (split[0].equalsIgnoreCase("teamchat")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.chat")) {
		    // Check if this command is on or not
		    if (!Settings.teamChat) {
			player.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
			return false;
		    }
		    // Check if in team
		    if (plugin.getPlayers().inTeam(playerUUID)) {
			// Check if team members are online
			boolean online = false;
			for (UUID teamMember : plugin.getPlayers().getMembers(playerUUID)) {
			    if (!teamMember.equals(playerUUID) && plugin.getServer().getPlayer(teamMember) != null) {
				online = true;
			    }
			}
			if (!online) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).teamChatNoTeamAround);
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).teamChatStatusOff);
			    plugin.getChatListener().unSetPlayer(playerUUID);
			    return true;
			}
			if (plugin.getChatListener().isTeamChat(playerUUID)) {
			    // Toggle
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).teamChatStatusOff);
			    plugin.getChatListener().unSetPlayer(playerUUID);
			} else {
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).teamChatStatusOn);
			    plugin.getChatListener().setPlayer(playerUUID);
			}
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).teamChatNoTeam);
		    }
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		}
		return true;
	    }

	    if (split[0].equalsIgnoreCase("ban")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.ban")) {
		    // Just show ban help
		    player.sendMessage(plugin.myLocale(playerUUID).helpColor + "/" + label + " ban <player>: " + ChatColor.WHITE + plugin.myLocale(playerUUID).islandhelpBan);
		} else {
		    player.sendMessage(plugin.myLocale(playerUUID).errorNoPermission);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("unban") && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.ban")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.ban")) {
		    // Just show unban help
		    player.sendMessage(plugin.myLocale(playerUUID).helpColor + "/" + label + " unban <player>: " + ChatColor.WHITE + plugin.myLocale(playerUUID).islandhelpUnban);
		} else {
		    player.sendMessage(plugin.myLocale(playerUUID).errorNoPermission);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("make")) {
		//plugin.getLogger().info("DEBUG: /is make called");
		if (!pendingNewIslandSelection.contains(playerUUID)) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		    return false;
		}
		pendingNewIslandSelection.remove(playerUUID);
		Island oldIsland = plugin.getGrid().getIsland(player.getUniqueId());
		newIsland(player);
		if (resettingIsland.contains(playerUUID)) {
		    resettingIsland.remove(playerUUID);
		    resetPlayer(player, oldIsland);
		}
		return true;
	    } else 
		if (split[0].equalsIgnoreCase("lang")) {
		    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lang")) {
			player.sendMessage("/" + label + " lang <locale>");
			player.sendMessage("English");
			player.sendMessage("Français");
			player.sendMessage("Deutsch");
			player.sendMessage("Español");
			player.sendMessage("Italiano");
			player.sendMessage("한국의 / Korean");
			player.sendMessage("Polski");
			player.sendMessage("Brasil");
			player.sendMessage("中国 / SimplifiedChinese");
			player.sendMessage("Čeština");
			player.sendMessage("Slovenčina");
			player.sendMessage("繁體中文 / TraditionalChinese");
			player.sendMessage("Nederlands");
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		    }
		    return true;
		} else if (split[0].equalsIgnoreCase("settings")) {
		    // Show what the plugin settings are
		    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.settings")) {
			try {
			    player.openInventory(plugin.getSettingsPanel().islandGuardPanel(player));
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
			}
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
			    // Remove any visitors
			    for (Player target : plugin.getServer().getOnlinePlayers()) {
				// See if target is on this player's island, not a mod, no bypass, and not a coop player
				if (!player.equals(target) && !target.isOp() && !VaultHelper.checkPerm(target, Settings.PERMPREFIX + "mod.bypassprotect")
					&& (target.getWorld().equals(ASkyBlock.getIslandWorld()) || target.getWorld().equals(ASkyBlock.getNetherWorld()))
					&& plugin.getGrid().isOnIsland(player, target)
					&& !CoopPlay.getInstance().getCoopPlayers(island.getCenter()).contains(target.getUniqueId())) {
				    // Send them home
				    if (plugin.getPlayers().inTeam(target.getUniqueId()) || plugin.getPlayers().hasIsland(target.getUniqueId())) {
					plugin.getGrid().homeTeleport(target);
				    } else {
					// Just move target to spawn
					if (!target.performCommand(Settings.SPAWNCOMMAND)) {
					    target.teleport(player.getWorld().getSpawnLocation());
					}
				    }
				    target.sendMessage(ChatColor.RED + plugin.myLocale(target.getUniqueId()).expelExpelled);
				    plugin.getLogger().info(player.getName() + " expelled " + target.getName() + " from their island when locking.");
				    // Yes they are
				    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).expelSuccess.replace("[name]", target.getDisplayName()));
				}
			    }
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).lockLocking);
			    plugin.getMessages().tellOfflineTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerLocked.replace("[name]", player.getDisplayName()));
			    plugin.getMessages().tellTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerLocked.replace("[name]", player.getDisplayName()));
			    island.setLocked(true);
			} else {
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).lockUnlocking);
			    plugin.getMessages().tellOfflineTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerUnlocked.replace("[name]", player.getDisplayName()));
			    plugin.getMessages().tellTeam(playerUUID, plugin.myLocale(playerUUID).lockPlayerUnlocked.replace("[name]", player.getDisplayName()));
			    island.setLocked(false);
			}
			return true;
		    }
		} else if (split[0].equalsIgnoreCase("go")) {
		    if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.go")) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
			return true;
		    }
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
		    player.sendMessage(ChatColor.GOLD + "(c) 2014 - 2015 by tastybento");
		    return true;
		    // Spawn enderman
		    // Enderman enderman = (Enderman)
		    // player.getWorld().spawnEntity(player.getLocation().add(new
		    // Vector(5,0,5)), EntityType.ENDERMAN);
		    // enderman.setCustomName("TastyBento's Ghost");
		    // enderman.setCarriedMaterial(new
		    // MaterialData(Material.GRASS));
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
		    // Check island
		    if (plugin.getGrid().getIsland(player.getUniqueId()) == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
			return true;
		    }
		    if (player.getWorld().equals(ASkyBlock.getIslandWorld()) || player.getWorld().equals(ASkyBlock.getNetherWorld())) {	
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
		    Collection<UUID> warpList = plugin.getWarpSignsListener().listWarps();
		    if (warpList.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).warpserrorNoWarpsYet);
			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp") && plugin.getGrid().playerIsOnIsland(player)) {
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
			}
			return true;
		    } else {
			if (Settings.useWarpPanel) {
			    // Try the warp panel
			    player.openInventory(plugin.getWarpPanel().getWarpPanel(0));
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
		    confirm.remove(playerUUID);
		    // Actually RESET the island
		    player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).islandresetPleaseWait);
		    if (plugin.getPlayers().getResetsLeft(playerUUID) == 0) {
			player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).islandResetNoMore);
		    }
		    if (plugin.getPlayers().getResetsLeft(playerUUID) > 0) {
			player.sendMessage(ChatColor.YELLOW
				+ plugin.myLocale(player.getUniqueId()).resetYouHave.replace("[number]", String.valueOf(plugin.getPlayers().getResetsLeft(playerUUID))));
		    }
		    // Show a schematic panel if the player has a choice
		    // Get the schematics that this player is eligible to use
		    List<Schematic> schems = getSchematics(player, false);
		    //plugin.getLogger().info("DEBUG: size of schematics for this player = " + schems.size());
		    Island oldIsland = plugin.getGrid().getIsland(player.getUniqueId());
		    if (schems.isEmpty()) {
			// No schematics - use default island
			newIsland(player);
			resetPlayer(player,oldIsland);
		    } else if (schems.size() == 1) {
			// Hobson's choice
			newIsland(player,schems.get(0));
			resetPlayer(player,oldIsland);
		    } else {
			// A panel can only be shown if there is >1 viable schematic
			if (Settings.useSchematicPanel) {
			    pendingNewIslandSelection.add(playerUUID);
			    resettingIsland.add(playerUUID);
			    player.openInventory(plugin.getSchematicsPanel().getPanel(player));
			} else {
			    // No panel
			    // Check schematics for specific permission
			    schems = getSchematics(player,true);
			    if (schems.isEmpty()) {
				newIsland(player);
			    } else if (Settings.chooseIslandRandomly) {
				// Choose an island randomly from the list
				newIsland(player, schems.get(random.nextInt(schems.size())));
			    } else {
				// Do the first one in the list
				newIsland(player, schems.get(0));
			    }
			    resetPlayer(player,oldIsland);
			}
		    }
		    return true;
		} else {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/island restart: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpRestart);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("sethome")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    // Check island
		    if (plugin.getGrid().getIsland(player.getUniqueId()) == null) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
			return true;
		    }
		    plugin.getGrid().homeSet(player);
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("help")) {
		player.sendMessage(ChatColor.GREEN + plugin.getName() + " " + plugin.getDescription().getVersion() + " help:");
		if (Settings.useControlPanel) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + ": " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpControlPanel);
		} else {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + ": " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpIsland);
		}
		// Dynamic home sizes with permissions
		int maxHomes = Settings.maxHomes;
		for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
		    if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.maxhomes.")) {
			// Get the max value should there be more than one
			maxHomes = Math.max(maxHomes, Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "island.maxhomes.")[1]));
		    }
		    // Do some sanity checking
		    if (maxHomes < 1) {
			maxHomes = 1;
		    }
		}
		if (maxHomes > 1 && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " go <1 - " + maxHomes + ">: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpTeleport);
		} else {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " go: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpTeleport);
		}
		if (plugin.getGrid() != null && plugin.getGrid().getSpawn() != null) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " spawn: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpSpawn);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.controlpanel")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " controlpanel or cp [on/off]: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpControlPanel);
		}
		player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " restart: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpRestart);
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		    if (maxHomes > 1) {
			player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " sethome <1 - " + maxHomes + ">: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpSetHome);
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
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.chat")
			&& plugin.getPlayers().inTeam(playerUUID)) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " teamchat: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).teamChatHelp);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.biomes")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " biomes: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpBiome);
		}
		// if (!Settings.allowPvP) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.expel")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " expel <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpExpel);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.ban")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " ban <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpBan);
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " unban <player>: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpUnban);
		}
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "coop")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " coop: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpCoop);
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " uncoop: " + ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).islandhelpUnCoop);
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
		    // Not allowed in the nether
		    if (plugin.getPlayers().getIslandLocation(playerUUID).getWorld().getEnvironment().equals(Environment.NETHER)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
			return true;
		    }
		    // player.sendMessage(plugin.myLocale(player.getUniqueId()).helpColor + "[Biomes]");
		    Inventory inv = plugin.getBiomes().getBiomePanel(player);
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
		Location l = ASkyBlock.getIslandWorld().getSpawnLocation();
		l.add(new Vector(0.5,0,0.5));
		Island spawn = plugin.getGrid().getSpawn();
		if (spawn != null && spawn.getSpawnPoint() != null) {
		    l = spawn.getSpawnPoint();
		}	
		player.teleport(l);
		return true;
	    } else if (split[0].equalsIgnoreCase("top")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.topten")) {
		    TopTen.topTenShow(player);
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("level")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.info")) {
		    if (!plugin.getPlayers().inTeam(playerUUID) && !plugin.getPlayers().hasIsland(playerUUID)) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
			return true;
		    } else {
			if (Settings.fastLevelCalc) {
			    calculateIslandLevel(player, playerUUID);
			    return true;
			} else {
			    // Legacy - forces player to be on island to reduce frivolous calculations
			    if (plugin.getGrid().playerIsOnIsland(player)) {
				calculateIslandLevel(player, playerUUID);
			    } else {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotOnIsland);
			    }
			    return true;
			}
		    }
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("invite")) {
		// Invite label with no name, i.e., /island invite - tells the
		// player how many more people they can invite
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.create")) {
		    player.sendMessage(plugin.myLocale(player.getUniqueId()).invitehelp);
		    // If the player who is doing the inviting has a team
		    if (plugin.getPlayers().inTeam(playerUUID)) {
			// Check to see if the player is the leader
			if (teamLeader.equals(playerUUID)) {
			    // Check to see if the team is already full
			    int maxSize = Settings.maxTeamSize;
			    // Dynamic team sizes with permissions
			    for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
				//plugin.getLogger().info("DEBUG perms: " + perms);
				if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
				    maxSize = Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.")[1]);
				}
				// Do some sanity checking
				if (maxSize < Settings.maxTeamSize) {
				    maxSize = Settings.maxTeamSize;
				}
			    }
			    // Account for deprecated permissions. These will be zero on new installs
			    // This avoids these permissions breaking on upgrades
			    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip")) {
				if (Settings.maxTeamSizeVIP > maxSize) {
				    maxSize = Settings.maxTeamSizeVIP;
				}
			    }
			    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.vip2")) {
				if (Settings.maxTeamSizeVIP2 > maxSize) {
				    maxSize = Settings.maxTeamSizeVIP2;
				}
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
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		    return true;
		}
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
			// Reset reset limit - note that a player can get around the reset
			// limit by begging someone to invite them to an island and then leaving
			plugin.getPlayers().setResetsLeft(playerUUID, Settings.resetLimit);
			player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).inviteyouHaveJoinedAnIsland);
			if (Bukkit.getPlayer(inviteList.get(playerUUID)) != null) {
			    Bukkit.getPlayer(inviteList.get(playerUUID)).sendMessage(
				    ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).invitehasJoinedYourIsland.replace("[name]", player.getName()));
			}
			// Remove the invite
			inviteList.remove(player.getUniqueId());
			plugin.getGrid().saveGrid();
			return true;
		    }
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		    return true;
		}
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

			    // Log the location that this player left so they
			    // cannot join again before the cool down ends
			    plugin.getPlayers().startInviteCoolDownTimer(playerUUID, plugin.getPlayers().getTeamIslandLocation(teamLeader));
			    // Remove from team
			    removePlayerFromTeam(playerUUID, teamLeader);
			    // Remove any warps
			    plugin.getWarpSignsListener().removeWarp(playerUUID);
			    player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).leaveyouHaveLeftTheIsland);
			    // Tell the leader if they are online
			    if (plugin.getServer().getPlayer(teamLeader) != null) {
				plugin.getServer().getPlayer(teamLeader)
				.sendMessage(ChatColor.RED + plugin.myLocale(teamLeader).leavenameHasLeftYourIsland.replace("[name]", player.getName()));
			    } else {
				// Leave them a message
				plugin.getMessages().setMessage(teamLeader, ChatColor.RED + plugin.myLocale(teamLeader).leavenameHasLeftYourIsland.replace("[name]", player.getName()));
			    }
			    // Check if the size of the team is now 1
			    // teamMembers.remove(playerUUID);
			    if (teamMembers.size() < 2) {
				// plugin.getLogger().info("DEBUG: Party is less than 2 - removing leader from team");
				removePlayerFromTeam(teamLeader, teamLeader);
			    }
			    // Clear all player variables and save
			    plugin.resetPlayer(player);
			    if (!player.performCommand(Settings.SPAWNCOMMAND)) {
				player.teleport(player.getWorld().getSpawnLocation());
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
		} else {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("team")) {
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    if (teamLeader.equals(playerUUID)) {
			int maxSize = Settings.maxTeamSize;
			// Dynamic team sizes with permissions
			for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
			    if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
				maxSize = Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.")[1]);
			    }
			    // Do some sanity checking
			    if (maxSize < Settings.maxTeamSize) {
				maxSize = Settings.maxTeamSize;
			    }
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
		player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorUnknownCommand);
		return true;
	    }
	    /*
	     * Commands that have two parameters
	     */
	case 2:
	    if (split[0].equalsIgnoreCase("controlpanel") || split[0].equalsIgnoreCase("cp")) {
		if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.controlpanel")) {
		    if (split[1].equalsIgnoreCase("on")) {
			plugin.getPlayers().setControlPanel(playerUUID, true);
		    } else if (split[1].equalsIgnoreCase("off")) {
			plugin.getPlayers().setControlPanel(playerUUID, false);
		    }
		    player.sendMessage(ChatColor.GREEN + plugin.myLocale(playerUUID).generalSuccess);
		    return true;
		} else {
		    player.sendMessage(plugin.myLocale(playerUUID).errorNoPermission);
		    return true;
		}
	    } else
		if (split[0].equalsIgnoreCase("warps")) {
		    if (Settings.useWarpPanel) {
			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.warp")) {
			    // Step through warp table
			    Set<UUID> warpList = plugin.getWarpSignsListener().listWarps();
			    if (warpList.isEmpty()) {
				player.sendMessage(ChatColor.YELLOW + plugin.myLocale(player.getUniqueId()).warpserrorNoWarpsYet);
				if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp") && plugin.getGrid().playerIsOnIsland(player)) {
				    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
				}
				return true;
			    } else {
				// Try the warp panel
				int panelNum = 0;
				try {
				    panelNum = Integer.valueOf(split[1]) - 1;
				} catch (Exception e) {
				    panelNum = 0;
				}
				player.openInventory(plugin.getWarpPanel().getWarpPanel(panelNum));
				return true;
			    }
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
			}
		    } else {
			player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorUnknownCommand);
			return true;
		    }
		} else
		    if (split[0].equalsIgnoreCase("make")) {
			//plugin.getLogger().info("DEBUG: /is make '" + split[1] + "' called");
			if (!pendingNewIslandSelection.contains(playerUUID)) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorUnknownCommand);
			    return true;
			}
			pendingNewIslandSelection.remove(playerUUID);
			// Create a new island using schematic
			if (!schematics.containsKey(split[1])) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorUnknownCommand);
			    return true;
			} else {
			    Schematic schematic = schematics.get(split[1]);
			    // Check perm again
			    if (schematic.getPerm().isEmpty() || VaultHelper.checkPerm(player, schematic.getPerm())) {
				Island oldIsland = plugin.getGrid().getIsland(player.getUniqueId());
				newIsland(player,schematic);
				if (resettingIsland.contains(playerUUID)) {
				    resettingIsland.remove(playerUUID);
				    resetPlayer(player, oldIsland);
				}
				return true;
			    } else {
				player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
				return true;
			    }    
			}
		    } else if (split[0].equalsIgnoreCase("lang")) {
			if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lang")) {
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
			    } else if (split[1].equalsIgnoreCase("SimplifiedChinese") || split[1].equalsIgnoreCase("中国")) {
				plugin.getPlayers().setLocale(playerUUID, "zh-CN");  
			    } else if (split[1].equalsIgnoreCase("Čeština") || split[1].equalsIgnoreCase("Cestina")) {
				plugin.getPlayers().setLocale(playerUUID, "cs-CS");  
			    } else if (split[1].equalsIgnoreCase("Slovenčina") || split[1].equalsIgnoreCase("Slovencina")) {
				plugin.getPlayers().setLocale(playerUUID, "sk-SK");  
			    } else if (split[1].equalsIgnoreCase("TraditionalChinese") || split[1].equalsIgnoreCase("繁體中文")) {
				plugin.getPlayers().setLocale(playerUUID, "nl-NL");  
			    } else if (split[1].equalsIgnoreCase("Nederlands") || split[1].equalsIgnoreCase("Dutch")) {
				plugin.getPlayers().setLocale(playerUUID, "nl-NL");  
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
				player.sendMessage("中国 / SimplifiedChinese");
				player.sendMessage("Čeština");
				player.sendMessage("Slovenčina");
				player.sendMessage("繁體中文 / TraditionalChinese");
				player.sendMessage("Nederlands");
				return true;
			    }
			    player.sendMessage("OK!");
			    return true;
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorNoPermission);
			    return true;
			}
		    } else 
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
				    //plugin.getLogger().info("DEBUG: number = " + number);
				    if (number < 1) {
					plugin.getGrid().homeTeleport(player,1);
				    } else {
					int maxHomes = Settings.maxHomes;
					// Dynamic home sizes with permissions
					for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
					    if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.maxhomes.")) {
						// Get the max value should there be more than one
						maxHomes = Math.max(maxHomes, Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "island.maxhomes.")[1]));
					    }
					    // Do some sanity checking
					    if (maxHomes < 1) {
						maxHomes = 1;
					    }
					}
					if (number > maxHomes) {
					    if (maxHomes > 1) {
						player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNumHomes.replace("[max]",String.valueOf(maxHomes)));
					    } else {
						plugin.getGrid().homeTeleport(player,1);
					    }
					} else {
					    // Teleport home
					    plugin.getGrid().homeTeleport(player,number);
					}
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
				Island island = plugin.getGrid().getIsland(playerUUID);
				if (island == null) {
				    // plugin.getLogger().info("DEBUG: player has no island in grid");
				    // Player has no island in the grid
				    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
				    return true;
				}
				int maxHomes = Settings.maxHomes;
				// Dynamic home sizes with permissions
				for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
				    if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.maxhomes.")) {
					// Get the max value should there be more than one
					maxHomes = Math.max(maxHomes, Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "island.maxhomes.")[1]));
				    }
				}
				if (maxHomes > 1) {
				    // Check the number given is a number
				    int number = 0;
				    try {
					number = Integer.valueOf(split[1]);
					if (number < 1 || number > maxHomes) {
					    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNumHomes.replace("[max]",String.valueOf(maxHomes)));
					} else {
					    plugin.getGrid().homeSet(player, number);
					}
				    } catch (Exception e) {
					player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNumHomes.replace("[max]",String.valueOf(maxHomes)));
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
				final Set<UUID> warpList = plugin.getWarpSignsListener().listWarps();
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
					final Location warpSpot = plugin.getWarpSignsListener().getWarp(foundWarp);
					// Check if the warp spot is safe
					if (warpSpot == null) {
					    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNotReadyYet);
					    plugin.getLogger().warning("Null warp found, owned by " + plugin.getPlayers().getName(foundWarp));
					    return true;
					}
					// Find out if island is locked
					Island island = plugin.getGrid().getIslandAt(warpSpot);
					// Check bans
					if (island != null && plugin.getPlayers().isBanned(island.getOwner(), playerUUID)) {
					    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).banBanned.replace("[name]", plugin.getPlayers().getName(island.getOwner())));
					    if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")
						    && !VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypasslock")) {
						return true;
					    }
					}
					if (island != null && island.isLocked() && !player.isOp() && !VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypasslock") 
						&& !VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
					    // Always inform that the island is locked
					    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).lockIslandLocked);
					    // Check if this is the owner, team member or coop
					    if (!plugin.getGrid().locationIsAtHome(player, true, warpSpot)) {
						//plugin.getLogger().info("DEBUG: not at home");
						return true;
					    }
					}
					boolean pvp = false;
					if ((warpSpot.getWorld().equals(ASkyBlock.getIslandWorld()) && island.getIgsFlag(Flags.allowPvP)) 
						|| (warpSpot.getWorld().equals(ASkyBlock.getNetherWorld()) && island.getIgsFlag(Flags.allowNetherPvP))) {
					    pvp = true;
					}
					// Find out which direction the warp is facing
					Block b = warpSpot.getBlock();
					if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
					    Sign sign = (Sign) b.getState();
					    org.bukkit.material.Sign s = (org.bukkit.material.Sign) sign.getData();
					    BlockFace directionFacing = s.getFacing();
					    Location inFront = b.getRelative(directionFacing).getLocation();
					    Location oneDown = b.getRelative(directionFacing).getRelative(BlockFace.DOWN).getLocation();
					    if ((GridManager.isSafeLocation(inFront))) {
						warpPlayer(player, inFront, foundWarp, directionFacing, pvp);
						return true;
					    } else if (b.getType().equals(Material.WALL_SIGN) && GridManager.isSafeLocation(oneDown)) {
						// Try one block down if this is a wall sign
						warpPlayer(player, oneDown, foundWarp, directionFacing, pvp);
						return true;
					    }
					} else {
					    // Warp has been removed
					    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorDoesNotExist);
					    plugin.getWarpSignsListener().removeWarp(warpSpot);
					    return true;
					}
					if (!(GridManager.isSafeLocation(warpSpot))) {
					    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNotSafe);
					    // WALL_SIGN's will always be unsafe if the place in front is obscured.
					    if (b.getType().equals(Material.SIGN_POST)) {
						plugin.getLogger().warning(
							"Unsafe warp found at " + warpSpot.toString() + " owned by " + plugin.getPlayers().getName(foundWarp));

					    }
					    return true;
					} else {
					    final Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5D, warpSpot.getBlockY(),
						    warpSpot.getBlockZ() + 0.5D);
					    player.teleport(actualWarp);
					    if (pvp) {
						player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + plugin.myLocale(player.getUniqueId()).igsPVP + " " + plugin.myLocale(player.getUniqueId()).igsAllowed);
						player.getWorld().playSound(player.getLocation(), Sound.ARROW_HIT, 1F, 1F);
					    } else {
						player.getWorld().playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1F, 1F);
					    }
					    return true;
					}
				    }
				}
			    } else {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
				return true;
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
				return true;
			    }
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
					    // Dynamic team sizes with permissions
					    for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
						if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
						    maxSize = Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.")[1]);
						}
						// Do some sanity checking
						if (maxSize < Settings.maxTeamSize) {
						    maxSize = Settings.maxTeamSize;
						}
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
				return true;
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
			} else if (split[0].equalsIgnoreCase("uncoop")) {
			    if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "coop")) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
				return true;
			    }
			    // Find out who they want to uncoop
			    final UUID targetPlayerUUID = plugin.getPlayers().getUUID(split[1]);
			    // Player must be known
			    if (targetPlayerUUID == null) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
				return true;
			    }
			    // Target should not be themselves
			    if (targetPlayerUUID.equals(playerUUID)) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).banNotYourself);
				return true;
			    }
			    // Target must be online
			    Player target = plugin.getServer().getPlayer(targetPlayerUUID);
			    if (target == null) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorOfflinePlayer);
				return true;
			    }
			    // Remove them from the coop list
			    boolean coop = CoopPlay.getInstance().removeCoopPlayer(player, target);
			    if (coop) {
				target.sendMessage(ChatColor.RED + plugin.myLocale(target.getUniqueId()).coopRemoved.replace("[name]", player.getDisplayName()));
				player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).coopRemoveSuccess.replace("[name]", target.getDisplayName()));
			    } else {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).coopNotInCoop.replace("[name]", target.getDisplayName()));
			    }
			    return true;
			} else if (split[0].equalsIgnoreCase("ban")) {
			    if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.ban")) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
				return true;
			    }
			    // Find out who they want to ban
			    final UUID targetPlayerUUID = plugin.getPlayers().getUUID(split[1]);
			    // Player must be known
			    if (targetPlayerUUID == null) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
				return true;
			    }
			    // Target should not be themselves
			    if (targetPlayerUUID.equals(playerUUID)) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).banNotYourself);
				return true;
			    }
			    // Target cannot be on the same team
			    if (plugin.getPlayers().inTeam(playerUUID) && plugin.getPlayers().inTeam(targetPlayerUUID)) {
				if (plugin.getPlayers().getTeamLeader(playerUUID).equals(plugin.getPlayers().getTeamLeader(targetPlayerUUID))) {
				    // Same team!
				    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).banNotTeamMember);
				    return true;
				}
			    }
			    // Check that the player is not banned already
			    if (plugin.getPlayers().isBanned(playerUUID, targetPlayerUUID)) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).banAlreadyBanned.replace("[name]", split[1]));
				return true;
			    }
			    // Check online/offline status
			    Player target = plugin.getServer().getPlayer(targetPlayerUUID);
			    // Get offline player
			    OfflinePlayer offlineTarget = plugin.getServer().getOfflinePlayer(targetPlayerUUID);
			    // Target cannot be op
			    if (offlineTarget.isOp()) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).banFail.replace("[name]", offlineTarget.getName()));
				return true;
			    }
			    if (target != null) {
				// Remove them from the coop list
				boolean coop = CoopPlay.getInstance().removeCoopPlayer(player, target);
				if (coop) {
				    target.sendMessage(ChatColor.RED + plugin.myLocale(target.getUniqueId()).coopRemoved.replace("[name]", player.getDisplayName()));
				    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).coopRemoveSuccess.replace("[name]", target.getDisplayName()));
				}
				// See if target is on this player's island and if so send them away
				if (plugin.getGrid().isOnIsland(player, target)) {
				    // Check to see if this player has an island or is just
				    // helping out
				    if (plugin.getPlayers().inTeam(targetPlayerUUID) || plugin.getPlayers().hasIsland(targetPlayerUUID)) {
					plugin.getGrid().homeTeleport(target);
				    } else {
					// Just move target to spawn
					if (!target.performCommand(Settings.SPAWNCOMMAND)) {
					    target.teleport(player.getWorld().getSpawnLocation());
					}
				    }
				}
				// Notifications
				// Target
				target.sendMessage(ChatColor.RED + plugin.myLocale(targetPlayerUUID).banBanned.replace("[name]", player.getDisplayName()));
			    } else {
				// Offline notification
				plugin.getMessages().setMessage(targetPlayerUUID, ChatColor.RED + plugin.myLocale(targetPlayerUUID).banBanned.replace("[name]", player.getDisplayName()));
			    }
			    // Console
			    plugin.getLogger().info(player.getName() + " banned " + offlineTarget.getName() + " from their island.");
			    // Player
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).banSuccess.replace("[name]", offlineTarget.getName()));
			    // Tell team
			    plugin.getMessages().tellTeam(playerUUID, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).banSuccess.replace("[name]", offlineTarget.getName()));
			    plugin.getMessages().tellOfflineTeam(playerUUID, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).banSuccess.replace("[name]", offlineTarget.getName()));
			    // Ban the sucker
			    plugin.getPlayers().ban(playerUUID, targetPlayerUUID);
			    plugin.getGrid().saveGrid();
			    return true;
			} else if (split[0].equalsIgnoreCase("unban")) {
			    if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.ban")) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
				return true;
			    }
			    // Find out who they want to unban
			    final UUID targetPlayerUUID = plugin.getPlayers().getUUID(split[1]);
			    // Player must be known
			    if (targetPlayerUUID == null) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
				return true;
			    }
			    // Target should not be themselves
			    if (targetPlayerUUID.equals(playerUUID)) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).banNotYourself);
				return true;
			    }
			    // Check that the player is actually banned
			    if (!plugin.getPlayers().isBanned(playerUUID, targetPlayerUUID)) {
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).banNotBanned.replace("[name]", split[1]));
				return true;
			    }
			    // Notifications
			    // Online check
			    Player target = plugin.getServer().getPlayer(targetPlayerUUID);
			    // Target
			    if (target != null) {
				// Online
				target.sendMessage(ChatColor.RED + plugin.myLocale(target.getUniqueId()).banLifted.replace("[name]", player.getDisplayName()));
			    } else {
				plugin.getMessages().setMessage(targetPlayerUUID, ChatColor.GREEN + plugin.myLocale(targetPlayerUUID).banLifted.replace("[name]", player.getDisplayName()));
			    }
			    OfflinePlayer offlineTarget = plugin.getServer().getOfflinePlayer(targetPlayerUUID);
			    // Player
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).banLiftedSuccess.replace("[name]", offlineTarget.getName()));
			    // Console
			    plugin.getLogger().info(player.getName() + " unbanned " + offlineTarget.getName() + " from their island.");
			    // Tell team
			    plugin.getMessages().tellTeam(playerUUID, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).banLiftedSuccess.replace("[name]", offlineTarget.getName()));
			    plugin.getMessages().tellOfflineTeam(playerUUID, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).banLiftedSuccess.replace("[name]", offlineTarget.getName()));
			    // Unban the redeemed one
			    plugin.getPlayers().unBan(playerUUID, targetPlayerUUID);
			    plugin.getGrid().saveGrid();
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
					if (target.getWorld().equals(ASkyBlock.getIslandWorld())) {
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
						plugin.getPlayers().resetAllChallenges(target.getUniqueId(), false);
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
					plugin.getMessages().setMessage(targetPlayer, ChatColor.RED + plugin.myLocale(player.getUniqueId()).kicknameRemovedYou.replace("[name]", player.getName()));
				    }
				    // Remove any warps
				    plugin.getWarpSignsListener().removeWarp(targetPlayer);
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
				return true;
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
						plugin.getMessages().setMessage(targetPlayer, plugin.myLocale(player.getUniqueId()).makeLeaderyouAreNowTheOwner);
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
					    plugin.getGrid().saveGrid();
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
				return true;
			    }
			} else {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorUnknownCommand);
			    return true;
			}
	}
	player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).errorUnknownCommand);
	return true;
    }


    /**
     * Warps a player to a spot in front of a sign
     * @param player
     * @param inFront
     * @param foundWarp
     * @param directionFacing
     */
    private void warpPlayer(Player player, Location inFront, UUID foundWarp, BlockFace directionFacing, boolean pvp) {
	// convert blockface to angle
	float yaw = Util.blockFaceToFloat(directionFacing);
	final Location actualWarp = new Location(inFront.getWorld(), inFront.getBlockX() + 0.5D, inFront.getBlockY(),
		inFront.getBlockZ() + 0.5D, yaw, 30F);
	player.teleport(actualWarp);
	if (pvp) {
	    player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + plugin.myLocale(player.getUniqueId()).igsPVP + " " + plugin.myLocale(player.getUniqueId()).igsAllowed);
	    player.getWorld().playSound(player.getLocation(), Sound.ARROW_HIT, 1F, 1F);
	} else {
	    player.getWorld().playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1F, 1F);
	}
	Player warpOwner = plugin.getServer().getPlayer(foundWarp);
	if (warpOwner != null && !warpOwner.equals(player)) {
	    warpOwner.sendMessage(plugin.myLocale(foundWarp).warpsPlayerWarped.replace("[name]", player.getDisplayName()));
	}
    }

    /**
     * Only run when a new island is created for the first time
     * @param player
     */
    private void chooseIsland(Player player) {
	// Get the schematics that this player is eligible to use
	List<Schematic> schems = getSchematics(player, false);
	//plugin.getLogger().info("DEBUG: size of schematics for this player = " + schems.size());
	if (schems.isEmpty()) {
	    // No schematics - use default island
	    newIsland(player);
	} else if (schems.size() == 1) {
	    // Hobson's choice
	    newIsland(player,schems.get(0));
	} else {
	    // A panel can only be shown if there is >1 viable schematic
	    if (Settings.useSchematicPanel) {
		pendingNewIslandSelection.add(player.getUniqueId());
		player.openInventory(plugin.getSchematicsPanel().getPanel(player));
	    } else {
		// No panel
		// Check schematics for specific permission
		schems = getSchematics(player,true);
		if (schems.isEmpty()) {
		    newIsland(player);
		} else if (Settings.chooseIslandRandomly) {
		    // Choose an island randomly from the list
		    newIsland(player, schems.get(random.nextInt(schems.size())));
		} else {
		    // Do the first one in the list
		    newIsland(player, schems.get(0));
		}
	    }
	}
    }

    private void resetPlayer(Player player, Island oldIsland) {
	// Deduct the reset
	plugin.getPlayers().setResetsLeft(player.getUniqueId(), plugin.getPlayers().getResetsLeft(player.getUniqueId()) - 1);
	// Clear any coop inventories
	// CoopPlay.getInstance().returnAllInventories(player);
	// Remove any coop invitees and grab their stuff
	CoopPlay.getInstance().clearMyInvitedCoops(player);
	CoopPlay.getInstance().clearMyCoops(player);
	//plugin.getLogger().info("DEBUG Reset command issued!");
	// Remove any warps
	plugin.getWarpSignsListener().removeWarp(player.getUniqueId());
	// Delete the old island, if it exists
	if (oldIsland != null) {
	    // Remove any coops
	    CoopPlay.getInstance().clearAllIslandCoops(oldIsland.getCenter());
	    plugin.getGrid().removePlayersFromIsland(oldIsland, player.getUniqueId());
	    //plugin.getLogger().info("DEBUG Deleting old island");
	    new DeleteIslandChunk(plugin, oldIsland);
	    // Fire event
	    final IslandResetEvent event = new IslandResetEvent(player, oldIsland.getCenter());
	    plugin.getServer().getPluginManager().callEvent(event);
	} else {
	    //plugin.getLogger().info("DEBUG oldisland = null!");
	}
	// Run any commands that need to be run at reset
	// Ignore commands with this perm
	if (!player.hasPermission(Settings.PERMPREFIX + "command.resetexempt")) {
	    runCommands(Settings.resetCommands, player);
	}
	plugin.getGrid().saveGrid();
    }

    /**
     * Runs commands when a player resets or leaves a team, etc.
     * Can be run for offline players
     * 
     * @param commands
     * @param offlinePlayer
     */
    private void runCommands(List<String> commands, OfflinePlayer offlinePlayer) {
	// Run commands
	for (String cmd : commands) {
	    if (cmd.startsWith("[SELF]")) {
		cmd = cmd.substring(6,cmd.length()).replace("[player]", offlinePlayer.getName()).trim();
		if (offlinePlayer.isOnline()) {
		    try {
			plugin.getLogger().info("Running command '" + cmd + "' as " + offlinePlayer.getName());
			((Player)offlinePlayer).performCommand(cmd);
		    } catch (Exception e) {
			plugin.getLogger().severe("Problem executing island command executed by player - skipping!");
			plugin.getLogger().severe("Command was : " + cmd);
			plugin.getLogger().severe("Error was: " + e.getMessage());
			e.printStackTrace();
		    }
		}
		continue;
	    }
	    // Substitute in any references to player
	    try {
		//plugin.getLogger().info("Running command " + cmd + " as console.");
		if (!plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("[player]", offlinePlayer.getName()))) {
		    plugin.getLogger().severe("Problem executing island command - skipping!");
		    plugin.getLogger().severe("Command was : " + cmd);
		}
	    } catch (Exception e) {
		plugin.getLogger().severe("Problem executing island command - skipping!");
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

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
	if (!(sender instanceof Player)) {
	    return new ArrayList<String>();
	}
	final Player player = (Player) sender;

	if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.create")) {
	    return new ArrayList<String>();
	}

	final UUID playerUUID = player.getUniqueId();
	final UUID teamLeader = plugin.getPlayers().getTeamLeader(playerUUID);
	List<UUID> teamMembers = new ArrayList<UUID>();
	if (teamLeader != null) {
	    teamMembers = plugin.getPlayers().getMembers(teamLeader);
	}

	final List<String> options = new ArrayList<String>();
	String lastArg = (args.length != 0 ? args[args.length - 1] : "");

	switch (args.length) {
	case 0: 
	case 1: 
	    options.add("help"); //No permission needed.
	    //options.add("make"); //Make is currently a private command never accessible to the player
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		options.add("go");
	    }
	    options.add("about"); //No permission needed. :-) Indeed.
	    if (plugin.getGrid() != null && plugin.getGrid().getSpawn() != null) {
		options.add("spawn");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.controlpanel")) {
		options.add("controlpanel");
		options.add("cp");
	    }
	    options.add("restart"); //No permission needed.
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		options.add("sethome");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.info")) {
		options.add("level");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.topten")) {
		options.add("top");
	    }
	    if (Settings.useEconomy && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.minishop")) {
		options.add("minishop");
		options.add("ms");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.warp")) {
		options.add("warp");
		options.add("warps");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.create")) {
		options.add("team");
		options.add("invite");
		options.add("leave");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.kick")) {
		options.add("kick");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.join")) {
		options.add("accept");
		options.add("reject");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.makeleader")) {
		options.add("makeleader");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.chat")) {
		options.add("teamchat");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.biomes")) {
		options.add("biomes");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.expel")) {
		options.add("expel");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "coop")) {
		options.add("coop");
		options.add("uncoop");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lock")) {
		options.add("lock");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.settings")) {
		options.add("settings");
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lang")) {
		options.add("lang");
	    }

	    break;
	case 2: 
	    if (args[0].equalsIgnoreCase("make")) {
		options.addAll(schematics.keySet());
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lang")) {
		if (args[0].equalsIgnoreCase("lang")) {
		    options.add("English");
		    options.add("Français");
		    options.add("Deutsch");
		    options.add("Español");
		    options.add("Italiano");
		    options.add("한국의");
		    options.add("Korean");
		    options.add("Polski");
		    options.add("Brasil");
		    options.add("中国");
		    options.add("SimplifiedChinese");
		    options.add("繁體中文");
		    options.add("TraditionalChinese");
		    options.add("Čeština");
		    options.add("Slovenčina");
		    options.add("Nederlands");
		}
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.sethome")) {
		if (args[0].equalsIgnoreCase("go") || args[0].equalsIgnoreCase("sethome")) {
		    for (int i = 0; i < Settings.maxHomes; i++) {
			options.add(Integer.toString(i));
		    }
		}
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.warp")
		    && args[0].equalsIgnoreCase("warp")) {
		final Set<UUID> warpList = plugin.getWarpSignsListener().listWarps();

		for (UUID warp : warpList) {
		    options.add(plugin.getPlayers().getName(warp));
		}
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.info")
		    && args[0].equalsIgnoreCase("level")) {
		options.addAll(Util.getOnlinePlayerList());
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.create")
		    && args[0].equalsIgnoreCase("invite")) {
		options.addAll(Util.getOnlinePlayerList());
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "coop")
		    && args[0].equalsIgnoreCase("coop")) {
		options.addAll(Util.getOnlinePlayerList());
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "coop")
		    && args[0].equalsIgnoreCase("uncoop")) {
		options.addAll(Util.getOnlinePlayerList());
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.expel")
		    && args[0].equalsIgnoreCase("expel")) {
		options.addAll(Util.getOnlinePlayerList());
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.kick") 
		    && (args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("remove"))) {
		for (UUID member : teamMembers) {
		    options.add(plugin.getPlayers().getName(member));
		}
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "team.makeleader")
		    && args[0].equalsIgnoreCase("makeleader")) {
		for (UUID member : teamMembers) {
		    options.add(plugin.getPlayers().getName(member));
		}
	    }
	    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.controlpanel")
		    && (args[0].equalsIgnoreCase("cp") || args[0].equalsIgnoreCase("controlpanel"))) {
		options.add("on");
		options.add("off");
	    }
	    break;
	}

	return Util.tabLimit(options, lastArg);
    }
}
