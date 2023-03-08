package com.github.vidtu.fixup185545;

import org.bukkit.attribute.AttributeModifier;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class used for storing knockback resistance data.
 *
 * @author VidTu
 */
public class Knockback {
    private final Vector override;
    private final double multiplier;
    private final double base;
    private final List<AttributeModifier> modifiers;

    /**
     * Creates a new instance of the knockback resistance data class.
     *
     * @param override   Overridden knockback, <code>null</code> if override is disabled
     * @param multiplier Calculated knockback modifier
     * @param base       Previous knockback resistance base value
     * @param modifiers  Previous knockback resistance modifiers
     */
    public Knockback(Vector override, double multiplier, double base, Collection<AttributeModifier> modifiers) {
        this.override = override;
        this.multiplier = multiplier;
        this.base = base;
        this.modifiers = Collections.unmodifiableList(new ArrayList<>(modifiers));
    }

    /**
     * Gets overridden knockback.
     *
     * @return Overridden knockback, <code>null</code> if override was disabled
     */
    public Vector override() {
        return override;
    }

    /**
     * Gets calculated knockback modifier.
     *
     * @return Calculated knockback modifier
     */
    public double multiplier() {
        return multiplier;
    }

    /**
     * Gets previous knockback resistance base value.
     *
     * @return Previous knockback resistance base value
     */
    public double base() {
        return base;
    }

    /**
     * Gets previous knockback resistance modifiers.
     *
     * @return Previous knockback resistance modifiers
     */
    public List<AttributeModifier> modifiers() {
        return modifiers;
    }
}
