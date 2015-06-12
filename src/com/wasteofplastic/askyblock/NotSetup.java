package com.wasteofplastic.askyblock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * This class runs when the config file is not set up enough, or is unsafe
 * It provides useful information to the admin on what is wrong.
 * 
 * @author tastybento
 * 
 */
public class NotSetup implements CommandExecutor {

    public enum Reason {
	DISTANCE, GENERATOR, WORLD_NAME
    };

    private Reason reason;

    /**
     * Handles plugin operation if a critical setup parameter is missing
     * 
     * @param reason
     */
    public NotSetup(Reason reason) {
	this.reason = reason;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
	sender.sendMessage(ChatColor.RED + "More set up is required before the plugin can start...");
	switch (reason) {
	case DISTANCE:
	    sender.sendMessage(ChatColor.RED + "Edit config.yml. Then restart server.");
	    sender.sendMessage(ChatColor.RED + "Make sure you set island distance. If upgrading, set it to what it was before.");
	    break;
	case GENERATOR:
	    sender.sendMessage(ChatColor.RED + "The world generator for the island world is not registered.");
	    sender.sendMessage(ChatColor.RED + "Potential reasons are:");
	    sender.sendMessage(ChatColor.RED + "  1. If you are configuring the island world as the only server world");
	    sender.sendMessage(ChatColor.RED + "     Make sure you have added the world to bukkit.yml");
	    sender.sendMessage(ChatColor.RED + "  2. You reloaded instead of restarting the server. Reboot and try again.");
	    if (Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
		sender.sendMessage(ChatColor.RED + "  3. Your Multiverse plugin is out of date. Upgrade to the latest version.");
	    }
	    break;
	case WORLD_NAME:
	    sender.sendMessage(ChatColor.RED + "The world name in config.yml is different to the world name in islands.yml.");
	    sender.sendMessage(ChatColor.RED + "If this is intentional, I assume you are doing a full reset. If so,");
	    sender.sendMessage(ChatColor.RED + "delete islands.yml and the previous world. If not, correct the world name in");
	    sender.sendMessage(ChatColor.RED + "config.yml and restart. This is probably the case if you are upgrading.");
	default:
	    break;
	}
	return true;
    }

}