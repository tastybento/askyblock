package com.wasteofplastic.askyblock;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.plugin.java.JavaPlugin;

public final class FileLister {
    private final ASkyBlock plugin;
    private final static String FOLDERPATH = "locale";

   public FileLister(ASkyBlock plugin) {
        this.plugin = plugin;
    }

   public List<String> list() throws IOException {
        List<String> result = new ArrayList<String>();
        
        // Check if the locale folder exists
        File localeDir = new File(plugin.getDataFolder(), FOLDERPATH);
        if (localeDir.exists()) {
            FilenameFilter ymlFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String lowercaseName = name.toLowerCase();
                    if (lowercaseName.endsWith(".yml")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };
            for (String fileName : localeDir.list(ymlFilter)) {
                result.add(fileName.replace(".yml", ""));
            }
            if (!result.isEmpty())
                return result;
        }
        // Else look in the JAR
        File jarfile = null;

        /**
         * Get the jar file from the plugin.
         */
        try {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);

            jarfile = (File) method.invoke(this.plugin);
        } catch (Exception e) {
            throw new IOException(e);
        }

        JarFile jar = new JarFile(jarfile);

        /**
         * Loop through all the entries.
         */
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();

            /**
             * Not in the folder.
             */
            if (!path.startsWith(FOLDERPATH)) {
                continue;
            }

            if (entry.getName().endsWith(".yml")) {
                result.add((entry.getName().replace(".yml","")).replace("locale/", ""));
            }

        }
        jar.close();
        return result;
    }
}