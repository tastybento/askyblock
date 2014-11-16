/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.wasteofplastic.askyblock;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class Spawn {
    private ASkyBlock plugin;
    private YamlConfiguration spawnConfig;
    private Location spawnLoc;  
    private Location bedrock;
    private ConfigurationSection spawn;
    private int range;

    /**
     * @param plugin
     * @param players
     */
    public Spawn(ASkyBlock plugin) {
	this.plugin = plugin;
	reload();
    }

    public void reload() {
	spawnConfig = ASkyBlock.loadYamlFile("spawn.yml");
	spawn = spawnConfig.getConfigurationSection("spawn");
	// load the config items
	Settings.allowSpawnDoorUse = spawn.getBoolean("allowdooruse", true);
	Settings.allowSpawnLeverButtonUse = spawn.getBoolean("allowleverbuttonuse", true);
	Settings.allowSpawnChestAccess = spawn.getBoolean("allowchestaccess", true);
	Settings.allowSpawnFurnaceUse = spawn.getBoolean("allowfurnaceuse", true);
	Settings.allowSpawnRedStone = spawn.getBoolean("allowredstone", false);
	Settings.allowSpawnMusic = spawn.getBoolean("allowmusic", true);
	Settings.allowSpawnCrafting = spawn.getBoolean("allowcrafting", true);
	Settings.allowSpawnBrewing = spawn.getBoolean("allowbrewing", true);
	Settings.allowSpawnGateUse = spawn.getBoolean("allowgateuse", true);	
	Settings.allowSpawnMobSpawn = spawn.getBoolean("allowmobspawn", false);
	Settings.allowSpawnNoAcidWater = spawn.getBoolean("allowspawnnoacidwater", false);
	this.spawnLoc = ASkyBlock.getLocationString(spawn.getString("location",""));
	this.bedrock = ASkyBlock.getLocationString(spawn.getString("bedrock",""));
	this.range = spawn.getInt("range",100);
    }
    public void save() {
	// Save the spawn location
	plugin.getLogger().info("Saving spawn.yml file");
	String spawnPlace = ASkyBlock.getStringLocation(spawnLoc);
	spawn.set("location", spawnPlace);
	spawn.set("bedrock", ASkyBlock.getStringLocation(bedrock));
	//plugin.getLogger().info("Spawn = " + spawnPlace);
	ASkyBlock.saveYamlFile(spawnConfig,"spawn.yml");
    }
    /**
     * @return the spawnLoc
     */
    public Location getSpawnLoc() {
	return spawnLoc;
    }

    /**
     * @param spawnLoc the spawnLoc to set
     * @param bedrock 
     */
    public void setSpawnLoc(Location bedrock, Location spawnLoc) {
	this.spawnLoc = spawnLoc;
	this.bedrock = bedrock;
    }

    /**
     * @return the range
     */
    public int getRange() {
	return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(int range) {
	this.range = range;
    }

    /**
     * @return the bedrock
     */
    public Location getBedrock() {
        return bedrock;
    }

    /**
     * Returns true if this location is within the spawn area
     * @param loc
     * @return
     */
    public boolean isAtSpawn(Location loc) {
	//plugin.getLogger().info("DEBUG: location is " + loc.toString());
	//plugin.getLogger().info("DEBUG spawnLoc is " + spawnLoc.toString());
	//plugin.getLogger().info("DEBUG: range = " + range);
	if (loc.distanceSquared(spawnLoc) < range * range) {
	    //plugin.getLogger().info("DEBUG: within range");
	    return true;
	}
	return false;
    }


}
