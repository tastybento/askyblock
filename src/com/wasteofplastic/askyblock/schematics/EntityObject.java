package com.wasteofplastic.askyblock.schematics;

import org.bukkit.entity.EntityType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public class EntityObject {
    private EntityType type;
    private BlockVector location;
    private byte color;
    private float yaw;
    private float pitch;
    private boolean sheared;
    private Vector motion;
    private int age;
    private int rabbitType;
    private int catType;
    private boolean sitting;
    private int profession;
    private boolean carryingChest;
    private boolean owned;
    private byte collarColor;
    
    /**
     * @return the type
     */
    public EntityType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(EntityType type) {
        this.type = type;
    }
    /**
     * @return the location
     */
    public BlockVector getLocation() {
        return location;
    }
    /**
     * @param location the location to set
     */
    public void setLocation(BlockVector location) {
        this.location = location;
    }
    /**
     * @return the color
     */
    public byte getColor() {
        return color;
    }
    /**
     * @param color the color to set
     */
    public void setColor(byte color) {
        this.color = color;
    }
    /**
     * @return the yaw
     */
    public float getYaw() {
        return yaw;
    }
    /**
     * @param yaw the yaw to set
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    /**
     * @return the roll
     */
    public float getPitch() {
        return pitch;
    }
    /**
     * @param roll the roll to set
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    /**
     * @return the sheared
     */
    public boolean isSheared() {
        return sheared;
    }
    /**
     * @param sheared the sheared to set
     */
    public void setSheared(boolean sheared) {
        this.sheared = sheared;
    }
    /**
     * @return the motion
     */
    public Vector getMotion() {
        return motion;
    }
    /**
     * @param motion the motion to set
     */
    public void setMotion(Vector motion) {
        this.motion = motion;
    }
    /**
     * @return the age
     */
    public int getAge() {
        return age;
    }
    /**
     * @param age the age to set
     */
    public void setAge(int age) {
        this.age = age;
    }
    /**
     * @return the profession
     */
    public int getProfession() {
        return profession;
    }
    /**
     * @param profession the profession to set
     */
    public void setProfession(int profession) {
        this.profession = profession;
    }
    /**
     * @return the rabbitType
     */
    public int getRabbitType() {
        return rabbitType;
    }
    /**
     * @param rabbitType the rabbitType to set
     */
    public void setRabbitType(int rabbitType) {
        this.rabbitType = rabbitType;
    }
    /**
     * @return the carryingChest
     */
    public boolean isCarryingChest() {
        return carryingChest;
    }
    /**
     * @param carryingChest the carryingChest to set
     */
    public void setCarryingChest(byte carryingChest) {
	if (carryingChest > (byte)0) {
	    this.carryingChest = true;
	}
        this.carryingChest = false;
    }
    /**
     * @return the catType
     */
    public int getCatType() {
        return catType;
    }
    /**
     * @param catType the catType to set
     */
    public void setCatType(int catType) {
        this.catType = catType;
    }
    /**
     * @return the sitting
     */
    public boolean isSitting() {
        return sitting;
    }
    /**
     * @param sitting the sitting to set
     */
    public void setSitting(byte sitting) {
	if (sitting > (byte)0) {
	    this.sitting = true;
	}
        this.sitting = false;
    }
    /**
     * @return the owned
     */
    public boolean isOwned() {
        return owned;
    }
    /**
     * @param owned the owned to set
     */
    public void setOwned(boolean owned) {
        this.owned = owned;
    }
    /**
     * @return the collarColor
     */
    public byte getCollarColor() {
        return collarColor;
    }
    /**
     * @param collarColor the collarColor to set
     */
    public void setCollarColor(byte collarColor) {
        this.collarColor = collarColor;
    }
}
