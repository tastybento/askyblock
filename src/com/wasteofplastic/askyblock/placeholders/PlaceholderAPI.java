package com.wasteofplastic.askyblock.placeholders;

import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;

/**
 * Simple interface for every PlaceholderAPI.
 * 
 * @author Poslovitch
 */
public interface PlaceholderAPI {
	boolean register(ASkyBlock plugin);
	void unregister(ASkyBlock plugin);
	String replacePlaceholders(Player player, String message);
}
