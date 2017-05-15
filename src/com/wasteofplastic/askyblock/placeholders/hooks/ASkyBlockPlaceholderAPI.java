package com.wasteofplastic.askyblock.placeholders.hooks;

import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.placeholders.PlaceholderAPI;
import com.wasteofplastic.askyblock.placeholders.Placeholders;
import com.wasteofplastic.askyblock.placeholders.Placeholders.Placeholder;

/**
 * Common PlaceholderAPI for internal placeholders.
 * 
 * @author Poslovitch
 */
public class ASkyBlockPlaceholderAPI implements PlaceholderAPI{
	
	@Override
	public boolean register(ASkyBlock plugin) {
		return true;
	}

	@Override
	public void unregister(ASkyBlock plugin) {
		// Not needed here : it will make the placeholders don't work
	}

	@Override
	public String replacePlaceholders(Player player, String message) {
		if(message == null || message.isEmpty()){
			return "";
		}
		
		for(Placeholder placeholder : Placeholders.getPlaceholders()){
			message = message.replaceAll("{" + placeholder.getIdentifier() + "}", placeholder.onRequest(player));
		}
		return message;
	}

    @Override
    public String getName() {
        return "ASkyBlock";
    }

}
