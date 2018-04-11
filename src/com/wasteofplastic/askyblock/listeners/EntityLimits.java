package com.wasteofplastic.askyblock.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class EntityLimits implements Listener {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG2 = false;
    private static final boolean DEBUG3 = false;
    private ASkyBlock plugin;
    private YamlConfiguration entities;

    /**
     * Handles entity and natural limitations
     * @param plugin - ASkyBlock plugin object
     */
    public EntityLimits(ASkyBlock plugin) {
        this.plugin = plugin;
        if (Settings.saveEntities) {
            entities = Util.loadYamlFile("entitylimits.yml");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Settings.saveEntities || !IslandGuard.inWorld(event.getWorld())) {
            return;
        }
        Arrays.asList(event.getChunk().getEntities()).forEach(entity -> {
            String loc = entities.getString(event.getWorld().getName() + "." + event.getChunk().getX() + "." + event.getChunk().getZ() + "."
                    + entity.getUniqueId().toString(), "");
            if (!loc.isEmpty()) {
                entity.setMetadata("spawnLoc", new FixedMetadataValue(plugin, loc ));             
            }
        });
        // Delete the chunk data
        entities.set(event.getWorld().getName() + "." + event.getChunk().getX() + "." + event.getChunk().getZ() , null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!Settings.saveEntities || !IslandGuard.inWorld(event.getWorld())) {
            return;
        }
        // Delete the chunk data
        entities.set(event.getWorld().getName() + "." + event.getChunk().getX() + "." + event.getChunk().getZ() , null);
        // Create new entry
        Arrays.asList(event.getChunk().getEntities()).stream().filter(x -> x.hasMetadata("spawnLoc")).forEach(entity -> {
            // Get the meta data
            entity.getMetadata("spawnLoc").stream().filter(y -> y.getOwningPlugin().equals(plugin)).forEach(v -> {
                entities.set(event.getWorld().getName() + "." 
                        + event.getChunk().getX() + "." + event.getChunk().getZ() + "." 
                        + entity.getUniqueId().toString(), v.asString());            
            });
        });
        Util.saveYamlFile(entities, "entitylimits.yml");
    }

    public void disable() {
        if (!Settings.saveEntities) {
            return;
        }
        ASkyBlock.getIslandWorld().getEntities().stream().filter(x -> x.hasMetadata("spawnLoc")).forEach(entity -> {
            // Get the meta data
            entity.getMetadata("spawnLoc").stream().filter(y -> y.getOwningPlugin().equals(plugin)).forEach(v -> {
                entities.set(entity.getWorld().getName() + "." + entity.getLocation().getChunk().getX() + "." + entity.getLocation().getChunk().getZ() + "."
            + entity.getUniqueId().toString(), v.asString());            
            });
        });
        if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
            ASkyBlock.getNetherWorld().getEntities().stream().filter(x -> x.hasMetadata("spawnLoc")).forEach(entity -> {
                // Get the meta data
                entity.getMetadata("spawnLoc").stream().filter(y -> y.getOwningPlugin().equals(plugin)).forEach(v -> {
                    entities.set(entity.getWorld().getName() + "." + entity.getLocation().getChunk().getX() + "." + entity.getLocation().getChunk().getZ() + "."
                + entity.getUniqueId().toString(), v.asString());            
                });
            }); 
        }
        Util.saveYamlFile(entities, "entitylimits.yml");
    }

    /**
     * Action allowed in this location
     * @param location
     * @param flag
     * @return true if allowed
     */
    private boolean actionAllowed(Location location, SettingsFlag flag) {
        Island island = plugin.getGrid().getProtectedIslandAt(location);
        if (island != null && island.getIgsFlag(flag)){
            return true;
        }
        if (island == null && Settings.defaultWorldSettings.get(flag)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if action is allowed for player in location for flag
     * @param player
     * @param location
     * @param flag
     * @return true if allowed
     */
    private boolean actionAllowed(Player player, Location location, SettingsFlag flag) {
        if (player == null) {
            return actionAllowed(location, flag);
        }
        // This permission bypasses protection
        if (player.isOp() || VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
            return true;
        }
        Island island = plugin.getGrid().getProtectedIslandAt(location);
        if (island != null && (island.getIgsFlag(flag) || island.getMembers().contains(player.getUniqueId()))){
            return true;
        }
        if (island == null && Settings.defaultWorldSettings.get(flag)) {
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAnimalSpawn(final CreatureSpawnEvent e) {
        // If not in the right world, return
        if (!IslandGuard.inWorld(e.getEntity())) {
            return;
        }
        // If not an animal
        if (!(e.getEntity() instanceof Animals) && !e.getEntityType().equals(EntityType.SQUID)) {
            return;
        }
        if (DEBUG2) {
            plugin.getLogger().info("Animal spawn event! " + e.getEventName());
            plugin.getLogger().info(e.getSpawnReason().toString());
            plugin.getLogger().info(e.getEntityType().toString());
        }
        // If there's no limit - leave it
        if (Settings.breedingLimit <= 0) {
            if (DEBUG2)
                plugin.getLogger().info("No limit on breeding or spawning");
            return;
        }
        // We only care about spawning and breeding
        if (e.getSpawnReason() != SpawnReason.SPAWNER && e.getSpawnReason() != SpawnReason.BREEDING && e.getSpawnReason() != SpawnReason.EGG
                && e.getSpawnReason() != SpawnReason.DISPENSE_EGG && e.getSpawnReason() != SpawnReason.SPAWNER_EGG && !e.getSpawnReason().name().contains("BABY")) {
            if (DEBUG2)
                plugin.getLogger().info("Not Spawner or breeding");
            return;
        }
        LivingEntity animal = e.getEntity();
        Island island = plugin.getGrid().getProtectedIslandAt(animal.getLocation());
        if (island == null) {
            // Animal is spawning outside of an island so ignore
            if (DEBUG2)
                plugin.getLogger().info("Outside island, so spawning is okay");
            return;
        }
        // Count how many animals are there and who is the most likely spawner if it was a player
        // This had to be reworked because the previous snowball approach doesn't work for large volumes
        List<Player> culprits = new ArrayList<Player>();
        boolean overLimit = false;
        int animals = 0;
        for (int x = island.getMinProtectedX() /16; x <= (island.getMinProtectedX() + island.getProtectionSize() - 1)/16; x++) {
            for (int z = island.getMinProtectedZ() /16; z <= (island.getMinProtectedZ() + island.getProtectionSize() - 1)/16; z++) {
                for (Entity entity : ASkyBlock.getIslandWorld().getChunkAt(x, z).getEntities()) {
                    if (entity instanceof Animals || entity.getType().equals(EntityType.SQUID)) {
                        if (DEBUG2)
                            plugin.getLogger().info("DEBUG: Animal count is " + animals);
                        animals++;
                        if (animals >= Settings.breedingLimit) {
                            // Delete any extra animals
                            overLimit = true;
                            animal.remove();
                            if (DEBUG2)
                                plugin.getLogger().info("Over limit! >=" + Settings.breedingLimit);
                            e.setCancelled(true);
                        }
                    } else if (entity instanceof Player && e.getSpawnReason() != SpawnReason.SPAWNER && e.getSpawnReason() != SpawnReason.DISPENSE_EGG) {
                        for (ItemStack itemInHand: Util.getPlayerInHandItems((Player) entity)) {
                            if (itemInHand != null) {
                                Material type = itemInHand.getType();
                                if (type == Material.EGG || type == Material.MONSTER_EGG || type == Material.WHEAT || type == Material.CARROT_ITEM
                                        || type == Material.SEEDS) {
                                    if (DEBUG2)
                                        plugin.getLogger().info("Player used egg or did breeding ");
                                    if (!culprits.contains((Player)entity)) {
                                        culprits.add(((Player) entity));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (DEBUG2)
            plugin.getLogger().info("Counting nether");
        // Nether check
        if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
            for (int x = island.getMinProtectedX() /16; x <= (island.getMinProtectedX() + island.getProtectionSize() - 1)/16; x++) {
                for (int z = island.getMinProtectedZ() /16; z <= (island.getMinProtectedZ() + island.getProtectionSize() - 1)/16; z++) {
                    for (Entity entity : ASkyBlock.getNetherWorld().getChunkAt(x, z).getEntities()) {
                        if (entity instanceof Animals || entity.getType().equals(EntityType.SQUID)) {
                            if (DEBUG2)
                                plugin.getLogger().info("DEBUG: Animal count is " + animals);
                            animals++;
                            if (animals >= Settings.breedingLimit) {
                                // Delete any extra animals
                                if (DEBUG2)
                                    plugin.getLogger().info("Over limit! >=" + Settings.breedingLimit);
                                overLimit = true;
                                animal.remove();
                                e.setCancelled(true);
                            }
                        } else if (entity instanceof Player && e.getSpawnReason() != SpawnReason.SPAWNER && e.getSpawnReason() != SpawnReason.DISPENSE_EGG) {
                            for (ItemStack itemInHand : Util.getPlayerInHandItems(((Player) entity))) {
                                Material type = itemInHand.getType();
                                if (type == Material.EGG || type == Material.MONSTER_EGG || type == Material.WHEAT || type == Material.CARROT_ITEM
                                        || type == Material.SEEDS) {
                                    if (!culprits.contains((Player)entity)) {
                                        culprits.add(((Player) entity));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (overLimit) {
            if (e.getSpawnReason() != SpawnReason.SPAWNER) {
                plugin.getLogger().warning(
                        "Island at " + island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ() + " hit the island animal breeding limit of "
                                + Settings.breedingLimit);
                for (Player player : culprits) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).moblimitsError.replace("[number]", String.valueOf(Settings.breedingLimit)));
                    plugin.getLogger().warning(player.getName() + " was trying to use " + Util.getPlayerInHandItems(player).toString());
                }
            }
        }
        // plugin.getLogger().info("DEBUG: Animal count is " + animals);
    }

    /**
     * Prevents mobs spawning naturally at spawn or in an island
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onNaturalMobSpawn(final CreatureSpawnEvent e) {
        // if grid is not loaded yet, return.
        if (plugin.getGrid() == null) {
            return;
        }
        // If not in the right world, return
        if (!IslandGuard.inWorld(e.getEntity())) {
            return;
        }
        // Deal with natural spawning
        if (e.getSpawnReason().equals(SpawnReason.NATURAL)
                || e.getSpawnReason().equals(SpawnReason.CHUNK_GEN)
                || e.getSpawnReason().equals(SpawnReason.DEFAULT)
                || e.getSpawnReason().equals(SpawnReason.MOUNT)
                || e.getSpawnReason().equals(SpawnReason.JOCKEY)
                || e.getSpawnReason().equals(SpawnReason.NETHER_PORTAL)) {
            if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime) {
                if (!actionAllowed(e.getLocation(), SettingsFlag.MONSTER_SPAWN)) {                
                    if (DEBUG3)
                        plugin.getLogger().info("Natural monster spawn cancelled.");
                    // Mobs not allowed to spawn
                    e.setCancelled(true);
                    return;
                }
            } else if (e.getEntity() instanceof Animals) {
                if (!actionAllowed(e.getLocation(), SettingsFlag.MOB_SPAWN)) {
                    // Animals are not allowed to spawn
                    if (DEBUG2)
                        plugin.getLogger().info("Natural animal spawn cancelled.");
                    e.setCancelled(true);
                    return;
                }
            }
        }
        if (DEBUG2) {
            plugin.getLogger().info("Mob spawn allowed " + e.getEventName());
            plugin.getLogger().info(e.getSpawnReason().toString());
            plugin.getLogger().info(e.getEntityType().toString());
        }
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBlockPlace(final BlockMultiPlaceEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: " + e.getEventName());
            if (e.getPlayer() == null) {
                plugin.getLogger().info("DEBUG: player is null");
            } else {
                plugin.getLogger().info("DEBUG: block placed by " + e.getPlayer().getName());
            }
            plugin.getLogger().info("DEBUG: Block is " + e.getBlock().toString());
        }
        if (Settings.allowedFakePlayers.contains(e.getPlayer().getName())) return;

        // plugin.getLogger().info(e.getEventName());
        if (IslandGuard.inWorld(e.getPlayer())) {
            // This permission bypasses protection
            if (e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            Island island = plugin.getGrid().getProtectedIslandAt(e.getBlock().getLocation());
            if (island == null) {
                if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                    e.setCancelled(true);
                }
                return;
            }
            // Island exists
            if (island.getIgsFlag(SettingsFlag.PLACE_BLOCKS) || island.getMembers().contains(e.getPlayer().getUniqueId()))  {
                // Check how many placed
                //plugin.getLogger().info("DEBUG: block placed " + e.getBlock().getType());
                String type = e.getBlock().getType().toString();
                if (!e.getBlock().getState().getClass().getName().endsWith("CraftBlockState")
                        // Not all blocks have that type of class, so we have to do some explicit checking...
                        || e.getBlock().getType().equals(Material.REDSTONE_COMPARATOR_OFF)
                        || type.endsWith("BANNER") // Avoids V1.7 issues
                        || e.getBlock().getType().equals(Material.ENDER_CHEST)
                        || e.getBlock().getType().equals(Material.ENCHANTMENT_TABLE)
                        || e.getBlock().getType().equals(Material.DAYLIGHT_DETECTOR)
                        || e.getBlock().getType().equals(Material.FLOWER_POT)){
                    // tile entity placed
                    if (Settings.limitedBlocks.containsKey(type) && Settings.limitedBlocks.get(type) > -1) {
                        int count = island.getTileEntityCount(e.getBlock().getType(),e.getBlock().getWorld());
                        if (Settings.limitedBlocks.get(type) <= count) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + (plugin.myLocale(e.getPlayer().getUniqueId()).entityLimitReached.replace("[entity]",
                                    Util.prettifyText(type))).replace("[number]", String.valueOf(Settings.limitedBlocks.get(type))));
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
                return;
            }
            // Outside of protection area or visitor
            Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
            e.setCancelled(true);
        }
    }

    /**
     * Prevents placing of blocks
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: " + e.getEventName());
            if (e.getPlayer() == null) {
                plugin.getLogger().info("DEBUG: player is null");
            } else {
                plugin.getLogger().info("DEBUG: block placed by " + e.getPlayer().getName());
            }
            plugin.getLogger().info("DEBUG: Block is " + e.getBlock().toString());
        }

        if (Settings.allowedFakePlayers.contains(e.getPlayer().getName())) return;

        // plugin.getLogger().info(e.getEventName());
        if (IslandGuard.inWorld(e.getPlayer())) {
            // This permission bypasses protection
            if (e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            //plugin.getLogger().info("DEBUG: checking is inside protection area");
            Island island = plugin.getGrid().getProtectedIslandAt(e.getBlock().getLocation());
            // Outside of island protection zone
            if (island == null) {
                if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                    e.setCancelled(true);
                }
                return;
            }
            if (actionAllowed(e.getPlayer(), e.getBlock().getLocation(), SettingsFlag.PLACE_BLOCKS))  {
                // Check how many placed
                //plugin.getLogger().info("DEBUG: block placed " + e.getBlock().getType());
                String type = e.getBlock().getType().toString();
                if (!e.getBlock().getState().getClass().getName().endsWith("CraftBlockState")
                        // Not all blocks have that type of class, so we have to do some explicit checking...
                        || e.getBlock().getType().equals(Material.REDSTONE_COMPARATOR_OFF)
                        || type.endsWith("BANNER") // Avoids V1.7 issues
                        || e.getBlock().getType().equals(Material.ENDER_CHEST)
                        || e.getBlock().getType().equals(Material.ENCHANTMENT_TABLE)
                        || e.getBlock().getType().equals(Material.DAYLIGHT_DETECTOR)
                        || e.getBlock().getType().equals(Material.FLOWER_POT)){
                    // tile entity placed
                    if (Settings.limitedBlocks.containsKey(type) && Settings.limitedBlocks.get(type) > -1) {
                        int count = island.getTileEntityCount(e.getBlock().getType(),e.getBlock().getWorld());
                        //plugin.getLogger().info("DEBUG: count is "+ count);
                        if (Settings.limitedBlocks.get(type) <= count) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + (plugin.myLocale(e.getPlayer().getUniqueId()).entityLimitReached.replace("[entity]",
                                    Util.prettifyText(type))).replace("[number]", String.valueOf(Settings.limitedBlocks.get(type))));
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            } else {
                // Visitor
                Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBlockPlace(final HangingPlaceEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("DEBUG: block placed " + e.getBlock().getType());
            plugin.getLogger().info("DEBUG: entity " + e.getEntity().getType());
        }

        if (Settings.allowedFakePlayers.contains(e.getPlayer().getName())) return;

        // plugin.getLogger().info(e.getEventName());
        if (IslandGuard.inWorld(e.getPlayer())) {
            // This permission bypasses protection
            if (e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            Island island = plugin.getGrid().getProtectedIslandAt(e.getBlock().getLocation());
            // Outside of island protection zone
            if (island == null) {
                if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                    e.setCancelled(true);
                }
                return;
            }
            if (island.getIgsFlag(SettingsFlag.PLACE_BLOCKS) || island.getMembers().contains(e.getPlayer().getUniqueId()))  {
                // Check how many placed
                String type = e.getEntity().getType().toString();
                if (e.getEntity().getType().equals(EntityType.ITEM_FRAME) || e.getEntity().getType().equals(EntityType.PAINTING)) {
                    // tile entity placed
                    if (Settings.limitedBlocks.containsKey(type) && Settings.limitedBlocks.get(type) > -1) {
                        // Convert from EntityType to Material via string - ugh
                        int count = island.getTileEntityCount(Material.valueOf(type),e.getEntity().getWorld());
                        if (Settings.limitedBlocks.get(type) <= count) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + (plugin.myLocale(e.getPlayer().getUniqueId()).entityLimitReached.replace("[entity]",
                                    Util.prettifyText(type))).replace("[number]", String.valueOf(Settings.limitedBlocks.get(type))));
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            } else {
                // Visitor
                Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents trees from growing outside of the protected area.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTreeGrow(final StructureGrowEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Check world
        if (!IslandGuard.inWorld(e.getLocation())) {
            return;
        }
        // Check if this is on an island
        Island island = plugin.getGrid().getIslandAt(e.getLocation());
        if (island == null || island.isSpawn()) {
            return;
        }
        Iterator<BlockState> it = e.getBlocks().iterator();
        while (it.hasNext()) {
            BlockState b = it.next();
            if (b.getType() == Material.LOG || b.getType() == Material.LOG_2
                    || b.getType() == Material.LEAVES || b.getType() == Material.LEAVES_2) {
                if (!island.onIsland(b.getLocation())) {
                    it.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVillagerSpawn(final CreatureSpawnEvent e) {
        // If not an villager
        if (!(e.getEntity() instanceof Villager)) {
            return;
        }
        if (DEBUG3) {
            plugin.getLogger().info("Villager spawn event! " + e.getEventName());
            plugin.getLogger().info("Reason:" + e.getSpawnReason().toString());
            plugin.getLogger().info("Entity:" + e.getEntityType().toString());
        }
        // Only cover overworld
        if (!e.getEntity().getWorld().equals(ASkyBlock.getIslandWorld())) {
            return;
        }
        // If there's no limit - leave it
        if (Settings.villagerLimit <= 0) {
            return;
        }
        // We only care about villagers breeding, being cured or coming from a spawn egg, etc.
        if (e.getSpawnReason() != SpawnReason.SPAWNER && e.getSpawnReason() != SpawnReason.BREEDING
                && e.getSpawnReason() != SpawnReason.DISPENSE_EGG && e.getSpawnReason() != SpawnReason.SPAWNER_EGG
                && e.getSpawnReason() != SpawnReason.CURED) {
            return;
        }
        Island island = plugin.getGrid().getProtectedIslandAt(e.getLocation());
        if (island == null || island.getOwner() == null || island.isSpawn()) {
            // No island, no limit
            return;
        }
        int limit = Settings.villagerLimit * Math.max(1,plugin.getPlayers().getMembers(island.getOwner()).size());
        //plugin.getLogger().info("DEBUG: villager limit = " + limit);
        //long time = System.nanoTime();
        int pop = island.getPopulation();
        //plugin.getLogger().info("DEBUG: time = " + ((System.nanoTime() - time)*0.000000001));
        if (pop >= limit) {
            plugin.getLogger().warning(
                    "Island at " + island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ() + " hit the island villager limit of "
                            + limit);
            //plugin.getLogger().info("Stopped villager spawning on island " + island.getCenter());
            // Get all players in the area
            List<Entity> players = e.getEntity().getNearbyEntities(10,10,10);
            for (Entity player: players) {
                if (player instanceof Player) {
                    Player p = (Player) player;
                    Util.sendMessage(p, ChatColor.RED + plugin.myLocale(island.getOwner()).villagerLimitError.replace("[number]", String.valueOf(limit)));
                }
            }
            plugin.getMessages().tellTeam(island.getOwner(), ChatColor.RED + plugin.myLocale(island.getOwner()).villagerLimitError.replace("[number]", String.valueOf(limit)));
            if (e.getSpawnReason().equals(SpawnReason.CURED)) {
                // Easter Egg. Or should I say Easter Apple?
                ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE);
                // Nerfed
                //goldenApple.setDurability((short)1);
                e.getLocation().getWorld().dropItemNaturally(e.getLocation(), goldenApple);
            }
            e.setCancelled(true);
        }
    }

    /**
     * Handles minecart placing
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMinecart(VehicleCreateEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: " + e.getEventName());
            plugin.getLogger().info("DEBUG: Vehicle type = " + e.getVehicle().getType());
        }
        if (!IslandGuard.inWorld(e.getVehicle())) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Not in world");
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Checking entity types");
        if (Settings.entityLimits.containsKey(e.getVehicle().getType())) {
            // If someone in that area has the bypass permission, allow the spawning
            for (Entity entity : e.getVehicle().getLocation().getWorld().getNearbyEntities(e.getVehicle().getLocation(), 5, 5, 5)) {
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    Boolean bypass = false;
                    if (player.isOp() || VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypass")) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: op or bypass");
                        bypass = true;
                    }
                    // Check island
                    Island island = plugin.getGrid().getProtectedIslandAt(e.getVehicle().getLocation());

                    if (island == null) {
                        // Only count island entities
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: island is null");
                        return;
                    }
                    // Ignore spawn
                    if (island.isSpawn()) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: ignore spawn");
                        return;
                    }
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Checking entity limits");
                    // Check if the player is at the limit
                    if (atLimit(island, bypass, e.getVehicle())) {
                        e.setCancelled(true);
                        for (Entity ent : e.getVehicle().getLocation().getWorld().getNearbyEntities(e.getVehicle().getLocation(), 5, 5, 5)) {
                            if (ent instanceof Player) { 
                                Util.sendMessage((Player)ent, ChatColor.RED 
                                        + (plugin.myLocale(player.getUniqueId()).entityLimitReached.replace("[entity]", 
                                                Util.prettifyText(e.getVehicle().getType().toString()))
                                                .replace("[number]", String.valueOf(Settings.entityLimits.get(e.getVehicle().getType())))));
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(final CreatureSpawnEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: entity tracker " + e.getEventName());
        // Check world
        if (!e.getLocation().getWorld().toString().contains(Settings.worldName)) {
            return;
        }
        if (!Settings.entityLimits.containsKey(e.getEntityType())) {
            // Unknown entity limit or unlimited
            return;
        }
        boolean bypass = false;
        if (DEBUG)
            plugin.getLogger().info("DEBUG: spawn reason = " + e.getSpawnReason());
        // Check why it was spawned
        switch (e.getSpawnReason()) {
        // These reasons are due to a player being involved (usually)
        case BREEDING:
        case BUILD_IRONGOLEM:
        case BUILD_SNOWMAN:
        case BUILD_WITHER:
        case CURED:
        case EGG:
        case SPAWNER_EGG:
            // If someone in that area has the bypass permission, allow the spawning
            for (Entity entity : e.getLocation().getWorld().getNearbyEntities(e.getLocation(), 5, 5, 5)) {
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    if (player.isOp() || VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypass")) {
                        //plugin.getLogger().info("DEBUG: bypass");
                        bypass = true;
                        break;
                    }
                }
            }
            break;
        default:
            break;
        }
        // Tag the entity with the island spawn location
        Island island = plugin.getGrid().getIslandAt(e.getLocation());
        if (island == null) {
            // Only count island entities
            return;
        }
        // Ignore spawn
        if (island.isSpawn()) {
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Checking entity limits");
        // Check if the player is at the limit
        if (atLimit(island, bypass, e.getEntity())) {
            e.setCancelled(true);
            if (!e.getSpawnReason().equals(SpawnReason.SPAWNER)) {
                for (Entity ent : e.getLocation().getWorld().getNearbyEntities(e.getLocation(), 5, 5, 5)) {
                    if (ent instanceof Player) {
                        Player player = (Player)ent; 
                        Util.sendMessage(player, ChatColor.RED 
                                + (plugin.myLocale(player.getUniqueId()).entityLimitReached.replace("[entity]", 
                                        Util.prettifyText(e.getEntityType().toString()))
                                        .replace("[number]", String.valueOf(Settings.entityLimits.get(e.getEntityType())))));
                    }
                }
            }
        }
    }

    /**
     * Checks if new entities can be added to island
     * @param island
     * @param bypass - true if this is being done by a player with authorization to bypass limits
     * @param ent - the entity
     * @return true if at the limit, false if not
     */
    private boolean atLimit(Island island, boolean bypass, Entity ent) {
        int count = 0;
        checkLimits:
            if (bypass || Settings.entityLimits.get(ent.getType()) > 0) {
                // If bypass, just tag the creature. If not, then we need to count creatures
                if (!bypass) {
                    // Run through all the current entities on this world
                    for (Entity entity: ent.getWorld().getEntities()) {
                        // If it is the right one
                        if (entity.getType().equals(ent.getType())) {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: " + entity.getType() + " found");
                            // Check spawn location
                            if (entity.hasMetadata("spawnLoc")) {
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: has meta");
                                // Get the meta data
                                List<MetadataValue> values = entity.getMetadata("spawnLoc");
                                for (MetadataValue v : values) {
                                    // There is a chance another plugin also uses the meta data spawnLoc
                                    if (v.getOwningPlugin().equals(plugin)) {
                                        // Get the island spawn location
                                        Location spawnLoc = Util.getLocationString(v.asString());
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: entity spawnLoc = " + spawnLoc);
                                        if (spawnLoc != null && spawnLoc.equals(island.getCenter())) {
                                            // Entity is on this island
                                            count++;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: entity is on island. Number = " + count);
                                            if (count >= Settings.entityLimits.get(ent.getType())) {
                                                // No more allowed!
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: no more allowed! >=" + count);
                                                break checkLimits;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Okay to spawn, but tag it
                ent.setMetadata("spawnLoc", new FixedMetadataValue(plugin, Util.getStringLocation(island.getCenter())));
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: spawn okay");
                return false;
            }
        // Cancel - no spawning - tell nearby players
        if (DEBUG)
            plugin.getLogger().info("DEBUG: spawn cancelled");
        return true;
    }
}
