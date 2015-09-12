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

package com.wasteofplastic.askyblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * Applies the acid effect to players
 * 
 * @author tastybento
 */
public class AcidEffect implements Listener {
    private final ASkyBlock plugin;
    private List<Player> burningPlayers = new ArrayList<Player>();
    private boolean isRaining = false;
    private List<Player> wetPlayers = new ArrayList<Player>();

    public AcidEffect(final ASkyBlock pluginI) {
	plugin = pluginI;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent e) {
	burningPlayers.remove((Player) e.getEntity());
	wetPlayers.remove((Player) e.getEntity());
	PlayerEvents.unsetFalling(((Player) e.getEntity()).getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
	final Player player = e.getPlayer();
	// Fast checks
	if (player.isDead()) {
	    return;
	}
	// Check that they are in the ASkyBlock world
	if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// Return if players are immune
	if (player.isOp()) {
	    if (!Settings.damageOps) {
		return;
	    }
	} else if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.noburn") || VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.noburn")) {
	    return;
	}

	if (player.getGameMode().equals(GameMode.CREATIVE)) {
	    return;
	}

	// Slow checks
	final Location playerLoc = player.getLocation();
	final Block block = playerLoc.getBlock();
	final Block head = block.getRelative(BlockFace.UP);

	// Check for acid rain
	if (Settings.rainDamage > 0D && isRaining) {
	    // Only check if they are in a non-dry biome
	    Biome biome = playerLoc.getBlock().getBiome();
	    if (biome != Biome.DESERT && biome != Biome.DESERT_HILLS && biome != Biome.DESERT_MOUNTAINS
		    && biome != Biome.SAVANNA && biome != Biome.SAVANNA_MOUNTAINS && biome != Biome.SAVANNA_PLATEAU
		    && biome != Biome.SAVANNA_PLATEAU_MOUNTAINS && biome != Biome.MESA && biome != Biome.MESA_BRYCE
		    && biome != Biome.MESA_PLATEAU && biome != Biome.MESA_PLATEAU_FOREST && biome != Biome.MESA_PLATEAU_FOREST_MOUNTAINS
		    && biome != Biome.MESA_PLATEAU_MOUNTAINS && biome != Biome.HELL) {
		// plugin.getLogger().info("Rain damage = " + Settings.rainDamage);
		boolean hitByRain = true;
		// Check if all air above player
		for (int y = playerLoc.getBlockY() + 2; y < playerLoc.getWorld().getMaxHeight(); y++) {
		    if (!playerLoc.getWorld().getBlockAt(playerLoc.getBlockX(), y, playerLoc.getBlockZ()).getType().equals(Material.AIR)) {
			hitByRain = false;
			break;
		    }
		}
		if (!hitByRain) {
		    // plugin.getLogger().info("DEBUG: not hit by rain");
		    wetPlayers.remove(player);
		} else {
		    // plugin.getLogger().info("DEBUG: hit by rain");
		    // Check if player has an active water potion or not
		    boolean acidPotion = false;
		    Collection<PotionEffect> activePotions = player.getActivePotionEffects();
		    for (PotionEffect s : activePotions) {
			// plugin.getLogger().info("Potion is : " +
			// s.getType().toString());
			if (s.getType().equals(PotionEffectType.WATER_BREATHING)) {
			    // Safe!
			    acidPotion = true;
			    // plugin.getLogger().info("Water breathing potion protection!");
			}
		    }
		    if (acidPotion) {
			// plugin.getLogger().info("DEBUG: Acid potion active");
			wetPlayers.remove(player);
		    } else {
			// plugin.getLogger().info("DEBUG: no acid potion");
			if (!wetPlayers.contains(player)) {
			    // plugin.getLogger().info("DEBUG: Start hurting player");
			    // Start hurting them
			    // Add to the list
			    wetPlayers.add(player);
			    // This runnable continuously hurts the player even if
			    // they are not
			    // moving but are in acid rain.
			    new BukkitRunnable() {
				@Override
				public void run() {
				    // Check if it is still raining or player is
				    // dead
				    if (!isRaining || player.isDead()) {
					// plugin.getLogger().info("DEBUG: Player is dead or it has stopped raining");
					wetPlayers.remove(player);
					this.cancel();
					// Check they are still in this world
				    } else if (player.getLocation().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
					// Check if they have drunk a potion
					// Check if player has an active water
					// potion or not
					Collection<PotionEffect> activePotions = player.getActivePotionEffects();
					for (PotionEffect s : activePotions) {
					    // plugin.getLogger().info("Potion is : "
					    // +
					    // s.getType().toString());
					    if (s.getType().equals(PotionEffectType.WATER_BREATHING)) {
						// Safe!
						// plugin.getLogger().info("DEBUG: Acid potion active");
						wetPlayers.remove(player);
						this.cancel();
						return;
						// plugin.getLogger().info("Water breathing potion protection!");
					    }
					}
					// Check if they are still in rain
					// Check if all air above player
					for (int y = player.getLocation().getBlockY() + 2; y < player.getLocation().getWorld().getMaxHeight(); y++) {
					    if (!player.getLocation().getWorld().getBlockAt(player.getLocation().getBlockX(), y, player.getLocation().getBlockZ())
						    .getType().equals(Material.AIR)) {
						// Safe!
						wetPlayers.remove(player);
						this.cancel();
						return;
					    }
					}
					// Apply damage if there is any - no potion
					// damage for rain
					if (Settings.rainDamage > 0D) {
					    double health = player.getHealth() - (Settings.rainDamage - Settings.rainDamage * getDamageReduced(player));
					    if (health < 0D) {
						health = 0D;
					    } else if (health > 20D) {
						health = 20D;
					    }
					    player.setHealth(health);
					    player.getWorld().playSound(playerLoc, Sound.FIZZ, 3F, 3F);
					}
				    } else {
					// plugin.getLogger().info("DEBUG: Player no longer in acid world");
					wetPlayers.remove(player);
					// plugin.getLogger().info("Cancelled!");
					this.cancel();
				    }
				}
			    }.runTaskTimer(plugin, 0L, 20L);
			}
		    }
		}
	    }
	}

