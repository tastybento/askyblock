package com.wasteofplastic.askyblock.placeholders;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.placeholders.hooks.DeluxeChatPlaceholderAPI;
import com.wasteofplastic.askyblock.placeholders.hooks.MVdWPlaceholderAPI;
import com.wasteofplastic.askyblock.placeholders.hooks.PAPIPlaceholderAPI;

/**
 * Handles hooks with other PlaceholderAPIs.
 * 
 * @author Poslovitch
 */
public class PlaceholderHandler {
	private static final PlaceholderAPI[] HOOKS = {
			new InternalPlaceholderAPI(),
			new PAPIPlaceholderAPI(),
			new DeluxeChatPlaceholderAPI(),
			new MVdWPlaceholderAPI()
	};
	
	private static List<PlaceholderAPI> apis = new ArrayList<>();
	
	public static void register(ASkyBlock plugin){
		new Placeholders(plugin);
		
		for(PlaceholderAPI hook : HOOKS){
			if(hook.register(plugin)){
				plugin.getLogger().info("Hooked into " + hook.getClass().getName());
				apis.add(hook);
			} else {
				plugin.getLogger().info("Failed to hook into " + hook.getClass().getName());
			}
		}
	}
	
	public static void unregister(ASkyBlock plugin){
		for(PlaceholderAPI api : apis){
			api.unregister(plugin);
			apis.remove(api);
		}
	}
	
	public static String replacePlaceholders(Player player, String message){
		if(message == null || message.isEmpty()) return "";
		
		for(PlaceholderAPI api : apis){
			message = api.replacePlaceholders(player, message);
		}
		
		return message;
	}
}
