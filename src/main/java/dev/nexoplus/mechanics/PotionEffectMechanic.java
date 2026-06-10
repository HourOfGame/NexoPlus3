package dev.nexoplus.mechanics;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectMechanic implements ItemMechanic {
    private final PotionEffectType effectType;
    private final int duration;
    private final int amplifier;

    public PotionEffectMechanic(ConfigurationSection cfg) {
        PotionEffectType pet = PotionEffectType.getByName(cfg.getString("effect", "SPEED"));
        this.effectType = pet != null ? pet : PotionEffectType.SPEED;
        this.duration = cfg.getInt("duration", 100);
        this.amplifier = cfg.getInt("amplifier", 0);
    }

    @Override public String getType() { return "POTION_EFFECT"; }

    @Override
    public void execute(Player player, ItemStack item, Event event) {
        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
    }
}
