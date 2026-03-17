package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.affix.Affix;
import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import com.starmaylight.enchant_mobs.compat.L2HostilityCompat;
import com.starmaylight.enchant_mobs.enchantment.ModEnchantments;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class MobCombatHandler {

    private static final String ARMOR_CORROSION_ID = Enchant_mobs.MODID + ":armor_corrosion";
    private static final String VITAL_STRIKE_ID = Enchant_mobs.MODID + ":vital_strike";
    private static final String BATTLE_FURY_ID = Enchant_mobs.MODID + ":battle_fury";
    private static final String TRAIT_PURGE_ID = Enchant_mobs.MODID + ":trait_purge";
    private static final String PHYSICAL_IMMUNITY_ID = Enchant_mobs.MODID + ":physical_immunity";
    private static final String DEATH_TOUCH_ID = Enchant_mobs.MODID + ":death_touch";

    private static final UUID DEATH_TOUCH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        // Affixed mob takes damage: Thorns + Physical Immunity
        if (target instanceof Mob mob && MobSpawnHandler.hasAffixes(mob)) {
            handleThornsEffect(mob, attacker, event);
            handlePhysicalImmunity(mob, event);
        }

        // Affixed mob attacks: custom enchantment effects
        if (attacker instanceof Mob mob && MobSpawnHandler.hasAffixes(mob)) {
            handleAttackEffects(mob, target, event);
        }

        // Player attacks with custom enchantments on weapon
        if (attacker instanceof Player player) {
            handlePlayerAttackEffects(player, target, event);
        }
    }

    private static void handlePlayerAttackEffects(Player player, LivingEntity target, LivingHurtEvent event) {
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        int armorCorrosionLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ARMOR_CORROSION.get(), weapon);
        if (armorCorrosionLv > 0) {
            handleArmorCorrosion(target, armorCorrosionLv);
        }

        int vitalStrikeLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.VITAL_STRIKE.get(), weapon);
        if (vitalStrikeLv > 0) {
            handleVitalStrike(target, vitalStrikeLv, event);
        }

        int battleFuryLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BATTLE_FURY.get(), weapon);
        if (battleFuryLv > 0) {
            handleBattleFuryForPlayer(player, battleFuryLv);
        }

        int traitPurgeLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.TRAIT_PURGE.get(), weapon);
        if (traitPurgeLv > 0) {
            handleTraitPurge(target);
        }

        int deathTouchLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DEATH_TOUCH.get(), weapon);
        if (deathTouchLv > 0) {
            handleDeathTouchFromPlayer(player, target, deathTouchLv);
        }
    }

    private static void handleAttackEffects(Mob mob, LivingEntity target, LivingHurtEvent event) {
        MobAffixData data = MobSpawnHandler.getAffixData(mob);

        for (AffixInstance inst : data.getAffixes()) {
            Affix affix = inst.getAffix();
            String enchId = affix.getEnchantmentId().toString();

            switch (enchId) {
                case ARMOR_CORROSION_ID -> handleArmorCorrosion(target, affix.getLevel());
                case VITAL_STRIKE_ID -> handleVitalStrike(target, affix.getLevel(), event);
                case BATTLE_FURY_ID -> handleBattleFury(mob, affix.getLevel());
                case TRAIT_PURGE_ID -> handleTraitPurge(target);
                case DEATH_TOUCH_ID -> handleDeathTouch(mob, target, affix.getLevel());
            }
        }
    }

    /**
     * Armor Corrosion: Reduces durability of a random armor piece by level * 5%.
     */
    private static void handleArmorCorrosion(LivingEntity target, int level) {
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        List<EquipmentSlot> validSlots = new ArrayList<>();
        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = target.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()) {
                validSlots.add(slot);
            }
        }

        if (validSlots.isEmpty()) return;

        EquipmentSlot chosenSlot = validSlots.get(target.getRandom().nextInt(validSlots.size()));
        ItemStack armor = target.getItemBySlot(chosenSlot);

        int maxDurability = armor.getMaxDamage();
        int durabilityLoss = Math.max(1, (int) (maxDurability * level * 0.05));
        armor.hurtAndBreak(durabilityLoss, target, (entity) -> entity.broadcastBreakEvent(chosenSlot));
    }

    /**
     * Vital Strike: Deals level * 2% of target's max HP as additional damage.
     */
    private static void handleVitalStrike(LivingEntity target, int level, LivingHurtEvent event) {
        float maxHealth = target.getMaxHealth();
        float bonusDamage = maxHealth * level * 0.02f;
        event.setAmount(event.getAmount() + bonusDamage);
    }

    /**
     * Battle Fury: 5% chance to gain Strength buff at enchantment level for 5 minutes.
     */
    private static void handleBattleFury(Mob mob, int level) {
        if (mob.getRandom().nextFloat() < 0.05f) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, level - 1));
        }
    }

    private static void handleBattleFuryForPlayer(Player player, int level) {
        if (player.getRandom().nextFloat() < 0.05f) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, level - 1));
        }
    }

    /**
     * Trait Purge: Removes all L2 Hostility traits from the target.
     * Only active when L2 Hostility is installed.
     */
    private static void handleTraitPurge(LivingEntity target) {
        try {
            if (L2HostilityCompat.isLoaded()) {
                L2HostilityCompat.clearTraits(target);
            }
        } catch (NoClassDefFoundError ignored) {
            // L2 Hostility not present at runtime
        }
    }

    /**
     * Physical Immunity: Cancels physical (melee/projectile) damage to the mob.
     */
    private static void handlePhysicalImmunity(Mob mob, LivingHurtEvent event) {
        MobAffixData data = MobSpawnHandler.getAffixData(mob);

        for (AffixInstance inst : data.getAffixes()) {
            if (inst.getAffix().getEnchantmentId().toString().equals(PHYSICAL_IMMUNITY_ID)) {
                // Cancel if damage is physical (not magic, not fire, not void, etc.)
                if (!event.getSource().is(DamageTypes.MAGIC)
                        && !event.getSource().is(DamageTypes.INDIRECT_MAGIC)
                        && !event.getSource().is(DamageTypes.WITHER)
                        && !event.getSource().is(DamageTypes.ON_FIRE)
                        && !event.getSource().is(DamageTypes.IN_FIRE)
                        && !event.getSource().is(DamageTypes.LAVA)
                        && !event.getSource().is(DamageTypes.LIGHTNING_BOLT)
                        && !event.getSource().is(DamageTypes.GENERIC_KILL)
                        && !event.getSource().is(DamageTypes.DRAGON_BREATH)
                        && !event.getSource().is(DamageTypes.SONIC_BOOM)) {
                    event.setCanceled(true);
                }
                break;
            }
        }
    }

    /**
     * Death Touch: lv*10% chance to set target's max health to 1 and deal void damage.
     */
    private static void handleDeathTouch(LivingEntity attacker, LivingEntity target, int level) {
        float chance = level * 0.10f;
        if (attacker.getRandom().nextFloat() < chance) {
            applyDeathTouch(target);
        }
    }

    private static void handleDeathTouchFromPlayer(Player player, LivingEntity target, int level) {
        float chance = level * 0.10f;
        if (player.getRandom().nextFloat() < chance) {
            applyDeathTouch(target);
        }
    }

    private static void applyDeathTouch(LivingEntity target) {
        // Set max health to 1 via attribute modifier
        var maxHealthAttr = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.removeModifier(DEATH_TOUCH_MODIFIER_UUID);
            double currentMax = maxHealthAttr.getBaseValue();
            double reduction = -(currentMax - 1.0);
            maxHealthAttr.addTransientModifier(new AttributeModifier(
                    DEATH_TOUCH_MODIFIER_UUID,
                    "Death Touch",
                    reduction,
                    AttributeModifier.Operation.ADDITION
            ));
        }

        // Reset invulnerability timer so damage applies immediately
        target.invulnerableTime = 0;
        // Deal void damage with Float.MAX_VALUE (Long.MAX_VALUE overflows to Infinity as float)
        target.hurt(target.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
    }

    private static void handleThornsEffect(Mob mob, Entity attacker, LivingHurtEvent event) {
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return;
        }

        MobAffixData data = MobSpawnHandler.getAffixData(mob);

        for (AffixInstance inst : data.getAffixes()) {
            Affix affix = inst.getAffix();
            String enchId = affix.getEnchantmentId().toString();

            if (enchId.equals("minecraft:thorns")) {
                float multiplier = inst.getEffectMultiplier();
                int level = affix.getLevel();

                float thornsDamage = (1.0f + level * 0.5f) * multiplier;

                if (!livingAttacker.hurtMarked) {
                    livingAttacker.hurt(mob.damageSources().thorns(mob), thornsDamage);
                }
                break;
            }
        }
    }
}
