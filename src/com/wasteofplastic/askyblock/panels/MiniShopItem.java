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
package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.commands.Challenges;
import com.wasteofplastic.askyblock.util.SpawnEgg1_9;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 * 
 */
public class MiniShopItem {
    private int slot;
    private double price;
    private double sellPrice;
    private int quantity;
    private Material material;
    private String extra;
    private String description;
    private ItemStack item;
    private EntityType entityType;

    // private ASkyBlock plugin = ASkyBlock.getPlugin();

    /**
     * 
     */
    @SuppressWarnings("deprecation")
    public MiniShopItem(Material material, String extra, int slot, String description, int quantity, Double price, Double sellPrice) {
        this.slot = slot;
        this.material = material;
        if (description.isEmpty()) {
            description = Util.prettifyText(material.name());
        }
        this.description = description;
        this.price = price;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        // Make the item(s)
        try {
            item = new ItemStack(material);
            if (quantity < 1) {
                quantity = 1;
            }
            item.setAmount(quantity);
           // Deal with extras
            if (!extra.isEmpty()) {
                // plugin.getLogger().info("DEBUG: extra is not empty");                
                // If it not a potion, then the extras should just be durability
                if (!material.name().contains("POTION")) {
                    if (material.equals(Material.MONSTER_EGG)) {
                        try {
                            EntityType type = EntityType.valueOf(extra.toUpperCase());
                            if (Bukkit.getServer().getVersion().contains("(MC: 1.8") || Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
                                item = new SpawnEgg(type).toItemStack(quantity);
                            } else {
                                try {
                                    item = new SpawnEgg1_9(type).toItemStack(quantity);
                                } catch (Exception ex) {
                                    item = new ItemStack(material);
                                    Bukkit.getLogger().severe("Monster eggs not supported with this server version.");
                                }
                                ItemMeta meta = item.getItemMeta();
                            }
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Spawn eggs must be described by name. Try one of these (not all are possible):");                          
                            for (EntityType type : EntityType.values()) {
                                if (type.isSpawnable() && type.isAlive()) {
                                    Bukkit.getLogger().severe(type.toString());
                                }
                            }
                        }
                    } else if (!material.equals(Material.MOB_SPAWNER)) {
                        item.setDurability(Short.parseShort(extra));
                    }
                } else {
                    // Potion, splash potion or linger potion
                    extra = "POTION:" + extra;
                    String[] extras = extra.split(":");
                    item = Challenges.getPotion(extras, quantity, "minishop.yml");
                }
            }
            // Set the description and price
            ItemMeta meta = item.getItemMeta();
            // Split up the description
            List<String> desc = new ArrayList<String>(Arrays.asList(description.split("\\|")));
            meta.setDisplayName(desc.get(0));
            ArrayList<String> buyAndSell = new ArrayList<String>();
            if (material.equals(Material.MOB_SPAWNER) && !extra.isEmpty()) {
                //Bukkit.getLogger().info("DEBUG: mob spawner and extra is " + extra);
                // Get the entity type
                for (EntityType type : EntityType.values()) {
                    if (extra.toUpperCase().equals(type.name())) {
                        entityType = type;
                        break;
                    }
                }
            }
            if (desc.size() > 1) {
                desc.remove(0);// Remove the name
                buyAndSell.addAll(desc); // Add the rest to the description
            }
            // Create prices for buying and selling
            if (price > 0D) {
                buyAndSell.add(ASkyBlock.getPlugin().myLocale().minishopBuy + " " + quantity + " @ " + VaultHelper.econ.format(price));
            }
            if (sellPrice > 0D) {
                buyAndSell.add(ASkyBlock.getPlugin().myLocale().minishopSell + " " + quantity + " @ " + VaultHelper.econ.format(sellPrice));
            }
            if (price < 0D && sellPrice < 0D) {
                buyAndSell.add(ASkyBlock.getPlugin().myLocale().minishopOutOfStock);
            }
            meta.setLore(buyAndSell);
            item.setItemMeta(meta);
 
        } catch (Exception ex) {
            ASkyBlock.getPlugin().getLogger().severe("Problem parsing shop item from minishop.yml so skipping it: " + material);
            ASkyBlock.getPlugin().getLogger().severe("Error is : " + ex.getMessage());
            ex.printStackTrace();
            ASkyBlock.getPlugin().getLogger().info("Potential potion types are: ");
            for (PotionType c : PotionType.values())
                ASkyBlock.getPlugin().getLogger().info(c.name());
            ASkyBlock.getPlugin().getLogger().info("Potions can also be EXTENDED, SPLASH or EXTENDEDSPLASH, example WATER_BREATHING:EXTENDED");
        }
        // If there's no description, then set it.
        if (description == null) {
            this.description = Util.prettifyText(getDataName(item));
        }

    }

