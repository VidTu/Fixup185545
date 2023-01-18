package com.github.vidtu.fixup185545;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Main plugin class.
 *
 * @author VidTu
 */
public class Fixup185545 extends JavaPlugin implements Listener {
    private final Map<Player, Knockback> knockbacks = new WeakHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return; // Don't spend server resources on non-player entities.
        Player player = (Player) event.getEntity();

        // Get knockback resistance attribute.
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) return; // What?! That player doesn't have knockback resistance attribute?

        // Calculate resistance multiplier and store it.
        double multiplier = 1D - Math.max(Math.min(attribute.getValue(), 1D), 0D);
        knockbacks.put(player, new Knockback(multiplier, attribute.getBaseValue(), attribute.getModifiers()));

        // Temporarily remove all modifiers to calculate knockback.
        attribute.setBaseValue(attribute.getDefaultValue());
        attribute.getModifiers().forEach(attribute::removeModifier);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onKnockback(PlayerVelocityEvent event) {
        Player player = event.getPlayer();

        // Get previously stored knockback and apply to player.
        Knockback knockback = knockbacks.remove(player);
        if (knockback == null) return;
        event.setVelocity(event.getVelocity().multiply(knockback.multiplier()));

        // Add all modifiers back.
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) return; // What?! That player doesn't have knockback resistance attribute?
        attribute.setBaseValue(knockback.base());
        knockback.modifiers().forEach(attribute::addModifier);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        knockbacks.remove(event.getPlayer());
    }
}
