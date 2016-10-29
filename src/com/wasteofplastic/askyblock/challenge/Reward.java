package com.wasteofplastic.askyblock.challenge;

import java.util.List;

import org.bukkit.inventory.ItemStack;

/**
 * Challenges reward
 * 
 * @author Poslovitch
 *
 */
public class Reward {
	
	List<ItemStack> rewardItems;
	List<String> rewardPermissions, rewardCommands;
	int rewardMoney, rewardXP;
	String rewardText;
	int repeats;
	
	public Reward(String rewardText, List<ItemStack> rewardItems, List<String> rewardPermissions, List<String> rewardCommands,
			int rewardMoney, int rewardXP){
		
		this.rewardText = rewardText;
		this.rewardItems = rewardItems;
		this.rewardPermissions = rewardPermissions;
		this.rewardCommands = rewardCommands;
		this.rewardXP = rewardXP;
		this.rewardMoney = rewardMoney;
		this.repeats = -1;
	}
	
	public Reward(String rewardText, List<ItemStack> rewardItems, List<String> rewardPermissions, List<String> rewardCommands,
			int rewardMoney, int rewardXP, int repeats){
		
		this.rewardText = rewardText;
		this.rewardItems = rewardItems;
		this.rewardPermissions = rewardPermissions;
		this.rewardCommands = rewardCommands;
		this.rewardXP = rewardXP;
		this.rewardMoney = rewardMoney;
		this.repeats = repeats;
	}
	
	public String getRewardText(){return rewardText;}
	public List<ItemStack> getRewardItems(){return rewardItems;}
	public List<String> getRewardPermissions(){return rewardPermissions;}
	public List<String> getRewardCommands(){return rewardCommands;}
	public int getRewardXP(){return rewardXP;}
	public int getRewardMoney(){return rewardMoney;}
	public int getRepeats(){return repeats;}
}
