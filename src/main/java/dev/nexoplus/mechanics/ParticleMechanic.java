package dev.nexoplus.mechanics;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

public class ParticleMechanic implements ItemMechanic {
    private final Particle particle;
    private final int count;
    private final double offsetX, offsetY, offsetZ;

    public ParticleMechanic(ConfigurationSection cfg) {
        Particle p = Particle.FLAME;
        try { p = Particle.valueOf(cfg.getString("particle", "FLAME")); } catch (Exception ignored) {}
        this.particle = p;
        this.count = cfg.getInt("count", 10);
        this.offsetX = cfg.getDouble("offset_x", 0.3);
        this.offsetY = cfg.getDouble("offset_y", 0.3);
        this.offsetZ = cfg.getDouble("offset_z", 0.3);
    }

    @Override public String getType() { return "PARTICLE"; }

    @Override
    public void execute(Player player, ItemStack item, Event event) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0),
                count, offsetX, offsetY, offsetZ);
    }
}
