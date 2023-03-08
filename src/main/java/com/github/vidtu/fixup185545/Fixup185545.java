package com.github.vidtu.fixup185545;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Main plugin class.
 *
 * @author VidTu
 */
public class Fixup185545 extends JavaPlugin implements Listener {
    private static final Boolean NO_OVERRIDE = Boolean.getBoolean("fixup185545.noOverride");
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
        Entity damager = null;
        if (!NO_OVERRIDE && event instanceof EntityDamageByEntityEvent &&
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) { // Ender Pearl
            damager = ((EntityDamageByEntityEvent) event).getDamager();
        }
        Vector override = damager != null ? attackVelocity(player, damager) : null;
        double multiplier = 1D - Math.max(Math.min(attribute.getValue(), 1D), 0D);
        knockbacks.put(player, new Knockback(override, multiplier, attribute.getBaseValue(), attribute.getModifiers()));

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
        if (knockback.override() != null) {
            event.setVelocity(knockback.override());
        } else {
            event.setVelocity(event.getVelocity().multiply(knockback.multiplier()));
        }

        // Add all modifiers back.
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) return; // What?! That player doesn't have knockback resistance attribute?
        attribute.setBaseValue(knockback.base());
        knockback.modifiers().stream().filter(modifier -> !attribute.getModifiers().contains(modifier)).forEach(attribute::addModifier);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        knockbacks.remove(event.getPlayer());
    }

    /**
     * Calculates total attack velocity for player.
     *
     * @param player   Target player
     * @param attacker Attacker
     * @return Calculated velocity
     */
    private static Vector attackVelocity(Player player, Entity attacker) {
        Vector velocity = player.getVelocity();

        // Apply initial knockback.
        // Copied from (Mojmap): net.minecraft.world.entity.LivingEntity.hurt(DamageSource, float)
        double diffX;
        double diffZ;
        Location playerLocation = player.getLocation();
        Location attackerLocation = attacker.getLocation();
        //noinspection StatementWithEmptyBody
        for (diffX = attackerLocation.getX() - playerLocation.getX(), diffZ = attackerLocation.getZ() - playerLocation
                .getZ(); diffX * diffX + diffZ * diffZ < 1.0E-4D; diffX = (Math.random() - Math.random()) * 0.01D,
                diffZ = (Math.random() - Math.random()) * 0.01D) {
            // NO-OP
        }
        velocity = knockback(player, velocity, 0.4000000059604645D, diffX, diffZ);

        if (attacker instanceof Player) {
            Player playerAttacker = (Player) attacker;

            // Apply knockback boost from sprint.
            // Copied from (Mojmap): net.minecraft.world.entity.Player.attack(Entity)
            int knockback = playerAttacker.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK);
            if (playerAttacker.isSprinting()) knockback++;
            if (knockback > 0) {
                velocity = knockback(player, velocity, knockback * 0.5D, Math.sin(attacker.getLocation().getYaw()
                        * 0.017453292F), -Math.cos(attacker.getLocation().getYaw() * 0.017453292F));
            }
        }

        return velocity;
    }

    /**
     * Calculates the knockback vector.
     * <br>
     * Copied from Minecraft (Mojmap): <code>net.minecraft.world.entity.LivingEntity.knockback(double, double, double)</code>
     *
     * @param player   Target player for applying knockback
     * @param original Original knockback vector, use {@link Player#getVelocity()} if there were no previous calculations
     * @param value    Knockback strength
     * @param x        Knockback X direction
     * @param z        Knockback Z direction
     * @return New knockback vector if knockback has been calculated, <code>original</code> vector if the player is immune to knockback
     */
    private static Vector knockback(Player player, Vector original, double value, double x, double z) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute != null) value *= 1.0 - attribute.getValue();
        if (value <= 0) return original;
        Vector multiplier = new Vector(x, 0.0, z).normalize().multiply(value);

        // This is deprecated by Spigot, but vanilla client uses exactly this.
        @SuppressWarnings("deprecation") boolean onGround = player.isOnGround();

        return new Vector(original.getX() / 2.0 - multiplier.getX(),
                onGround ? Math.min(0.4, original.getY() / 2.0 + value) : original.getY(),
                original.getZ() / 2.0 - multiplier.getZ());
    }
}
