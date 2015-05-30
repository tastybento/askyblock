package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * A class that calculates finds a safe spot asynchronously and then teleports the player there. 
 * @author tastybento
 * 
 */
public class SafeSpotTeleport {

    private boolean setHome = false;
    private int homeNumber = 1;
    private String failureMessage = "";

    /**
     * Teleport to a safe place and if it fails, show a failure message
     * @param plugin
     * @param player
     * @param l
     * @param failureMessage
     */
    public SafeSpotTeleport(final ASkyBlock plugin, final Player player, final Location l, final String failureMessage) {
	this.failureMessage = failureMessage;
	new SafeSpotTeleport(plugin, player, l);
    }

    /**
     * Teleport to a safe place and set home
     * @param plugin
     * @param player
     * @param l
     * @param setHome
     */
    public SafeSpotTeleport(final ASkyBlock plugin, final Player player, final Location l, final int number) {
	this.setHome = true;
	this.homeNumber = number;
	new SafeSpotTeleport(plugin, player, l);
    }
    /**
     * Teleport to a safe spot on an island

     * @param plugin
     * @param player
     * @param islandLoc
     */
    public SafeSpotTeleport(final ASkyBlock plugin, final Player player, final Location islandLoc) {
	//plugin.getLogger().info("DEBUG: running safe spot");
	// Get island
	Island island = plugin.getGrid().getIslandAt(islandLoc);
	if (island != null) {
	    World world = islandLoc.getWorld();
	    // Get the chunks
	    List<ChunkSnapshot> chunkSnapshot = new ArrayList<ChunkSnapshot>();
	    // Add the center chunk
	    chunkSnapshot.add(island.getCenter().toVector().toLocation(world).getChunk().getChunkSnapshot());
	    // Add immediately adjacent chunks
	    for (int x = islandLoc.getChunk().getX()-1; x <= islandLoc.getChunk().getX()+1; x++) {
		for (int z = islandLoc.getChunk().getZ()-1; z <= islandLoc.getChunk().getZ()+1; z++) {
		    if (x != islandLoc.getChunk().getX() || z != islandLoc.getChunk().getZ()) {
			chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());
		    }
		}
	    }
	    // Add the rest of the island protected area
	    for (int x = island.getMinProtectedX() /16; x <= (island.getMinProtectedX() + island.getProtectionSize() - 1)/16; x++) {
		for (int z = island.getMinProtectedZ() /16; z <= (island.getMinProtectedZ() + island.getProtectionSize() - 1)/16; z++) {
		    // This includes the center spots again, so is not as efficient...
		    chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());
		}  
	    }
	    //plugin.getLogger().info("DEBUG: size of chunk ss = " + chunkSnapshot.size());
	    final List<ChunkSnapshot> finalChunk = chunkSnapshot;
	    int maxHeight = world.getMaxHeight();
	    if (world.getEnvironment().equals(Environment.NETHER)) {
		// We need to ignore the roof
		maxHeight -= 20;
	    }
	    final int worldHeight = maxHeight;
	    //plugin.getLogger().info("DEBUG:world height = " + worldHeight);
	    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

		@Override
		public void run() {
		    // Find a safe spot, defined as a solid block, with 2 air spaces above it
		    //long time = System.nanoTime();
		    int x = 0;
		    int y = 0;
		    int z = 0;
		    ChunkSnapshot currentChunk = null;
		    boolean safeSpotFound = false;
		    search:
			for (ChunkSnapshot chunk: finalChunk) {
			    currentChunk = chunk;
			    for (x = 0; x< 16; x++) {
				for (z = 0; z < 16; z++) {
				    // Work down from the entry point up
				    for (y = 0; y <= Math.min(chunk.getHighestBlockYAt(x, z), worldHeight) ; y++) {
					//System.out.println("Trying " + (16 * chunk.getX() + x) + " " + y + " " + (16 * chunk.getZ() + z));
					if (checkBlock(chunk,x,y,z)) {
					    safeSpotFound = true;
					    break search;
					}
				    }
				} //end z
			    } // end x
			}
		    // End search
		    //System.out.print("Seconds = " + ((System.nanoTime() - time) * 0.000000001));
		    if (currentChunk != null && safeSpotFound) {
			final Vector spot = new Vector((16 *currentChunk.getX()) + x + 0.5D, y, (16 * currentChunk.getZ()) + z + 0.5D);
			// Return to main thread and teleport the player
			plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

			    @Override
			    public void run() {
				Location destination = spot.toLocation(islandLoc.getWorld());
				//plugin.getLogger().info("DEBUG: safe spot found = " + destination);
				// Check that the destination is actually inside the player's island
				player.teleport(destination);
				if (setHome) {
				    plugin.getPlayers().setHomeLocation(player.getUniqueId(), destination, homeNumber);
				}
			    }});
		    } else {
			// We did not find a spot
			plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

			    @Override
			    public void run() {
				//plugin.getLogger().info("DEBUG: safe spot not found");
				player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNotSafe);
				// Send additional message
				if (!failureMessage.isEmpty()) {
				    player.sendMessage(failureMessage);
				}
			    }});
		    }
		}

		@SuppressWarnings("deprecation")
		private boolean checkBlock(ChunkSnapshot chunk, int x, int y, int z) {
		    int type = chunk.getBlockTypeId(x, y, z);
		    if (type != 0) { // AIR
			int space1 = chunk.getBlockTypeId(x, y + 1, z);
			int space2 = chunk.getBlockTypeId(x, y + 2, z);
			if (space1 == 0 && space2 == 0) {
			    // Now there is a chance that this is a safe spot
			    // Check for safe ground
			    Material mat = Material.getMaterial(type);
			    if (!mat.toString().contains("FENCE") 
				    && !mat.toString().contains("DOOR")
				    && !mat.toString().contains("GATE")
				    && !mat.toString().contains("PLATE")) {
				switch (mat) {
				// Unsafe
				case ANVIL:
				case BARRIER:
				case BOAT:
				case CACTUS:
				case DOUBLE_PLANT:
				case ENDER_PORTAL:
				case FIRE:
				case FLOWER_POT:
				case LADDER:
				case LAVA:
				case LEVER:
				case LONG_GRASS:
				case PISTON_EXTENSION:
				case PISTON_MOVING_PIECE:
				case PORTAL:
				case SIGN_POST:
				case SKULL:
				case STANDING_BANNER:
				case STATIONARY_LAVA:
				case STATIONARY_WATER:
				case STONE_BUTTON:
				case TORCH:
				case TRIPWIRE:
				case WATER:
				case WEB:
				case WOOD_BUTTON:
				    //System.out.println("Block is dangerous " + mat.toString());
				    break;
				default:
				    // Safe
				    //System.out.println("Block is safe " + mat.toString());
				    return true;
				}
			    }
			}
		    }
		    return false;
		}});
	}
    }
}