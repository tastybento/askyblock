package com.wasteofplastic.askyblock.placeholders;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

/**
 * Handles hooks with other PlaceholderAPIs.
 * 
 * @author Poslovitch
 */
public class PlaceholderHandler {
    private static final String PACKAGE = "com.wasteofplastic." + Settings.PERMPREFIX + "placeholders.hooks.";
    /**
     * List of API classes in the above package
     */
    private static final String[] HOOKS = {
        "ASkyBlock", // TODO: needs to fix for AcidIsland
        "PlaceholderAPI",
        "DeluxeChat",
        "MVdW"
    };

    private static List<PlaceholderAPI> apis = new ArrayList<>();


    public static void register(ASkyBlock plugin){
        new Placeholders(plugin);
        for(String hook : HOOKS) {
            if (Bukkit.getPluginManager().isPluginEnabled(hook)) {
                try {
                    Class<?> clazz = Class.forName(PACKAGE + hook + "PlaceholderAPI");
                    PlaceholderAPI api = (PlaceholderAPI)clazz.newInstance();
                    if(api.register(plugin)){
                        if (!hook.equals("ASkyBlock")) { //TODO: needs fixing for AcidIsland
                            plugin.getLogger().info("Hooked into " + hook);
                        }
                        apis.add(api);
                    } else {
                        plugin.getLogger().info("Failed to hook into " + hook);
                    }
                } catch (Exception e) {
                    plugin.getLogger().info("Failed to hook into " + hook);
                    e.printStackTrace();
                }
            }
        }
    }

    public static Object newInstance(String className, Object...args) throws Exception {
        Class<?> clazz = Class.forName(className);
        if(args == null || args.length == 0) {
            return clazz.newInstance();
        }

        List<Class<?>> argTypes = new ArrayList<Class<?>>();
        for(Object object : args) {
            argTypes.add(object.getClass());
        }
        Constructor<?> explicitConstructor = clazz.getConstructor(argTypes.toArray(new Class[argTypes.size()]));
        return explicitConstructor.newInstance(args);
    }

    public static void unregister(ASkyBlock plugin) {
        Iterator<PlaceholderAPI> it = apis.iterator();
        while (it.hasNext()) {
            PlaceholderAPI api = it.next();
            //plugin.getLogger().info("DEBUG: " + api.getName());
            api.unregister(plugin);
            //plugin.getLogger().info("DEBUG: unregistered");
            it.remove();
        }
    }

    public static String replacePlaceholders(Player player, String message){
        if(message == null || message.isEmpty()) return "";

        for(PlaceholderAPI api : apis){
            message = api.replacePlaceholders(player, message);
        }

        return message;
    }

    /**
     * @return true if APIs are registered, otherwise false
     */
    public static boolean hasAPIs() {
        return apis != null ? true : false;  
    }
}
