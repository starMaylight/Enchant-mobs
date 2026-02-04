package com.starmaylight.enchant_mobs.equipment;

import com.starmaylight.enchant_mobs.affix.Affix;
import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.AffixType;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import com.starmaylight.enchant_mobs.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class AffixEquipmentManager {

    public static void applyAffixEquipment(Mob mob, MobAffixData data) {
        if (data.isEmpty()) {
            return;
        }

        List<AffixInstance> armorAffixes = new ArrayList<>();
        List<AffixInstance> weaponAffixes = new ArrayList<>();

        for (AffixInstance affix : data.getAffixes()) {
            if (affix.getAffix().getType() == AffixType.ARMOR) {
                armorAffixes.add(affix);
            } else if (affix.getAffix().getType() == AffixType.WEAPON) {
                weaponAffixes.add(affix);
            }
        }

        if (!armorAffixes.isEmpty()) {
            ItemStack armorItem = createEnchantedArmor(armorAffixes);
            mob.setItemSlot(EquipmentSlot.CHEST, armorItem);
            mob.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }

        if (!weaponAffixes.isEmpty()) {
            ItemStack weaponItem = createEnchantedWeapon(weaponAffixes);
            mob.setItemSlot(EquipmentSlot.MAINHAND, weaponItem);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    private static ItemStack createEnchantedArmor(List<AffixInstance> affixes) {
        ItemStack item = new ItemStack(ModItems.INVISIBLE_CHESTPLATE.get());

        for (AffixInstance affix : affixes) {
            applyEnchantmentToItem(item, affix);
        }

        return item;
    }

    private static ItemStack createEnchantedWeapon(List<AffixInstance> affixes) {
        ItemStack item = new ItemStack(ModItems.INVISIBLE_SWORD.get());

        for (AffixInstance affix : affixes) {
            applyEnchantmentToItem(item, affix);
        }

        return item;
    }

    private static void applyEnchantmentToItem(ItemStack item, AffixInstance affix) {
        Affix a = affix.getAffix();
        Enchantment enchantment = a.getEnchantment();

        if (enchantment == null) {
            return;
        }

        int baseLevel = a.getLevel();
        int scaledLevel = (int) Math.ceil(baseLevel * affix.getEffectMultiplier());

        int maxAllowedLevel = Math.max(enchantment.getMaxLevel() * 2, 10);
        scaledLevel = Math.min(scaledLevel, maxAllowedLevel);

        item.enchant(enchantment, scaledLevel);
    }
}
