package com.wasteofplastic.askyblock.challenge;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * Stores all informations about a challenge
 * 
 * @author Poslovitch
 */
public class Challenge {
	//General challenge settings
	String name, friendlyName, description, level;
	ItemStack icon;
	ChallengeType type;
	List<String> requiredChallenges, requiredPermissions;
	Reward reward;
	boolean resetAllowed;
	//PLAYER Challenges settings
	List<ItemStack> requiredItems;
	int requiredMoney, requiredXP;
	boolean takeRequirements;
	List<Reward> repeatRewards;
	int maxTimes;
	//ISLAND Challenges settings
	HashMap<Material, Integer> requiredBlocks;
	HashMap<EntityType, Integer> requiredEntities;
	int searchRadius;
	//ISLAND_LEVEL Challenges settings
	int requiredIslandLevel;
	
	/**
	 * PLAYER Challenge. Repeatable. Handle the required island levels.
	 * @param name
	 * @param friendlyname
	 * @param description
	 * @param level
	 * @param icon
	 * @param requiredItems
	 * @param requiredIslandLevel
	 * @param requiredChallenges
	 * @param requiredPermissions
	 * @param requiredMoney
	 * @param requiredXP
	 * @param takeRequirements
	 * @param reward
	 * @param repeatReward
	 * @param maxTimes
	 * @param resetAllowed
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			List<ItemStack> requiredItems, int requiredIslandLevel, List<String> requiredChallenges, List<String> requiredPermissions, int requiredMoney, int requiredXP,
			boolean takeRequirements, Reward reward, List<Reward> repeatReward, int maxTimes, boolean resetAllowed){
		
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.PLAYER;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		this.resetAllowed = resetAllowed;
		
		this.requiredItems = requiredItems;
		this.requiredMoney = requiredMoney;
		this.requiredXP = requiredXP;
		this.takeRequirements = takeRequirements;
		this.repeatRewards = repeatReward;
		this.maxTimes = maxTimes;
		this.requiredIslandLevel = requiredIslandLevel;

		this.searchRadius = 0;
		this.requiredBlocks = null;
		this.requiredEntities = null;
	}
	
	/**
	 * ISLAND Challenge. Not repeatable.
	 * @param name
	 * @param friendlyName
	 * @param description
	 * @param level
	 * @param icon
	 * @param requiredBlocks
	 * @param requiredEntities
	 * @param requiredIslandLevel
	 * @param requiredChallenges
	 * @param requiredPermissions
	 * @param searchRadius
	 * @param reward
	 * @param resetAllowed
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			HashMap<Material, Integer> requiredBlocks, HashMap<EntityType, Integer> requiredEntities, int requiredIslandLevel, List<String> requiredChallenges, List<String> requiredPermissions,
			int searchRadius, Reward reward, boolean resetAllowed){
				
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.ISLAND;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		this.resetAllowed = resetAllowed;
		
		this.requiredBlocks = requiredBlocks;
		this.requiredEntities = requiredEntities;
		this.requiredIslandLevel = requiredIslandLevel;
		this.searchRadius = searchRadius;
		
		this.requiredMoney = 0;
		this.requiredXP = 0;
		this.takeRequirements = false;
		this.repeatRewards = null;
		this.maxTimes = 1;
	}
	
	/**
	 * ISLAND_LEVEL Challenge. Not repeatable.
	 * @param name
	 * @param friendlyName
	 * @param description
	 * @param level
	 * @param icon
	 * @param requiredIslandLevel
	 * @param requiredChallenges
	 * @param requiredPermissions
	 * @param reward
	 * @param resetAllowed
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			int requiredIslandLevel, List<String> requiredChallenges, List<String> requiredPermissions,
			Reward reward, boolean resetAllowed){
		
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.ISLAND_LEVEL;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		this.resetAllowed = resetAllowed;
		
		this.requiredIslandLevel = requiredIslandLevel;

		this.requiredBlocks = null;
		this.requiredEntities = null;
		this.searchRadius = 0;
		this.requiredMoney = 0;
		this.requiredXP = 0;
		this.takeRequirements = false;
		this.repeatRewards = null;
		this.maxTimes = 1;
	}
	
	/**
	 * Gets the name (id) of the challenge. Works with all types of challenge.
	 * @return the challenge's name
	 */
	public String getName(){return name;}
	
	/**
	 * Gets the friendlyname of the challenge. Works with all types of challenge.
	 * @return the challenge's friendlyname
	 */
	public String getFriendlyName(){return friendlyName;}
	
