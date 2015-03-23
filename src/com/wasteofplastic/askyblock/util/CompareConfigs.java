package com.wasteofplastic.askyblock.util;

import java.io.File;
import java.io.InputStream;

import com.wasteofplastic.askyblock.ASkyBlock;

/**
 * Compares the current config with the one in the jar and updates it if required with new values.
 * @author tastybento
 *
 */
public class CompareConfigs {

    private static ASkyBlock plugin = ASkyBlock.getPlugin();
    private static SimpleConfigManager manager;

    public static void compareConfigs() {
	plugin.getLogger().info("Comparing configs");
	File dataFolder = plugin.getDataFolder();
	File yamlFile = new File(dataFolder, "config.yml");
	
	manager = new SimpleConfigManager(plugin);
	
	
	//YamlConfiguration config = new YamlConfiguration();
	//YamlConfiguration internalConfig = new YamlConfiguration();
	SimpleConfig config;
	SimpleConfig internalConfig;
	
	// Expand in the future to compare all yml files
	
	InputStream internalFile = plugin.getResource("config.yml");
	// Only proceed if there is both an internal and external file
	if (yamlFile.exists() && internalFile != null) {
	    try {
		// Load the external config
		config = manager.getNewConfig("config.yml");
		// Check to see if the version is up to date
		String configVersion = config.getString("configVersion","");
		if (configVersion.isEmpty() || !configVersion.equals(plugin.getDescription().getVersion())) {
		    // The config appears to be out of date
		    plugin.getLogger().info("The config.yml appears to be out of date - checking");
		    // Load the internal config
		    internalConfig = new SimpleConfig(manager.getConfigContent(internalFile), null, 0, plugin);
		    //internalConfig.load(internalFile);
		    // Now run through all the settings and compare
		    for (String setting : internalConfig.getKeys()) {
			//plugin.getLogger().info("Checking setting: " + setting);
			if (!config.contains(setting)) {
			    plugin.getLogger().info(setting + " is missing. Adding it.");
			    config.set(setting, internalConfig.get(setting));
			}
		    }
		    config.saveConfig();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} 
    }
}
