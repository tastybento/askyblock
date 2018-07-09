package com.wasteofplastic.askyblock.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffectType;

/**
 * This event is fired when a player is going to be burned by acid (not rain)
 *
 * @author tastybento
 *
 */
public class AcidEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private Player player;
    private double totalDamage;
    private final double protection;
    private List<PotionEffectType> potionEffects = new ArrayList<>();

    /**
     * @param player
     * @param totalDamage
     * @param protection
     * @param potionEffects
     * @param cancelled
     */
    public AcidEvent(Player player, double totalDamage, double protection, List<PotionEffectType> potionEffects) {
        this.player = player;
        this.totalDamage = totalDamage;
        this.protection = protection;
        this.potionEffects = potionEffects;
    }

    private boolean cancelled;

    /**
     * @return the player being damaged by acid rain
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @param player the player to set
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Get the amount of damage caused
     * @return the totalDamage
     */
    public double getTotalDamage() {
        return totalDamage;
    }

    /**
     * Get the amount the damage was reduced for this player due to armor, etc.
     * @return the protection
     */
    public double getProtection() {
        return protection;
    }

    /**
     * @param acidDamage the rainDamage to set
     */
    public void setTotalDamage(double totalDamage) {
        this.totalDamage = totalDamage;
    }

    /**
     * @return the potionEffects
     */
    public List<PotionEffectType> getPotionEffects() {
        return potionEffects;
    }

    /**
     * @param potionEffects the potionEffects to set
     */
    public void setPotionEffects(List<PotionEffectType> potionEffects) {
        this.potionEffects = potionEffects;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
