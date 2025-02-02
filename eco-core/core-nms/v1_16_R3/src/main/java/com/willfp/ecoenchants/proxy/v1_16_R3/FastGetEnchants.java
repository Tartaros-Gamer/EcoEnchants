package com.willfp.ecoenchants.proxy.v1_16_R3;

import com.willfp.ecoenchants.proxy.proxies.FastGetEnchantsProxy;
import net.minecraft.server.v1_16_R3.ItemEnchantedBook;
import net.minecraft.server.v1_16_R3.Items;
import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftNamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FastGetEnchants implements FastGetEnchantsProxy {
    @Override
    public Map<Enchantment, Integer> getEnchantmentsOnItem(@NotNull final ItemStack itemStack,
                                                           final boolean checkStored) {
        net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        NBTTagList enchantmentNBT = checkStored && nmsStack.getItem() == Items.ENCHANTED_BOOK ? ItemEnchantedBook.d(nmsStack) : nmsStack.getEnchantments();
        Map<Enchantment, Integer> foundEnchantments = new HashMap<>();

        for (NBTBase base : enchantmentNBT) {
            NBTTagCompound compound = (NBTTagCompound) base;
            String key = compound.getString("id");
            int level = '\uffff' & compound.getShort("lvl");

            Enchantment found = Enchantment.getByKey(CraftNamespacedKey.fromStringOrNull(key));
            if (found != null) {
                foundEnchantments.put(found, level);
            }
        }
        return foundEnchantments;
    }

    @Override
    public int getLevelOnItem(@NotNull final ItemStack itemStack,
                              @NotNull final Enchantment enchantment,
                              final boolean checkStored) {
        net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        NBTTagList enchantmentNBT = checkStored && nmsStack.getItem() == Items.ENCHANTED_BOOK ? ItemEnchantedBook.d(nmsStack) : nmsStack.getEnchantments();

        for (NBTBase base : enchantmentNBT) {
            NBTTagCompound compound = (NBTTagCompound) base;
            String key = compound.getString("id");
            if (!key.equals(enchantment.getKey().toString())) {
                continue;
            }

            return '\uffff' & compound.getShort("lvl");
        }
        return 0;
    }
}
