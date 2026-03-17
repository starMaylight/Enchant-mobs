package com.starmaylight.enchant_mobs.enchantment;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Enchant_mobs.MODID);

    public static final RegistryObject<Enchantment> ARMOR_CORROSION = ENCHANTMENTS.register("armor_corrosion",
            ArmorCorrosionEnchantment::new);

    public static final RegistryObject<Enchantment> VITAL_STRIKE = ENCHANTMENTS.register("vital_strike",
            VitalStrikeEnchantment::new);

    public static final RegistryObject<Enchantment> BATTLE_FURY = ENCHANTMENTS.register("battle_fury",
            BattleFuryEnchantment::new);

    public static final RegistryObject<Enchantment> TRAIT_PURGE = ENCHANTMENTS.register("trait_purge",
            TraitPurgeEnchantment::new);

    public static final RegistryObject<Enchantment> PHYSICAL_IMMUNITY = ENCHANTMENTS.register("physical_immunity",
            PhysicalImmunityEnchantment::new);

    public static final RegistryObject<Enchantment> DEATH_TOUCH = ENCHANTMENTS.register("death_touch",
            DeathTouchEnchantment::new);

    public static final RegistryObject<Enchantment> SLOWNESS_AURA = ENCHANTMENTS.register("slowness_aura",
            SlownessAuraEnchantment::new);

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
