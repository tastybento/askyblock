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


import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Squid;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;


/**
 * @author ben
 * Provides protection to islands
 */
public class IslandGuard implements Listener {
    private final ASkyBlock plugin;

    public IslandGuard(final ASkyBlock plugin) {
	this.plugin = plugin;

    }

    /**
     * Prevents mobs spawning at spawn
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onMobSpawn(final CreatureSpawnEvent e) {
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (Settings.allowSpawnMobSpawn) {
	    return;
	}
	//plugin.getLogger().info("DEBUG: mob spawn");
	// prevent at spawn
	/*
	if (plugin.getSpawn().getBedrock() != null) {
	    plugin.getLogger().info("DEBUG: spawn loc exists");
	plugin.getLogger().info("DEBUG: distance sq = " + e.getLocation().distanceSquared(plugin.getSpawn().getBedrock()));
	plugin.getLogger().info("DEBUG: range sq = " + (plugin.getSpawn().getRange()*plugin.getSpawn().getRange()));
	} else {
	    plugin.getLogger().info("DEBUG: spawn loc does not exist");
	}*/
	if (plugin.getSpawn().getBedrock() != null && 
		(e.getLocation().distanceSquared(plugin.getSpawn().getBedrock()) < (double)((double)plugin.getSpawn().getRange()*plugin.getSpawn().getRange()))) {
	    //plugin.getLogger().info("DEBUG: prevented mob spawn at spawn");
	    e.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onExplosion(final EntityExplodeEvent e) {
	// Find out what is exploding
	Entity expl = e.getEntity();
	if (expl == null) {
	    return;
	}
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// prevent at spawn
	if (plugin.getSpawn().getBedrock() != null && 
		(e.getLocation().distanceSquared(plugin.getSpawn().getBedrock()) < plugin.getSpawn().getRange()*plugin.getSpawn().getRange())) {
	    e.setCancelled(true);
	}
	// Find out what is exploding
	EntityType exploding = e.getEntityType();
	if (exploding == null) {
	    return;
	}
	switch (exploding) {
	case CREEPER:
	    if (!Settings.allowCreeperDamage) {
		//plugin.getLogger().info("Creeper block damage prevented");
		e.blockList().clear();
	    }
	    break;
	case PRIMED_TNT:
	case MINECART_TNT:
	    if (!Settings.allowTNTDamage) {
		//plugin.getLogger().info("TNT block damage prevented");
		e.blockList().clear();
	    }	    
	    break;
	default:
	    break;
	}
    }




    /**
     * Allows or prevents enderman griefing
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onEndermanGrief(final EntityChangeBlockEvent e) {
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// prevent at spawn
	if (plugin.getSpawn().getBedrock() != null && 
		(e.getEntity().getLocation().distanceSquared(plugin.getSpawn().getBedrock()) < plugin.getSpawn().getRange()*plugin.getSpawn().getRange())) {
	    e.setCancelled(true);
	}
	if (Settings.allowEndermanGriefing)
	    return;
	if (!(e.getEntity() instanceof Enderman)) {
	    return;
	}
	// Stop the Enderman from griefing
	//plugin.getLogger().info("Enderman stopped from griefing");
	e.setCancelled(true);
    }



    /**
     * Drops the Enderman's block when he dies if he has one
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onEndermanDeath(final EntityDeathEvent e) {
	if (!Settings.endermanDeathDrop)
	    return;
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (!(e.getEntity() instanceof Enderman)) {
	    //plugin.getLogger().info("Not an Enderman!");
	    return;
	}
	// Get the block the enderman is holding
	Enderman ender = (Enderman)e.getEntity();
	MaterialData m = ender.getCarriedMaterial();
	if (m != null && !m.getItemType().equals(Material.AIR)) {
	    // Drop the item
	    //plugin.getLogger().info("Dropping item " + m.toString());
	    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), m.toItemStack(1));
	}
    }


    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
    public void onBlockBreak(final BlockBreakEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBreakBlocks) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    /**
     * This method protects players from PVP if it is not allowed and from arrows fired by other players
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	// Check world
	if (!Settings.worldName.equalsIgnoreCase(e.getEntity().getWorld().getName())) {
	    return;
	}
	//plugin.getLogger().info(e.getEventName());
	// Ops can do anything
	if (e.getDamager() instanceof Player) {
	    if (((Player)e.getDamager()).isOp()) {
		return;
	    }
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm((Player)e.getDamager(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	}
	// Check to see if it's an item frame
	if (e.getEntity() instanceof ItemFrame) {
	    //plugin.getLogger().info("Item frame being dmaged");
	    if (!Settings.allowBreakBlocks) {
		// Try and protect against all player instigated damage
		//plugin.getLogger().info("Damager is = " + e.getDamager().toString());
		if (e.getDamager() instanceof Player) {
		    if (!plugin.playerIsOnIsland((Player)e.getDamager())) {
			((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		} else if (e.getDamager() instanceof Projectile) {
		    // Find out who threw the arrow
		    Projectile p = (Projectile)e.getDamager();
		    //plugin.getLogger().info("Shooter is " + p.getShooter().toString());
		    if (p.getShooter() instanceof Player) {
			((Player)p.getShooter()).sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		} 
	    }
	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}

	//plugin.getLogger().info("Entity is " + e.getEntity().toString());
	// Check for player initiated damage
	if (e.getDamager() instanceof Player) {
	    //plugin.getLogger().info("Damager is " + ((Player)e.getDamager()).getName());
	    // If the target is not a player check if mobs can be hurt
	    if (!(e.getEntity() instanceof Player)) {
		if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime || e.getEntity() instanceof Squid) {
		    //plugin.getLogger().info("Entity is a monster - ok to hurt"); 
		    return;
		} else {
		    //plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
		    //UUID playerUUID = e.getDamager().getUniqueId();
		    //if (playerUUID == null) {
		    //plugin.getLogger().info("player ID is null");
		    //}
		    if (!Settings.allowHurtMobs) {
			if (!plugin.playerIsOnIsland((Player)e.getDamager())) {
			    ((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.islandProtected);
			    e.setCancelled(true);
			    return;
			}
		    }
		    return;
		}
	    } else {
		// PVP
		// If PVP is okay then return
		if (Settings.allowPvP.equalsIgnoreCase("allow")) {
		    //plugin.getLogger().info("PVP allowed");
		    return;
		}
		//plugin.getLogger().info("PVP not allowed");

	    }
	}

	//plugin.getLogger().info("Player attack (or arrow)");
	// Only damagers who are players or arrows are left
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    //plugin.getLogger().info("Arrow attack");
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		Player shooter = (Player)arrow.getShooter();
		//plugin.getLogger().info("Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    //plugin.getLogger().info("Player vs Player!");
		    // Arrow shot by a player at another player
		    if (!Settings.allowPvP.equalsIgnoreCase("allow")) {
			//plugin.getLogger().info("Target player is in a no-PVP area!");
			((Player)arrow.getShooter()).sendMessage("Target is in a no-PVP area!");
			e.setCancelled(true);
			return;
		    } 
		} else {
		    if (!(e.getEntity() instanceof Monster) && !(e.getEntity() instanceof Slime) && !(e.getEntity() instanceof Squid)) {
			//plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
			if (!Settings.allowHurtMobs) {
			    if (!plugin.playerIsOnIsland((Player)arrow.getShooter())) {
				shooter.sendMessage(ChatColor.RED + Locale.islandProtected);
				e.setCancelled(true);
				return;
			    }
			}
			return;
		    }
		}
	    }
	} else if (e.getDamager() instanceof Player){
	    //plugin.getLogger().info("Player attack");
	    // Just a player attack
	    if (!Settings.allowPvP.equalsIgnoreCase("allow")) {
		((Player)e.getDamager()).sendMessage("Target is in a no-PVP area!");
		e.setCancelled(true);
		return;
	    } 
	}
	return;
	/*
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}

	// If the target is not a player return
	if (!(e.getEntity() instanceof Player)) {
	    return;
	}
	// If PVP is okay then return
	if (Settings.allowPvP.equalsIgnoreCase("allow")) {
	    return;
	}
	// Only damagers who are players or arrows are left
	// If the projectile is anything else than an arrow don't worry about it in this listener
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		// Arrow shot by a player at another player
		if (Settings.allowPvP.equalsIgnoreCase("allow")) {
		    return;
		} else {
		    e.setCancelled(true);
		    return;
		}
	    }
	}
	return;
	 */
    }


