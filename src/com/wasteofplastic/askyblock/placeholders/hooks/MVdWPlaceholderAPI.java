package com.wasteofplastic.askyblock.placeholders.hooks;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.placeholders.PlaceholderAPI;
import com.wasteofplastic.askyblock.placeholders.PlaceholderHandler;
import com.wasteofplastic.askyblock.placeholders.Placeholders;
import com.wasteofplastic.askyblock.placeholders.Placeholders.Placeholder;

import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;

/**
 * MVdWPlaceholder hook
 * 
 * @author Poslovitch
 */
public class MVdWPlaceholderAPI implements PlaceholderAPI{

    @Override
    public boolean register(ASkyBlock plugin) {
        if(available()){
            PlaceholderReplacer replacer = new PlaceholderReplacer() {

                @Override
                public String onPlaceholderReplace(PlaceholderReplaceEvent event) {
                    if(Placeholders.getPlaceholders().contains(event.getPlaceholder())){
                        return PlaceholderHandler.replacePlaceholders(event.getPlayer(), event.getPlaceholder());
                    }
                    return null;
                }
            };
            for(Placeholder placeholder : Placeholders.getPlaceholders()){
                be.maximvdw.placeholderapi.PlaceholderAPI.registerPlaceholder(plugin, placeholder.getIdentifier(), replacer);
            }
            return true;
        }
        return false;
    }

    @Override
    public void unregister(ASkyBlock plugin) {
        if(available()){
            for (Iterator<String> iterator = be.maximvdw.placeholderapi.PlaceholderAPI.getCustomPlaceholders().keySet().iterator(); iterator.hasNext(); ) {
                if (iterator.next().startsWith(Settings.PERMPREFIX)) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public String replacePlaceholders(Player player, String message) {
        if (available()) {
            return be.maximvdw.placeholderapi.PlaceholderAPI.replacePlaceholders(player, message);
        }
        return message;
    }

    public boolean available() {
        // May not be enabled yet...
        return Bukkit.getPluginManager().getPlugin("MVdWPlaceholderAPI") != null;
    }

    @Override
    public String getName() {
        return "MVdWPlaceholder";
    }
}
