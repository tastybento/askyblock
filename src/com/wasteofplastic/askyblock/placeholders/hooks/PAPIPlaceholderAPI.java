package com.wasteofplastic.askyblock.placeholders.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.placeholders.PlaceholderAPI;
import com.wasteofplastic.askyblock.placeholders.PlaceholderHandler;
import com.wasteofplastic.askyblock.placeholders.Placeholders;

import me.clip.placeholderapi.PlaceholderHook;

/**
 * PlaceholderAPI (plugin) hook
 * 
 * @author Poslovitch
 */
public class PAPIPlaceholderAPI implements PlaceholderAPI{

	@Override
	public boolean register(ASkyBlock plugin) {
		if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			me.clip.placeholderapi.PlaceholderAPI.registerPlaceholderHook(plugin, new PlaceholderHook() {
				
				@Override
				public String onPlaceholderRequest(Player player, String identifier) {
					if(Placeholders.getPlaceholders().contains(identifier)){
            			return PlaceholderHandler.replacePlaceholders(player, identifier);
            		}
					return null;
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public void unregister(ASkyBlock plugin) {
		if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			me.clip.placeholderapi.PlaceholderAPI.unregisterPlaceholderHook(plugin);
		}
	}

	@Override
	public String replacePlaceholders(Player player, String message) {
		return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
	}

}