    /**
     * Prevents placing of blocks
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	//plugin.getLogger().info(e.getEventName());
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowPlaceBlocks) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final HangingPlaceEvent e) {
	//plugin.getLogger().info(e.getEventName());
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowPlaceBlocks) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
	// Check world
	if (Settings.worldName.equalsIgnoreCase(e.getPlayer().getWorld().getName())) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBedUse) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }
    /**
     * Prevents the breakage of hanging items
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	//plugin.getLogger().info(e.getEventName());
	if (e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!Settings.allowBreakBlocks) {
		if (e.getRemover() instanceof Player) {
		    Player p = (Player)e.getRemover();
		    // This permission bypasses protection
		    if (VaultHelper.checkPerm(p, "askyblock.mod.bypassprotect")) {
			return;
		    }
		    if (!plugin.playerIsOnIsland(p) && !p.isOp()) {
			p.sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBucketUse) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBucketUse) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.NORMAL)
    public void onShear(final PlayerShearEntityEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowShearing) {	
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (plugin.playerIsOnIsland(e.getPlayer()) || e.getPlayer().isOp()) {
	    // You can do anything on your island or if you are Op
	    return;
	}
	// This permission bypasses protection
	if (VaultHelper.checkPerm(e.getPlayer(), "askyblock.mod.bypassprotect")) {
	    return;
	}
	// Player is off island
	// Check if player is at spawn
	// prevent at spawn
	boolean playerAtSpawn = false;
	if (plugin.getSpawn().getBedrock() != null && 
		(e.getPlayer().getLocation().distanceSquared(plugin.getSpawn().getBedrock()) < plugin.getSpawn().getRange()*plugin.getSpawn().getRange())) {
	    playerAtSpawn = true;
	}

	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    //plugin.getLogger().info("DEBUG: clicked block " + e.getClickedBlock());
	    //plugin.getLogger().info("DEBUG: Material " + e.getMaterial());

	    switch (e.getClickedBlock().getType()) {
	    case WOODEN_DOOR:
	    case TRAP_DOOR:
		if (!Settings.allowDoorUse && !(playerAtSpawn && Settings.allowSpawnDoorUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case FENCE_GATE:
		if (!Settings.allowGateUse && !(playerAtSpawn && Settings.allowSpawnGateUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return;  
		}
		break;
	    case CHEST:
	    case TRAPPED_CHEST:
	    case ENDER_CHEST:
	    case DISPENSER:
	    case DROPPER:
	    case HOPPER:
	    case HOPPER_MINECART:
	    case STORAGE_MINECART:
		if (!Settings.allowChestAccess && !(playerAtSpawn && Settings.allowSpawnChestAccess)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case SOIL:
		if (!Settings.allowCropTrample) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (!Settings.allowBrewing && !(playerAtSpawn && Settings.allowSpawnBrewing)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case CAKE_BLOCK:
		break;
	    case DIODE:
	    case DIODE_BLOCK_OFF:
	    case DIODE_BLOCK_ON:
	    case REDSTONE_COMPARATOR_ON:
	    case REDSTONE_COMPARATOR_OFF:
		if (!Settings.allowRedStone && !(playerAtSpawn && Settings.allowSpawnRedStone)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ENCHANTMENT_TABLE:
		break;
	    case FURNACE:
	    case BURNING_FURNACE:
		if (!Settings.allowFurnaceUse && !(playerAtSpawn && Settings.allowSpawnFurnaceUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ICE:
		break;
	    case ITEM_FRAME:
		break;
	    case JUKEBOX:
	    case NOTE_BLOCK:
		if (!Settings.allowMusic && !(playerAtSpawn && Settings.allowSpawnMusic)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case PACKED_ICE:
		break;
	    case STONE_BUTTON:
	    case WOOD_BUTTON:
	    case LEVER:
		if (!Settings.allowLeverButtonUse && !(playerAtSpawn && Settings.allowSpawnLeverButtonUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}	
		break;
	    case TNT:
		break;
	    case WORKBENCH:
		if (!Settings.allowCrafting && !(playerAtSpawn && Settings.allowSpawnCrafting)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    default:
		break;
	    }
	}
	// Check for disallowed in-hand items
	if (e.getMaterial() != null) {
	    if (e.getMaterial().equals(Material.BOAT) && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
		// Trying to put a boat on non-liquid
		e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		e.setCancelled(true);
		return;
	    }
	    if (e.getMaterial().equals(Material.ENDER_PEARL)) {
		if (!Settings.allowEnderPearls) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.POTION) && e.getItem().getDurability() != 0) {
		// Potion
		//plugin.getLogger().info("DEBUG: potion");
		try {
		    Potion p = Potion.fromItemStack(e.getItem());
		    if (!p.isSplash()) {
			//plugin.getLogger().info("DEBUG: not a splash potion");
			return;
		    } else {
			// Splash potions are allowed only if PVP is allowed
			if (!Settings.allowPvP.equalsIgnoreCase("allow")) {
			    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
			    e.setCancelled(true);
			}
		    }
		} catch (Exception ex) {
		}
	    }
	    // Everything else is okay
	}
    }


    /**
     * Prevents crafting of EnderChest unless the player has permission
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCraft(CraftItemEvent event) {
	Player player = (Player) event.getWhoClicked();
	if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName) || 
		player.getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    if(event.getRecipe().getResult().getType() == Material.ENDER_CHEST) {
		if(!(player.hasPermission("askyblock.craft.enderchest"))) {
		    event.setCancelled(true);
		}
	    }
	}
    }

    /**
     * Prevents usage of an Ender Chest
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    void PlayerInteractEvent(PlayerInteractEvent event){
	Player player = (Player) event.getPlayer();
	if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName) || 
		player.getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    if (event.getAction() == Action.RIGHT_CLICK_BLOCK ){
		if (event.getClickedBlock().getType() == Material.ENDER_CHEST){
		    if(!(event.getPlayer().hasPermission("askyblock.craft.enderchest"))) {
			event.setCancelled(true);
		    }
		}
	    }
	}
    }
}


