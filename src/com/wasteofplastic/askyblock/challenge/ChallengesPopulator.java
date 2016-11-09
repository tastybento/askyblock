package com.wasteofplastic.askyblock.challenge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

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
				if (challengeList.containsKey(level)) {
					challengeList.get(level).add(loadChallenge("challenges.challengeList." + level + "." + challenge));
				} else {
					List<Challenge> t = new ArrayList<Challenge>();
					t.add(loadChallenge("challenges.challengeList." + level + "." + challenge));
					challengeList.put(level, t);
				}
			}
		}
		// Debug and stats :)
		plugin.getServer().getLogger().info("Successfully loaded " + Settings.challengeList.size() + " challenges in " + Settings.challengeLevels.size() + " levels.");
	}
	
	public Challenge loadChallenge(String path){
		Challenge c = null;
		
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
