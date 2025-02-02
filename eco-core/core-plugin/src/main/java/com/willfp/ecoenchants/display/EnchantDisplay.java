package com.willfp.ecoenchants.display;

import com.google.common.collect.Lists;
import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.display.Display;
import com.willfp.eco.core.display.DisplayModule;
import com.willfp.eco.core.display.DisplayPriority;
import com.willfp.eco.util.NumberUtils;
import com.willfp.ecoenchants.display.options.DisplayOptions;
import com.willfp.ecoenchants.enchantments.meta.EnchantmentTarget;
import com.willfp.ecoenchants.enchantments.util.ItemConversionOptions;
import com.willfp.ecoenchants.proxy.proxies.FastGetEnchantsProxy;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All methods and fields pertaining to showing players the enchantments on their items.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
public class EnchantDisplay extends DisplayModule {
    /**
     * The meta key to hide enchantments in lore.
     * <p>
     * EcoEnchants packet lore implementation of HideEnchants.
     */
    @Getter
    private final NamespacedKey keySkip;

    /**
     * The legacy V key.
     * <p>
     * Exists for backwards compatibility.
     */
    @Getter
    @Deprecated
    private final NamespacedKey legacyV;

    /**
     * The configurable options for displaying enchantments.
     */
    @Getter
    private final DisplayOptions options;

    /**
     * Create EcoEnchants display module.
     *
     * @param plugin Instance of EcoEnchants.
     */
    public EnchantDisplay(@NotNull final EcoPlugin plugin) {
        super(plugin, DisplayPriority.HIGH);
        keySkip = this.getPlugin().getNamespacedKeyFactory().create("ecoenchantlore-skip");
        legacyV = this.getPlugin().getNamespacedKeyFactory().create("ecoenchantlore-v");
        options = new DisplayOptions(this.getPlugin());
    }

    /**
     * Update config values.
     */
    public void update() {
        options.update();
        EnchantmentCache.update();
    }

    @Override
    protected void display(@NotNull final ItemStack itemStack,
                           @NotNull final Object... args) {
        if (options.isRequireTarget()) {
            if (!EnchantmentTarget.ALL.getMaterials().contains(itemStack.getType())) {
                return;
            }
        }

        ItemMeta meta = itemStack.getItemMeta();

        assert meta != null;

        boolean hide = (boolean) args[0];

        List<String> itemLore = null;

        if (hide || meta.getPersistentDataContainer().has(keySkip, PersistentDataType.INTEGER)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            if (meta instanceof EnchantmentStorageMeta) {
                meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            }
            meta.getPersistentDataContainer().set(keySkip, PersistentDataType.INTEGER, 1);
            itemStack.setItemMeta(meta);
            return;
        }

        if (meta.hasLore()) {
            itemLore = meta.getLore();
        }

        if (itemLore == null) {
            itemLore = new ArrayList<>();
        }

        List<String> lore = new ArrayList<>();

        LinkedHashMap<Enchantment, Integer> enchantments = new LinkedHashMap<>(this.getPlugin().getProxy(FastGetEnchantsProxy.class).getEnchantmentsOnItem(itemStack, true));

        enchantments.entrySet().removeIf(enchantmentIntegerEntry -> enchantmentIntegerEntry.getValue().equals(0));

        List<Enchantment> unsorted = new ArrayList<>();
        enchantments.forEach((enchantment, integer) -> unsorted.add(enchantment));

        Map<Enchantment, Integer> tempEnchantments = new HashMap<>(enchantments);

        options.getSorter().sortEnchantments(unsorted);

        enchantments.clear();
        unsorted.forEach(enchantment -> enchantments.put(enchantment, tempEnchantments.get(enchantment)));

        enchantments.forEach((enchantment, level) -> {
            String name = EnchantmentCache.getEntry(enchantment).getName();

            if (!(enchantment.getMaxLevel() == 1 && level == 1)) {
                if (options.getNumbersOptions().isUseNumerals() && level < options.getNumbersOptions().getThreshold()) {
                    name += " " + NumberUtils.toNumeral(level);
                } else {
                    name += " " + level;
                }
            }

            lore.add(Display.PREFIX + name);
            if (!options.getDescriptionOptions().isShowingAtBottom()) {
                if (enchantments.size() <= options.getDescriptionOptions().getThreshold() && options.getDescriptionOptions().isEnabled()) {
                    lore.addAll(EnchantmentCache.getEntry(enchantment).getDescription());
                }
            }
        });

        if (options.getShrinkOptions().isEnabled() && (enchantments.size() > options.getShrinkOptions().getThreshold())) {
            List<List<String>> partitionedCombinedLoreList = Lists.partition(lore, options.getShrinkOptions().getShrinkPerLine());
            List<String> newLore = new ArrayList<>();
            partitionedCombinedLoreList.forEach(list -> {
                StringBuilder builder = new StringBuilder();
                for (String s : list) {
                    builder.append(s);
                    builder.append(", ");
                }
                String line = builder.toString();
                line = line.substring(0, line.length() - 2);
                newLore.add(line);
            });
            lore.clear();
            lore.addAll(newLore);
        }

        if (options.getDescriptionOptions().isShowingAtBottom()) {
            if (enchantments.size() <= options.getDescriptionOptions().getThreshold() && options.getDescriptionOptions().isEnabled()) {
                for (Enchantment enchantment : enchantments.keySet()) {
                    lore.addAll(EnchantmentCache.getEntry(enchantment).getDescription());
                }
            }
        }

        if (meta instanceof EnchantmentStorageMeta) {
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        lore.addAll(itemLore);
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
    }

    @Override
    protected void revert(@NotNull final ItemStack itemStack) {
        if (!EnchantmentTarget.ALL.getMaterials().contains(itemStack.getType())) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();

        assert meta != null;

        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());

        lore.removeIf(s -> s.startsWith("§w"));
        meta.setLore(lore);

        meta.getPersistentDataContainer().remove(legacyV);

        if (!meta.getPersistentDataContainer().has(keySkip, PersistentDataType.INTEGER)) {
            meta.removeItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.getPersistentDataContainer().remove(keySkip);
        itemStack.setItemMeta(meta);
    }

    @Override
    protected Object[] generateVarArgs(@NotNull final ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return new Object[]{false};
        }

        boolean hideEnchants = meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS) || meta.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS);

        if (meta.getPersistentDataContainer().has(legacyV, PersistentDataType.INTEGER)) {
            hideEnchants = false;
        }

        if (Display.isFinalized(itemStack)) {
            hideEnchants = false;
        }

        if (ItemConversionOptions.isUsingExperimentalHideFixer() && ItemConversionOptions.isUsingForceHideFixer()) {
            hideEnchants = false;
        }

        if (ItemConversionOptions.isUsingExperimentalHideFixer() && meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS) && meta.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) {
            hideEnchants = false;
        }

        return new Object[]{hideEnchants};
    }
}
