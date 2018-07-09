package com.wasteofplastic.askyblock.util.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;

public class SafeTeleportBuilder {

    private ASkyBlock plugin;
    private Entity entity;
    private int homeNumber = 0;
    private boolean portal = false;
    private String failureMessage = "";
    private Location location;


    public SafeTeleportBuilder(ASkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Set who or what is going to teleport
     * @param entity
     * @return
     */
    public SafeTeleportBuilder entity(Entity entity) {
        this.entity = entity;
        return this;
    }

    /**
     * Set the island to teleport to
     * @param island
     * @return
     */
    public SafeTeleportBuilder island(Island island) {
        this.location = island.getCenter();
        return this;
    }

    /**
     * Set the home number to this number
     * @param homeNumber
     * @return
     */
    public SafeTeleportBuilder homeNumber(int homeNumber) {
        this.homeNumber = homeNumber;
        return this;
    }

    /**
     * This is a portal teleportation
     * @param setHome
     * @return
     */
    public SafeTeleportBuilder portal() {
        this.portal = true;
        return this;
    }

    /**
     * Set the failure message if this teleport cannot happen
     * @param failureMessage
     * @return
     */
    public SafeTeleportBuilder failureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    /**
     * Set the desired location
     * @param location
     * @return
     */
    public SafeTeleportBuilder location(Location location) {
        this.location = location;
        return this;
    }

    /**
     * Try to teleport the player
     * @return
     */
    public SafeSpotTeleport build() {      
        // Error checking
        if (entity == null) {
            plugin.getLogger().severe("Attempt to safe teleport a null entity!");
            return null;
        }
        if (location == null) {
            plugin.getLogger().severe("Attempt to safe teleport to a null location!");
            return null;
        }
        if (failureMessage.isEmpty() && entity instanceof Player) {
            failureMessage = plugin.myLocale(entity.getUniqueId()).warpserrorNotSafe;
        }
        return new SafeSpotTeleport(plugin, entity, location, failureMessage, portal, homeNumber);
    }


}
