package com.wasteofplastic.askyblock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.commands.Challenges;
import com.wasteofplastic.askyblock.listeners.LavaCheck;
import com.wasteofplastic.askyblock.util.SpawnEgg1_9;
import com.wasteofplastic.askyblock.util.Util;

@SuppressWarnings("deprecation")
public class PluginConfig {
    // No push scoreboard name
    private final static String NO_PUSH_TEAM_NAME = "ASkyBlockNP";
    private static final boolean DEBUG = false;

    /**
     * Loads the various settings from the config.yml file into the plugin
     * @param plugin - ASkyBlock plugin object - askyblock
     * @return true if plugin config is loaded correctly
     */
    public static boolean loadPluginConfig(ASkyBlock plugin) {
        try {
            plugin.getConfig();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        // The order in this file should match the order in config.yml so that it is easy to check that everything is covered
        // ********************** Island settings **************************
        Settings.islandDistance = plugin.getConfig().getInt("island.distance", 200);
        if (Settings.islandDistance < 50) {
            Settings.islandDistance = 50;
            plugin.getLogger().info("Setting minimum island distance to 50");
        }
        if (Settings.islandDistance % 2 != 0) {
            plugin.getLogger().warning("Island distance should be even!");
        }
        Settings.islandProtectionRange = plugin.getConfig().getInt("island.protectionRange", 100);
        if (Settings.islandProtectionRange > Settings.islandDistance) {
            plugin.getLogger().warning("Protection range cannot be > island distance. Setting them to be equal.");
            Settings.islandProtectionRange = Settings.islandDistance;
        }
        if (Settings.islandProtectionRange <= 0) {
            plugin.getLogger().warning("Protection range of 0 in config.yml: To disable protection, the range should");
            plugin.getLogger().warning("equal the island distance and then you should allow all island protection flags");
            plugin.getLogger().warning("in config.yml. Protection range will be set to island distance (" + Settings.islandDistance + ")");
            Settings.islandProtectionRange = Settings.islandDistance;
        }
        if (Settings.islandProtectionRange % 2 != 0) {
            Settings.islandProtectionRange--;
            plugin.getLogger().warning("Protection range must be even, using " + Settings.islandProtectionRange);
        }
        // xoffset and zoffset are not public and only used for IslandWorld compatibility
        Settings.islandXOffset = plugin.getConfig().getInt("island.xoffset", 0);
        if (Settings.islandXOffset < 0) {
            Settings.islandXOffset = 0;
            plugin.getLogger().info("Setting minimum island X Offset to 0");
        } else if (Settings.islandXOffset > Settings.islandDistance) {
            Settings.islandXOffset = Settings.islandDistance;
            plugin.getLogger().info("Setting maximum island X Offset to " + Settings.islandDistance);
        }
        Settings.islandZOffset = plugin.getConfig().getInt("island.zoffset", 0);
        if (Settings.islandZOffset < 0) {
            Settings.islandZOffset = 0;
            plugin.getLogger().info("Setting minimum island Z Offset to 0");
        } else if (Settings.islandZOffset > Settings.islandDistance) {
            Settings.islandZOffset = Settings.islandDistance;
            plugin.getLogger().info("Setting maximum island Z Offset to " + Settings.islandDistance);
        }

        // This is the origin of new islands
        long x = plugin.getConfig().getLong("island.startx", 0);
        // Check this is a multiple of island distance
        long z = plugin.getConfig().getLong("island.startz", 0);
        Settings.islandStartX = Math.round((double) x / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
        Settings.islandStartZ = Math.round((double) z / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;

        // ASkyBlock and AcidIsland difference
        if (Settings.GAMETYPE.equals(Settings.GameType.ACIDISLAND)) {
            Settings.islandHeight = plugin.getConfig().getInt("island.islandlevel", 50) - 5;
            // The island's center is actually 5 below sea level
            Settings.seaHeight = plugin.getConfig().getInt("island.sealevel", 50);
        } else {
            // ASkyBlock
            Settings.islandHeight = plugin.getConfig().getInt("island.islandlevel", 120) - 5;
            // The island's center is actually 5 below sea level
            Settings.seaHeight = plugin.getConfig().getInt("island.sealevel", 0);
        }
        if (Settings.islandHeight < 0) {
            Settings.islandHeight = 0;
        }
        if (Settings.seaHeight < 0) {
            Settings.seaHeight = 0;
        }
        // Island reset settings
        Settings.resetLimit = plugin.getConfig().getInt("island.resetlimit", 2);
        if (Settings.resetWait < 0) {
            Settings.resetWait = -1;
        }
        Settings.resetWait = plugin.getConfig().getInt("island.resetwait", 300);
        if (Settings.resetWait < 0) {
            Settings.resetWait = 0;
        }
        // Seconds to wait for a confirmation of reset
        Settings.resetConfirmWait = plugin.getConfig().getInt("island.resetconfirmwait", 10);
        if (Settings.resetConfirmWait < 0) {
            Settings.resetConfirmWait = 0;
        }
        // Timeout required between duplicate team join/leaves
        Settings.inviteWait = plugin.getConfig().getInt("island.invitewait", 60);
        if (Settings.inviteWait < 0) {
            Settings.inviteWait = 0;
        }
        // Invite timeout before accept/reject timesout
        Settings.inviteTimeout = plugin.getConfig().getInt("island.invitetimeout", 60);
        Settings.inviteTimeout *= 20; // Convert to ticks

        // Max team size
        Settings.maxTeamSize = plugin.getConfig().getInt("island.maxteamsize", 4);
        // Deprecated settings - use permission askyblock.team.maxsize.<number> instead
        /*
        Settings.maxTeamSizeVIP = plugin.getConfig().getInt("island.vipteamsize", 0);
        Settings.maxTeamSizeVIP2 = plugin.getConfig().getInt("island.vip2teamsize", 0);
        if (Settings.maxTeamSizeVIP > 0 || Settings.maxTeamSizeVIP2 > 0) {
            plugin.getLogger().warning(Settings.PERMPREFIX + "team.vip and " + Settings.PERMPREFIX + "team.vip2 are deprecated!");
            plugin.getLogger().warning("Use permission " + Settings.PERMPREFIX + "team.maxsize.<number> instead.");
        }
         */
        // Island level cool down time
        Settings.levelWait = plugin.getConfig().getInt("island.levelwait", 60);
        if (Settings.levelWait < 0) {
            Settings.levelWait = 0;
        }

        // Get chest items
        String chestItems = plugin.getConfig().getString("island.chestItems","");
        if (!chestItems.isEmpty()) {
            final String[] chestItemString = chestItems.split(" ");
            // plugin.getLogger().info("DEBUG: chest items = " + chestItemString);
            final ItemStack[] tempChest = new ItemStack[chestItemString.length];
            for (int i = 0; i < tempChest.length; i++) {
                String[] amountdata = chestItemString[i].split(":");
                try {
                    if (amountdata.length == 3 && amountdata[0].equalsIgnoreCase("MONSTER_EGG")) {
                        try {
                            EntityType type = EntityType.valueOf(amountdata[1].toUpperCase());
                            if (Bukkit.getServer().getVersion().contains("(MC: 1.8") || Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
                                tempChest[i] = new SpawnEgg(type).toItemStack(Integer.parseInt(amountdata[2]));
                            } else {
                                try {
                                    tempChest[i] = new SpawnEgg1_9(type).toItemStack(Integer.parseInt(amountdata[2]));
                                } catch (Exception ex) {
                                    tempChest[i] = new ItemStack(Material.MONSTER_EGG);
                                    plugin.getLogger().severe("Monster eggs not supported with this server version.");
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Spawn eggs must be described by name. Try one of these (not all are possible):");
                            for (EntityType type : EntityType.values()) {
                                if (type.isSpawnable() && type.isAlive()) {
                                    plugin.getLogger().severe(type.toString());
                                }
                            }
                        }
                    } else if (amountdata[0].equals("POTION")) {
                        // plugin.getLogger().info("DEBUG: Potion length " +
                        // amountdata.length);
                        if (amountdata.length == 6) {
                            tempChest[i] = Challenges.getPotion(amountdata, Integer.parseInt(amountdata[5]), "config.yml");
                        } else {
                            plugin.getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
                            plugin.getLogger().severe("Potions for the chest must be fully defined as POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY");
                        }
                    } else {
                        Material mat;
                        if (StringUtils.isNumeric(amountdata[0])) {
                            mat = Material.getMaterial(Integer.parseInt(amountdata[0]));
                        } else {
                            mat = Material.getMaterial(amountdata[0].toUpperCase());
                        }
                        if (amountdata.length == 2) {
                            tempChest[i] = new ItemStack(mat, Integer.parseInt(amountdata[1]));
                        } else if (amountdata.length == 3) {
                            tempChest[i] = new ItemStack(mat, Integer.parseInt(amountdata[2]), Short.parseShort(amountdata[1]));
                        }
                    }
                } catch (java.lang.IllegalArgumentException ex) {
                    ex.printStackTrace();
                    plugin.getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
                    plugin.getLogger().severe("Error is : " + ex.getMessage());
                    plugin.getLogger().info("Potential potion types are: ");
                    for (PotionType c : PotionType.values())
                        plugin.getLogger().info(c.name());
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
                    plugin.getLogger().info("Potential material types are: ");
                    for (Material c : Material.values())
                        plugin.getLogger().info(c.name());
                    // e.printStackTrace();
                }
            }
            Settings.chestItems = tempChest;
        } else {
            // Nothing in the chest
            Settings.chestItems = new ItemStack[0];
        }
        // Defaul companion
        String companion = plugin.getConfig().getString("island.companion", "COW").toUpperCase();
        Settings.islandCompanion = null;
        if (!companion.equalsIgnoreCase("NOTHING")) {
            String commaList = "NOTHING, ";
            for (EntityType type : EntityType.values()) {
                if (companion.equalsIgnoreCase(type.toString())) {
                    Settings.islandCompanion = type;
                    break;
                }
                commaList += ", " + type.toString();
            }
            if (Settings.islandCompanion == null) {
                plugin.getLogger().warning("Island companion is not recognized. Pick from " + commaList);
            }
        }
        // Companion names
        List<String> companionNames = plugin.getConfig().getStringList("island.companionnames");
        Settings.companionNames = new ArrayList<String>();
        for (String name : companionNames) {
            Settings.companionNames.add(ChatColor.translateAlternateColorCodes('&', name));
        }

        // Island name length
        Settings.minNameLength = plugin.getConfig().getInt("island.minnamelength", 1);
        Settings.maxNameLength = plugin.getConfig().getInt("island.maxnamelength", 20);
        if (Settings.minNameLength < 0) {
            Settings.minNameLength = 0;
        }
        if (Settings.maxNameLength < 1) {
            Settings.maxNameLength = 1;
        }
        if (Settings.minNameLength > Settings.maxNameLength) {
            Settings.minNameLength = Settings.maxNameLength;
        }

        // Flymode expiration while flying oustide island boundaries
        Settings.flyTimeOutside = plugin.getConfig().getInt("island.flytimeoutside", 0);
        if(Settings.flyTimeOutside < 0) {
            Settings.flyTimeOutside = 0;
        }

        // Temporary Permissions while inside island
        Settings.temporaryPermissions = plugin.getConfig().getStringList("island.islandtemporaryperms");

        // System settings
        Settings.allowEndermanGriefing = plugin.getConfig().getBoolean("island.allowendermangriefing", true);
        Settings.endermanDeathDrop = plugin.getConfig().getBoolean("island.endermandeathdrop", true);
        Settings.allowCreeperDamage = plugin.getConfig().getBoolean("island.allowcreeperdamage", true);
        Settings.allowCreeperGriefing = plugin.getConfig().getBoolean("island.allowcreepergriefing", false);
        Settings.allowTNTDamage = plugin.getConfig().getBoolean("island.allowtntdamage", false);
        Settings.allowFireExtinguish = plugin.getConfig().getBoolean("island.allowfireextinguish", false);
        Settings.allowChestDamage = plugin.getConfig().getBoolean("island.allowchestdamage", false);
        Settings.allowVisitorKeepInvOnDeath = plugin.getConfig().getBoolean("island.allowvisitorkeepinvondeath", false);
        Settings.allowPistonPush = plugin.getConfig().getBoolean("island.allowpistonpush", true);
        Settings.allowMobDamageToItemFrames = plugin.getConfig().getBoolean("island.allowitemframedamage", false);

        // Clean up blocks around edges when deleting islands - this is a hidden setting not in the config.yml
        // Add if admins complain about slow cleaning.
        Settings.cleanRate = plugin.getConfig().getInt("island.cleanrate", 2);
        if (Settings.cleanRate < 1) {
            Settings.cleanRate = 1;
        }


        // ******** General Settings **********
        // Load world name
        Settings.worldName = plugin.getConfig().getString("general.worldName");
        // Check if the world name matches island.yml info
        File islandFile = new File(plugin.getDataFolder(), "islands.yml");
        if (islandFile.exists()) {
            YamlConfiguration islandYaml = new YamlConfiguration();
            try {
                islandYaml.load(islandFile);
                if (!islandYaml.contains(Settings.worldName)) {
                    // Bad news, stop everything and tell the admin
                    plugin.getLogger().severe("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
                    plugin.getLogger().severe("More set up is required. Go to config.yml and edit it.");
                    plugin.getLogger().severe("");
                    plugin.getLogger().severe("Check island world name is same as world in islands.yml.");
                    plugin.getLogger().severe("If you are resetting and changing world, delete island.yml and restart.");
                    plugin.getLogger().severe("");
                    plugin.getLogger().severe("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
                    return false;
                }
            } catch (Exception e) {}
        }

        // Get the default language
        Settings.defaultLanguage = plugin.getConfig().getString("general.defaultlanguage", "en-US");

        // Load languages
        HashMap<String,ASLocale> availableLocales = new HashMap<String,ASLocale>();
        FileLister fl = new FileLister(plugin);
        try {
            int index = 1;
            for (String code: fl.list()) {
                //plugin.getLogger().info("DEBUG: lang file = " + code);
                availableLocales.put(code, new ASLocale(plugin, code, index++));
            }
        } catch (IOException e1) {
            plugin.getLogger().severe("Could not add locales!");
        }
        if (!availableLocales.containsKey(Settings.defaultLanguage)) {
            plugin.getLogger().severe("'" + Settings.defaultLanguage + ".yml' not found in /locale folder. Using /locale/en-US.yml");
            Settings.defaultLanguage = "en-US";
            availableLocales.put(Settings.defaultLanguage, new ASLocale(plugin, Settings.defaultLanguage, 0));
        }
        plugin.setAvailableLocales(availableLocales);

        // Check for updates
        Settings.updateCheck = plugin.getConfig().getBoolean("general.checkupdates", true);

        // Silence command feedback
        Settings.silenceCommandFeedback = plugin.getConfig().getBoolean("general.silencecommandfeedback", true);

        // Action bar settings
        Settings.showInActionBar = plugin.getConfig().getBoolean("general.showinactionbar",true);

        // Max Islands
        Settings.maxIslands = plugin.getConfig().getInt("general.maxIslands",0);

        // Use control panel
        Settings.useControlPanel = plugin.getConfig().getBoolean("general.usecontrolpanel", false);

        // Create nether or not
        Settings.createNether = plugin.getConfig().getBoolean("general.createnether", true);
        if (!Settings.createNether) {
            plugin.getLogger().info("The Nether is disabled");
        }
        // Use the island nether
        Settings.newNether = plugin.getConfig().getBoolean("general.newnether", true);
        // Nether trees
        Settings.netherTrees = plugin.getConfig().getBoolean("general.nethertrees", true);
        // Nether roof option
        Settings.netherRoof = plugin.getConfig().getBoolean("general.netherroof", true);
        // Nether spawn protection radius
        Settings.netherSpawnRadius = plugin.getConfig().getInt("general.netherspawnradius", 25);
        if (Settings.netherSpawnRadius < 0) {
            Settings.netherSpawnRadius = 0;
        } else if (Settings.netherSpawnRadius > 100) {
            Settings.netherSpawnRadius = 100;
        }
        // Nether roof option
        Settings.netherRoof = plugin.getConfig().getBoolean("general.netherroof", true);

        // Run level calc at login
        Settings.loginLevel = plugin.getConfig().getBoolean("general.loginlevel", false);

        // Island reset commands
        Settings.resetCommands = plugin.getConfig().getStringList("general.resetcommands");
        Settings.leaveCommands = plugin.getConfig().getStringList("general.leavecommands");
        Settings.startCommands = plugin.getConfig().getStringList("general.startcommands");
        Settings.teamStartCommands = plugin.getConfig().getStringList("general.teamstartcommands");
        Settings.visitorCommandBlockList = plugin.getConfig().getStringList("general.visitorbannedcommands");

        // How long a player has to wait after deactivating PVP until they can activate PVP again
        Settings.pvpRestartCooldown = plugin.getConfig().getLong("general.pvpcooldown",60);

        // Invincible visitors
        Settings.invincibleVisitors = plugin.getConfig().getBoolean("general.invinciblevisitors", false);
        if(Settings.invincibleVisitors){
            Settings.visitorDamagePrevention = new HashSet<DamageCause>();
            List<String> damageSettings = plugin.getConfig().getStringList("general.invinciblevisitorsoptions");
            for (DamageCause cause: DamageCause.values()) {
                if (damageSettings.contains(cause.toString())) {
                    Settings.visitorDamagePrevention.add(cause);
                }
            }
        }
        // ASkyBlock and AcidIsland difference
        if (Settings.GAMETYPE.equals(Settings.GameType.ACIDISLAND)) {
            Settings.acidDamage = plugin.getConfig().getDouble("general.aciddamage", 5D);
            if (Settings.acidDamage > 100D) {
                Settings.acidDamage = 100D;
            } else if (Settings.acidDamage < 0D) {
                Settings.acidDamage = 0D;
            }
            Settings.mobAcidDamage = plugin.getConfig().getDouble("general.mobaciddamage", 10D);
            if (Settings.mobAcidDamage > 100D) {
                Settings.mobAcidDamage = 100D;
            } else if (Settings.mobAcidDamage < 0D) {
                Settings.mobAcidDamage = 0D;
            }
            Settings.rainDamage = plugin.getConfig().getDouble("general.raindamage", 0.5D);
            if (Settings.rainDamage > 100D) {
                Settings.rainDamage = 100D;
            } else if (Settings.rainDamage < 0D) {
                Settings.rainDamage = 0D;
            }
        } else {
            Settings.acidDamage = plugin.getConfig().getDouble("general.aciddamage", 0D);
            if (Settings.acidDamage > 100D) {
                Settings.acidDamage = 100D;
            } else if (Settings.acidDamage < 0D) {
                Settings.acidDamage = 0D;
            }
            Settings.mobAcidDamage = plugin.getConfig().getDouble("general.mobaciddamage", 0D);
            if (Settings.mobAcidDamage > 100D) {
                Settings.mobAcidDamage = 100D;
            } else if (Settings.mobAcidDamage < 0D) {
                Settings.mobAcidDamage = 0D;
            }
            Settings.rainDamage = plugin.getConfig().getDouble("general.raindamage", 0D);
            if (Settings.rainDamage > 100D) {
                Settings.rainDamage = 100D;
            } else if (Settings.rainDamage < 0D) {
                Settings.rainDamage = 0D;
            }
        }
        Settings.animalAcidDamage = plugin.getConfig().getDouble("general.animaldamage", 0D);
        if (Settings.animalAcidDamage > 100D) {
            Settings.animalAcidDamage = 100D;
        } else if (Settings.animalAcidDamage < 0D) {
            Settings.animalAcidDamage = 0D;
        }
        Settings.damageChickens = plugin.getConfig().getBoolean("general.damagechickens", false);
        // Destroy items in acid timer
        Settings.acidItemDestroyTime = plugin.getConfig().getLong("general.itemdestroyafter",0L) * 20L;
        Settings.damageOps = plugin.getConfig().getBoolean("general.damageops", false);
        // Helmet and full armor acid protection options
        Settings.helmetProtection = plugin.getConfig().getBoolean("general.helmetprotection", true);
        Settings.fullArmorProtection = plugin.getConfig().getBoolean("general.fullarmorprotection", false);
        // Damage Type
        List<String> acidDamageType = plugin.getConfig().getStringList("general.damagetype");
        Settings.acidDamageType.clear();
        if (acidDamageType != null) {
            for (String effect : acidDamageType) {
                PotionEffectType newPotionType = PotionEffectType.getByName(effect);
                if (newPotionType != null) {
                    // Check if it is a valid addition
                    if (newPotionType.equals(PotionEffectType.BLINDNESS) || newPotionType.equals(PotionEffectType.CONFUSION)
                            || newPotionType.equals(PotionEffectType.HUNGER) || newPotionType.equals(PotionEffectType.POISON)
                            || newPotionType.equals(PotionEffectType.SLOW) || newPotionType.equals(PotionEffectType.SLOW_DIGGING)
                            || newPotionType.equals(PotionEffectType.WEAKNESS)) {
                        Settings.acidDamageType.add(newPotionType);
                    }
                } else {
                    plugin.getLogger().warning("Could not interpret acid damage modifier: " + effect + " - skipping");
                    plugin.getLogger().warning("Types can be : SLOW, SLOW_DIGGING, CONFUSION,");
                    plugin.getLogger().warning("BLINDNESS, HUNGER, WEAKNESS and POISON");
                }
            }
        }

        Settings.logInRemoveMobs = plugin.getConfig().getBoolean("general.loginremovemobs", true);
        Settings.islandRemoveMobs = plugin.getConfig().getBoolean("general.islandremovemobs", false);
        List<String> mobWhiteList = plugin.getConfig().getStringList("general.mobwhitelist");
        Settings.mobWhiteList.clear();
        for (String mobName : mobWhiteList) {
            boolean mobFound = false;
            for (EntityType type: EntityType.values()) {
                if (mobName.toUpperCase().equals(type.toString())) {
                    try {
                        Settings.mobWhiteList.add(EntityType.valueOf(mobName.toUpperCase()));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error in config.yml, mobwhitelist value '" + mobName + "' is invalid.");
                    }
                    mobFound = true;
                    break;
                }
            }
            if (!mobFound) {
                plugin.getLogger().severe("Error in config.yml, mobwhitelist value '" + mobName + "' is invalid.");
                plugin.getLogger().severe("Possible values are : ");
                for (EntityType e : EntityType.values()) {
                    if (e.isAlive()) {
                        plugin.getLogger().severe(e.name());
                    }
                }
            }
        }

        Settings.monsterSpawnLimit = plugin.getConfig().getInt("general.monsterspawnlimit", 100);
        if (Settings.monsterSpawnLimit < -1) {
            Settings.monsterSpawnLimit = -1;
        }

        Settings.animalSpawnLimit = plugin.getConfig().getInt("general.animalspawnlimit", 15);
        if (Settings.animalSpawnLimit < -1) {
            Settings.animalSpawnLimit = -1;
        }

        Settings.breedingLimit = plugin.getConfig().getInt("general.breedinglimit", 0);

        Settings.waterAnimalSpawnLimit = plugin.getConfig().getInt("general.wateranimalspawnlimit", 15);
        if (Settings.waterAnimalSpawnLimit < -1) {
            Settings.waterAnimalSpawnLimit = -1;
        }

        Settings.villagerLimit = plugin.getConfig().getInt("general.villagerlimit", 0);

        Settings.limitedBlocks = new HashMap<String,Integer>();
        Settings.entityLimits = new HashMap<EntityType, Integer>();
        Settings.saveEntities = plugin.getConfig().getBoolean("general.saveentitylimits");
        Settings.coopsCanCreateWarps = plugin.getConfig().getBoolean("general.coopscancreatewarps");
        Settings.deleteProtectedOnly = plugin.getConfig().getBoolean("general.deleteprotectedonly", true);
        plugin.getLogger().info("Loading entity limits");
        ConfigurationSection entityLimits = plugin.getConfig().getConfigurationSection("general.entitylimits");
        if (entityLimits != null) {
            for (String entity: entityLimits.getKeys(false)) {
                int limit = entityLimits.getInt(entity.toUpperCase(), -1);
                // Check if this is an entity
                for (EntityType type : EntityType.values()) {
                    //plugin.getLogger().info("DEBUG: is " + entity + " = " + type.name() + "?");
                    if (type.name().equals(entity.toUpperCase())) {
                        //plugin.getLogger().info("DEBUG: yes");
                        Settings.entityLimits.put(type, limit);
                        if (limit > 0) {
                            plugin.getLogger().info(entity.toUpperCase() + " will be limited to " + limit);
                        }
                        break;
                    }
                }
                if (Material.getMaterial(entity.toUpperCase()) != null && limit > -1) {
                    Settings.limitedBlocks.put(entity.toUpperCase(), limit);
                    plugin.getLogger().info(entity.toUpperCase() + " will be limited to " + limit);
                    if (entity.equalsIgnoreCase("REDSTONE_COMPARATOR")) {
                        // Player can only ever place a redstone comparator in the OFF state
                        Settings.limitedBlocks.put("REDSTONE_COMPARATOR_OFF", limit);
                    } else if (entity.equalsIgnoreCase("BANNER")) {
                        // To simplify banners, the banner is allowed and automatically made wall and standing banner
                        Settings.limitedBlocks.put("WALL_BANNER", limit);
                        Settings.limitedBlocks.put("STANDING_BANNER", limit);
                    } else if (entity.equalsIgnoreCase("SIGN")) {
                        // To simplify signs, the sign is allowed and automatically made wall and standing signs
                        Settings.limitedBlocks.put("WALL_SIGN", limit);
                        Settings.limitedBlocks.put("SIGN_POST", limit);
                    }
                }
            }
        }

        // Level to purge
        Settings.abandonedIslandLevel = plugin.getConfig().getInt("general.abandonedislandlevel", 10);
        if (Settings.abandonedIslandLevel < 0) {
            Settings.abandonedIslandLevel = 0;
        }
        Settings.maxPurge = plugin.getConfig().getInt("general.maxpurge", 500);
        if (Settings.maxPurge < 0) {
            Settings.maxPurge = 0;
        }
        // Use economy or not
        // In future expand to include internal economy
        Settings.useEconomy = plugin.getConfig().getBoolean("general.useeconomy", true);

        // Reset money when an island is started
        Settings.resetMoney = plugin.getConfig().getBoolean("general.resetmoney", true);

        // Use the minishop or not
        Settings.useMinishop = plugin.getConfig().getBoolean("general.useminishop", true);

        // Starting money - default $0
        Settings.startingMoney = plugin.getConfig().getDouble("general.startingmoney", 0D);

        // Things to reset when an island is reset
        Settings.resetChallenges = plugin.getConfig().getBoolean("general.resetchallenges", true);
        Settings.clearInventory = plugin.getConfig().getBoolean("general.resetinventory", true);

        // Kicked players keep inventory
        Settings.kickedKeepInv = plugin.getConfig().getBoolean("general.kickedkeepinv", false);

        // Leavers lose resets
        Settings.leaversLoseReset = plugin.getConfig().getBoolean("general.leaversloseresets", true);

        // Reset the ender chest
        Settings.resetEnderChest = plugin.getConfig().getBoolean("general.resetenderchest", false);

        // Check if /island command is allowed when falling
        Settings.allowTeleportWhenFalling = plugin.getConfig().getBoolean("general.allowfallingteleport", true);
        Settings.fallingCommandBlockList = plugin.getConfig().getStringList("general.blockingcommands");

        // Challenges
        Settings.broadcastMessages = plugin.getConfig().getBoolean("general.broadcastmessages", true);
        Settings.removeCompleteOntimeChallenges = plugin.getConfig().getBoolean("general.removecompleteonetimechallenges", false);
        Settings.addCompletedGlow = plugin.getConfig().getBoolean("general.addcompletedglow", true);

        // Max home number
        Settings.maxHomes = plugin.getConfig().getInt("general.maxhomes",1);
        if (Settings.maxHomes < 1) {
            Settings.maxHomes = 1;
        }

        // Make island automatically
        Settings.makeIslandIfNone = plugin.getConfig().getBoolean("general.makeislandifnone", false);
        // Immediate teleport
        Settings.immediateTeleport = plugin.getConfig().getBoolean("general.immediateteleport", false);
        // Respawn on island
        Settings.respawnOnIsland = plugin.getConfig().getBoolean("general.respawnonisland", false);

        // Team chat
        Settings.teamChat = plugin.getConfig().getBoolean("general.teamchat", true);
        Settings.logTeamChat = plugin.getConfig().getBoolean("general.logteamchat", true);

        // Chat prefixes
        Settings.chatLevelPrefix = plugin.getConfig().getString("general.chatlevelprefix","{ISLAND_LEVEL}");
        Settings.chatChallengeLevelPrefix = plugin.getConfig().getString("general.chatchallanegelevelprefix","{ISLAND_CHALLENGE_LEVEL}");
        Settings.chatIslandPlayer = plugin.getConfig().getString("general.chatislandplayer","{ISLAND_PLAYER}");
        // Chat team suffixes - Not public right now
        Settings.setTeamName = plugin.getConfig().getBoolean("general.setteamsuffix", false);
        Settings.teamSuffix = plugin.getConfig().getString("general.teamsuffix","([level])");

        // Restrict wither
        Settings.restrictWither = plugin.getConfig().getBoolean("general.restrictwither", true);

        // Warp Restriction
        Settings.warpLevelsRestriction = plugin.getConfig().getInt("general.warplevelrestriction", 10);

        // Warp panel
        Settings.useWarpPanel = plugin.getConfig().getBoolean("general.usewarppanel", true);

        // Mute death messages
        Settings.muteDeathMessages = plugin.getConfig().getBoolean("general.mutedeathmessages", false);

        // How often the grid will be saved to file. Default is 5 minutes
        Settings.backupDuration = (plugin.getConfig().getLong("general.backupduration", 5) * 20 * 60);

        // Allow pushing
        Settings.allowPushing = plugin.getConfig().getBoolean("general.allowpushing", true);
        // try to remove the team from the scoreboard
        if (Settings.allowPushing) {
            try {
                ScoreboardManager manager = plugin.getServer().getScoreboardManager();
                if (manager != null) {
                    Scoreboard scoreboard = manager.getMainScoreboard();
                    if (scoreboard != null) {
                        Team pTeam = scoreboard.getTeam(NO_PUSH_TEAM_NAME);
                        if (pTeam != null) {
                            pTeam.unregister();
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Problem removing no push from scoreboard.");
            }
        }

        // Recover superflat
        Settings.recoverSuperFlat = plugin.getConfig().getBoolean("general.recoversuperflat");
        if (Settings.recoverSuperFlat) {
            plugin.getLogger().warning("*********************************************************");
            plugin.getLogger().warning("WARNING: Recover super flat mode is enabled");
            plugin.getLogger().warning("This will regenerate any chunks with bedrock at y=0 when they are loaded");
            plugin.getLogger().warning("Switch off when superflat chunks are cleared");
            plugin.getLogger().warning("You should back up your world before running this");
            plugin.getLogger().warning("*********************************************************");
        }

        // Persistent coops
        Settings.persistantCoops = plugin.getConfig().getBoolean("general.persistentcoops");
        // Only leader can coop
        Settings.onlyLeaderCanCoop = plugin.getConfig().getBoolean("general.onlyleadercancoop", false);
        // Can coop requests be rejected
        Settings.coopIsRequest = plugin.getConfig().getBoolean("general.coopisrequest", true);

        // Fake players
        Settings.allowedFakePlayers = plugin.getConfig().getStringList("general.fakeplayers");

        // Allow Obsidian Scooping
        Settings.allowObsidianScooping = plugin.getConfig().getBoolean("general.allowobsidianscooping", true);

        // Use old display (chat instead of GUI) for Island top ten
        Settings.displayIslandTopTenInChat = plugin.getConfig().getBoolean("general.islandtopteninchat", false);

        // Magic Cobble Generator
        Settings.useMagicCobbleGen = plugin.getConfig().getBoolean("general.usemagiccobblegen", false);
        if (Settings.useMagicCobbleGen) {
            Settings.magicCobbleGenOnlyAtSpawn = plugin.getConfig().getBoolean("general.magiccobblegenonlyatspawn", false);
            if(plugin.getConfig().isSet("general.magiccobblegenchances")){
                //plugin.getLogger().info("DEBUG: magic cobble gen enabled and chances section found");
                // Clear the cobble gen chances so they can be reloaded
                LavaCheck.clearChances();
                Settings.magicCobbleGenChances = new TreeMap<Long, TreeMap<Double,Material>>();
                for(String level : plugin.getConfig().getConfigurationSection("general.magiccobblegenchances").getKeys(false)){
                    long levelLong = 0;
                    try{
                        if(level.equals("default")) {
                            levelLong = Long.MIN_VALUE;
                        } else {
                            levelLong = Long.parseLong(level);
                        }
                        TreeMap<Double,Material> blockMapTree = new TreeMap<Double, Material>();
                        double chanceTotal = 0;
                        for(String block : plugin.getConfig().getConfigurationSection("general.magiccobblegenchances." + level).getKeys(false)){
                            double chance = plugin.getConfig().getDouble("general.magiccobblegenchances." + level + "." + block, 0D);
                            if(chance > 0 && Material.getMaterial(block) != null && Material.getMaterial(block).isBlock()) {
                                // Store the cumulative chance in the treemap. It does not need to add up to 100%
                                chanceTotal += chance;
                                blockMapTree.put(chanceTotal, Material.getMaterial(block));
                            }
                        }
                        if (!blockMapTree.isEmpty() && chanceTotal > 0) {
                            Settings.magicCobbleGenChances.put(levelLong, blockMapTree);
                            // Store the requested values as a % chance
                            Map<Material, Double> chances = new HashMap<Material, Double>();
                            for (Entry<Double, Material> en : blockMapTree.entrySet()) {
                                double chance = plugin.getConfig().getDouble("general.magiccobblegenchances." + level + "." + en.getValue(), 0D);
                                chances.put(en.getValue(), (chance/chanceTotal) * 100);
                            }
                            LavaCheck.storeChances(levelLong, chances);
                        }
                    } catch(NumberFormatException e){
                        // Putting the catch here means that an invalid level is skipped completely
                        plugin.getLogger().severe("Unknown level '" + level + "' listed in magiccobblegenchances section! Must be an integer or 'default'. Skipping...");
                    }
                }
            }
        }

        // Disable offline redstone
        Settings.disableOfflineRedstone = plugin.getConfig().getBoolean("general.disableofflineredstone", false);

        // Allow/disallow TNT pusing
        Settings.allowTNTPushing = plugin.getConfig().getBoolean("general.allowTNTpushing",true);

        // Fancy island level display
        Settings.fancyIslandLevelDisplay = plugin.getConfig().getBoolean("general.fancylevelinchat", false);

        // Check config.yml version
        String configVersion = plugin.getConfig().getString("general.version", "");
        //plugin.getLogger().info("DEBUG: config ver length " + configVersion.split("\\.").length);
        // Ignore last digit if it is 4 digits long
        if (configVersion.split("\\.").length == 4) {
            configVersion = configVersion.substring(0, configVersion.lastIndexOf('.'));
        }
        // Save for plugin version
        String version = plugin.getDescription().getVersion();
        //plugin.getLogger().info("DEBUG: version length " + version.split("\\.").length);
        if (version.split("\\.").length == 4) {
            version = version.substring(0, version.lastIndexOf('.'));
        }
        if (configVersion.isEmpty() || !configVersion.equalsIgnoreCase(version)) {
            // Check to see if this has already been shared
            File newConfig = new File(plugin.getDataFolder(),"config.new.yml");
            plugin.getLogger().warning("***********************************************************");
            plugin.getLogger().warning("Config file is out of date. See config.new.yml for updates!");
            plugin.getLogger().warning("config.yml version is '" + configVersion + "'");
            plugin.getLogger().warning("Latest config version is '" + version + "'");
            plugin.getLogger().warning("***********************************************************");
            if (!newConfig.exists()) {
                File oldConfig = new File(plugin.getDataFolder(),"config.yml");
                File bakConfig = new File(plugin.getDataFolder(),"config.bak");
                if (oldConfig.renameTo(bakConfig)) {
                    plugin.saveResource("config.yml", false);
                    oldConfig.renameTo(newConfig);
                    bakConfig.renameTo(oldConfig);
                }
            }
        }

        // *** Non-Public Settings - these are "secret" settings that may not be used anymore
        // This may be required if head issues grow...
        Settings.warpHeads = plugin.getConfig().getBoolean("general.warpheads", false);
        // Level logging
        Settings.levelLogging = plugin.getConfig().getBoolean("general.levellogging");
        // Custom generator
        Settings.useOwnGenerator = plugin.getConfig().getBoolean("general.useowngenerator", false);
        // Use physics when pasting island block schematics
        Settings.usePhysics = plugin.getConfig().getBoolean("general.usephysics", false);
        // Legacy setting support for hopper limiting
        if (Settings.limitedBlocks.isEmpty()) {
            Settings.hopperLimit = plugin.getConfig().getInt("general.hopperlimit", -1);
            if (Settings.hopperLimit > 0) {
                Settings.limitedBlocks.put("HOPPER", Settings.hopperLimit);
            }
        }
        // No acid bottles or buckets
        Settings.acidBottle = plugin.getConfig().getBoolean("general.acidbottles", true);

        // ************ Protection Settings ****************
        // Default settings hashmaps - make sure this is kept up to date with new settings
        // If a setting is not listed, the world default is used
        Settings.defaultWorldSettings.clear();
        Settings.defaultIslandSettings.clear();
        Settings.defaultSpawnSettings.clear();
        Settings.visitorSettings.clear();
        ConfigurationSection protectionWorld = plugin.getConfig().getConfigurationSection("protection.world");
        for (String setting: protectionWorld.getKeys(false)) {
            try {
                SettingsFlag flag = SettingsFlag.valueOf(setting.toUpperCase());
                boolean value = plugin.getConfig().getBoolean("protection.world." + flag.name());
                Settings.defaultWorldSettings.put(flag, value);
                Settings.defaultSpawnSettings.put(flag, value);
                Settings.defaultIslandSettings.put(flag, value);
            } catch (Exception e) {
                plugin.getLogger().severe("Unknown setting in config.yml:protection.world " + setting.toUpperCase() + " skipping...");
            }
        }
        // Establish defaults if they are missing in the config file.
        for (SettingsFlag flag: SettingsFlag.values()) {
            if (!Settings.defaultWorldSettings.containsKey(flag)) {
                plugin.getLogger().warning("config.yml:protection.world."+flag.name() + " is missing. You should add it to the config file. Setting to false by default");
                Settings.defaultWorldSettings.put(flag, false);
            }
            if (!Settings.defaultIslandSettings.containsKey(flag)) {
                Settings.defaultIslandSettings.put(flag, false);
            }
            if (!Settings.defaultSpawnSettings.containsKey(flag)) {
                Settings.defaultSpawnSettings.put(flag, false);
            }
        }
        ConfigurationSection protectionIsland = plugin.getConfig().getConfigurationSection("protection.island");
        for (String setting: protectionIsland.getKeys(false)) {
            try {
                SettingsFlag flag = SettingsFlag.valueOf(setting.toUpperCase());
                // Only items in the config.yml can be per island customized
                Settings.visitorSettings.put(flag, protectionIsland.getBoolean(setting));
                //plugin.getLogger().info("DEBUG: visitor flag added " + flag);
                Settings.defaultIslandSettings.put(flag, Settings.visitorSettings.get(flag));
            } catch (Exception e) {
                plugin.getLogger().severe("Unknown setting in config.yml:island.world " + setting.toUpperCase() + " skipping...");
            }
        }
        // ******************** Biome Settings *********************
        Settings.biomeCost = plugin.getConfig().getDouble("biomesettings.defaultcost", 100D);
        if (Settings.biomeCost < 0D) {
            Settings.biomeCost = 0D;
            plugin.getLogger().warning("Biome default cost is < $0, so set to zero.");
        }
        String defaultBiome = plugin.getConfig().getString("biomesettings.defaultbiome", "PLAINS");
        try {
            Settings.defaultBiome = Biome.valueOf(defaultBiome);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not parse biome " + defaultBiome + " using PLAINS instead.");
            Settings.defaultBiome = Biome.PLAINS;
        }
        // ******************** Schematic Section *******************
        // Hack skeleton spawners for 1.11
        Settings.hackSkeletonSpawners = plugin.getConfig().getBoolean("schematicsection.hackskeletonspawners", true);

        // ****************** Levels blockvalues.yml ****************
        // Get the blockvalues.yml file
        YamlConfiguration blockValuesConfig = Util.loadYamlFile("blockvalues.yml");
        // Get the under water multiplier
        Settings.deathpenalty = blockValuesConfig.getInt("deathpenalty", 0);
        Settings.sumTeamDeaths = blockValuesConfig.getBoolean("sumteamdeaths");
        Settings.maxDeaths = blockValuesConfig.getInt("maxdeaths", 10);
        Settings.islandResetDeathReset = blockValuesConfig.getBoolean("islandresetdeathreset", true);
        Settings.teamJoinDeathReset = blockValuesConfig.getBoolean("teamjoindeathreset", true);
        Settings.underWaterMultiplier = blockValuesConfig.getDouble("underwater", 1D);
        Settings.levelCost = blockValuesConfig.getInt("levelcost", 100);
        if (Settings.levelCost < 1) {
            Settings.levelCost = 1;
            plugin.getLogger().warning("levelcost in blockvalues.yml cannot be less than 1. Setting to 1.");
        }
        Settings.blockLimits = new HashMap<MaterialData, Integer>();
        if (blockValuesConfig.isSet("limits")) {
            for (String material : blockValuesConfig.getConfigurationSection("limits").getKeys(false)) {
                try {
                    String[] split = material.split(":");
                    byte data = 0;
                    if (split.length>1) {
                        data = Byte.valueOf(split[1]);
                    }
                    Material mat;
                    if (StringUtils.isNumeric(split[0])) {
                        mat = Material.getMaterial(Integer.parseInt(split[0]));
                    } else {
                        mat = Material.valueOf(split[0].toUpperCase());
                    }
                    MaterialData materialData = new MaterialData(mat);
                    materialData.setData(data);
                    Settings.blockLimits.put(materialData, blockValuesConfig.getInt("limits." + material, 0));
                    if (DEBUG) {
                        plugin.getLogger().info("Maximum number of " + materialData + " will be " + Settings.blockLimits.get(materialData));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Unknown material (" + material + ") in blockvalues.yml Limits section. Skipping...");
                }
            }
        }
        Settings.blockValues = new HashMap<MaterialData, Integer>();
        if (blockValuesConfig.isSet("blocks")) {
            for (String material : blockValuesConfig.getConfigurationSection("blocks").getKeys(false)) {
                try {
                    String[] split = material.split(":");
                    byte data = 0;
                    if (split.length>1) {
                        data = Byte.valueOf(split[1]);
                    }
                    MaterialData materialData = null;
                    if (StringUtils.isNumeric(split[0])) {
                        materialData = new MaterialData(Integer.parseInt(split[0]));
                    } else {
                        materialData = new MaterialData(Material.valueOf(split[0].toUpperCase()));
                    }

                    materialData.setData(data);
                    Settings.blockValues.put(materialData, blockValuesConfig.getInt("blocks." + material, 0));
                    if (DEBUG) {
                        plugin.getLogger().info(materialData.toString() + " value " + Settings.blockValues.get(materialData));
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                    plugin.getLogger().warning("Unknown material (" + material + ") in blockvalues.yml blocks section. Skipping...");
                }
            }
        } else {
            plugin.getLogger().severe("No block values in blockvalues.yml! All island levels will be zero!");
        }
        // All done
        return true;
    }
}
