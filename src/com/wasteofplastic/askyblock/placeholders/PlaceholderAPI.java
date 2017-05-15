package com.wasteofplastic.askyblock.placeholders;

import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;

/**
 * Simple interface for every PlaceholderAPI.
 * 
 * @author Poslovitch
 */
public interface PlaceholderAPI {
    
    /**
     * @return name of the placeholder plugin
     */
    String getName();
	/**
	 * Register a placeholder
	 * @param plugin
	 * @return true if registered
	 */
	boolean register(ASkyBlock plugin);
	
	/**
	 * Unregister a placeholder
	 * @param plugin
	 */
	void unregister(ASkyBlock plugin);
	
	/**
	 * Replace message for player
	 * @param player
	 * @param message
	 * @return Updated message
	 */
	String replacePlaceholders(Player player, String message);
}
