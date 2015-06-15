package com.wasteofplastic.askyblock.listeners;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;

/**
 * This class handles the Wither. Spawning withers is a reality on some servers as it is the way
 * to obtain a nether star. This class enables players to spawn a Wither without it adversely affecting
 * other players or their islands. 
 * 
 * @author tastybento
 *
 */
public class WitherEvents implements Listener {
    private final ASkyBlock plugin;
    private final boolean debug = false;
    private HashMap<UUID, Island> witherSpawnInfo;
    /**
     * @param plugin
     */
    public WitherEvents(ASkyBlock plugin) {
	this.plugin = plugin;
	this.witherSpawnInfo = new HashMap<UUID, Island>();
    }

    /**
     * Track where the wither was created. This will determine its allowable attack zone.
     * @param event
     */
    @EventHandler
    public void WitherSpawn(CreatureSpawnEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// Only cover withers in the island world
	if (e.getEntityType() != EntityType.WITHER || !IslandGuard.inWorld(e.getEntity()) ) {
	    return;
	}
	// Store where this wither originated
	Island island = plugin.getGrid().getIslandAt(e.getLocation());
	if (island != null) {
	    //plugin.getLogger().info("DEBUG: Wither spawned on known island - id = " + e.getEntity().getUniqueId());
	    witherSpawnInfo.put(e.getEntity().getUniqueId(),island);
	} // Else do nothing - maybe an Op spawned it? If so, on their head be it!
    }

    @EventHandler
    public void WitherExplosion(EntityExplodeEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// Only cover withers in the island world
	if (e.getEntity() == null || !IslandGuard.inWorld(e.getEntity())) {
	    return;
	}
	// The wither or wither skulls can both blow up
	if (e.getEntityType() == EntityType.WITHER || e.getEntityType() == EntityType.WITHER_SKULL) {
	    // Check the location
	    //plugin.getLogger().info("DEBUG: Wither or wither skull");
	    if (witherSpawnInfo.containsKey(e.getEntity().getUniqueId())) {
		// We know about this wither
		//plugin.getLogger().info("DEBUG: We know about this wither");
		if (!witherSpawnInfo.get(e.getEntity().getUniqueId()).inIslandSpace(e.getLocation())) {
		    // Cancel the explosion and block damage
		    //plugin.getLogger().info("DEBUG: cancel");
		    e.blockList().clear();
		    e.setCancelled(true);
		}
	    }
	}
    }

    /**
     * Deal with pre-explosions
     */
    @EventHandler
    public void WitherExplode(ExplosionPrimeEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// Only cover withers in the island world
	if (!IslandGuard.inWorld(e.getEntity()) || e.getEntity() == null) {
	    return;
	}
	// The wither or wither skulls can both blow up
	if (e.getEntityType() == EntityType.WITHER) {
	    //plugin.getLogger().info("DEBUG: Wither");
	    // Check the location
	    if (witherSpawnInfo.containsKey(e.getEntity().getUniqueId())) {
		// We know about this wither
		//plugin.getLogger().info("DEBUG: We know about this wither");
		if (!witherSpawnInfo.get(e.getEntity().getUniqueId()).inIslandSpace(e.getEntity().getLocation())) {
		    // Cancel the explosion
		    //plugin.getLogger().info("DEBUG: cancel");
		    e.setCancelled(true);
		}
	    }
	    // Testing only e.setCancelled(true);
	}
	if (e.getEntityType() == EntityType.WITHER_SKULL) {
	    //plugin.getLogger().info("DEBUG: Wither skull");
	    // Get shooter
	    Projectile projectile = (Projectile)e.getEntity();
	    if (projectile.getShooter() instanceof Wither) {
		//plugin.getLogger().info("DEBUG: shooter is wither");
		Wither wither = (Wither)projectile.getShooter();
		// Check the location
		if (witherSpawnInfo.containsKey(wither.getUniqueId())) {
		    // We know about this wither
		    //plugin.getLogger().info("DEBUG: We know about this wither");
		    if (!witherSpawnInfo.get(wither.getUniqueId()).inIslandSpace(e.getEntity().getLocation())) {
			// Cancel the explosion
			//plugin.getLogger().info("DEBUG: cancel");
			e.setCancelled(true);
		    }
		}
	    }
	}
    }

    /**
     * Clean up the hashmap. It's probably not needed, but just in case.
     */
    @EventHandler
    public void WitherDeath(EntityDeathEvent e) {
	if (e.getEntityType() == EntityType.WITHER) {
	    witherSpawnInfo.remove(e.getEntity().getUniqueId());
	}
    }

}