	/**
	 * Gets the description of the challenge. Works with all types of challenge.
	 * @return the challenge's description
	 */
	public String getDescription(){return description;}
	
	/**
	 * Gets the level (category) of the challenge. Works with all types of challenge.
	 * @return the challenge's level
	 */
	public String getLevel(){return level;}
	
	/**
	 * Gets the icon of the challenge. Works with all types of challenge.
	 * @return the challenge's icon
	 */
	public ItemStack getIcon(){return icon;}
	
	/**
	 * Gets the type of the challenge. Works with all types of challenge.
	 * @return the challenge's type
	 */
	public ChallengeType getChallengeType(){return type;}
	
	/**
	 * Gets the required items for the challenge. 
	 * Only works with PLAYER and MEGA_PLAYER. Others will return null.
	 * @return the challenge's required items.
	 */
	public List<ItemStack> getRequiredItems(){return requiredItems;}
	
	/**
	 * Gets the required Blocks for the challenge.
	 * Only works with ISLAND and MEGA_ISLAND. Other will return null.
	 * @return the challenge's required blocks.
	 */
	public HashMap<Material, Integer> getRequiredBlocks(){return requiredBlocks;}
	
	/**
	 * Gets the required Entities for the challenge.
	 * Only works with ISLAND and MEGA_ISLAND. Other will return null.
	 * @return the challenge's required entities.
	 */
	public HashMap<EntityType, Integer> getRequiredEntities(){return requiredEntities;}
	
	/**
	 * Gets the required island level for the challenge. 
	 * Only works with ISLAND_LEVEL, MEGA_PLAYER and MEGA_ISLAND challenges. Others will return 0.
	 * @return the required island level.
	 */
	public int getRequiredIslandLevel(){return requiredIslandLevel;}
	
	/**
	 * Gets the required amount of money for the challenge.
	 * Only works with PLAYER challenges. Others will return 0.
	 * @return the required amount of money.
	 */
	public int getRequiredMoney(){return requiredMoney;}
	
	/**
	 * Gets the required amount of XP for the challenge.
	 * Only works with PLAYER challenges. Others will return 0.
	 * @return the required amount of XP.
	 */
	public int getRequiredExp(){return requiredXP;}
	
	/**
	 * Gets if the challenge has to take the requirements when completed. 
	 * Only works with PLAYER challenges. Others will return false.
	 * @return true if it has to take items, else false.
	 */
	public boolean takeRequirements(){return takeRequirements;}
	
	/**
	 * Gets a list of challenges' names that have to be completed before the selected one can be. 
	 * Works with all challenges types.
	 * @return list of required challenges
	 */
	public List<String> getRequiredChallenges(){return requiredChallenges;}
	
	/**
	 * Gets a list of permissions that must be owned to be able to complete the challenge. 
	 * Works with all challenges types.
	 * @return list of required permissions
	 */
	public List<String> getRequiredPermissions(){return requiredPermissions;}
	
	/**
	 * Gets the first completion reward. Works with all types of challenges.
	 * @return the first completion reward
	 */
	public Reward getReward(){return reward;}
	
	/**
	 * Gets if the challenge is repeatable or not. 
	 * Only works with PLAYER challenges. Others will return false.
	 * @return true if the challenge can be repeated, false if not.
	 */
	public boolean isRepeatable(){return repeatRewards != null;}
	
	/**
	 * Gets the repetition reward. 
	 * Only works with PLAYER challenges. Other will return null.
	 * @return the repetition reward.
	 */
	public List<Reward> getRepeatReward(){return repeatRewards;}
	
	/**
	 * Gets the max times before a challenge can't be completed again. 
	 * Only works with PLAYER challenges. Others will return 1.
	 * @return the challenge completion's max times
	 */
	public int getMaxTimes(){return maxTimes;}
	
	/**
	 * Gets the search radius around player to check the requirements.
	 * Only works with ISLAND challenges. Others will return 0.
	 * @return the search radius.
	 */
	public int getSearchRadius(){return searchRadius;}
	
	@Override
	public String toString(){
		//TODO
		return "";
	}
	
	public enum ChallengeType {
		PLAYER,
		ISLAND,
		ISLAND_LEVEL;
		
		static ChallengeType getFromString(String s){
			if(s == null || s.trim().isEmpty()) return PLAYER;
			switch(s.trim()){
			case "player":
				return PLAYER;
			case "island":
				return ISLAND;
			case "level":
				return ISLAND_LEVEL;
			default:
				return PLAYER;
			}
		}
	}
}
