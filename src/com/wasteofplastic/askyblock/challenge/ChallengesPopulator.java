package com.wasteofplastic.askyblock.challenge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.SpawnEgg1_9;

import net.md_5.bungee.api.ChatColor;

/**
 * 
 * Loads the challenge list
 * 
 * @author Poslovitch
 *
 */
public class ChallengesPopulator {

	ASkyBlock plugin;

	public ChallengesPopulator(ASkyBlock plugin) {
		this.plugin = plugin;
	}

	// Database of challenges
	private LinkedHashMap<String, List<Challenge>> challengeList = new LinkedHashMap<String, List<Challenge>>();

	// Where challenges are stored
	private static FileConfiguration challengeFile = null;
	private static File challengeConfigFile = null;

	/**
	 * Saves the challenge.yml file if it does not exist
	 */
	public void saveDefaultChallengeConfig() {
		if (challengeConfigFile == null) {
			challengeConfigFile = new File(plugin.getDataFolder(), "challenges.yml");
		}
		if (!challengeConfigFile.exists()) {
			plugin.saveResource("challenges.yml", false);
		}
	}

	/**
	 * Reloads the challenge config file
	 */
	public void reloadChallengeConfig() {
		if (challengeConfigFile == null) {
			challengeConfigFile = new File(plugin.getDataFolder(), "challenges.yml");
		}
		challengeFile = YamlConfiguration.loadConfiguration(challengeConfigFile);

		// Look for defaults in the jar
		/*
        if (plugin.getResource("challenges.yml") != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            challengeFile.setDefaults(defConfig);
        }*/
		Settings.challengeList = getChallengeConfig().getConfigurationSection("challenges.challengeList").getKeys(false);
		Settings.challengeLevels = Arrays.asList(getChallengeConfig().getString("challenges.levels","").split(" "));
		Settings.freeLevels = Arrays.asList(getChallengeConfig().getString("challenges.freelevels","").split(" "));
		Settings.waiverAmount = getChallengeConfig().getInt("challenges.waiveramount", 1);
		if (Settings.waiverAmount < 0) {
			Settings.waiverAmount = 0;
		}
		populateChallengeList();
	}

	/**
	 * Goes through all the challenges in the config.yml file and puts them into
	 * the challenges list
	 */
	public void populateChallengeList() {
		challengeList.clear();
		for (String s : Settings.challengeList) {
			String level = getChallengeConfig().getString("challenges.challengeList." + s + ".level", "");
			// Verify that this challenge's level is in the list of levels
			if (Settings.challengeLevels.contains(level) || level.isEmpty()) {
				if (challengeList.containsKey(level)) {
					challengeList.get(level).add(loadChallenge(s));
				} else {
					List<Challenge> t = new ArrayList<Challenge>();
					t.add(loadChallenge(s));
					challengeList.put(level, t);
				}
			} else {
				plugin.getServer().getLogger().severe("Level (" + level + ") for challenge " + s + " does not exist. Check challenges.yml.");
			}
		}
	}

