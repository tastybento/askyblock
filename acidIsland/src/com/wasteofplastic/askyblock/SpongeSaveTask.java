package com.wasteofplastic.askyblock;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class SpongeSaveTask implements Runnable {
    final ASkyBlock plugin;

    public SpongeSaveTask(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    public void run() {
	try {
	    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(plugin.spongeDbLocation));
	    oos.writeObject(plugin.spongeAreas);
	    oos.flush();
	    oos.close();
	} catch (Exception e) {
	    plugin.getLogger().severe("Error occured while saving sponge database!");
	}
    }

}
