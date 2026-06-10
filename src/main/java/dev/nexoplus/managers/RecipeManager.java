package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoItem;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class RecipeManager {
    private final NexoPlus plugin;
    private final Map<String, Recipe> recipes = new ConcurrentHashMap<>();
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public RecipeManager(NexoPlus plugin) { this.plugin = plugin; }

    public void loadRecipes() {
        // Remove previously registered
        for (NamespacedKey key : registeredKeys) Bukkit.removeRecipe(key);
        registeredKeys.clear();
        recipes.clear();

        File folder = new File(plugin.getDataFolder(), "recipes");
        if (!folder.exists()) { folder.mkdirs(); return; }
        scanDirectory(folder);
        plugin.getLogger().info("Loaded " + recipes.size() + " custom recipes.");
    }

    public void reload() { loadRecipes(); }

    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanDirectory(f);
            else if (f.getName().endsWith(".yml")) loadFile(f);
        }
    }

    private void loadFile(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            if (!cfg.isConfigurationSection(key)) continue;
            ConfigurationSection s = cfg.getConfigurationSection(key);
            try { registerRecipe(key, s); }
            catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Recipe '" + key + "' error: " + e.getMessage()); }
        }
    }

    private void registerRecipe(String id, ConfigurationSection s) {
        String type = s.getString("type", "CRAFTING_SHAPED").toUpperCase();
        NamespacedKey key = new NamespacedKey(plugin, id.toLowerCase().replace(" ", "_"));

        ItemStack result = resolveItem(s.getString("result.item"), s.getInt("result.amount", 1));
        if (result == null) return;

        Recipe recipe = switch (type) {
            case "CRAFTING_SHAPED" -> buildShapedRecipe(key, result, s);
            case "CRAFTING_SHAPELESS" -> buildShapelessRecipe(key, result, s);
            case "FURNACE" -> buildFurnaceRecipe(key, result, s);
            case "SMOKING" -> buildSmokingRecipe(key, result, s);
            case "BLASTING" -> buildBlastingRecipe(key, result, s);
            case "CAMPFIRE" -> buildCampfireRecipe(key, result, s);
            case "STONECUTTING" -> buildStonecuttingRecipe(key, result, s);
            default -> null;
        };

        if (recipe != null) {
            Bukkit.addRecipe(recipe);
            recipes.put(id, recipe);
            registeredKeys.add(key);
        }
    }

    private ShapedRecipe buildShapedRecipe(NamespacedKey key, ItemStack result, ConfigurationSection s) {
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        List<String> shape = s.getStringList("shape");
        recipe.shape(shape.toArray(new String[0]));
        ConfigurationSection ingr = s.getConfigurationSection("ingredients");
        if (ingr != null) {
            for (String ch : ingr.getKeys(false)) {
                ItemStack ing = resolveItem(ingr.getString(ch), 1);
                if (ing != null) recipe.setIngredient(ch.charAt(0), ing.getType());
            }
        }
        return recipe;
    }

    private ShapelessRecipe buildShapelessRecipe(NamespacedKey key, ItemStack result, ConfigurationSection s) {
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (String ingStr : s.getStringList("ingredients")) {
            ItemStack ing = resolveItem(ingStr, 1);
            if (ing != null) recipe.addIngredient(ing.getType());
        }
        return recipe;
    }

    private FurnaceRecipe buildFurnaceRecipe(NamespacedKey key, ItemStack result, ConfigurationSection s) {
        ItemStack input = resolveItem(s.getString("input"), 1);
        if (input == null) return null;
        return new FurnaceRecipe(key, result, input.getType(),
                (float) s.getDouble("experience", 0.1), s.getInt("cooking_time", 200));
    }

    private SmokingRecipe buildSmokingRecipe(NamespacedKey key, ItemStack result, ConfigurationSection s) {
        ItemStack input = resolveItem(s.getString("input"), 1);
        if (input == null) return null;
        return new SmokingRecipe(key, result, input.getType(),
                (float) s.getDouble("experience", 0.1), s.getInt("cooking_time", 100));
    }

    private BlastingRecipe buildBlastingRecipe(NamespacedKey key, ItemStack result, ConfigurationSection s) {
        ItemStack input = resolveItem(s.getString("input"), 1);
        if (input == null) return null;
        return new BlastingRecipe(key, result, input.getType(),
                (float) s.getDouble("experience", 0.1), s.getInt("cooking_time", 100));
    }

    private CampfireRecipe buildCampfireRecipe(NamespacedKey key, ItemStack result, ConfigurationSection s) {
        ItemStack input = resolveItem(s.getString("input"), 1);
        if (input == null) return null;
        return new CampfireRecipe(key, result, input.getType(),
                (float) s.getDouble("experience", 0.1), s.getInt("cooking_time", 600));
    }

    private StonecuttingRecipe buildStonecuttingRecipe(NamespacedKey key, ItemStack result, ConfigurationSection s) {
        ItemStack input = resolveItem(s.getString("input"), 1);
        if (input == null) return null;
        return new StonecuttingRecipe(key, result, input.getType());
    }

    private ItemStack resolveItem(String itemStr, int amount) {
        if (itemStr == null) return null;
        if (itemStr.contains(":") && !itemStr.startsWith("minecraft:")) {
            // NexoPlus item
            ItemStack stack = plugin.getItemManager().buildItemStack(itemStr, amount);
            return stack;
        }
        try {
            String matName = itemStr.replace("minecraft:", "").toUpperCase();
            return new ItemStack(org.bukkit.Material.valueOf(matName), amount);
        } catch (Exception e) { return null; }
    }

    public int getLoadedCount() { return recipes.size(); }
    public Collection<Recipe> getAllRecipes() { return Collections.unmodifiableCollection(recipes.values()); }
}
