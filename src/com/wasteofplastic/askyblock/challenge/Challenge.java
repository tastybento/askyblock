package com.wasteofplastic.askyblock.challenge;

import java.util.List;

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
	List<String> requirements, requiredChallenges, requiredPermissions;
	Reward reward;
	//PLAYER Challenges settings
	int requiredMoney, requiredXP;
	boolean takeRequirements;
	Reward repeatReward;
	int maxTimes;
	//ISLAND Challenges settings
	int searchRadius;
	//ISLAND_LEVEL Challenges settings
	int requiredIslandLevel;
	
	/**
	 * PLAYER Challenge. Repeatable.
	 * @param name
	 * @param friendlyname
	 * @param description
	 * @param level
	 * @param icon
	 * @param requiredItems
	 * @param requiredChallenges
	 * @param requiredPermissions
	 * @param requiredMoney
	 * @param requiredXP
	 * @param takeRequirements
	 * @param reward
	 * @param repeatReward
	 * @param maxTimes
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			List<String> requiredItems, List<String> requiredChallenges, List<String> requiredPermissions, int requiredMoney, int requiredXP,
			boolean takeRequirements, Reward reward, Reward repeatReward, int maxTimes){
		
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.PLAYER;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		
		this.requirements = requiredItems;
		this.requiredMoney = requiredMoney;
		this.requiredXP = requiredXP;
		this.takeRequirements = takeRequirements;
		this.repeatReward = repeatReward;
		this.maxTimes = maxTimes;
		
		this.searchRadius = 0;
		this.requiredIslandLevel = 0;
	}
	
	/**
	 * MEGA_PLAYER Challenge. Repeatable. Handle the required island levels.
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
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			List<String> requiredItems, int requiredIslandLevel, List<String> requiredChallenges, List<String> requiredPermissions, int requiredMoney, int requiredXP,
			boolean takeRequirements, Reward reward, Reward repeatReward, int maxTimes){
		
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.MEGA_PLAYER;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		
		this.requirements = requiredItems;
		this.requiredMoney = requiredMoney;
		this.requiredXP = requiredXP;
		this.takeRequirements = takeRequirements;
		this.repeatReward = repeatReward;
		this.maxTimes = maxTimes;
		this.requiredIslandLevel = requiredIslandLevel;

		this.searchRadius = 0;
	}
	
	/**
	 * ISLAND Challenge. Not repeatable.
	 * @param name
	 * @param friendlyName
	 * @param description
	 * @param level
	 * @param icon
	 * @param requirements
	 * @param requiredChallenges
	 * @param requiredPermissions
	 * @param searchRadius
	 * @param reward
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			List<String> requirements, List<String> requiredChallenges, List<String> requiredPermissions,
			int searchRadius, Reward reward){
				
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.ISLAND;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		
		this.requirements = requirements;
		this.searchRadius = searchRadius;
		
		this.requiredMoney = 0;
		this.requiredXP = 0;
		this.takeRequirements = false;
		this.repeatReward = null;
		this.maxTimes = 1;
		this.requiredIslandLevel = 0;
	}
	
	/**
	 * MEGA_ISLAND Challenge. Not repeatable.
	 * @param name
	 * @param friendlyName
	 * @param description
	 * @param level
	 * @param icon
	 * @param requirements
	 * @param requiredIslandLevel
	 * @param requiredChallenges
	 * @param requiredPermissions
	 * @param searchRadius
	 * @param reward
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			List<String> requirements, int requiredIslandLevel, List<String> requiredChallenges, List<String> requiredPermissions,
			int searchRadius, Reward reward){
				
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.MEGA_ISLAND;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		
		this.requirements = requirements;
		this.requiredIslandLevel = requiredIslandLevel;
		this.searchRadius = searchRadius;
		
		this.requiredMoney = 0;
		this.requiredXP = 0;
		this.takeRequirements = false;
		this.repeatReward = null;
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
	 */
	public Challenge(String name, String friendlyName, String description, String level, ItemStack icon,
			int requiredIslandLevel, List<String> requiredChallenges, List<String> requiredPermissions,
			Reward reward){
		
		this.name = name;
		this.friendlyName = friendlyName;
		this.description = description;
		this.level = level;
		this.icon = icon;
		this.type = ChallengeType.ISLAND_LEVEL;
		this.requiredChallenges = requiredChallenges;
		this.requiredPermissions = requiredPermissions;
		this.reward = reward;
		
		this.requiredIslandLevel = requiredIslandLevel;

		this.requirements = null;
		this.searchRadius = 0;
		this.requiredMoney = 0;
		this.requiredXP = 0;
		this.takeRequirements = false;
		this.repeatReward = null;
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
	 * Gets the required items, blocks or entities required for the challenge. 
	 * Only works with PLAYER and ISLAND challenges. ISLAND_LEVEL will return null.
	 * @return the challenge's requirements.
	 */
	public List<String> getRequirements(){return requirements;}
	
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
	public boolean isRepeatable(){return repeatReward != null;}
	
	/**
	 * Gets the repetition reward. 
	 * Only works with PLAYER challenges. Other will return null.
	 * @return the repetition reward.
	 */
	public Reward getRepeatReward(){return repeatReward;}
	
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
}
