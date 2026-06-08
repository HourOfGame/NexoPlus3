package dev.nexoplus.mechanics;

import org.bukkit.configuration.ConfigurationSection;

public class MechanicFactory {
    public static ItemMechanic create(String type, ConfigurationSection cfg) {
        if (type == null || cfg == null) return null;
        return switch (type.toUpperCase()) {
            case "POTION_EFFECT" -> new PotionEffectMechanic(cfg);
            case "COMMAND" -> new CommandMechanic(cfg);
            case "SOUND" -> new SoundMechanic(cfg);
            case "PARTICLE" -> new ParticleMechanic(cfg);
            default -> null;
        };
    }
}