	@SuppressWarnings("deprecation")
	private Challenge loadChallenge(String id){
		Challenge c = null;
		String path = "challenges.challengeList." + id + ".";
		String friendlyName = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString(path + "friendlyname", ""));
		String description = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString(path + "description", ""));
		ItemStack icon = null;
        String iconName = getChallengeConfig().getString(path + "icon", "");
        if (!iconName.isEmpty()) {
            try {
                // Split if required
                String[] split = iconName.split(":");
                if (split.length == 1) {
                    // Some material does not show in the inventory
                    if (iconName.equalsIgnoreCase("potato")) {
                        iconName = "POTATO_ITEM";
                    } else if (iconName.equalsIgnoreCase("brewing_stand")) {
                        iconName = "BREWING_STAND_ITEM";
                    } else if (iconName.equalsIgnoreCase("carrot")) {
                        iconName = "CARROT_ITEM";
                    } else if (iconName.equalsIgnoreCase("cauldron")) {
                        iconName = "CAULDRON_ITEM";
                    } else if (iconName.equalsIgnoreCase("lava") || iconName.equalsIgnoreCase("stationary_lava")) {
                        iconName = "LAVA_BUCKET";
                    } else if (iconName.equalsIgnoreCase("water") || iconName.equalsIgnoreCase("stationary_water")) {
                        iconName = "WATER_BUCKET";
                    } else if (iconName.equalsIgnoreCase("portal")) {
                        iconName = "OBSIDIAN";
                    } else if (iconName.equalsIgnoreCase("PUMPKIN_STEM")) {
                        iconName = "PUMPKIN";
                    } else if (iconName.equalsIgnoreCase("skull")) {
                        iconName = "SKULL_ITEM";
                    } else if (iconName.equalsIgnoreCase("COCOA")) {
                        iconName = "INK_SACK:3";
                    } else if (iconName.equalsIgnoreCase("NETHER_WARTS")) {
                        iconName = "NETHER_STALK";
                    }
                    if (StringUtils.isNumeric(iconName)) {
                        icon = new ItemStack(Integer.parseInt(iconName));
                    } else {
                        icon = new ItemStack(Material.valueOf(iconName));
                    }
                    // Check POTION for V1.9 - for some reason, it must be declared as WATER otherwise comparison later causes an NPE
                    if (icon.getType().name().contains("POTION")) {
                        if (!plugin.getServer().getVersion().contains("(MC: 1.8") && !plugin.getServer().getVersion().contains("(MC: 1.7")) {                        
                            PotionMeta potionMeta = (PotionMeta)icon.getItemMeta();
                            potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
                            icon.setItemMeta(potionMeta);
                        }
                    }
                } else if (split.length == 2) {
                    if (StringUtils.isNumeric(split[0])) {
                        icon = new ItemStack(Integer.parseInt(split[0]));
                    } else {
                        icon = new ItemStack(Material.valueOf(split[0]));
                    }
                    // Check POTION for V1.9 - for some reason, it must be declared as WATER otherwise comparison later causes an NPE
                    if (icon.getType().name().contains("POTION")) {
                        if (!plugin.getServer().getVersion().contains("(MC: 1.8") && !plugin.getServer().getVersion().contains("(MC: 1.7")) {                       
                            PotionMeta potionMeta = (PotionMeta)icon.getItemMeta();
                            try {
                                potionMeta.setBasePotionData(new PotionData(PotionType.valueOf(split[1].toUpperCase())));
                            } catch (Exception e) {
                                plugin.getLogger().severe("Challenges icon: Potion type of " + split[1] + " is unknown, setting to WATER. Valid types are:");
                                for (PotionType type: PotionType.values()) {
                                    plugin.getLogger().severe(type.name());
                                }
                                potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
                            } 
                            icon.setItemMeta(potionMeta);
                        }
                    } else if (icon.getType().equals(Material.MONSTER_EGG)) {
                        // Handle monster egg icons
                        try {                                
                            EntityType type = EntityType.valueOf(split[1].toUpperCase());
                            if (Bukkit.getServer().getVersion().contains("(MC: 1.8") || Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
                                icon = new SpawnEgg(type).toItemStack();
                            } else {
                                try {
                                    icon = new SpawnEgg1_9(type).toItemStack();
                                } catch (Exception ex) {
                                    plugin.getLogger().severe("Monster eggs not supported with this server version.");
                                }
                            }
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Spawn eggs must be described by name. Try one of these (not all are possible):");                          
                            for (EntityType type : EntityType.values()) {
                                if (type.isSpawnable() && type.isAlive()) {
                                    plugin.getLogger().severe(type.toString());
                                }
                            }
                        }
                    } else {
                        icon.setDurability(Integer.valueOf(split[1]).shortValue());
                    }
                }
            } catch (Exception e) {
                // Icon was not well formatted
                plugin.getLogger().warning("Error in challenges.yml - icon format is incorrect for " + id + ":" + iconName);
                plugin.getLogger().warning("Format should be 'icon: MaterialType:Damage' where Damage is optional");
            }
        }
        if (icon == null || icon.getType() == Material.AIR) icon = new ItemStack(Material.PAPER);
		
        String level = getChallengeConfig().getString(path + "level", "");
		ChallengeType type = ChallengeType.getFromString(getChallengeConfig().getString(path + "type", "player"));
		//TODO REQUIRED ITEMS/MOBS/BLOCKS
		int requiredMoney = getChallengeConfig().getInt(path + "requiredMoney", 0);
		int requiredXP = getChallengeConfig().getInt(path + "requiredExp", 0);
		int requiredIslandLevel = getChallengeConfig().getInt(path + "requiredIslandLevel", 0);
		List<String> requiredPermissions = getChallengeConfig().getStringList(path + "requiredPermissions");
		List<String> requiredChallenges = getChallengeConfig().getStringList(path + "requiredChallenges");
		boolean takeRequirements = getChallengeConfig().getBoolean(path + "takeRequirements", false);
		String rewardText = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString(path + "rewardText", ""));

		String repeatRewardText = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString(path + "repeatRewardText", ""));
		int maxTimes = getChallengeConfig().getInt(path + "maxTimes", -1);
		int searchRadius = getChallengeConfig().getInt(path + "searchRadius", 0);
		
		switch (type) {
		case PLAYER:
			c = new Challenge(id, friendlyName, description, level, icon, requiredItems, requiredChallenges, requiredPermissions, requiredMoney, requiredXP, takeRequirements, reward, repeatReward, maxTimes);
			break;
		case ISLAND:
			c = new Challenge(id, friendlyName, description, level, icon, requirements, requiredChallenges, requiredPermissions, searchRadius, reward);
			break;
		case ISLAND_LEVEL:
			c = new Challenge(id, friendlyName, description, level, icon, requiredIslandLevel, requiredChallenges, requiredPermissions, reward);
			break;
		case MEGA_PLAYER:
			c = new Challenge(id, friendlyName, description, level, icon, requiredItems, requiredIslandLevel, requiredChallenges, requiredPermissions, requiredMoney, requiredXP, takeRequirements, reward, repeatReward, maxTimes);
			break;
		case MEGA_ISLAND:
			c = new Challenge(id, friendlyName, description, level, icon, requirements, requiredIslandLevel, requiredChallenges, requiredPermissions, searchRadius, reward);
		default:
			break;
		}
		return c;
	}

	/**
	 * @return challenges FileConfiguration object
	 */
	public FileConfiguration getChallengeConfig() {
		if (challengeFile == null) {
			reloadChallengeConfig();
		}
		return challengeFile;
	}

	/**
	 * Saves challenges.yml
	 */
	public void saveChallengeConfig() {
		if (challengeFile == null || challengeConfigFile == null) {
			return;
		}
		try {
			getChallengeConfig().save(challengeConfigFile);
		} catch (IOException ex) {
			plugin.getLogger().severe("Could not save config to " + challengeConfigFile);
		}
	}

	public LinkedHashMap<String, List<Challenge>> getChallengeList(){return challengeList;}
}
