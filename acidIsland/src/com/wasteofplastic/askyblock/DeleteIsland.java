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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;


public class DeleteIsland extends BukkitRunnable {
    private ASkyBlock plugin;
    private Location l;
    private int counter;
    private int px;
    private int pz;
    private int slice;
    private static NMSAbstraction nms = null;


    /**
     * Class dedicated to deleting islands
     * @param plugin
     * @param loc
     */
    public DeleteIsland(ASkyBlock plugin, Location loc) {
	this.plugin = plugin;
	this.l = loc;
	this.counter = 255;
	this.px = l.getBlockX();
	this.pz = l.getBlockZ();
	try {
	    this.slice = checkVersion();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Checks what version the server is running and picks the appropriate NMS handler, or fallback
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private int checkVersion() throws ClassNotFoundException, IllegalArgumentException,
    SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException,
    NoSuchMethodException {
	// Calculate how many slices we should take without killing the server
	int slice = (int)Math.floor(255D * ((double)10000/(Settings.islandDistance*Settings.islandDistance)));
	if (slice < 10) {
	    slice = 10;
	}
	String serverPackageName = plugin.getServer().getClass().getPackage().getName();
	String pluginPackageName = plugin.getClass().getPackage().getName();
	String version = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
	Class<?> clazz;
	try {
	    //plugin.getLogger().info("Trying " + pluginPackageName + "." + version + ".NMSHandler");
	    clazz = Class.forName(pluginPackageName + "." + version + ".NMSHandler");
	} catch (Exception e) {
	    plugin.getLogger().info("No NMS Handler found, falling back to slow island delete.");
	    clazz = Class.forName(pluginPackageName + ".fallback.NMSHandler");
	    slice = (int)Math.floor(51D * ((double)10000/(Settings.islandDistance*Settings.islandDistance)));
	    if (slice < 10) {
		slice = 10;
	    }
	}
	//plugin.getLogger().info("Slice is = " + slice);
	//plugin.getLogger().info(serverPackageName);
	//plugin.getLogger().info(pluginPackageName);
	// Check if we have a NMSAbstraction implementing class at that location.
	if (NMSAbstraction.class.isAssignableFrom(clazz)) {
	    nms = (NMSAbstraction) clazz.getConstructor().newInstance();
	} else {
	    throw new IllegalStateException("Class " + clazz.getName() + " does not implement NMSAbstraction");
	}
	return slice;
    }

    @Override
    public void run() {
	//plugin.getLogger().info("DEBUG: removeIsland at location " + l.toString());
	removeSlice(counter, (counter-slice));
	counter = counter - slice;
	if (counter <=0) {
	    // Remove file from file system if it exists
	    final File islandFolder = new File(plugin.getDataFolder() + File.separator + "islands");
	    if (islandFolder.exists()) {
		String islandName = l.getBlockX() + "," + l.getBlockZ() + ".yml";
		final File islandFile = new File(plugin.getDataFolder() + File.separator + "islands" + File.separator + islandName);
		if (!islandFile.delete()) {
		    plugin.getLogger().severe("Problem deleting " + islandName + " from filesystem.");
		}
	    }
	    this.cancel();
	}
    }

    static class Pair {
	private final int left;
	private final int right;
	public Pair(int left, int right) {
	    this.left = left;
	    this.right = right;
	}
	public int getLeft() { return left; }
	public int getRight() { return right; }

	@Override
	public boolean equals(Object o) {
	    if (o == null) return false;
	    if (!(o instanceof Pair)) return false;
	    Pair pairo = (Pair) o;
	    return (this.left == pairo.getLeft()) && (this.right == pairo.getRight());
	}
    }

    void removeSlice(int top, int bottom) {
	List<Pair> chunks = new ArrayList<Pair>();	
	if (bottom <0)
	    bottom = 0;
	//plugin.unregisterEvents();
	// Cut island in slices
	for (int y = top; y >= bottom; y--) {
	    for (int x = Settings.island_protectionRange / 2 * -1; x <= Settings.island_protectionRange / 2; x++) {
		for (int z = Settings.island_protectionRange / 2 * -1; z <= Settings.island_protectionRange / 2; z++) {
		    final Block b = new Location(l.getWorld(), px + x, y, pz + z).getBlock();
		    final Pair chunkCoords = new Pair(b.getChunk().getX(),b.getChunk().getZ());
		    if (!chunks.contains(chunkCoords)) {
			chunks.add(chunkCoords);
		    }
		    final Material bt = b.getType();
		    Material setTo = Material.AIR;
		    // Split depending on below or above water line
		    if (y < Settings.sea_level) {
			setTo = Material.STATIONARY_WATER;
		    }
		    // Grab anything out of containers (do that it is
		    // destroyed)
		    switch (bt) {
		    case CHEST:
			//getLogger().info("DEBUG: Chest");
		    case TRAPPED_CHEST:
			//getLogger().info("DEBUG: Trapped Chest");
			final Chest c = (Chest) b.getState();
			final ItemStack[] items = new ItemStack[c.getInventory().getContents().length];
			c.getInventory().setContents(items);
			b.setType(setTo);
			break;
		    case FURNACE:
			final Furnace f = (Furnace) b.getState();
			final ItemStack[] i2 = new ItemStack[f.getInventory().getContents().length];
			f.getInventory().setContents(i2);
			b.setType(setTo);
			break;
		    case DISPENSER:
			final Dispenser d = (Dispenser) b.getState();
			final ItemStack[] i3 = new ItemStack[d.getInventory().getContents().length];
			d.getInventory().setContents(i3);
			b.setType(setTo);
			break;
		    case HOPPER:
			final Hopper h = (Hopper) b.getState();
			final ItemStack[] i4 = new ItemStack[h.getInventory().getContents().length];
			h.getInventory().setContents(i4);
			b.setType(setTo);
			break;
		    case SIGN_POST:
		    case WALL_SIGN:
		    case SIGN:
			//getLogger().info("DEBUG: Sign");
			b.setType(setTo);
			break;
		    case AIR:	
			if (setTo.equals(Material.STATIONARY_WATER)) {
			    nms.setBlockSuperFast(b, setTo);
			}
		    case STATIONARY_WATER:
			if (setTo.equals(Material.AIR)) {
			    nms.setBlockSuperFast(b, setTo);
			}
		    default:
			nms.setBlockSuperFast(b, setTo);
			break;
		    }
		}
	    }
	}	    
	//l.getWorld().refreshChunk(l.getChunk().getX(), l.getChunk().getZ());
	//plugin.restartEvents();
	// Refresh chunks that have been affected
	//plugin.getLogger().info("DEBUG: " + chunks.size() + " chunks need refreshing!");
	for (Pair p: chunks) {
	    l.getWorld().refreshChunk(p.getLeft(), p.getRight());
	    //plugin.getLogger().info("DEBUG: refreshing " + p.getLeft() + "," + p.getRight());
	}
    }
}
