package com.starmaylight.enchant_mobs.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class EnchantStripEnchantment extends Enchantment {
    public EnchantStripEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override public int getMaxLevel() { return 5; }
    @Override public boolean isTradeable() { return false; }
    @Override public boolean isDiscoverable() { return false; }
    @Override public boolean isAllowedOnBooks() { return true; }
}
