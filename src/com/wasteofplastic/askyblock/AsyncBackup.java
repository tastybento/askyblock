package com.wasteofplastic.askyblock;

public class AsyncBackup {

    /**
     * Class to save the register and name database. This is done in an async way.
     * @param plugin - ASkyBlock plugin object - ASkyBlock plugin
     */
    public AsyncBackup(final ASkyBlock plugin) {
        // Save grid every 5 minutes
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getGrid().saveGrid();
            plugin.getTinyDB().asyncSaveDB();
            if (plugin.getTopTen() != null) {
                plugin.getTopTen().topTenSave();
            }
        }, Settings.backupDuration, Settings.backupDuration);
    }
    
}
