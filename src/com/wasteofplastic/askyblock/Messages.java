package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.util.Util;

/**
 * Handles offline messaging to players and teams
 * 
 * @author tastybento
 * 
 */
public class Messages {
    private static ASkyBlock plugin = ASkyBlock.getPlugin();
    // Offline Messages
    private static HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private static YamlConfiguration messageStore;

    /**
     * Returns what messages are waiting for the player or null if none
     * 
     * @param playerUUID
     * @return
     */
    public static List<String> getMessages(UUID playerUUID) {
	List<String> playerMessages = messages.get(playerUUID);
	return playerMessages;
    }

    /**
     * Clears any messages for player
     * 
     * @param playerUUID
     */
    public static void clearMessages(UUID playerUUID) {
	messages.remove(playerUUID);
    }

    public static void saveMessages() {
	if (messageStore == null) {
	    return;
	}
	plugin.getLogger().info("Saving offline messages...");
	try {
	    // Convert to a serialized string
	    final HashMap<String, Object> offlineMessages = new HashMap<String, Object>();
	    for (UUID p : messages.keySet()) {
		offlineMessages.put(p.toString(), messages.get(p));
	    }
	    // Convert to YAML
	    messageStore.set("messages", offlineMessages);
	    Util.saveYamlFile(messageStore, "messages.yml");
	    return;
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
    }

    public static boolean loadMessages() {
	plugin.getLogger().info("Loading offline messages...");
	try {
	    messageStore = Util.loadYamlFile("messages.yml");
	    if (messageStore.getConfigurationSection("messages") == null) {
		messageStore.createSection("messages"); // This is only used to
							// create
	    }
	    HashMap<String, Object> temp = (HashMap<String, Object>) messageStore.getConfigurationSection("messages").getValues(true);
	    for (String s : temp.keySet()) {
		List<String> messageList = messageStore.getStringList("messages." + s);
		if (!messageList.isEmpty()) {
		    messages.put(UUID.fromString(s), messageList);
		}
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    /**
     * Provides the messages for the player
     * 
     * @param playerUUID
     * @return List of messages
     */
    public static List<String> get(UUID playerUUID) {
	return messages.get(playerUUID);
    }

    /**
     * Stores a message for player
     * 
     * @param playerUUID
     * @param playerMessages
     */
    public static void put(UUID playerUUID, List<String> playerMessages) {
	messages.put(playerUUID, playerMessages);

    }

    /**
     * Sends a message to every player in the team that is offline
     * 
     * @param playerUUID
     * @param message
     */
    public static void tellOfflineTeam(UUID playerUUID, String message) {
	// getLogger().info("DEBUG: tell offline team called");
	if (!plugin.getPlayers().inTeam(playerUUID)) {
	    // getLogger().info("DEBUG: player is not in a team");
	    return;
	}
	UUID teamLeader = plugin.getPlayers().getTeamLeader(playerUUID);
	List<UUID> teamMembers = plugin.getPlayers().getMembers(teamLeader);
	for (UUID member : teamMembers) {
	    // getLogger().info("DEBUG: trying UUID " + member.toString());
	    if (plugin.getServer().getPlayer(member) == null) {
		// Offline player
		setMessage(member, message);
	    }
	}
    }

    /**
     * Tells all online team members something happened
     * 
     * @param playerUUID
     * @param message
     */
    public static void tellTeam(UUID playerUUID, String message) {
	// getLogger().info("DEBUG: tell offline team called");
	if (!plugin.getPlayers().inTeam(playerUUID)) {
	    // getLogger().info("DEBUG: player is not in a team");
	    return;
	}
	UUID teamLeader = plugin.getPlayers().getTeamLeader(playerUUID);
	List<UUID> teamMembers = plugin.getPlayers().getMembers(teamLeader);
	for (UUID member : teamMembers) {
	    // getLogger().info("DEBUG: trying UUID " + member.toString());
	    if (!member.equals(playerUUID) && plugin.getServer().getPlayer(member) != null) {
		// Online player
		plugin.getServer().getPlayer(member).sendMessage(message);
	    }
	}
    }

    /**
     * Sets a message for the player to receive next time they login
     * 
     * @param player
     * @param message
     * @return true if player is offline, false if online
     */
    public static boolean setMessage(UUID playerUUID, String message) {
	// getLogger().info("DEBUG: received message - " + message);
	Player player = plugin.getServer().getPlayer(playerUUID);
	// Check if player is online
	if (player != null) {
	    if (player.isOnline()) {
		// player.sendMessage(message);
		return false;
	    }
	}
	// Player is offline so store the message
	// getLogger().info("DEBUG: player is offline - storing message");
	List<String> playerMessages = Messages.get(playerUUID);
	if (playerMessages != null) {
	    playerMessages.add(message);
	} else {
	    playerMessages = new ArrayList<String>(Arrays.asList(message));
	}
	Messages.put(playerUUID, playerMessages);
	return true;
    }

}