    /**
     * @return the item
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns a clean version of this item with no meta data
     * 
     * @return Clean item stack
     */
    public ItemStack getItemClean() {
        ItemStack temp = this.item.clone();
        ItemMeta meta = temp.getItemMeta();
        meta.setDisplayName(null);
        List<String> lore = new ArrayList<String>(1);
        if (item.getType().equals(Material.MOB_SPAWNER)) {  
            lore.add(Util.prettifyText(entityType.name()));
        }
        meta.setLore(lore);
        temp.setItemMeta(meta);
        return temp;
    }

    /**
     * @return the slot
     */
    public int getSlot() {
        return slot;
    }

    /**
     * @return the price
     */
    public double getPrice() {
        return price;
    }

    /**
     * @return the sellPrice
     */
    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * @return the material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param slot
     *            the slot to set
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }

    /**
     * @param price
     *            the price to set
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * @param quantity
     *            the quantity to set
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * @param material
     *            the material to set
     */
    public void setMaterial(Material material) {
        this.material = material;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the extra
     */
    public String getExtra() {
        return extra;
    }

    /**
     * @param extra
     *            the extra to set
     */
    public void setExtra(String extra) {
        this.extra = extra;
    }

    /**
     * Converts a given ItemStack into a pretty string
     * 
     * @param item
     *            The item stack
     * @return A string with the name of the item.
     */
    @SuppressWarnings("deprecation")
    private static String getDataName(ItemStack item) {
        Material mat = item.getType();
        // Find out durability, which indicates additional information on the
        // item, color, etc.
        short damage = item.getDurability();
        switch (mat) {
        case WOOL:
            switch ((int) damage) {
            case 0:
                return "WHITE_WOOL";
            case 1:
                return "ORANGE_WOOL";
            case 2:
                return "MAGENTA_WOOL";
            case 3:
                return "LIGHT_BLUE_WOOL";
            case 4:
                return "YELLOW_WOOL";
            case 5:
                return "LIME_WOOL";
            case 6:
                return "PINK_WOOL";
            case 7:
                return "GRAY_WOOL";
            case 8:
                return "LIGHT_GRAY_WOOL";
            case 9:
                return "CYAN_WOOL";
            case 10:
                return "PURPLE_WOOL";
            case 11:
                return "BLUE_WOOL";
            case 12:
                return "BROWN_WOOL";
            case 13:
                return "GREEN_WOOL";
            case 14:
                return "RED_WOOL";
            case 15:
                return "BLACK_WOOL";
            }
            return mat.toString();
        case INK_SACK:
            switch ((int) damage) {
            case 0:
                return "INK_SAC";
            case 1:
                return "ROSE_RED";
            case 2:
                return "CACTUS_GREEN";
            case 3:
                return "COCOA_BEANS";
            case 4:
                return "LAPIS_LAZULI";
            case 5:
                return "PURPLE_DYE";
            case 6:
                return "CYAN_DYE";
            case 7:
                return "LIGHT_GRAY_DYE";
            case 8:
                return "GRAY_DYE";
            case 9:
                return "PINK_DYE";
            case 10:
                return "LIME_DYE";
            case 11:
                return "DANDELION_YELLOW";
            case 12:
                return "LIGHT_BLUE_DYE";
            case 13:
                return "MAGENTA_DYE";
            case 14:
                return "ORANGE_DYE";
            case 15:
                return "BONE_MEAL";
            }
            return mat.toString();
        case SMOOTH_BRICK:
            switch ((int) damage) {
            case 0:
                return "STONE_BRICKS";
            case 1:
                return "MOSSY_STONE_BRICKS";
            case 2:
                return "CRACKED_STONE_BRICKS";
            case 3:
                return "CHISELED_STONE_BRICKS";
            }
            return mat.toString();
        case POTION:
            // Special case,.. Why?
            if (damage == 0)
                return "WATER_BOTTLE";
            Potion pot;
            // Convert the item stack to a potion. The try is just in case this
            // is not a potion, which it should be

            try {
                pot = Potion.fromItemStack(item);
            } catch (Exception e) {
                return "CUSTOM_POTION";
            }
            // Now we can parse out what the potion is from its effects and type
            String prefix = "";
            String suffix = "";
            if (pot.getLevel() > 0)
                suffix += "_" + pot.getLevel();
            if (pot.hasExtendedDuration())
                prefix += "EXTENDED_";
            if (pot.isSplash())
                prefix += "SPLASH_";
            // These are the useless or unused potions. Usually, these can only
            // be obtained by /give
            if (pot.getEffects().isEmpty()) {
                switch ((int) damage) {
                case 64:
                    return prefix + "MUNDANE_POTION" + suffix;
                case 7:
                    return prefix + "CLEAR_POTION" + suffix;
                case 11:
                    return prefix + "DIFFUSE_POTION" + suffix;
                case 13:
                    return prefix + "ARTLESS_POTION" + suffix;
                case 15:
                    return prefix + "THIN_POTION" + suffix;
                case 16:
                    return prefix + "AWKWARD_POTION" + suffix;
                case 32:
                    return prefix + "THICK_POTION" + suffix;
                case 23:
                    return prefix + "BUNGLING_POTION" + suffix;
                case 27:
                    return prefix + "SMOOTH_POTION" + suffix;
                case 31:
                    return prefix + "DEBONAIR_POTION" + suffix;
                case 39:
                    return prefix + "CHARMING_POTION" + suffix;
                case 43:
                    return prefix + "REFINED_POTION" + suffix;
                case 47:
                    return prefix + "SPARKLING_POTION" + suffix;
                case 48:
                    return prefix + "POTENT_POTION" + suffix;
                case 55:
                    return prefix + "RANK_POTION" + suffix;
                case 59:
                    return prefix + "ACRID_POTION" + suffix;
                case 63:
                    return prefix + "STINKY_POTION" + suffix;
                }
            } else {
                String effects = "";
                for (PotionEffect effect : pot.getEffects()) {
                    effects += effect.toString().split(":")[0];
                }
                return prefix + effects + suffix;
            }
            return mat.toString();
        case SAPLING:
            switch ((int) damage) {
            case 0:
                return "OAK_SAPLING";
            case 1:
                return "PINE_SAPLING";
            case 2:
                return "BIRCH_SAPLING";
            case 3:
                return "JUNGLE_TREE_SAPLING";
            case 4:
                return "Acacia_Sapling";
            case 5:
                return "Dark_Oak_Sapling";
            }
            return mat.toString();

        case WOOD:
            switch ((int) damage) {
            case 0:
                return "OAK_PLANKS";
            case 1:
                return "PINE_PLANKS";
            case 2:
                return "BIRCH_PLANKS";
            case 3:
                return "JUNGLE_PLANKS";
            case 4:
                return "Acacia Planks";
            case 5:
                return "Dark Oak Planks";
            }
            return mat.toString();
        case LOG:
            switch (damage) {
            case 0:
                return "OAK_LOG";
            case 1:
                return "PINE_LOG";
            case 2:
                return "BIRCH_LOG";
            case 3:
                return "JUNGLE_LOG";
            }
            return mat.toString();
        case LEAVES:
            damage = (short) (damage % 4);
            switch (damage) {
            case 0:
                return "OAK_LEAVES";
            case 1:
                return "PINE_LEAVES";
            case 2:
                return "BIRCH_LEAVES";
            case 3:
                return "JUNGLE_LEAVES";
            } // Note Acacia and Dark Oak are LEAVES_2 for some reason...
            return mat.toString();
        case COAL:
            switch (damage) {
            case 0:
                return "COAL";
            case 1:
                return "CHARCOAL";
            }
            return mat.toString();
        case SANDSTONE:
            switch ((int) damage) {
            case 0:
                return "SANDSTONE";
            case 1:
                return "CHISELED_SANDSTONE";
            case 2:
                return "SMOOTH_SANDSTONE";
            }
            return mat.toString();
        case LONG_GRASS:
            switch ((int) damage) {
            case 0:
                return "DEAD_SHRUB";
            case 1:
                return "TALL_GRASS";
            case 2:
                return "FERN";
            }
            return mat.toString();
        case STEP:
            switch ((int) damage) {
            case 0:
                return "STONE_SLAB";
            case 1:
                return "SANDSTONE_SLAB";
            case 2:
                return "WOODEN_SLAB";
            case 3:
                return "COBBLESTONE_SLAB";
            case 4:
                return "BRICK_SLAB";
            case 5:
                return "STONE_BRICK_SLAB";
            case 6:
                return "Nether Brick Slab";
            case 7:
                return "Quartz Slab";
            }
            return mat.toString();
        case MONSTER_EGG:
            switch ((int) damage) {
            case 50:
                return "CREEPER_EGG";
            case 51:
                return "SKELETON_EGG";
            case 52:
                return "SPIDER_EGG";
            case 53:
                // Unused
                return "GIANT_EGG";
            case 54:
                return "ZOMBIE_EGG";
            case 55:
                return "SLIME_EGG";
            case 56:
                return "GHAST_EGG";
            case 57:
                return "ZOMBIE_PIGMAN_EGG";
            case 58:
                return "ENDERMAN_EGG";
            case 59:
                return "CAVE_SPIDER_EGG";
            case 60:
                return "SILVERFISH_EGG";
            case 61:
                return "BLAZE_EGG";
            case 62:
                return "MAGMA_CUBE_EGG";
            case 63:
                return "ENDER_DRAGON_EGG";
            case 65:
                return "BAT_EGG";
            case 66:
                return "WITCH_EGG";
            case 90:
                return "PIG_EGG";
            case 91:
                return "SHEEP_EGG";
            case 92:
                return "COW_EGG";
            case 93:
                return "CHICKEN_EGG";
            case 94:
                return "SQUID_EGG";
            case 95:
                return "WOLF_EGG";
            case 96:
                return "MOOSHROOM_EGG";
            case 97:
                return "SNOW_GOLEM_EGG";
            case 98:
                return "OCELOT_EGG";
            case 99:
                return "IRON_GOLEM_EGG";
            case 100:
                return "HORSE_EGG";
            case 120:
                return "VILLAGER_EGG";
            case 200:
                return "ENDER_CRYSTAL_EGG";
            case 14:
                return "PRIMED_TNT_EGG";
            }
            return mat.toString();
        case SKULL_ITEM:
            switch ((int) damage) {
            case 0:
                return "SKELETON_SKULL";
            case 1:
                return "WITHER_SKULL";
            case 2:
                return "ZOMBIE_HEAD";
            case 3:
                return "PLAYER_HEAD";
            case 4:
                return "CREEPER_HEAD";
            }
            break;
        case REDSTONE_TORCH_OFF:
        case REDSTONE_TORCH_ON:
            return "REDSTONE_TORCH";
        case NETHER_STALK:
            return "NETHER_WART";
        case WEB:
            return "COBWEB";
        case THIN_GLASS:
            return "GLASS_PANE";
        case IRON_FENCE:
            return "IRON_BARS";
        case WORKBENCH:
            return "CRAFTING_TABLE";
        case REDSTONE_LAMP_ON:
        case REDSTONE_LAMP_OFF:
            return "REDSTONE_LAMP";
        case POTATO_ITEM:
            return "POTATO";
        case SULPHUR:
            return "GUNPOWDER";
        case CARROT_ITEM:
            return "CARROT";
        case GOLDEN_APPLE:
            switch ((int) damage) {
            case 0:
                return "GOLDEN_APPLE";
            case 1:
                return "ENCHANTED_GOLDEN_APPLE";
            }
            break;
        case FLOWER_POT:
            return "FLOWER_POT";
        case ANVIL:
            switch ((int) damage) {
            case 0:
                return "ANVIL";
            case 1:
                return "SLIGHTLY_DAMAGED_ANVIL";
            case 2:
                return "VERY_DAMAGED:ANVIL";
            }
            break;
        case EXP_BOTTLE:
            return "BOTTLE_O'_ENCHANTING";
        case FIREWORK_CHARGE:
            return "FIREWORK_STAR";
        case FIREBALL:
            return "FIREWORK_CHARGE";
        case ACACIA_STAIRS:
            break;
        case ACTIVATOR_RAIL:
            break;
        case AIR:
            break;
        case APPLE:
            break;
        case ARROW:
            break;
        case BAKED_POTATO:
            break;
        case BEACON:
            break;
        case BED:
            break;
        case BEDROCK:
            break;
        case BED_BLOCK:
            break;
        case BIRCH_WOOD_STAIRS:
            break;
        case BLAZE_POWDER:
            break;
        case BLAZE_ROD:
            break;
        case BOAT:
            break;
        case BONE:
            break;
        case BOOK:
            break;
        case BOOKSHELF:
            break;
        case BOOK_AND_QUILL:
            break;
        case BOW:
            break;
        case BOWL:
            break;
        case BREAD:
            break;
        case BREWING_STAND:
            break;
        case BREWING_STAND_ITEM:
            return "Brewing Stand";
        case BRICK:
            break;
        case BRICK_STAIRS:
            break;
        case BROWN_MUSHROOM:
            break;
        case BUCKET:
            break;
        case BURNING_FURNACE:
            break;
        case CACTUS:
            break;
        case CAKE:
            break;
        case CAKE_BLOCK:
            break;
        case CARPET:
            switch ((int) damage) {
            case 0:
                return "WHITE_CARPET";
            case 1:
                return "ORANGE_CARPET";
            case 2:
                return "MAGENTA_CARPET";
            case 3:
                return "LIGHT_BLUE_CARPET";
            case 4:
                return "YELLOW_CARPET";
            case 5:
                return "LIME_CARPET";
            case 6:
                return "PINK_CARPET";
            case 7:
                return "GRAY_CARPET";
            case 8:
                return "LIGHT_GRAY_CARPET";
            case 9:
                return "CYAN_CARPET";
            case 10:
                return "PURPLE_CARPET";
            case 11:
                return "BLUE_CARPET";
            case 12:
                return "BROWN_CARPET";
            case 13:
                return "GREEN_CARPET";
            case 14:
                return "RED_CARPET";
            case 15:
                return "BLACK_CARPET";
            }
            return mat.toString();
        case CARROT:
            break;
        case CARROT_STICK:
            break;
        case CAULDRON:
            break;
        case CAULDRON_ITEM:
            return "CAULDRON";
        case CHAINMAIL_BOOTS:
            break;
        case CHAINMAIL_CHESTPLATE:
            break;
        case CHAINMAIL_HELMET:
            break;
        case CHAINMAIL_LEGGINGS:
            break;
        case CHEST:
            break;
        case CLAY:
            break;
        case CLAY_BALL:
            break;
        case CLAY_BRICK:
            break;
        case COAL_BLOCK:
            break;
        case COAL_ORE:
            break;
        case COBBLESTONE:
            break;
        case COBBLESTONE_STAIRS:
            break;
        case COBBLE_WALL:
            break;
        case COCOA:
            break;
        case COMMAND:
            return "COMMAND_BLOCK";
        case COMMAND_MINECART:
            break;
        case COMPASS:
            break;
        case COOKED_BEEF:
            break;
        case COOKED_CHICKEN:
            break;
        case COOKED_FISH:
            break;
        case COOKIE:
            break;
        case CROPS:
            break;
        case DARK_OAK_STAIRS:
            break;
        case DAYLIGHT_DETECTOR:
            break;
        case DEAD_BUSH:
            break;
        case DETECTOR_RAIL:
            break;
        case DIAMOND:
            break;
        case DIAMOND_AXE:
            break;
        case DIAMOND_BARDING:
            break;
        case DIAMOND_BLOCK:
            break;
        case DIAMOND_BOOTS:
            break;
        case DIAMOND_CHESTPLATE:
            break;
        case DIAMOND_HELMET:
            break;
        case DIAMOND_HOE:
            break;
        case DIAMOND_LEGGINGS:
            break;
        case DIAMOND_ORE:
            break;
        case DIAMOND_PICKAXE:
            break;
        case DIAMOND_SPADE:
            return "Diamond Shovel";
        case DIAMOND_SWORD:
            break;
        case DIODE:
            break;
        case DIODE_BLOCK_OFF:
            break;
        case DIODE_BLOCK_ON:
            break;
        case DIRT:
            break;
        case DISPENSER:
            break;
        case DOUBLE_PLANT:
            switch ((int) damage) {
            case 0:
                return "SUNFLOWER";
            case 1:
                return "LILAC";
            case 2:
                return "DOUBLE_TALL_GRASS";
            case 3:
                return "LARGE_FERN";
            case 4:
                return "Rose Bush";
            case 5:
                return "Peony";
            }
            break;
        case DOUBLE_STEP:
            switch ((int) damage) {
            case 0:
                return "STONE_SLAB (DOUBLE)";
            case 1:
                return "SANDSTONE_SLAB (DOUBLE)";
            case 2:
                return "WOODEN_SLAB (DOUBLE)";
            case 3:
                return "COBBLESTONE_SLAB (DOUBLE)";
            case 4:
                return "BRICK_SLAB (DOUBLE)";
            case 5:
                return "STONE_BRICK_SLAB (DOUBLE)";
            case 6:
                return "Nether Brick Slab (DOUBLE)";
            case 7:
                return "Quartz Slab (DOUBLE)";
            case 8:
                return "Smooth Stone Slab (Double)";
            case 9:
                return "Smooth Sandstone Slab (Double)";
            }
            break;
        case DRAGON_EGG:
            break;
        case DROPPER:
            break;
        case EGG:
            break;
        case EMERALD:
            break;
        case EMERALD_BLOCK:
            break;
        case EMERALD_ORE:
            break;
        case EMPTY_MAP:
            break;
        case ENCHANTED_BOOK:
            break;
        case ENCHANTMENT_TABLE:
            break;
        case ENDER_CHEST:
            break;
        case ENDER_PEARL:
            break;
        case ENDER_PORTAL:
            break;
        case ENDER_PORTAL_FRAME:
            break;
        case ENDER_STONE:
            break;
        case EXPLOSIVE_MINECART:
            break;
        case EYE_OF_ENDER:
            break;
        case FEATHER:
            break;
        case FENCE:
            break;
        case FENCE_GATE:
            break;
        case FERMENTED_SPIDER_EYE:
            break;
        case FIRE:
            break;
        case FIREWORK:
            return "Firework Rocket";
        case FLINT:
            break;
        case FLINT_AND_STEEL:
            break;
        case FLOWER_POT_ITEM:
            return "Flower Pot";
        case FURNACE:
            break;
        case GHAST_TEAR:
            break;
        case GLASS:
            break;
        case GLASS_BOTTLE:
            break;
        case GLOWING_REDSTONE_ORE:
            break;
        case GLOWSTONE:
            break;
        case GLOWSTONE_DUST:
            break;
        case GOLDEN_CARROT:
            break;
        case GOLD_AXE:
            break;
        case GOLD_BARDING:
            return "Gold Horse Armor";
        case GOLD_BLOCK:
            break;
        case GOLD_BOOTS:
            return "Golden Boots";
        case GOLD_CHESTPLATE:
            return "Golden Chestplate";
        case GOLD_HELMET:
            return "Golden Helmet";
        case GOLD_HOE:
            return "Golden Hoe";
        case GOLD_INGOT:
            break;
        case GOLD_LEGGINGS:
            return "Golden Leggings";
        case GOLD_NUGGET:
            break;
        case GOLD_ORE:
            break;
        case GOLD_PICKAXE:
            return "Golden_Pickaxe";
        case GOLD_PLATE:
            return "Weighted_Pressure_Plate_(Light)";
        case GOLD_RECORD:
            return "Golden Record";
        case GOLD_SPADE:
            return "Golden Shovel";
        case GOLD_SWORD:
            return "Golden Sword";
        case GRASS:
            break;
        case GRAVEL:
            break;
        case GREEN_RECORD:
            break;
        case GRILLED_PORK:
            break;
        case HARD_CLAY:
            break;
        case HAY_BLOCK:
            break;
        case HOPPER:
            break;
        case HOPPER_MINECART:
            break;
        case HUGE_MUSHROOM_1:
            break;
        case HUGE_MUSHROOM_2:
            break;
        case ICE:
            break;
        case IRON_AXE:
            break;
        case IRON_BARDING:
            return "Iron_Horse_Armor";
        case IRON_BLOCK:
            break;
        case IRON_BOOTS:
            break;
        case IRON_CHESTPLATE:
            break;
        case IRON_DOOR:
            break;
        case IRON_DOOR_BLOCK:
            break;
        case IRON_HELMET:
            break;
        case IRON_HOE:
            break;
        case IRON_INGOT:
            break;
        case IRON_LEGGINGS:
            break;
        case IRON_ORE:
            break;
        case IRON_PICKAXE:
            break;
        case IRON_PLATE:
            break;
        case IRON_SPADE:
            return "Iron_Shovel";
        case IRON_SWORD:
            break;
        case ITEM_FRAME:
            break;
        case JACK_O_LANTERN:
            return "Jack_O'Lantern";
        case JUKEBOX:
            break;
        case JUNGLE_WOOD_STAIRS:
            break;
        case LADDER:
            break;
        case LAPIS_BLOCK:
            break;
        case LAPIS_ORE:
            break;
        case LAVA:
            break;
        case LAVA_BUCKET:
            break;
        case LEASH:
            break;
        case LEATHER:
            break;
        case LEATHER_BOOTS:
            break;
        case LEATHER_CHESTPLATE:
            break;
        case LEATHER_HELMET:
            break;
        case LEATHER_LEGGINGS:
            break;
        case LEAVES_2:
            switch ((int) damage) {
            case 0:
                return "Acacia_Leaves";
            case 1:
                return "Dark_Oak_Leaves";
            }
            return mat.toString();
        case LEVER:
            break;
        case LOG_2:
            switch ((int) damage) {
            case 0:
                return "ACACIA_LOG";
            case 1:
                return "DARK_OAK_LOG";
            }
            return mat.toString();
        case MAGMA_CREAM:
            break;
        case MAP:
            break;
        case MELON:
            break;
        case MELON_BLOCK:
            break;
        case MELON_SEEDS:
            break;
        case MELON_STEM:
            break;
        case MILK_BUCKET:
            break;
        case MINECART:
            break;
        case MOB_SPAWNER:
            break;
        case MONSTER_EGGS:
            break;
        case MOSSY_COBBLESTONE:
            break;
        case MUSHROOM_SOUP:
            break;
        case MYCEL:
            return "MYCELIUM";
        case NAME_TAG:
            break;
        case NETHERRACK:
            break;
        case NETHER_BRICK:
            break;
        case NETHER_BRICK_ITEM:
            return "Nether Brick (Small)";
        case NETHER_BRICK_STAIRS:
            break;
        case NETHER_FENCE:
            break;
        case NETHER_STAR:
            break;
        case NETHER_WARTS:
            break;
        case NOTE_BLOCK:
            break;
        case OBSIDIAN:
            break;
        case PACKED_ICE:
            break;
        case PAINTING:
            break;
        case PAPER:
            break;
        case PISTON_BASE:
            break;
        case PISTON_EXTENSION:
            break;
        case PISTON_MOVING_PIECE:
            break;
        case PISTON_STICKY_BASE:
            break;
        case POISONOUS_POTATO:
            break;
        case PORK:
            break;
        case PORTAL:
            break;
        case POTATO:
            break;
        case POWERED_MINECART:
            break;
        case POWERED_RAIL:
            break;
        case PUMPKIN:
            break;
        case PUMPKIN_PIE:
            break;
        case PUMPKIN_SEEDS:
            break;
        case PUMPKIN_STEM:
            break;
        case QUARTZ:
            break;
        case QUARTZ_BLOCK:
            break;
        case QUARTZ_ORE:
            break;
        case QUARTZ_STAIRS:
            break;
        case RAILS:
            break;
        case RAW_BEEF:
            break;
        case RAW_CHICKEN:
            break;
        case RAW_FISH:
            break;
        case RECORD_10:
            return "Ward Record";
        case RECORD_11:
            break;
        case RECORD_12:
            return "Wait Record (12)";
        case RECORD_3:
            return "Blocks Record (3)";
        case RECORD_4:
            return "Chirp Record (4)";
        case RECORD_5:
            return "Far Record (5)";
        case RECORD_6:
            return "Mall Record (6)";
        case RECORD_7:
            return "Mellohi Record (7)";
        case RECORD_8:
            return "Stal Record (8)";
        case RECORD_9:
            return "Strad Record (9)";
        case REDSTONE:
            break;
        case REDSTONE_BLOCK:
            break;
        case REDSTONE_COMPARATOR:
            break;
        case REDSTONE_COMPARATOR_OFF:
            break;
        case REDSTONE_COMPARATOR_ON:
            break;
        case REDSTONE_ORE:
            break;
        case REDSTONE_WIRE:
            break;
        case RED_MUSHROOM:
            break;
        case RED_ROSE:
            switch ((int) damage) {
            case 0:
                return "POPPY";
            case 1:
                return "BLUE_ORCHID";
            case 2:
                return "ALLIUM";
            case 3:
                return "AZURE_BLUET";
            case 4:
                return "RED_TULIP";
            case 5:
                return "ORANGE_TULIP";
            case 6:
                return "WHITE TULIP";
            case 7:
                return "PINK_TULIP";
            case 8:
                return "OXEYE_DAISY";
            }
            return mat.toString();
        case ROTTEN_FLESH:
            break;
        case SADDLE:
            break;
        case SAND:
            break;
        case SANDSTONE_STAIRS:
            break;
        case SEEDS:
            break;
        case SHEARS:
            break;
        case SIGN:
            break;
        case SIGN_POST:
            break;
        case SKULL:
            break;
        case SLIME_BALL:
            break;
        case SMOOTH_STAIRS:
            break;
        case SNOW:
            break;
        case SNOW_BALL:
            break;
        case SNOW_BLOCK:
            break;
        case SOIL:
            break;
        case SOUL_SAND:
            break;
        case SPECKLED_MELON:
            return "Glistering Melon";
        case SPIDER_EYE:
            break;
        case SPONGE:
            break;
        case SPRUCE_WOOD_STAIRS:
            break;
        case STAINED_CLAY:
            switch ((int) damage) {
            case 0:
                return "WHITE_STAINED_CLAY";
            case 1:
                return "ORANGE_STAINED_CLAY";
            case 2:
                return "MAGENTA_STAINED_CLAY";
            case 3:
                return "LIGHT_BLUE_STAINED_CLAY";
            case 4:
                return "YELLOW_STAINED_CLAY";
            case 5:
                return "LIME_STAINED_CLAY";
            case 6:
                return "PINK_STAINED_CLAY";
            case 7:
                return "GRAY_STAINED_CLAY";
            case 8:
                return "LIGHT_GRAY_STAINED_CLAY";
            case 9:
                return "CYAN_STAINED_CLAY";
            case 10:
                return "PURPLE_STAINED_CLAY";
            case 11:
                return "BLUE_STAINED_CLAY";
            case 12:
                return "BROWN_STAINED_CLAY";
            case 13:
                return "GREEN_STAINED_CLAY";
            case 14:
                return "RED_STAINED_CLAY";
            case 15:
                return "BLACK_STAINED_CLAY";
            }
            return mat.toString();
        case STAINED_GLASS:
            switch ((int) damage) {
            case 0:
                return "WHITE_STAINED_GLASS";
            case 1:
                return "ORANGE_STAINED_GLASS";
            case 2:
                return "MAGENTA_STAINED_GLASS";
            case 3:
                return "LIGHT_BLUE_STAINED_GLASS";
            case 4:
                return "YELLOW_STAINED_GLASS";
            case 5:
                return "LIME_STAINED_GLASS";
            case 6:
                return "PINK_STAINED_GLASS";
            case 7:
                return "GRAY_STAINED_GLASS";
            case 8:
                return "LIGHT_GRAY_STAINED_GLASS";
            case 9:
                return "CYAN_STAINED_GLASS";
            case 10:
                return "PURPLE_STAINED_GLASS";
            case 11:
                return "BLUE_STAINED_GLASS";
            case 12:
                return "BROWN_STAINED_GLASS";
            case 13:
                return "GREEN_STAINED_GLASS";
            case 14:
                return "RED_STAINED_GLASS";
            case 15:
                return "BLACK_STAINED_GLASS";
            }
            return mat.toString();
        case STAINED_GLASS_PANE:
            switch ((int) damage) {
            case 0:
                return "WHITE_STAINED_GLASS_PANE";
            case 1:
                return "ORANGE_STAINED_GLASS_PANE";
            case 2:
                return "MAGENTA_STAINED_GLASS_PANE";
            case 3:
                return "LIGHT_BLUE_STAINED_GLASS_PANE";
            case 4:
                return "YELLOW_STAINED_GLASS_PANE";
            case 5:
                return "LIME_STAINED_GLASS_PANE";
            case 6:
                return "PINK_STAINED_GLASS_PANE";
            case 7:
                return "GRAY_STAINED_GLASS_PANE";
            case 8:
                return "LIGHT_GRAY_STAINED_GLASS_PANE";
            case 9:
                return "CYAN_STAINED_GLASS_PANE";
            case 10:
                return "PURPLE_STAINED_GLASS_PANE";
            case 11:
                return "BLUE_STAINED_GLASS_PANE";
            case 12:
                return "BROWN_STAINED_GLASS_PANE";
            case 13:
                return "GREEN_STAINED_GLASS_PANE";
            case 14:
                return "RED_STAINED_GLASS_PANE";
            case 15:
                return "BLACK_STAINED_GLASS_PANE";
            }
            return mat.toString();
        case STATIONARY_LAVA:
            break;
        case STATIONARY_WATER:
            break;
        case STICK:
            break;
        case STONE:
            break;
        case STONE_AXE:
            break;
        case STONE_BUTTON:
            break;
        case STONE_HOE:
            break;
        case STONE_PICKAXE:
            break;
        case STONE_PLATE:
            break;
        case STONE_SPADE:
            return "Stone Shovel";
        case STONE_SWORD:
            break;
        case STORAGE_MINECART:
            break;
        case STRING:
            break;
        case SUGAR:
            break;
        case SUGAR_CANE:
            break;
        case SUGAR_CANE_BLOCK:
            break;
        case TNT:
            break;
        case TORCH:
            break;
        case TRAPPED_CHEST:
            break;
        case TRAP_DOOR:
            break;
        case TRIPWIRE:
            break;
        case TRIPWIRE_HOOK:
            break;
        case VINE:
            break;
        case WALL_SIGN:
            break;
        case WATCH:
            break;
        case WATER:
            break;
        case WATER_BUCKET:
            break;
        case WATER_LILY:
            break;
        case WHEAT:
            break;
        case WOODEN_DOOR:
            break;
        case WOOD_AXE:
            return "Wooden Axe";
        case WOOD_BUTTON:
            return "Wooden Button";
        case WOOD_DOOR:
            return "Wooden Door";
        case WOOD_DOUBLE_STEP:
            return "Wooden Double Step";
        case WOOD_HOE:
            return "Wooden Hoe";
        case WOOD_PICKAXE:
            return "Wooden Pickaxe";
        case WOOD_PLATE:
            return "Pressure Plate";
        case WOOD_SPADE:
            return "Wooden Shovel";
        case WOOD_STAIRS:
            return "Wooden Stairs";
        case WOOD_STEP:
            return "Wooden Slab";
        case WOOD_SWORD:
            return "Wooden Sword";
        case WRITTEN_BOOK:
            break;
        case YELLOW_FLOWER:
            return "Dandelion";
        default:
            break;
        }
        // This covers the rest of the items that have a "reasonable" name
        if (damage == 0 || isTool(mat))
            return mat.toString();
        // This returns something that has a durability qualifier, but we don't
        // know what it is.
        return mat.toString() + ":" + damage;
    }

    private static boolean isTool(Material mat) {
        switch (mat) {
        case BOW:
        case SHEARS:
        case FLINT_AND_STEEL:

        case CHAINMAIL_BOOTS:
        case CHAINMAIL_CHESTPLATE:
        case CHAINMAIL_HELMET:
        case CHAINMAIL_LEGGINGS:

        case WOOD_AXE:
        case WOOD_HOE:
        case WOOD_PICKAXE:
        case WOOD_SPADE:
        case WOOD_SWORD:

        case LEATHER_BOOTS:
        case LEATHER_CHESTPLATE:
        case LEATHER_HELMET:
        case LEATHER_LEGGINGS:

        case DIAMOND_AXE:
        case DIAMOND_HOE:
        case DIAMOND_PICKAXE:
        case DIAMOND_SPADE:
        case DIAMOND_SWORD:

        case DIAMOND_BOOTS:
        case DIAMOND_CHESTPLATE:
        case DIAMOND_HELMET:
        case DIAMOND_LEGGINGS:
        case STONE_AXE:
        case STONE_HOE:
        case STONE_PICKAXE:
        case STONE_SPADE:
        case STONE_SWORD:

        case GOLD_AXE:
        case GOLD_HOE:
        case GOLD_PICKAXE:
        case GOLD_SPADE:
        case GOLD_SWORD:

        case GOLD_BOOTS:
        case GOLD_CHESTPLATE:
        case GOLD_HELMET:
        case GOLD_LEGGINGS:
        case IRON_AXE:
        case IRON_HOE:
        case IRON_PICKAXE:
        case IRON_SPADE:
        case IRON_SWORD:

        case IRON_BOOTS:
        case IRON_CHESTPLATE:
        case IRON_HELMET:
        case IRON_LEGGINGS:
            return true;
        default:
            return false;
        }

    }

    /**
     * @return the entityType
     */
    public EntityType getEntityType() {
        return entityType;
    }

}