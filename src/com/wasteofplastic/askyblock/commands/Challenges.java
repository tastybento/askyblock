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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.events.ChallengeCompleteEvent;
import com.wasteofplastic.askyblock.events.ChallengeLevelCompleteEvent;
import com.wasteofplastic.askyblock.panels.CPItem;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * Handles challenge commands and related methods
 */
public class Challenges implements CommandExecutor, TabCompleter {
    private ASkyBlock plugin;
    // Database of challenges
    private LinkedHashMap<String, List<String>> challengeList = new LinkedHashMap<String, List<String>>();
    private HashMap<UUID, List<CPItem>> playerChallengeGUI = new HashMap<UUID, List<CPItem>>();
    // Where challenges are stored
    private static FileConfiguration challengeFile = null;
    private static File challengeConfigFile = null;
    // Potion constants
    private static final int EXTENDED_BIT = 0x40;
    private static final int POTION_BIT = 0xF;
    private static final int SPLASH_BIT = 0x4000;
    private static final int TIER_BIT = 0x20;
    private static final int TIER_SHIFT = 5;
    private static final int NAME_BIT = 0x3F;


    public Challenges(ASkyBlock plugin) {
	this.plugin = plugin;
	saveDefaultChallengeConfig();
	// Get the challenges
	getChallengeConfig();
	final Set<String> challengeList = getChallengeConfig().getConfigurationSection("challenges.challengeList").getKeys(false);
	Settings.challengeList = challengeList;
	Settings.challengeLevels = Arrays.asList(getChallengeConfig().getString("challenges.levels").split(" "));
	Settings.waiverAmount = getChallengeConfig().getInt("challenges.waiveramount", 1);
	if (Settings.waiverAmount < 0) {
	    Settings.waiverAmount = 0;
	}
	populateChallengeList();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] cmd) {
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player) sender;
	/*
	 * if
	 * (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	 * player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
	 * return true;
	 * }
	 */
	// Check permissions
	if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.challenges")) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
	    return true;
	}
	// Check island
	if (plugin.getGrid().getIsland(player.getUniqueId()) == null) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
	    return true;
	}
	switch (cmd.length) {
	case 0:
	    // Display panel
	    player.openInventory(challengePanel(player));
	    return true;
	case 1:
	    if (cmd[0].equalsIgnoreCase("help") || cmd[0].equalsIgnoreCase("complete") || cmd[0].equalsIgnoreCase("c")) {
		sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengeshelp1);
		sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengeshelp2);
	    } else if (isLevelAvailable(player, getChallengeConfig().getString("challenges.challengeList." + cmd[0].toLowerCase() + ".level"))) {
		// Provide info on the challenge
		// Challenge Name
		// Description
		// Type
		// Items taken or not
		// island or not
		final String challenge = cmd[0].toLowerCase();
		sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesname + ": " + ChatColor.WHITE + challenge);
		sender.sendMessage(ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).challengeslevel + ": " + ChatColor.GOLD
			+ getChallengeConfig().getString("challenges.challengeList." + challenge + ".level", ""));
		sender.sendMessage(ChatColor.GOLD + getChallengeConfig().getString("challenges.challengeList." + challenge + ".description", ""));
		final String type = getChallengeConfig().getString("challenges.challengeList." + challenge + ".type", "").toLowerCase();
		if (type.equals("inventory")) {
		    if (getChallengeConfig().getBoolean("challenges.challengeList." + cmd[0].toLowerCase() + ".takeItems")) {
			sender.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesitemTakeWarning);
		    }
		} else if (type.equals("island")) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorItemsNotThere);
		}
		if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)
			&& (!type.equals("inventory") || !getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable", false))) {
		    sender.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
		    return true;
		}
		double moneyReward = 0;
		int expReward = 0;
		String rewardText = "";

		if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
		    // First time
		    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0D);
		    rewardText = ChatColor.translateAlternateColorCodes('&',
			    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!"));
		    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".expReward", 0);
		    sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesfirstTimeRewards);
		} else {
		    // Repeat challenge
		    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0D);
		    rewardText = ChatColor.translateAlternateColorCodes('&',
			    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!"));
		    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
		    sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesrepeatRewards);

		}
		sender.sendMessage(ChatColor.WHITE + rewardText);
		if (expReward > 0) {
		    sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward);
		}
		if (Settings.useEconomy && moneyReward > 0) {
		    sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
		}
		sender.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengestoCompleteUse + ChatColor.WHITE + " /" + label + " c " + challenge);
	    } else {
		sender.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesinvalidChallengeName);
	    }
	    return true;
	case 2:
	    if (cmd[0].equalsIgnoreCase("complete") || cmd[0].equalsIgnoreCase("c")) {
		if (!player.getWorld().equals(ASkyBlock.getIslandWorld())) {
		    // Check if in new nether
		    if (!Settings.createNether || !Settings.newNether || !player.getWorld().equals(ASkyBlock.getNetherWorld())) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
			return true;
		    }
		}
		if (checkIfCanCompleteChallenge(player, cmd[1].toLowerCase())) {
		    int oldLevel = levelDone(player);
		    giveReward(player, cmd[1].toLowerCase());
		    int newLevel = levelDone(player);
		    // Fire an event if they are different
		    //plugin.getLogger().info("DEBUG: " + oldLevel + " " + newLevel);
		    if (oldLevel < newLevel) {
			ChallengeLevelCompleteEvent event = new ChallengeLevelCompleteEvent(player, oldLevel, newLevel);
			plugin.getServer().getPluginManager().callEvent(event);
			// Run commands and give rewards
			//plugin.getLogger().info("DEBUG: old level = " + oldLevel + " new level = " + newLevel);
			String level = Settings.challengeLevels.get(newLevel);
			if (!level.isEmpty()) {
			    //plugin.getLogger().info("DEBUG: level name = " + level);
			    String message = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.levelUnlock." + level + ".message", ""));
			    if (!message.isEmpty()) {
				player.sendMessage(ChatColor.GREEN + message);
			    }

			    String[] itemReward = getChallengeConfig().getString("challenges.levelUnlock." + level + ".itemReward", "").split(" ");
			    String rewardDesc = getChallengeConfig().getString("challenges.levelUnlock." + level + ".rewardDesc", "");
			    if (!rewardDesc.isEmpty()) {
				player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesrewards + ": " + ChatColor.WHITE + rewardDesc);
			    }
			    giveItems(player, itemReward);
			    double moneyReward = getChallengeConfig().getDouble("challenges.levelUnlock." + level + ".moneyReward", 0D);
			    int expReward = getChallengeConfig().getInt("challenges.levelUnlock." + level + ".expReward", 0);
			    if (expReward > 0) {
				player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward);
				player.giveExp(expReward);
			    }
			    if (Settings.useEconomy && moneyReward > 0 && (VaultHelper.econ != null)) {
				EconomyResponse e = VaultHelper.econ.depositPlayer(player, Settings.worldName, moneyReward);
				if (e.transactionSuccess()) {
				    player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
				} else {
				    plugin.getLogger().severe("Error giving player " + player.getUniqueId() + " challenge money:" + e.errorMessage);
				    plugin.getLogger().severe("Reward was $" + moneyReward);
				}
			    }
			    String[] permList = getChallengeConfig().getString("challenges.levelUnlock." + level + ".permissionReward", "").split(" ");

			    for (final String s : permList) {			
				if (!s.isEmpty()) {
				    VaultHelper.addPerm(player, s);
				    plugin.getLogger().info("Added permission " + s + " to " + player.getName() + "");
				}
			    }
			    List<String> commands = getChallengeConfig().getStringList("challenges.levelUnlock." + level + ".commands");
			    runCommands(player,commands);
			}
		    }
		}
		return true;
	    }
	default:
	    return false;
	}
    }

    /**
     * Checks the highest level this player has achieved
     * @param player
     * @return level number
     */
    private int levelDone(Player player) {
	int level = 0;
	int done = 0;
	for (String levelName : Settings.challengeLevels) {
	    if (checkLevelCompletion(player, levelName) <= 0) {
		level = (done + 1);
	    }
	    done++;
	}
	return level;
    }

    /**
     * Gives the reward for completing the challenge 
     * 
     * @param player
     * @param challenge
     * @return
     */
    private boolean giveReward(final Player player, final String challenge) {
	// Grab the rewards from the config.yml file
	String[] permList;
	String[] itemRewards;
	double moneyReward = 0;
	int expReward = 0;
	String rewardText = "";
	// If the friendly name is available use it
	String challengeName = getChallengeConfig().getString("challenges.challengeList." + challenge + ".friendlyname",
		challenge.substring(0, 1).toUpperCase() + challenge.substring(1));

	// Gather the rewards due
	// If player has done a challenge already, the rewards are different
	if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
	    // First time
	    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).challengesyouHaveCompleted.replace("[challenge]", challengeName));
	    if (Settings.broadcastMessages) {
		plugin.getServer().broadcastMessage(
			ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesnameHasCompleted.replace("[name]", player.getName()).replace("[challenge]", challengeName));
	    }
	    plugin.getMessages().tellOfflineTeam(player.getUniqueId(),
		    ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesnameHasCompleted.replace("[name]", player.getName()).replace("[challenge]", challengeName));
	    itemRewards = getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".itemReward", "").split(" ");
	    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0D);
	    rewardText = ChatColor.translateAlternateColorCodes('&',
		    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!"));
	    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".expReward", 0);
	} else {
	    // Repeat challenge
	    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).challengesyouRepeated.replace("[challenge]", challengeName));
	    itemRewards = getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatItemReward", "").split(" ");
	    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0);
	    rewardText = ChatColor.translateAlternateColorCodes('&',
		    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!"));
	    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
	}
	// Report the rewards and give out exp, money and permissions if
	// appropriate
	player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesrewards + ": " + ChatColor.WHITE + rewardText);
	if (expReward > 0) {
	    player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward);
	    player.giveExp(expReward);
	}
	if (Settings.useEconomy && moneyReward > 0 && (VaultHelper.econ != null)) {
	    EconomyResponse e = VaultHelper.econ.depositPlayer(player, Settings.worldName, moneyReward);
	    if (e.transactionSuccess()) {
		player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
	    } else {
		plugin.getLogger().severe("Error giving player " + player.getUniqueId() + " challenge money:" + e.errorMessage);
		plugin.getLogger().severe("Reward was $" + moneyReward);
	    }
	}
	// Dole out permissions
	permList = getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".permissionReward", "").split(" ");
	for (final String s : permList) {
	    if (!s.isEmpty()) {
		if (!VaultHelper.checkPerm(player, s)) {
		    VaultHelper.addPerm(player, s);
		    plugin.getLogger().info("Added permission " + s + " to " + player.getName() + "");
		}
	    }
	}
	// Give items
	if (!giveItems(player, itemRewards)) {
	    return false;
	}

	// Run reward commands
	if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
	    // First time
	    List<String> commands = getChallengeConfig().getStringList("challenges.challengeList." + challenge.toLowerCase() + ".rewardcommands");
	    runCommands(player,commands);
	} else {
	    // Repeat challenge
	    List<String> commands = getChallengeConfig().getStringList("challenges.challengeList." + challenge.toLowerCase() + ".repeatrewardcommands");
	    runCommands(player,commands);
	}

	// Mark the challenge as complete
	// if (!plugin.getPlayers().checkChallenge(player.getUniqueId(),challenge)) {
	plugin.getPlayers().completeChallenge(player.getUniqueId(), challenge);
	// }
	// Call the Challenge Complete Event
	final ChallengeCompleteEvent event = new ChallengeCompleteEvent(player, challenge, permList, itemRewards, moneyReward, expReward, rewardText);
	plugin.getServer().getPluginManager().callEvent(event);
	return true;
    }

    private void runCommands(Player player, List<String> commands) {
	for (String cmd : commands) {
	    // Substitute in any references to player
	    try {
		if (!plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("[player]", player.getName()))) {
		    plugin.getLogger().severe("Problem executing challenge reward commands - skipping!");
		    plugin.getLogger().severe("Command was : " + cmd);
		}
	    } catch (Exception e) {
		plugin.getLogger().severe("Problem executing challenge reward commands - skipping!");
		plugin.getLogger().severe("Command was : " + cmd);
		plugin.getLogger().severe("Error was: " + e.getMessage());
		e.printStackTrace();
	    }
	}
    }

    private boolean giveItems(Player player, String[] itemRewards) {
	Material rewardItem;
	int rewardQty;
	// Build the item stack of rewards to give the player
	for (final String s : itemRewards) {
	    final String[] element = s.split(":");
	    if (element.length == 2) {
		try {
		    if (StringUtils.isNumeric(element[0])) {
			rewardItem = Material.getMaterial(Integer.parseInt(element[0]));
		    } else {
			rewardItem = Material.getMaterial(element[0].toUpperCase());
		    }
		    rewardQty = Integer.parseInt(element[1]);
		    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { new ItemStack(rewardItem, rewardQty) });
		    if (!leftOvers.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
		    }
		    player.getWorld().playSound(player.getLocation(), Sound.ITEM_PICKUP, 1F, 1F);
		} catch (Exception e) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorRewardProblem);
		    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " for challenge reward!");
		    String materialList = "";
		    boolean hint = false;
		    for (Material m : Material.values()) {
			materialList += m.toString() + ",";
			if (element[0].length() > 3) {
			    if (m.toString().startsWith(element[0].substring(0, 3))) {
				plugin.getLogger().severe("Did you mean " + m.toString() + "? If so, put that in challenges.yml.");
				hint = true;
			    }
			}
		    }
		    if (!hint) {
			plugin.getLogger().severe("Sorry, I have no idea what " + element[0] + " is. Pick from one of these:");
			plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
		    }
		}
	    } else if (element.length == 3) {
		try {
		    if (StringUtils.isNumeric(element[0])) {
			rewardItem = Material.getMaterial(Integer.parseInt(element[0]));
		    } else {
			rewardItem = Material.getMaterial(element[0].toUpperCase());
		    }
		    rewardQty = Integer.parseInt(element[2]);
		    // Check for POTION
		    if (rewardItem.equals(Material.POTION)) {
			// Add the effect of the potion
			final PotionEffectType potionType = PotionEffectType.getByName(element[1]);
			if (potionType == null) {
			    plugin.getLogger().severe("Reward potion effect type in config.yml challenges is unknown - skipping!");
			} else {
			    final Potion rewPotion = new Potion(PotionType.getByEffect(potionType));
			    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { rewPotion.toItemStack(rewardQty) });
			    if (!leftOvers.isEmpty()) {
				player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
			    }
			}
		    } else {
			// Normal item, not a potion
			int rewMod = Integer.parseInt(element[1]);
			final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(
				new ItemStack[] { new ItemStack(rewardItem, rewardQty, (short) rewMod) });
			if (!leftOvers.isEmpty()) {
			    player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
			}
		    }
		    player.getWorld().playSound(player.getLocation(), Sound.ITEM_PICKUP, 1F, 1F);
		} catch (Exception e) {
		    player.sendMessage(ChatColor.RED + "There was a problem giving your reward. Ask Admin to check log!");
		    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " for challenge reward!");
		    if (element[0].equalsIgnoreCase("POTION")) {
			String potionList = "";
			boolean hint = false;
			for (PotionEffectType m : PotionEffectType.values()) {
			    potionList += m.toString() + ",";
			    if (element[1].length() > 3) {
				if (m.toString().startsWith(element[1].substring(0, 3))) {
				    plugin.getLogger().severe("Did you mean " + m.toString() + "?");
				    hint = true;
				}
			    }
			}
			if (!hint) {
			    plugin.getLogger().severe("Sorry, I have no idea what potion type " + element[1] + " is. Pick from one of these:");
			    plugin.getLogger().severe(potionList.substring(0, potionList.length() - 1));
			}
		    } else {
			String materialList = "";
			boolean hint = false;
			for (Material m : Material.values()) {
			    materialList += m.toString() + ",";
			    if (m.toString().startsWith(element[0].substring(0, 3))) {
				plugin.getLogger().severe("Did you mean " + m.toString() + "? If so, put that in challenges.yml.");
				hint = true;
			    }
			}
			if (!hint) {
			    plugin.getLogger().severe("Sorry, I have no idea what " + element[0] + " is. Pick from one of these:");
			    plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
			}
		    }
		    return false;
		}
	    } else if (element.length == 6) {
		//plugin.getLogger().info("DEBUG: 6 element reward");
		// Potion format = POTION:name:level:extended:splash:qty
		try {
		    if (StringUtils.isNumeric(element[0])) {
			rewardItem = Material.getMaterial(Integer.parseInt(element[0]));
		    } else {
			rewardItem = Material.getMaterial(element[0].toUpperCase());
		    }
		    rewardQty = Integer.parseInt(element[5]);
		    // Check for POTION
		    if (rewardItem.equals(Material.POTION)) {
			// Add the effect of the potion
			final PotionType potionType = PotionType.valueOf(element[1]);
			if (potionType == null) {
			    plugin.getLogger().severe("Reward potion effect type in config.yml challenges is unknown - skipping!");
			} else {
			    final Potion rewPotion = new Potion(potionType);
			    // Add extended, splash, level etc.
			    rewPotion.setLevel(Integer.valueOf(element[2]));
			    //plugin.getLogger().info("DEBUG: level = " + Integer.valueOf(element[2]));
			    if (element[3].equalsIgnoreCase("EXTENDED")) {
				//plugin.getLogger().info("DEBUG: Extended");
				if (potionType != PotionType.INSTANT_DAMAGE && potionType != PotionType.INSTANT_HEAL) {
				    // Instant potions cannot be extended
				    rewPotion.setHasExtendedDuration(true);
				} else {
				    plugin.getLogger().warning("Reward potion is an instant potion and cannot be extended!");
				}
			    }
			    if (element[4].equalsIgnoreCase("SPLASH")) {
				//plugin.getLogger().info("DEBUG: splash");
				rewPotion.setSplash(true);
			    }
			    //plugin.getLogger().info("DEBUG: adding items!");
			    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { rewPotion.toItemStack(rewardQty) });
			    if (!leftOvers.isEmpty()) {
				player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
			    }
			}
		    }
		} catch (Exception e) {
		    player.sendMessage(ChatColor.RED + "There was a problem giving your reward. Ask Admin to check log!");
		    plugin.getLogger().severe("Problem with reward potion: " + s);
		    plugin.getLogger().severe("Format POTION:NAME:<LEVEL>:<EXTENDED/NOTEXTENDED>:<SPLASH/NOSPLASH>:QTY");
		    plugin.getLogger().severe("LEVEL, EXTENDED and SPLASH are optional");
		    plugin.getLogger().severe("LEVEL is a number");
		    plugin.getLogger().severe("Examples:");
		    plugin.getLogger().severe("POTION:STRENGTH:1:EXTENDED:SPLASH:1");
		    plugin.getLogger().severe("POTION:JUMP:2:NOTEXTENDED:NOSPLASH:1");
		    plugin.getLogger().severe("POTION:WEAKNESS::::1   -  any weakness potion");
		    plugin.getLogger().severe("Available names are:");
		    String potionNames = "";
		    for (PotionType p : PotionType.values()) {
			potionNames += p.toString() + ", ";
		    }
		    plugin.getLogger().severe(potionNames.substring(0, potionNames.length()-2));
		    return false;
		}
	    }
	}
	return true;
    }

    /**
     * Returns a color formatted string for all the challenges of a particular
     * level for a player Repeatable challenges are AQUA, completed are Dark
     * Green and yet to be done are green.
     * 
     * @param player
     * @param level
     * @return string of challenges
     */
    /*
     * private String getChallengesByLevel(final Player player, final String
     * level) {
     * List<String> levelChallengeList = challengeList.get(level);
     * String response = "";
     * for (String challenge : levelChallengeList) {
     * if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
     * if (getChallengeConfig().getBoolean("challenges.challengeList." +
     * challenge + ".repeatable", false)) {
     * response += ChatColor.AQUA + challenge + ", ";
     * } else {
     * response += ChatColor.DARK_GREEN + challenge + ", ";
     * }
     * } else {
     * response += ChatColor.GREEN + challenge + ", ";
     * }
     * }
     * // Trim the final dash
     * if (response.length() > 3) {
     * response = response.substring(0, response.length() - 2);
     * }
     * return response;
     * }
     */
    /**
     * Returns the number of challenges that must still be completed to finish a
     * level Based on how many challenges there are in a level, how many have
     * been done and how many are okay to leave undone.
     * 
     * @param player
     * @param level
     * @return
     */
    public int checkLevelCompletion(final Player player, final String level) {
	int challengesCompleted = 0;
	List<String> levelChallengeList = challengeList.get(level);
	int waiver = Settings.waiverAmount;
	if (levelChallengeList != null) {
	    for (String challenge : levelChallengeList) {
		if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
		    challengesCompleted++;
		}
	    }
	    // If the number of challenges in a level is below the waiver amount, then they all need to be done
	    if (levelChallengeList.size() <= Settings.waiverAmount) {
		waiver = 0;
	    }
	    return levelChallengeList.size() - waiver - challengesCompleted;
	}
	return 0;
    }

    /**
     * Checks if player can complete challenge
     * 
     * @param player
     * @param challenge
     * @return true if player can complete otherwise false
     */
    public boolean checkIfCanCompleteChallenge(final Player player, final String challenge) {
	// plugin.getLogger().info("DEBUG: " + player.getDisplayName() + " " +
	// challenge);
	// plugin.getLogger().info("DEBUG: 1");
	// Check if the challenge exists
	if (!plugin.getPlayers().challengeExists(player.getUniqueId(), challenge)) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesunknownChallenge);
	    return false;
	}
	// Check if this challenge level is available
	String level = getChallengeConfig().getString("challenges.challengeList." + challenge + ".level");
	// Only check if the challenge has a level, otherwise it's a free level
	if (!level.isEmpty()) {
	    if (!isLevelAvailable(player, level)) {
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesyouHaveNotUnlocked);
		return false;
	    }
	}
	// Check if the player has maxed out the challenge
	if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable")) {
	    int maxTimes = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".maxtimes", 0);
	    if (maxTimes > 0) {
		// There is a limit
		if (plugin.getPlayers().checkChallengeTimes(player.getUniqueId(), challenge) >= maxTimes) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
		    return false;
		}
	    }
	}
	// plugin.getLogger().info("DEBUG: 2");
	// Check if it is repeatable
	if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)
		&& !getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable")) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
	    return false;
	}
	// plugin.getLogger().info("DEBUG: 3");
	// If the challenge is an island type and already done, then this too is
	// not repeatable
	if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)
		&& getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("island")) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
	    return false;
	}
	// plugin.getLogger().info("DEBUG: 4");
	// Check if this is an inventory challenge
	if (getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("inventory")) {
	    // Check if the player has the required items
	    if (!hasRequired(player, challenge, "inventory")) {
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotEnoughItems);
		player.sendMessage(ChatColor.RED + getChallengeConfig().getString("challenges.challengeList." + challenge + ".description"));
		return false;
	    }
	    return true;
	}
	// plugin.getLogger().info("DEBUG: 5");
	// Check if this is an island-based challenge
	if (getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("island")) {
	    // plugin.getLogger().info("DEBUG: 6");
	    if (!plugin.getGrid().playerIsOnIsland(player)) {
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotOnIsland);
		return false;
	    }
	    if (!hasRequired(player, challenge, "island")) {
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotCloseEnough);
		player.sendMessage(ChatColor.RED + getChallengeConfig().getString("challenges.challengeList." + challenge + ".description"));
		return false;
	    }
	    // plugin.getLogger().info("DEBUG: 7");
	    return true;
	}
	// Island level check
	if (getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("level")) {
	    if (plugin.getPlayers().getIslandLevel(player.getUniqueId()) >= getChallengeConfig().getInt("challenges.challengeList." + challenge + ".requiredItems")) {
		return true;
	    }

	    player.sendMessage(ChatColor.RED
		    + plugin.myLocale(player.getUniqueId()).challengeserrorIslandLevel.replace("[level]",
			    String.valueOf(getChallengeConfig().getInt("challenges.challengeList." + challenge + ".requiredItems"))));
	    return false;
	}
	player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
	plugin.getLogger().severe(
		"The challenge " + challenge + " is of an unknown type " + getChallengeConfig().getString("challenges.challengeList." + challenge + ".type"));
	plugin.getLogger().severe("Types should be 'island', 'inventory' or 'level'");
	return false;
    }

    /**
     * Goes through all the challenges in the config.yml file and puts them into
     * the challenges list
     */
    public void populateChallengeList() {
	for (String s : Settings.challengeList) {
	    String level = getChallengeConfig().getString("challenges.challengeList." + s + ".level", "");
	    // Verify that this challenge's level is in the list of levels
	    if (Settings.challengeLevels.contains(level) || level.isEmpty()) {
		if (challengeList.containsKey(level)) {
		    challengeList.get(level).add(s);
		} else {
		    List<String> t = new ArrayList<String>();
		    t.add(s);
		    challengeList.put(level, t);
		}
	    } else {
		plugin.getServer().getLogger().severe("Level (" + level + ") for challenge " + s + " does not exist. Check challenges.yml.");
	    }
	}
    }

    /**
     * Checks if a player has enough for a challenge. Supports two types of
     * checks, inventory and island. Removes items if required.
     * 
     * @param player
     * @param challenge
     * @param type
     * @return true if the player has everything required
     */

    @SuppressWarnings("deprecation")
    public boolean hasRequired(final Player player, final String challenge, final String type) {
	// Check money
	double moneyReq = 0D;
	if (Settings.useEconomy) {
	    moneyReq = getChallengeConfig().getDouble("challenges.challengeList." + challenge + ".requiredMoney", 0D);
	    if (moneyReq > 0D) {
		if (!VaultHelper.econ.has(player, Settings.worldName, moneyReq)) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotEnoughItems);
		    player.sendMessage(ChatColor.RED + getChallengeConfig().getString("challenges.challengeList." + challenge + ".description"));
		    return false;
		}
	    }
	}
	final String[] reqList = getChallengeConfig().getString("challenges.challengeList." + challenge + ".requiredItems").split(" ");
	// The format of the requiredItems is as follows:
	// Material:Qty
	// or
	// Material:DamageModifier:Qty
	// This second one is so that items such as potions or variations on
	// standard items can be collected
	if (type.equalsIgnoreCase("inventory")) {
	    List<ItemStack> toBeRemoved = new ArrayList<ItemStack>();
	    Material reqItem;
	    int reqAmount = 0;
	    for (final String s : reqList) {
		final String[] part = s.split(":");
		// Material:Qty
		if (part.length == 2) {
		    try {
			// Correct some common mistakes
			if (part[0].equalsIgnoreCase("potato")) {
			    part[0] = "POTATO_ITEM";
			} else if (part[0].equalsIgnoreCase("brewing_stand")) {
			    part[0] = "BREWING_STAND_ITEM";
			} else if (part[0].equalsIgnoreCase("carrot")) {
			    part[0] = "CARROT_ITEM";
			} else if (part[0].equalsIgnoreCase("cauldron")) {
			    part[0] = "CAULDRON_ITEM";
			} else if (part[0].equalsIgnoreCase("skull")) {
			    part[0] = "SKULL_ITEM";
			}
			// TODO: add netherwart vs. netherstalk?
			if (StringUtils.isNumeric(part[0])) {
			    reqItem = Material.getMaterial(Integer.parseInt(part[0]));
			} else {
			    reqItem = Material.getMaterial(part[0].toUpperCase());
			}
			reqAmount = Integer.parseInt(part[1]);
			ItemStack item = new ItemStack(reqItem);
			// plugin.getLogger().info("DEBUG: required item = " +
			// reqItem.toString());
			// plugin.getLogger().info("DEBUG: item amount = " +
			// reqAmount);

			if (!player.getInventory().contains(reqItem)) {
			    return false;
			} else {
			    // check amount
			    int amount = 0;
			    // plugin.getLogger().info("DEBUG: Amount in inventory = "
			    // + player.getInventory().all(reqItem).size());
			    // Go through all the inventory and try to find
			    // enough required items
			    for (Entry<Integer, ? extends ItemStack> en : player.getInventory().all(reqItem).entrySet()) {
				// Get the item
				ItemStack i = en.getValue();
				// Map needs special handling because the
				// durability increments every time a new one is
				// made by the player
				// TODO: if there are any other items that act
				// in the same way, they need adding too...
				if (i.getDurability() == 0 || (reqItem == Material.MAP && i.getType() == Material.MAP)) {
				    // Clear any naming, or lore etc.
				    i.setItemMeta(null);
				    player.getInventory().setItem(en.getKey(), i);
				    // #1 item stack qty + amount is less than
				    // required items - take all i
				    // #2 item stack qty + amount = required
				    // item -
				    // take all
				    // #3 item stack qty + amount > req items -
				    // take
				    // portion of i
				    // amount += i.getAmount();
				    if ((amount + i.getAmount()) < reqAmount) {
					// Remove all of this item stack - clone
					// otherwise it will keep a reference to
					// the
					// original
					toBeRemoved.add(i.clone());
					amount += i.getAmount();
					// plugin.getLogger().info("DEBUG: amount is <= req Remove "
					// + i.toString() + ":" +
					// i.getDurability() + " x " +
					// i.getAmount());
				    } else if ((amount + i.getAmount()) == reqAmount) {
					// plugin.getLogger().info("DEBUG: amount is = req Remove "
					// + i.toString() + ":" +
					// i.getDurability() + " x " +
					// i.getAmount());
					toBeRemoved.add(i.clone());
					amount += i.getAmount();
					break;
				    } else {
					// Remove a portion of this item
					// plugin.getLogger().info("DEBUG: amount is > req Remove "
					// + i.toString() + ":" +
					// i.getDurability() + " x " +
					// i.getAmount());

					item.setAmount(reqAmount - amount);
					item.setDurability(i.getDurability());
					toBeRemoved.add(item);
					amount += i.getAmount();
					break;
				    }
				}
			    }
			    // plugin.getLogger().info("DEBUG: amount "+
			    // amount);
			    if (amount < reqAmount) {
				return false;
			    }
			}
		    } catch (Exception e) {
			plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
			String materialList = "";
			boolean hint = false;
			for (Material m : Material.values()) {
			    materialList += m.toString() + ",";
			    if (m.toString().contains(s.substring(0, 3).toUpperCase())) {
				plugin.getLogger().severe("Did you mean " + m.toString() + "?");
				hint = true;
			    }
			}
			if (!hint) {
			    plugin.getLogger().severe("Sorry, I have no idea what " + s + " is. Pick from one of these:");
			    plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
			} else {
			    plugin.getLogger().severe("Correct challenges.yml with the correct material.");
			}
			return false;
		    }
		} else if (part.length == 3) {
		    // This handles items with durability or potions
		    try {
			// Correct some common mistakes
			if (part[0].equalsIgnoreCase("potato")) {
			    part[0] = "POTATO_ITEM";
			} else if (part[0].equalsIgnoreCase("brewing_stand")) {
			    part[0] = "BREWING_STAND_ITEM";
			} else if (part[0].equalsIgnoreCase("carrot")) {
			    part[0] = "CARROT_ITEM";
			} else if (part[0].equalsIgnoreCase("cauldron")) {
			    part[0] = "CAULDRON_ITEM";
			} else if (part[0].equalsIgnoreCase("skull")) {
			    part[0] = "SKULL_ITEM";
			}
			if (StringUtils.isNumeric(part[0])) {
			    reqItem = Material.getMaterial(Integer.parseInt(part[0]));
			} else {
			    reqItem = Material.getMaterial(part[0].toUpperCase());
			}
			reqAmount = Integer.parseInt(part[2]);
			int reqDurability = Integer.parseInt(part[1]);
			int count = reqAmount;
			// plugin.getLogger().info("DEBUG: 3 part " +
			// reqItem.toString() + ":" + reqDurability + " x " +
			// reqAmount);
			ItemStack item = new ItemStack(reqItem);
			// Check for potions
			if (reqItem.equals(Material.POTION)) {
			    //Logger.logger(2,"DEBUG: Potion");
			    // Contains at least does not work for potions
			    ItemStack[] playerInv = player.getInventory().getContents();
			    for (ItemStack i : playerInv) {
				if (i != null && i.getType().equals(Material.POTION)) {
				    // plugin.getLogger().info("DEBUG: Potion found, durability = "+
				    // i.getDurability());

				    // Potion type was given by number
				    // Only included for backward compatibility
				    if (i.getDurability() == reqDurability) {
					item = i.clone();
					if (item.getAmount() > reqAmount) {
					    item.setAmount(reqAmount);
					}
					count = count - item.getAmount();
					// plugin.getLogger().info("Matched! count = "
					// + count);
					// If the item stack has more in it than
					// required, just take the minimum
					// plugin.getLogger().info("DEBUG: Found "
					// + item.toString() + ":" +
					// item.getDurability() + " x " +
					// item.getAmount());
					toBeRemoved.add(item);
				    } 
				}
				if (count == 0) {
				    break;
				}
			    }
			    if (count > 0) {
				return false;
			    }
			    // They have enough
			} else {
			    // Item
			    item.setDurability((short) reqDurability);
			    // plugin.getLogger().info("DEBUG: item with durability "
			    // + item.toString());
			    // item.setAmount(reqAmount);
			    /*
			     * if (!player.getInventory().containsAtLeast(item,
			     * reqAmount)) {
			     * plugin.getLogger().info(
			     * "DEBUG: item with durability not enough");
			     * return false;
			     * }
			     */
			    // check amount
			    int amount = 0;
			    // Go through all the inventory and try to find
			    // enough required items
			    for (Entry<Integer, ? extends ItemStack> en : player.getInventory().all(reqItem).entrySet()) {
				// Get the item
				ItemStack i = en.getValue();
				if (i.getDurability() == reqDurability) {
				    // Clear any naming, or lore etc.
				    i.setItemMeta(null);
				    player.getInventory().setItem(en.getKey(), i);
				    // #1 item stack qty + amount is less than
				    // required items - take all i
				    // #2 item stack qty + amount = required
				    // item -
				    // take all
				    // #3 item stack qty + amount > req items -
				    // take
				    // portion of i
				    // amount += i.getAmount();
				    if ((amount + i.getAmount()) < reqAmount) {
					// Remove all of this item stack - clone
					// otherwise it will keep a reference to
					// the
					// original
					toBeRemoved.add(i.clone());
					amount += i.getAmount();
					// plugin.getLogger().info("DEBUG: amount is <= req Remove "
					// + i.toString() + ":" +
					// i.getDurability()
					// + " x " + i.getAmount());
				    } else if ((amount + i.getAmount()) == reqAmount) {
					toBeRemoved.add(i.clone());
					amount += i.getAmount();
					break;
				    } else {
					// Remove a portion of this item
					// plugin.getLogger().info("DEBUG: amount is > req Remove "
					// + i.toString() + ":" +
					// i.getDurability()
					// + " x " + i.getAmount());

					item.setAmount(reqAmount - amount);
					item.setDurability(i.getDurability());
					toBeRemoved.add(item);
					amount += i.getAmount();
					break;
				    }
				}
			    }
			    // plugin.getLogger().info("DEBUG: amount is " +
			    // amount);
			    // plugin.getLogger().info("DEBUG: req amount is " +
			    // reqAmount);
			    if (amount < reqAmount) {
				return false;
			    }
			}
			// plugin.getLogger().info("DEBUG: before set amount " +
			// item.toString() + ":" + item.getDurability() + " x "
			// + item.getAmount());
			// item.setAmount(reqAmount);
			// plugin.getLogger().info("DEBUG: after set amount " +
			// item.toString() + ":" + item.getDurability() + " x "
			// + item.getAmount());
			// toBeRemoved.add(item);
		    } catch (Exception e) {
			plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
			if (part[0].equalsIgnoreCase("POTION")) {
			    plugin.getLogger().severe("Format POTION:TYPE:QTY where TYPE is the number of the following:");
			    for (PotionType p : PotionType.values()) {
				plugin.getLogger().info(p.toString() + ":" + p.getDamageValue());
			    }
			} else {
			    String materialList = "";
			    boolean hint = false;
			    for (Material m : Material.values()) {
				materialList += m.toString() + ",";
				if (m.toString().contains(s.substring(0, 3))) {
				    plugin.getLogger().severe("Did you mean " + m.toString() + "?");
				    hint = true;
				}
			    }
			    if (!hint) {
				plugin.getLogger().severe("Sorry, I have no idea what " + s + " is. Pick from one of these:");
				plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
			    } else {
				plugin.getLogger().severe("Correct challenges.yml with the correct material.");
			    }
			    return false;
			}
			return false;
		    }
		} else if (part.length == 6) {
		    //plugin.getLogger().info("DEBUG:6 part potion check!");
		    // POTION:Name:Level:Extended:Splash:Qty
		    try {
			if (StringUtils.isNumeric(part[0])) {
			    reqItem = Material.getMaterial(Integer.parseInt(part[0]));
			} else {
			    reqItem = Material.getMaterial(part[0].toUpperCase());
			}
			reqAmount = Integer.parseInt(part[5]);
			ItemStack item = new ItemStack(reqItem);
			int count = reqAmount;
			// Compare
			if (reqItem == Material.POTION) {
			    //plugin.getLogger().info("DEBUG: required item is a potion");
			    ItemStack[] playerInv = player.getInventory().getContents();
			    for (ItemStack i : playerInv) {
				if (i != null && i.getType().equals(Material.POTION)) {
				    //plugin.getLogger().info("DEBUG: Item in inventory = " + i.toString());
				    Potion p = fromDamage(i.getDurability());
				    // Check type
				    //Potion p = Potion.fromItemStack(i);

				    //plugin.getLogger().info("DEBUG: " + p.getType() + ":" + p.getLevel() + ":" + p.hasExtendedDuration() + ":" + p.isSplash() );
				    // Check type
				    PotionType typeCheck = PotionType.valueOf(part[1].toUpperCase());
				    //plugin.getLogger().info("DEBUG: potion type is:" + p.getType().toString() + " desired is:" + part[1].toUpperCase());
				    //if (p.getType().toString().equalsIgnoreCase(part[1].toUpperCase())) {
				    if (p.getType().equals(typeCheck)) {
					//plugin.getLogger().info("DEBUG: potion type is the same");
					// Check level
					//plugin.getLogger().info("DEBUG: check level " + part[2] + " = " + p.getLevel());
					if (part[2].isEmpty() || p.getLevel() == Integer.valueOf(part[2])) {
					    //plugin.getLogger().info("DEBUG: level is ok ");
					    //plugin.getLogger().info("DEBUG: check splash = " + part[4] + " = " + p.isSplash());
					    if (part[4].isEmpty() || (p.isSplash() && part[4].equalsIgnoreCase("SPLASH")) 
						    || (!p.isSplash() && part[4].equalsIgnoreCase("NOSPLASH"))) {
						//plugin.getLogger().info("DEBUG: splash is ok = " + part[4] + " = " + p.isSplash());
						//plugin.getLogger().info("DEBUG: check extended = " + part[4] + " = " + p.hasExtendedDuration());
						if (part[3].isEmpty() || (p.hasExtendedDuration() && part[3].equalsIgnoreCase("EXTENDED"))
							|| (!p.hasExtendedDuration() && part[3].equalsIgnoreCase("NOTEXTENDED"))) {
						    //plugin.getLogger().info("DEBUG: Everything is matching");
						    item = i.clone();
						    if (item.getAmount() > reqAmount) {
							item.setAmount(reqAmount);
						    }
						    count = count - item.getAmount();
						    toBeRemoved.add(item);
						}
					    }
					}
				    }
				}
				if (count <= 0) {
				    break;
				}
			    }
			    if (count > 0) {
				return false;
			    }
			} else {
			    plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
			}
		    } catch (Exception e) {
			plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
			//e.printStackTrace();
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
			if (part[0].equalsIgnoreCase("POTION")) {
			    plugin.getLogger().severe("Format POTION:NAME:<LEVEL>:<EXTENDED/NOTEXTENDED>:<SPLASH/NOSPLASH>:QTY");
			    plugin.getLogger().severe("LEVEL, EXTENDED and SPLASH are optional");
			    plugin.getLogger().severe("LEVEL is a number");
			    plugin.getLogger().severe("Examples:");
			    plugin.getLogger().severe("POTION:STRENGTH:1:EXTENDED:SPLASH:1");
			    plugin.getLogger().severe("POTION:JUMP:2:NOTEXTENDED:NOSPLASH:1");
			    plugin.getLogger().severe("POTION:WEAKNESS::::1   -  any weakness potion");
			    plugin.getLogger().severe("Available names are:");
			    String potionNames = "";
			    for (PotionType p : PotionType.values()) {
				potionNames += p.toString() + ", ";
			    }
			    plugin.getLogger().severe(potionNames.substring(0, potionNames.length()-2));
			} 
			return false;
		    }

		}

	    } 
	    // Build up the items in the inventory and remove them if they are
	    // all there.

	    if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".takeItems")) {
		// checkChallengeItems(player, challenge);
		// int qty = 0;
		// plugin.getLogger().info("DEBUG: Removing items");
		for (ItemStack i : toBeRemoved) {
		    // qty += i.getAmount();
		    // plugin.getLogger().info("DEBUG: Remove " + i.toString() +
		    // "::" + i.getDurability() + " x " + i.getAmount());
		    HashMap<Integer, ItemStack> leftOver = player.getInventory().removeItem(i);
		    if (!leftOver.isEmpty()) {
			plugin.getLogger().warning(
				"Exploit? Could not remove the following in challenge " + challenge + " for player " + player.getName() + ":");
			for (ItemStack left : leftOver.values()) {
			    plugin.getLogger().info(left.toString());
			}
			return false;
		    }
		}
		// Remove money
		if (moneyReq > 0D) {
		    EconomyResponse er = VaultHelper.econ.withdrawPlayer(player, moneyReq);
		    if (!er.transactionSuccess()) {
			plugin.getLogger().warning(
				"Exploit? Could not remove " + VaultHelper.econ.format(moneyReq) + " from " + player.getName()
				+ " in challenge " + challenge);
			plugin.getLogger().warning("Player's balance is " + VaultHelper.econ.format(VaultHelper.econ.getBalance(player)));
		    }
		}
		// plugin.getLogger().info("DEBUG: total = " + qty);
	    }
	    return true;
	}
	if (type.equalsIgnoreCase("island")) {
	    final HashMap<Material, Integer> neededItem = new HashMap<Material, Integer>();
	    final HashMap<EntityType, Integer> neededEntities = new HashMap<EntityType, Integer>();
	    for (int i = 0; i < reqList.length; i++) {
		final String[] sPart = reqList[i].split(":");
		// Parse the qty required first
		try {
		    final int qty = Integer.parseInt(sPart[1]);
		    // Find out if the needed item is a Material or an Entity
		    boolean isEntity = false;
		    for (EntityType entityType : EntityType.values()) {
			if (entityType.toString().equalsIgnoreCase(sPart[0])) {
			    isEntity = true;
			    break;
			}
		    }
		    if (isEntity) {
			// plugin.getLogger().info("DEBUG: Item " +
			// sPart[0].toUpperCase() + " is an entity");
			EntityType entityType = EntityType.valueOf(sPart[0].toUpperCase());
			if (entityType != null) {
			    neededEntities.put(entityType, qty);
			    // plugin.getLogger().info("DEBUG: Needed entity is "
			    // + Integer.parseInt(sPart[1]) + " x " +
			    // EntityType.valueOf(sPart[0].toUpperCase()).toString());
			}
		    } else {	
			Material item;
			if (StringUtils.isNumeric(sPart[0])) {
			    item = Material.getMaterial(Integer.parseInt(sPart[0]));
			} else {
			    item = Material.getMaterial(sPart[0].toUpperCase());
			}
			if (item != null) {
			    neededItem.put(item, qty);
			    // plugin.getLogger().info("DEBUG: Needed item is "
			    // + Integer.parseInt(sPart[1]) + " x " +
			    // Material.getMaterial(sPart[0]).toString());

			} else {
			    plugin.getLogger().warning("Problem parsing required item for challenge " + challenge + " in challenges.yml!");
			    return false;
			}
		    }
		} catch (Exception intEx) {
		    plugin.getLogger().warning("Problem parsing required items for challenge " + challenge + " in challenges.yml - skipping");
		    return false;
		}

	    }
	    // We now have two sets of required items or entities
	    // Check the items first
	    final Location l = player.getLocation();
	    // if (!neededItem.isEmpty()) {
	    final int px = l.getBlockX();
	    final int py = l.getBlockY();
	    final int pz = l.getBlockZ();
	    for (int x = -10; x <= 10; x++) {
		for (int y = -10; y <= 10; y++) {
		    for (int z = -10; z <= 10; z++) {
			final Material b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock().getType();
			if (neededItem.containsKey(b)) {
			    if (neededItem.get(b) == 1) {
				neededItem.remove(b);
			    } else {
				// Reduce the require amount by 1
				neededItem.put(b, neededItem.get(b) - 1);
			    }
			}
		    }
		}
	    }
	    // }
	    // Check if all the needed items have been amassed
	    if (!neededItem.isEmpty()) {
		// plugin.getLogger().info("DEBUG: Insufficient items around");
		for (Material missing : neededItem.keySet()) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorYouAreMissing + " " + neededItem.get(missing) + " x "
			    + Util.prettifyText(missing.toString()));
		}
		return false;
	    } else {
		// plugin.getLogger().info("DEBUG: Items are there");
		// Check for needed entities
		for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
		    // plugin.getLogger().info("DEBUG: Entity found:" +
		    // entity.getType().toString());
		    if (neededEntities.containsKey(entity.getType())) {
			// plugin.getLogger().info("DEBUG: Entity in list");
			if (neededEntities.get(entity.getType()) == 1) {
			    neededEntities.remove(entity.getType());
			    // plugin.getLogger().info("DEBUG: Entity qty satisfied");
			} else {
			    neededEntities.put(entity.getType(), neededEntities.get(entity.getType()) - 1);
			    // plugin.getLogger().info("DEBUG: Entity qty reduced by 1");
			}
		    } else {
			// plugin.getLogger().info("DEBUG: Entity not in list");
		    }
		}
		if (neededEntities.isEmpty()) {
		    return true;
		} else {
		    for (EntityType missing : neededEntities.keySet()) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorYouAreMissing + " " + neededEntities.get(missing) + " x "
				+ Util.prettifyText(missing.toString()));
		    }
		    return false;
		}
	    }
	}

	return true;
    }

    /**
     * Returns true if the level is unlocked and false if not
     * 
     * @param player
     * @param level
     * @return true/false
     */
    public boolean isLevelAvailable(final Player player, final String level) {
	if (challengeList.size() < 2) {
	    return true;
	}
	for (int i = 0; i < Settings.challengeLevels.size(); i++) {
	    if (Settings.challengeLevels.get(i).equalsIgnoreCase(level)) {
		if (i == 0) {
		    return true;
		}

		if (checkLevelCompletion(player, Settings.challengeLevels.get(i - 1)) <= 0) {
		    return true;
		}
	    }

	}

	return false;
    }

    /**
     * Dynamically creates an inventory of challenges for the player
     * 
     * @param player
     * @return inventory
     */
    public Inventory challengePanel(Player player) {
	String maxLevel = "";
	for (String level : Settings.challengeLevels) {
	    if (checkLevelCompletion(player, level) > 0) {
		maxLevel = level;
		break;
	    }
	}
	return challengePanel(player, maxLevel);
    }

    /**
     * Dynamically creates an inventory of challenges for the player showing the
     * level
     * 
     * @param player
     * @param level
     * @return inventory
     */
    public Inventory challengePanel(Player player, String level) {
	//plugin.getLogger().info("DEBUG: level requested = " + level);
	// Create the challenges control panel
	// New panel map
	List<CPItem> cp = new ArrayList<CPItem>();

	// Do some checking
	// plugin.getLogger().severe("DEBUG: Opening level " + level);

	if (level.isEmpty() && !challengeList.containsKey("")) {
	    if (!Settings.challengeLevels.isEmpty()) {
		level = Settings.challengeLevels.get(0);
	    } else {
		// We have no challenges!
		plugin.getLogger().severe("There are no challenges to show!");
		Inventory error = Bukkit.createInventory(null, 9, plugin.myLocale(player.getUniqueId()).challengesguiTitle);
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
		return error;
	    }
	}
	// Only show a control panel for the level requested.
	for (String challengeName : challengeList.get(level)) {
	    CPItem item = createItem(challengeName, player);
	    if (item != null) {
		cp.add(item);
	    }
	}
	// Add the missing levels so player can navigate to them
	int levelDone = 0;
	for (; levelDone < Settings.challengeLevels.size(); levelDone++) {
	    if (checkLevelCompletion(player, Settings.challengeLevels.get(levelDone)) > 0) {
		break;
	    }
	}
	//plugin.getLogger().info("DEBUG: level done = " + levelDone);
	for (int i = 0; i < Settings.challengeLevels.size(); i++) {
	    if (!level.equalsIgnoreCase(Settings.challengeLevels.get(i))) {
		// Add a navigation book
		List<String> lore = new ArrayList<String>();
		if (i <= levelDone) {
		    CPItem item = new CPItem(Material.BOOK_AND_QUILL, ChatColor.GOLD + Settings.challengeLevels.get(i), null, null);
		    lore = Util.chop(ChatColor.WHITE, plugin.myLocale(player.getUniqueId()).challengesNavigation.replace("[level]", Settings.challengeLevels.get(i)), 25);
		    item.setNextSection(Settings.challengeLevels.get(i));
		    item.setLore(lore);
		    cp.add(item);
		} else {
		    // Hint at what is to come
		    CPItem item = new CPItem(Material.BOOK, ChatColor.GOLD + Settings.challengeLevels.get(i), null, null);
		    // Add the level
		    int toDo = checkLevelCompletion(player, Settings.challengeLevels.get(i - 1));
		    lore = Util.chop(
			    ChatColor.WHITE,
			    plugin.myLocale(player.getUniqueId()).challengestoComplete.replace("[challengesToDo]", String.valueOf(toDo)).replace("[thisLevel]",
				    Settings.challengeLevels.get(i - 1)), 25);
		    item.setLore(lore);
		    cp.add(item);
		}
	    }
	}
	// Add the free challenges if not already shown (which can happen if all of the challenges are done!)
	if (!level.equals("") && challengeList.containsKey("")) {
	    for (String freeChallenges: challengeList.get("")) {
		CPItem item = createItem(freeChallenges, player);
		if (item != null) {
		    cp.add(item);
		} 
	    }
	}
	// Create the panel
	if (cp.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = cp.size() + 8;
	    size -= (size % 9);
	    Inventory newPanel = Bukkit.createInventory(null, size, plugin.myLocale(player.getUniqueId()).challengesguiTitle);
	    // Store the panel details for retrieval later
	    playerChallengeGUI.put(player.getUniqueId(), cp);
	    // Fill the inventory and return
	    for (CPItem i : cp) {
		newPanel.addItem(i.getItem());
	    }
	    return newPanel;
	}
	return null;
    }

    /**
     * Creates an inventory item for the challenge
     * @param challengeName
     * @param player
     * @return Control Panel item
     */
    @SuppressWarnings("deprecation")
    private CPItem createItem(String challengeName, Player player) {
	CPItem item = null;
	// Get the icon
	ItemStack icon = null;
	String iconName = getChallengeConfig().getString("challenges.challengeList." + challengeName + ".icon", "");
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
		    }
		    if (StringUtils.isNumeric(iconName)) {
			icon = new ItemStack(Integer.parseInt(iconName));
		    } else {
			icon = new ItemStack(Material.valueOf(iconName));
		    }
		} else if (split.length == 2) {
		    if (StringUtils.isNumeric(split[0])) {
			icon = new ItemStack(Integer.parseInt(split[0]));
		    } else {
			icon = new ItemStack(Material.valueOf(split[0]));
		    }
		    icon.setDurability(Integer.valueOf(split[1]).shortValue());
		}
	    } catch (Exception e) {
		// Icon was not well formatted
		plugin.getLogger().warning("Error in challenges.yml - icon format is incorrect for " + challengeName + ":" + iconName);
		plugin.getLogger().warning("Format should be 'icon: MaterialType:Damage' where Damage is optional");
	    }
	}
	if (icon == null || icon.equals(Material.AIR)) {
	    icon = new ItemStack(Material.PAPER);
	}
	String description = ChatColor.GREEN
		+ getChallengeConfig().getString("challenges.challengeList." + challengeName + ".friendlyname",
			challengeName.substring(0, 1).toUpperCase() + challengeName.substring(1));

	// Check if completed or not
	boolean complete = false;
	if (Settings.addCompletedGlow && plugin.getPlayers().checkChallenge(player.getUniqueId(), challengeName)) {
	    // Complete! Make the icon glow
	    ItemMeta im = icon.getItemMeta();
	    im.addEnchant(Enchantment.ARROW_DAMAGE, 0, true);
	    icon.setItemMeta(im);
	    icon.removeEnchantment(Enchantment.ARROW_DAMAGE);
	    complete = true;
	}
	boolean repeatable = false;
	if (getChallengeConfig().getBoolean("challenges.challengeList." + challengeName + ".repeatable", false)) {
	    // Repeatable
	    repeatable = true;
	}
	// Only show this challenge if it is not done or repeatable if the
	// setting Settings.removeCompleteOntimeChallenges
	if (!complete || ((complete && repeatable) || !Settings.removeCompleteOntimeChallenges)) {
	    // Store the challenge panel item and the command that will be
	    // called if it is activated.
	    item = new CPItem(icon, description, Settings.CHALLENGECOMMAND + " c " + challengeName, null);
	    // Get the challenge description, that changes depending on
	    // whether the challenge is complete or not.
	    List<String> lore = challengeDescription(challengeName, player);
	    item.setLore(lore);
	}
	return item;
    }

    public List<CPItem> getCP(Player player) {
	return playerChallengeGUI.get(player.getUniqueId());
    }

    /**
     * Creates the challenge description for the "item" in the inventory
     * 
     * @param challenge
     * @param player
     * @return
     */
    private List<String> challengeDescription(String challenge, Player player) {
	List<String> result = new ArrayList<String>();
	final int length = 25;
	// plugin.getLogger().info("DEBUG: challenge is '"+challenge+"'");
	// plugin.getLogger().info("challenges.challengeList." + challenge +
	// ".level");
	// plugin.getLogger().info(getChallengeConfig().getString("challenges.challengeList."
	// + challenge + ".level"));
	String level = getChallengeConfig().getString("challenges.challengeList." + challenge + ".level", "");
	if (!level.isEmpty()) {
	    result.addAll(Util.chop(ChatColor.WHITE, plugin.myLocale(player.getUniqueId()).challengeslevel + ": " + level, length));
	}
	// Check if completed or not
	boolean complete = false;
	int maxTimes = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".maxtimes", 0);
	int doneTimes = plugin.getPlayers().checkChallengeTimes(player.getUniqueId(), challenge);
	if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
	    // Complete!
	    // result.add(ChatColor.AQUA + plugin.myLocale(player.getUniqueId()).challengescomplete);
	    complete = true;
	}
	boolean repeatable = false;
	if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable", false)) {
	    repeatable = true;
	}

	if (repeatable) {
	    if (maxTimes == 0) {
		if (complete) {
		    result.add(ChatColor.AQUA + plugin.myLocale(player.getUniqueId()).challengescomplete);
		}
	    } else {
		// Check if the player has maxed out the challenge
		if (doneTimes < maxTimes) {
		    result.add(plugin.myLocale(player.getUniqueId()).challengescompletedtimes.replace("[donetimes]", String.valueOf(doneTimes))
			    .replace("[maxtimes]", String.valueOf(maxTimes)));
		} else {
		    repeatable = false;
		    result.add(plugin.myLocale(player.getUniqueId()).challengesmaxreached.replace("[donetimes]", String.valueOf(doneTimes)).replace("[maxtimes]", String.valueOf(maxTimes)));
		}
	    }
	} else if (complete) {
	    result.add(ChatColor.AQUA + plugin.myLocale(player.getUniqueId()).challengescomplete);
	}

	final String type = getChallengeConfig().getString("challenges.challengeList." + challenge + ".type", "").toLowerCase();
	if (!complete || (complete && repeatable)) {
	    result.addAll(Util.chop(ChatColor.GOLD, getChallengeConfig().getString("challenges.challengeList." + challenge + ".description", ""), length));
	    if (type.equals("inventory")) {
		if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge.toLowerCase() + ".takeItems")) {
		    result.addAll(Util.chop(ChatColor.RED, plugin.myLocale(player.getUniqueId()).challengesitemTakeWarning, length));
		}
	    } else if (type.equals("island")) {
		result.addAll(Util.chop(ChatColor.RED, plugin.myLocale(player.getUniqueId()).challengeserrorItemsNotThere, length));
	    }
	}
	if (complete && (!type.equals("inventory") || !repeatable)) {
	    result.addAll(Util.chop(ChatColor.RED, plugin.myLocale(player.getUniqueId()).challengesnotRepeatable, length));
	    return result;
	}
	double moneyReward = 0;
	int expReward = 0;
	String rewardText = "";
	if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
	    // First time
	    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0);
	    rewardText = ChatColor.translateAlternateColorCodes('&',
		    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!"));
	    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".xpReward", 0);
	    result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesfirstTimeRewards, length));
	} else {
	    // Repeat challenge
	    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0);
	    rewardText = ChatColor.translateAlternateColorCodes('&',
		    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!"));
	    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
	    result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesrepeatRewards, length));

	}
	result.addAll(Util.chop(ChatColor.WHITE, rewardText, length));
	if (expReward > 0) {
	    result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward, length));
	}
	if (Settings.useEconomy && moneyReward > 0) {
	    result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward), length));
	}
	return result;
    }

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

	InputStream defConfigStream = plugin.getResource("challenges.yml");
	if (defConfigStream != null) {
	    @SuppressWarnings("deprecation")
	    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	    challengeFile.setDefaults(defConfig);
	}
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

    /**
     * Returns a potion based on a damage value
     * @param damage
     * @return
     */
    @SuppressWarnings("deprecation")
    public static Potion fromDamage(int damage) {
	PotionType type = PotionType.getByDamageValue(damage & POTION_BIT);
	Potion potion;
	if (type == null || (type == PotionType.WATER && damage != 0)) {
	    potion = new Potion(damage & NAME_BIT);
	} else {
	    int level = (damage & TIER_BIT) >> TIER_SHIFT;
	level++;
	potion = new Potion(type, level);
	}
	if ((damage & SPLASH_BIT) > 0) {
	    potion = potion.splash();
	}
	if (type != PotionType.INSTANT_DAMAGE && type != PotionType.INSTANT_HEAL) {
	    if ((damage & EXTENDED_BIT) > 0) {
		potion = potion.extend();
	    }
	}
	return potion;
    }

    /**
     * Gets a list of all challenges in existence.
     */
    public List<String> getAllChallenges() {
	List<String> returned = new ArrayList<String>();
	for (List<String> challenges : challengeList.values()) {
	    returned.addAll(challenges);
	}
	return returned;
    }

    /**
     * Creates a list of challenges that the specified player is able to complete.
     * @param player
     * @return
     */
    public List<String> getAvailableChallenges(Player player) {
	List<String> returned = new ArrayList<String>();
	for (Map.Entry<String, List<String>> e : challengeList.entrySet())
	    if (isLevelAvailable(player, e.getKey())) {
		returned.addAll(e.getValue());
	    }
	return returned;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
	if (!(sender instanceof Player)) {
	    return new ArrayList<String>();
	}
	Player player = (Player) sender;

	List<String> options = new ArrayList<String>();

	switch (args.length) {
	case 0: 
	case 1:
	    options.add("complete");
	    //Fall through
	case 2:
	    options.addAll(getAvailableChallenges(player));
	    break;
	}

	return Util.tabLimit(options, args.length != 0 ? args[args.length - 1] : "");
    }
}