	// If they are not in liquid, then return
	if (!block.isLiquid() && !head.isLiquid()) {
	    return;
	}
	// Find out if they are at the bottom of the sea and if so bounce them
	// back up
	if (playerLoc.getBlockY() < 1) {
	    final Vector v = new Vector(player.getVelocity().getX(), 1D, player.getVelocity().getZ());
	    player.setVelocity(v);
	}
	// If they are already burning in acid then return
	if (burningPlayers.contains(player)) {
	    return;
	}
	// Check if they are in spawn and therefore water above sea-level is not
	// acid
	if (Settings.allowSpawnNoAcidWater) {
	    // plugin.getLogger().info("DEBUG: no acid water is true");
	    // Check if the player is above sealevel because the sea is always
	    // acid
	    if (playerLoc.getBlockY() > Settings.sea_level) {
		// plugin.getLogger().info("DEBUG: player is above sea level");
		if (plugin.getGrid().isAtSpawn(playerLoc)) {
		    // plugin.getLogger().info("DEBUG: player is at spawn");
		    return;
		}
	    }
	}
	// plugin.getLogger().info("DEBUG: no acid water is false");
	// Check if they are in water
	if (block.getType().equals(Material.STATIONARY_WATER) || block.getType().equals(Material.WATER)
		|| head.getType().equals(Material.STATIONARY_WATER) || head.getType().equals(Material.WATER)) {
	    //plugin.getLogger().info("DEBUG: head = " + head.getType() + " body = " + block.getType());
	    // Check if player has just exited a boat - in which case, they are
	    // immune for 1 tick
	    // This is needed because safeboat.java cannot teleport the player
	    // for 1 tick
	    // Don't remove this!!
	    // if (SafeBoat.exitedBoat(player)) {
	    // return;
	    // }
	    // Check if player is in a boat
	    Entity playersVehicle = player.getVehicle();
	    if (playersVehicle != null) {
		// They are in a Vehicle
		if (playersVehicle.getType().equals(EntityType.BOAT)) {
		    // I'M ON A BOAT! I'M ON A BOAT! A %^&&* BOAT!
		    return;
		}
	    }
	    // Check if player has an active water potion or not
	    Collection<PotionEffect> activePotions = player.getActivePotionEffects();
	    for (PotionEffect s : activePotions) {
		// plugin.getLogger().info("Potion is : " +
		// s.getType().toString());
		if (s.getType().equals(PotionEffectType.WATER_BREATHING)) {
		    // Safe!
		    //plugin.getLogger().info("DEBUG: Water breathing potion protection!");
		    return;
		}
	    }
	    // ACID!
	    //plugin.getLogger().info("DEBUG: Acid!");
	    // Put the player into the acid list
	    burningPlayers.add(player);
	    // This runnable continuously hurts the player even if they are not
	    // moving but are in acid.
	    new BukkitRunnable() {
		@Override
		public void run() {
		    if (player.isDead()) {
			burningPlayers.remove(player);
			this.cancel();
		    } else if ((player.getLocation().getBlock().isLiquid() || player.getLocation().getBlock().getRelative(BlockFace.UP).isLiquid())
			    && player.getLocation().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
			// plugin.getLogger().info("Damage setting = " +
			// Settings.acidDamage);
			// plugin.getLogger().info("Damage to player = " +
			// (Settings.general_acidDamage -
			// Settings.general_acidDamage *
			// getDamageReduced(player)));
			// plugin.getLogger().info("Player health is " +
			// player.getHealth());
			// Apply additional potion effects
			// plugin.getLogger().info("Potion damage " +
			// Settings.acidDamageType.toString());
			if (!Settings.acidDamageType.isEmpty()) {
			    for (PotionEffectType t : Settings.acidDamageType) {
				// plugin.getLogger().info("Applying " +
				// t.toString());
				// player.addPotionEffect(new PotionEffect(t,
				// 20, amplifier));
				if (t.equals(PotionEffectType.BLINDNESS) || t.equals(PotionEffectType.CONFUSION) || t.equals(PotionEffectType.HUNGER)
					|| t.equals(PotionEffectType.SLOW) || t.equals(PotionEffectType.SLOW_DIGGING) || t.equals(PotionEffectType.WEAKNESS)) {
				    player.addPotionEffect(new PotionEffect(t, 600, 1));
				} else {
				    // Poison
				    player.addPotionEffect(new PotionEffect(t, 200, 1));
				}
			    }
			}
			// double health = player.getHealth();
			// Apply damage if there is any
			if (Settings.acidDamage > 0D) {
			    double health = player.getHealth() - (Settings.acidDamage - Settings.acidDamage * getDamageReduced(player));
			    if (health < 0D) {
				health = 0D;
			    } else if (health > 20D) {
				health = 20D;
			    }
			    player.setHealth(health);

			    player.getWorld().playSound(playerLoc, Sound.FIZZ, 2F, 2F);
			}

		    } else {
			burningPlayers.remove(player);
			// plugin.getLogger().info("Cancelled!");
			this.cancel();
		    }
		}
	    }.runTaskTimer(plugin, 0L, 20L);
	}
    }

    /**
     * Enables changing of obsidian back into lava
     * 
     * @param e
     */
    // Deprecation is due to the updateinventory that still is required for some
    // reason.
    // @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (plugin.getGrid().playerIsOnIsland(e.getPlayer())) {
	    boolean otherOb = false;
	    ItemStack inHand = e.getPlayer().getItemInHand();
	    if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && inHand.getType().equals(Material.BUCKET)
		    && e.getClickedBlock().getType().equals(Material.OBSIDIAN)) {
		// Look around to see if this is a lone obsidian block
		Block b = e.getClickedBlock();
		for (int x = -2; x <= 2; x++) {
		    for (int y = -2; y <= 2; y++) {
			for (int z = -2; z <= 2; z++) {
			    final Block testBlock = b.getWorld().getBlockAt(b.getX() + x, b.getY() + y, b.getZ() + z);
			    if ((x != 0 || y != 0 || z != 0) && testBlock.getType().equals(Material.OBSIDIAN)) {
				otherOb = true;
			    }
			}
		    }
		}
		if (!otherOb) {
		    e.getPlayer().sendMessage(ChatColor.YELLOW + plugin.myLocale(e.getPlayer().getUniqueId()).changingObsidiantoLava);
		    e.getPlayer().getInventory().setItemInHand(null);
		    // e.getPlayer().getInventory().removeItem(new
		    // ItemStack(Material.BUCKET, 1));
		    e.getPlayer().getInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
		    if (inHand.getAmount() > 1) {
			e.getPlayer().getInventory().addItem(new ItemStack(Material.BUCKET, inHand.getAmount()-1));
		    }
		    e.getPlayer().updateInventory();
		    e.getClickedBlock().setType(Material.AIR);
		    e.setCancelled(true);
		}
	    }
	}
    }

    /**
     * @param player
     * @return A double between 0.0 and 0.80 that reflects how much armor the
     *         player has on. The higher the value, the more protection they
     *         have.
     */
    static public double getDamageReduced(Player player) {
	org.bukkit.inventory.PlayerInventory inv = player.getInventory();
	ItemStack boots = inv.getBoots();
	ItemStack helmet = inv.getHelmet();
	ItemStack chest = inv.getChestplate();
	ItemStack pants = inv.getLeggings();
	double red = 0.0;
	if (helmet != null) {
	    if (helmet.getType() == Material.LEATHER_HELMET)
		red = red + 0.04;
	    else if (helmet.getType() == Material.GOLD_HELMET)
		red = red + 0.08;
	    else if (helmet.getType() == Material.CHAINMAIL_HELMET)
		red = red + 0.08;
	    else if (helmet.getType() == Material.IRON_HELMET)
		red = red + 0.08;
	    else if (helmet.getType() == Material.DIAMOND_HELMET)
		red = red + 0.12;
	}
	if (boots != null) {
	    if (boots.getType() == Material.LEATHER_BOOTS)
		red = red + 0.04;
	    else if (boots.getType() == Material.GOLD_BOOTS)
		red = red + 0.04;
	    else if (boots.getType() == Material.CHAINMAIL_BOOTS)
		red = red + 0.04;
	    else if (boots.getType() == Material.IRON_BOOTS)
		red = red + 0.08;
	    else if (boots.getType() == Material.DIAMOND_BOOTS)
		red = red + 0.12;
	}
	// Pants
	if (pants != null) {
	    if (pants.getType() == Material.LEATHER_LEGGINGS)
		red = red + 0.08;
	    else if (pants.getType() == Material.GOLD_LEGGINGS)
		red = red + 0.12;
	    else if (pants.getType() == Material.CHAINMAIL_LEGGINGS)
		red = red + 0.16;
	    else if (pants.getType() == Material.IRON_LEGGINGS)
		red = red + 0.20;
	    else if (pants.getType() == Material.DIAMOND_LEGGINGS)
		red = red + 0.24;
	}
	// Chest plate
	if (chest != null) {
	    if (chest.getType() == Material.LEATHER_CHESTPLATE)
		red = red + 0.12;
	    else if (chest.getType() == Material.GOLD_CHESTPLATE)
		red = red + 0.20;
	    else if (chest.getType() == Material.CHAINMAIL_CHESTPLATE)
		red = red + 0.20;
	    else if (chest.getType() == Material.IRON_CHESTPLATE)
		red = red + 0.24;
	    else if (chest.getType() == Material.DIAMOND_CHESTPLATE)
		red = red + 0.32;
	}
	return red;
    }

    /**
     * Tracks weather changes and acid rain
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeatherChange(final WeatherChangeEvent e) {
	// Check that they are in the ASkyBlock world
	// plugin.getLogger().info("weather change noted");
	if (!e.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	this.isRaining = e.toWeatherState();
	// plugin.getLogger().info("is raining = " + isRaining);
    }

}