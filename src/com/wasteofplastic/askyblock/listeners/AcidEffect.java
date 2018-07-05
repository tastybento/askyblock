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
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.Settings.GameType;
import com.wasteofplastic.askyblock.events.AcidEvent;
import com.wasteofplastic.askyblock.events.AcidRainEvent;
import com.wasteofplastic.askyblock.util.Util;
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
    private static final boolean DEBUG = false;

    public AcidEffect(final ASkyBlock pluginI) {
        plugin = pluginI;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());

        burningPlayers.remove(e.getEntity());
        wetPlayers.remove(e.getEntity());
        PlayerEvents.unsetFalling(e.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        // Fast return if acid isn't being used
        if (Settings.rainDamage == 0 && Settings.acidDamage == 0) {
            return;
        }
        final Player player = e.getPlayer();
        // Fast checks
        if (player.isDead() || player.getGameMode().toString().startsWith("SPECTATOR")) {
            return;
        }
        // Check if in teleport
        if (plugin.getPlayers().isInTeleport(player.getUniqueId())) {
            return;
        }
        // Check that they are in the ASkyBlock world
        if (!player.getWorld().equals(ASkyBlock.getIslandWorld())) {
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

        /*
        if (!e.getTo().toVector().equals(e.getFrom().toVector())) {
            // Head movements only
            return;
        }*/
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Acid Effect " + e.getEventName());

        // Slow checks
        final Location playerLoc = player.getLocation();
        final Block block = playerLoc.getBlock();
        final Block head = block.getRelative(BlockFace.UP);

        // Check for acid rain
        if (Settings.rainDamage > 0D && isRaining) {
            // Only check if they are in a non-dry biome
            Biome biome = playerLoc.getBlock().getBiome();
            if (biome != Biome.DESERT && biome != Biome.DESERT_HILLS
                    && biome != Biome.SAVANNA && biome != Biome.MESA && biome != Biome.HELL) {
                if (isSafeFromRain(player)) {
                    // plugin.getLogger().info("DEBUG: not hit by rain");
                    wetPlayers.remove(player);
                } else {
                    // plugin.getLogger().info("DEBUG: hit by rain");
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
                                // Check if it is still raining or player is safe or dead or there is no damage
                                if (!isRaining || player.isDead() || isSafeFromRain(player) || Settings.rainDamage <= 0D) {
                                    // plugin.getLogger().info("DEBUG: Player is dead or it has stopped raining");
                                    wetPlayers.remove(player);
                                    this.cancel();
                                    // Check they are still in this world
                                } else {
                                    double reduction = Settings.rainDamage * getDamageReduced(player);
                                    double totalDamage = (Settings.rainDamage - reduction);
                                    AcidRainEvent e = new AcidRainEvent(player, totalDamage, reduction);
                                    plugin.getServer().getPluginManager().callEvent(e);
                                    if (!e.isCancelled()) {
                                        player.damage(e.getRainDamage());
                                        if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                            player.getWorld().playSound(playerLoc, Sound.valueOf("FIZZ"), 3F, 3F);
                                        } else {
                                            player.getWorld().playSound(playerLoc, Sound.ENTITY_CREEPER_PRIMED, 3F, 3F);
                                        }
                                    }
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 20L);
                    }
                }
            }
        }

        // Find out if they are at the bottom of the sea and if so bounce them
        // back up
        if (playerLoc.getBlockY() < 1 && Settings.GAMETYPE.equals(GameType.ACIDISLAND)) {
            final Vector v = new Vector(player.getVelocity().getX(), 1D, player.getVelocity().getZ());
            player.setVelocity(v);
        }
        // If they are already burning in acid then return
        if (burningPlayers.contains(player)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: already burning in acid");
            return;
        }
        // plugin.getLogger().info("DEBUG: no acid water is false");
        if (isSafeFromAcid(player)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: safe from acid");
            return;
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
                if (player.isDead() || isSafeFromAcid(player)) {
                    burningPlayers.remove(player);
                    this.cancel();
                } else {
                    AcidEvent acidEvent = new AcidEvent(player, (Settings.acidDamage - Settings.acidDamage * getDamageReduced(player)), Settings.acidDamage * getDamageReduced(player), Settings.acidDamageType);
                    plugin.getServer().getPluginManager().callEvent(acidEvent);
                    if (!acidEvent.isCancelled()) {
                        for (PotionEffectType t : acidEvent.getPotionEffects()) {
                            if (t.equals(PotionEffectType.BLINDNESS) || t.equals(PotionEffectType.CONFUSION) || t.equals(PotionEffectType.HUNGER)
                                    || t.equals(PotionEffectType.SLOW) || t.equals(PotionEffectType.SLOW_DIGGING) || t.equals(PotionEffectType.WEAKNESS)) {
                                player.addPotionEffect(new PotionEffect(t, 600, 1));
                            } else {
                                // Poison
                                player.addPotionEffect(new PotionEffect(t, 200, 1));
                            }
                        }

                        // Apply damage if there is any
                        if (acidEvent.getTotalDamage() > 0D) {
                            player.damage(acidEvent.getTotalDamage());
                            if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                player.getWorld().playSound(playerLoc, Sound.valueOf("FIZZ"), 3F, 3F);
                            } else {
                                player.getWorld().playSound(playerLoc, Sound.ENTITY_CREEPER_PRIMED, 3F, 3F);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Check if player is safe from rain
     * @param player
     * @return true if they are safe
     */
    private boolean isSafeFromRain(Player player) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: safe from acid rain");
        if (!player.getWorld().equals(ASkyBlock.getIslandWorld())) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: wrong world");
            return true;
        }
        // Check if player has a helmet on and helmet protection is true
        if (Settings.helmetProtection && (player.getInventory().getHelmet() != null
                && player.getInventory().getHelmet().getType().name().contains("HELMET"))) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: wearing helmet.");
            return true;
        }
        // Check potions
        Collection<PotionEffect> activePotions = player.getActivePotionEffects();
        for (PotionEffect s : activePotions) {
            if (s.getType().equals(PotionEffectType.WATER_BREATHING)) {
                // Safe!
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: potion");
                return true;
            }
        }
        // Check if all air above player
        for (int y = player.getLocation().getBlockY() + 2; y < player.getLocation().getWorld().getMaxHeight(); y++) {
            if (!player.getLocation().getWorld().getBlockAt(player.getLocation().getBlockX(), y, player.getLocation().getBlockZ()).getType().equals(Material.AIR)) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: something other than air above player");
                return true;
            }
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: acid rain damage");
        return false;
    }

    /**
     * Check if player can be burned by acid
     * @param player
     * @return true if player is not safe
     */
    private boolean isSafeFromAcid(Player player) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: safe from acid");
        if (!player.getWorld().equals(ASkyBlock.getIslandWorld())) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: wrong world");
            return true;
        }
        // In liquid
        Material bodyMat = player.getLocation().getBlock().getType();
        Material headMat = player.getLocation().getBlock().getRelative(BlockFace.UP).getType();
        if (bodyMat.equals(Material.STATIONARY_WATER))
            bodyMat = Material.WATER;
        if (headMat.equals(Material.STATIONARY_WATER))
            headMat = Material.WATER;
        if (bodyMat != Material.WATER && headMat != Material.WATER) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: not in water " + player.getLocation().getBlock().isLiquid() + " " + player.getLocation().getBlock().getRelative(BlockFace.UP).isLiquid());
            return true;
        }
        // Check if player is in a boat
        Entity playersVehicle = player.getVehicle();
        if (playersVehicle != null) {
            // They are in a Vehicle
            if (playersVehicle.getType().equals(EntityType.BOAT)) {
                // I'M ON A BOAT! I'M ON A BOAT! A %^&&* BOAT!
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: boat");
                return true;
            }
        }
        // Check if full armor protects
        if (Settings.fullArmorProtection) {
            boolean fullArmor = true;
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item == null || (item != null && item.getType().equals(Material.AIR))) {
                    fullArmor = false;
                    break;
                }
            }
            if (fullArmor) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: full armor");
                return true;
            }
        }
        // Check if player has an active water potion or not
        Collection<PotionEffect> activePotions = player.getActivePotionEffects();
        for (PotionEffect s : activePotions) {
            // plugin.getLogger().info("Potion is : " +
            // s.getType().toString());
            if (s.getType().equals(PotionEffectType.WATER_BREATHING)) {
                // Safe!
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Water breathing potion protection!");
                return true;
            }
        }
        // Check if water above sea-level is not acid
        Island island = plugin.getGrid().getIslandAt(player.getLocation());
        if (island != null && !island.getIgsFlag(SettingsFlag.ACID_DAMAGE) && player.getLocation().getBlockY() > Settings.seaHeight) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG:no acid damage above sea level 1");
            return true;
        }
        if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.ACID_DAMAGE) && player.getLocation().getBlockY() > Settings.seaHeight) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: no acid damage above sea level");
            return true;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: burn in acid");
        return false;
    }

    /**
     * Enables changing of obsidian back into lava
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        //plugin.getLogger().info("DEBUG: " + Settings.allowObsidianScooping);
        if (!Settings.allowObsidianScooping) {
            return;
        }
        // Check that they are in the ASkyBlock world
        if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
            return;
        }
        if (!e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
            // Only Survival players can do this
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: obsidian scoop " + e.getEventName());

        if (plugin.getGrid().playerIsOnIsland(e.getPlayer())) {
            boolean otherOb = false;
            @SuppressWarnings("deprecation")
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
                    Util.sendMessage(e.getPlayer(), ChatColor.YELLOW + plugin.myLocale(e.getPlayer().getUniqueId()).changingObsidiantoLava);
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
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeatherChange(final WeatherChangeEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());

        // Check that they are in the ASkyBlock world
        // plugin.getLogger().info("weather change noted");
        if (!e.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
            return;
        }
        this.isRaining = e.toWeatherState();
        // plugin.getLogger().info("is raining = " + isRaining);
    }

}