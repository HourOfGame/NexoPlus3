package dev.nexoplus.mechanics;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

public class SoundMechanic implements ItemMechanic {
    private final String sound;
    private final float volume;
    private final float pitch;

    public SoundMechanic(ConfigurationSection cfg) {
        this.sound = cfg.getString("sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.volume = (float) cfg.getDouble("volume", 1.0);
        this.pitch = (float) cfg.getDouble("pitch", 1.0);
    }

    @Override public String getType() { return "SOUND"; }

    @Override
    public void execute(Player player, ItemStack item, Event event) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(sound), volume, pitch);
        } catch (Exception ignored) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
