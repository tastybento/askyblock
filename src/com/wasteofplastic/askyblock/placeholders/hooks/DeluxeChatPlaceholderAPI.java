package com.wasteofplastic.askyblock.placeholders.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.clip.deluxechat.placeholders.*;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.placeholders.PlaceholderAPI;
import com.wasteofplastic.askyblock.placeholders.PlaceholderHandler;
import com.wasteofplastic.askyblock.placeholders.Placeholders;

/**
 * DeluxeChat PlaceholderAPI hook
 * 
 * @author Poslovitch
 */
public class DeluxeChatPlaceholderAPI implements PlaceholderAPI{

    @Override
    public boolean register(ASkyBlock plugin) {
        me.clip.deluxechat.placeholders.PlaceholderHandler.registerPlaceholderHook(plugin, new DeluxePlaceholderHook(){
            @Override
            public String onPlaceholderRequest(Player player, String placeholder) {
                if(Placeholders.getPlaceholders().contains(placeholder)){
                    return PlaceholderHandler.replacePlaceholders(player, placeholder);
                }
                return null;
            }
        });
        return true;
    }

    @Override
    public void unregister(ASkyBlock plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("DeluxeChat")) {
            me.clip.deluxechat.placeholders.PlaceholderHandler.unregisterPlaceholderHook(plugin);
        }
    }

    @Override
    public String replacePlaceholders(Player player, String message) {
        return me.clip.deluxechat.placeholders.PlaceholderHandler.setPlaceholders(player, message);
    }

    @Override
    public String getName() {
        return "DeluxeChat";
    }

}
