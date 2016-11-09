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
import com.wasteofplastic.askyblock.challenge.Challenge.ChallengeType;
import com.wasteofplastic.askyblock.util.SpawnEgg1_9;

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

		Settings.challengeLevels = new ArrayList<String>(getChallengeConfig().getConfigurationSection("challenges.levels").getKeys(false));
		for(String level : Settings.challengeLevels){
			Settings.challengeList.addAll(getChallengeConfig().getConfigurationSection("challenges.challengeList." + level).getKeys(false));
		}
		Settings.freeLevels = Arrays.asList(getChallengeConfig().getString("challenges.freelevels","").split(" "));
		Settings.waiverAmount = getChallengeConfig().getInt("challenges.waiveramount", 1);
		if (Settings.waiverAmount < 0) Settings.waiverAmount = 0;

		populateChallengeList();
	}

	/**
	 * Goes through all the challenges in the config.yml file and puts them into
	 * the challenges list
	 */
	public void populateChallengeList() {
		challengeList.clear();
		for(String level : Settings.challengeLevels){
			for(String challenge : Settings.challengeList){
				Challenge c = loadChallenge(level, challenge);
				if(c != null) {
					if (challengeList.containsKey(level)) {
						challengeList.get(level).add(c);
					} else {
						List<Challenge> t = new ArrayList<Challenge>();
						t.add(c);
						challengeList.put(level, t);
					}
				}
				else plugin.getServer().getLogger().severe("Challenge \"" + challenge + "\" could not be loaded. Passing it.");
			}
		}
		// Debug and stats :)
		plugin.getServer().getLogger().info("Successfully loaded " + Settings.challengeList.size() + " challenges in " + Settings.challengeLevels.size() + " levels.");
	}

	private Challenge loadChallenge(String level, String id){
		String path = "challenges.challengeList." + level + "." + id;
		Challenge c = null;

		//Get general infos about the challenge
		ChallengeType type = ChallengeType.getFromString(getChallengeConfig().getString(path + ".type", ""));
		String name = id;
		String friendlyName = getChallengeConfig().getString(path + ".friendlyname", "");
		String description = getChallengeConfig().getString(path + ".description", "");
		ItemStack icon = loadIconFromString(id, getChallengeConfig().getString(path + ".icon", ""));
		boolean resetAllowed = getChallengeConfig().getBoolean(path + ".resetAllowed", true);

		//Get more specific options about the challenge
		boolean takeRequirements = getChallengeConfig().getBoolean(path + ".takeRequirements", false);
		int maxTimes = getChallengeConfig().getInt(path + ".maxTimes", -1);
		int searchRadius = getChallengeConfig().getInt(path + ".searchRadius", 10);

		//Get conditions about the challenge (challenges & permissions)
		List<String> requiredChallenges = getChallengeConfig().getStringList(path + ".require.challenges");
		List<String> requiredPermissions = getChallengeConfig().getStringList(path + ".require.permissions");

		//Get requirements
		int requiredIslandLevel = getChallengeConfig().getInt(path + ".require.islandlevel", 0);
		int requiredMoney = getChallengeConfig().getInt(path + ".require.money", 0);
		int requiredXP = getChallengeConfig().getInt(path + ".require.exp", 0);

		//Get rewards

		//Create challenge
		switch (type) {
		case PLAYER:
			c = new Challenge(name, friendlyName, description, level, icon, requiredItems, requiredIslandLevel, requiredChallenges, requiredPermissions, requiredMoney, requiredXP, takeRequirements, reward, repeatReward, maxTimes, resetAllowed);
			break;
		case ISLAND:
			c = new Challenge(name, friendlyName, description, level, icon, requiredBlocks, requiredEntities, requiredIslandLevel, requiredChallenges, requiredPermissions, searchRadius, reward, resetAllowed);
			break;
		case ISLAND_LEVEL:
			c = new Challenge(name, friendlyName, description, level, icon, requiredIslandLevel, requiredChallenges, requiredPermissions, reward, resetAllowed);
			break;
		case ERROR:
			plugin.getServer().getLogger().severe("Challenge \"" + id + "\" has invalid ChallengeType.");
			plugin.getServer().getLogger().severe("Try them : " + ChallengeType.values());
			break;
		default:
			break;
		}
		return c;
	}

	@SuppressWarnings("deprecation")
	private ItemStack loadIconFromString(String challenge, String iconType){
		ItemStack icon = null;
		if (!iconType.isEmpty()) {
			try {
				// Split if required
				String[] split = iconType.split(":");
				if (split.length == 1) {
					// Some material does not show in the inventory
					if (iconType.equalsIgnoreCase("potato")) {
						iconType = "POTATO_ITEM";
					} else if (iconType.equalsIgnoreCase("brewing_stand")) {
						iconType = "BREWING_STAND_ITEM";
					} else if (iconType.equalsIgnoreCase("carrot")) {
						iconType = "CARROT_ITEM";
					} else if (iconType.equalsIgnoreCase("cauldron")) {
						iconType = "CAULDRON_ITEM";
					} else if (iconType.equalsIgnoreCase("lava") || iconType.equalsIgnoreCase("stationary_lava")) {
						iconType = "LAVA_BUCKET";
					} else if (iconType.equalsIgnoreCase("water") || iconType.equalsIgnoreCase("stationary_water")) {
						iconType = "WATER_BUCKET";
					} else if (iconType.equalsIgnoreCase("portal")) {
						iconType = "OBSIDIAN";
					} else if (iconType.equalsIgnoreCase("PUMPKIN_STEM")) {
						iconType = "PUMPKIN";
					} else if (iconType.equalsIgnoreCase("skull")) {
						iconType = "SKULL_ITEM";
					} else if (iconType.equalsIgnoreCase("COCOA")) {
						iconType = "INK_SACK:3";
					} else if (iconType.equalsIgnoreCase("NETHER_WARTS")) {
						iconType = "NETHER_STALK";
					}
					if (StringUtils.isNumeric(iconType)) {
						icon = new ItemStack(Integer.parseInt(iconType));
					} else {
						icon = new ItemStack(Material.valueOf(iconType));
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
				plugin.getLogger().warning("Error in challenges.yml - icon format is incorrect for " + challenge + ":" + iconType);
				plugin.getLogger().warning("Format should be 'icon: MaterialType:Damage' where Damage is optional");
			}
		}
		if (icon == null || icon.getType() == Material.AIR) {
			icon = new ItemStack(Material.PAPER);
			plugin.getLogger().warning("Icon for challenge \"" + challenge + "\" is not generated. Setting this to PAPER.");
		}
		return icon;
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
