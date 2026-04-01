package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.affix.Affix;
import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import com.starmaylight.enchant_mobs.compat.L2HostilityCompat;
import com.starmaylight.enchant_mobs.enchantment.ModEnchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class MobCombatHandler {

    private static final String ARMOR_CORROSION_ID = Enchant_mobs.MODID + ":armor_corrosion";
    private static final String VITAL_STRIKE_ID = Enchant_mobs.MODID + ":vital_strike";
    private static final String BATTLE_FURY_ID = Enchant_mobs.MODID + ":battle_fury";
    private static final String TRAIT_PURGE_ID = Enchant_mobs.MODID + ":trait_purge";
    private static final String PHYSICAL_IMMUNITY_ID = Enchant_mobs.MODID + ":physical_immunity";
    private static final String DEATH_TOUCH_ID = Enchant_mobs.MODID + ":death_touch";
    private static final String ANNIHILATION_ID = Enchant_mobs.MODID + ":annihilation";
    private static final String OVERFLOW_HEAL_ID = Enchant_mobs.MODID + ":overflow_heal";
    private static final String ENCHANT_STRIP_ID = Enchant_mobs.MODID + ":enchant_strip";

    private static final UUID DEATH_TOUCH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    // Overflow Heal: tracks current heal level per target entity UUID
    private static final Map<UUID, Integer> overflowHealCounters = new ConcurrentHashMap<>();

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

        int annihilationLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ANNIHILATION.get(), weapon);
        if (annihilationLv > 0) {
            handleAnnihilationFromPlayer(player, target, annihilationLv);
        }

        int overflowHealLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.OVERFLOW_HEAL.get(), weapon);
        if (overflowHealLv > 0) {
            handleOverflowHeal(target, overflowHealLv);
        }

        int enchantStripLv = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ENCHANT_STRIP.get(), weapon);
        if (enchantStripLv > 0) {
            handleEnchantStrip(target, enchantStripLv);
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
                case ANNIHILATION_ID -> handleAnnihilation(mob, target, affix.getLevel());
                case OVERFLOW_HEAL_ID -> handleOverflowHeal(target, affix.getLevel());
                case ENCHANT_STRIP_ID -> handleEnchantStrip(target, affix.getLevel());
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

    /**
     * Annihilation: lv*5% chance to bypass all protections and force-kill the target.
     * Uses a fallback chain: remove invulnerable flag -> set health to 0 -> kill() -> remove().
     */
    private static void handleAnnihilation(LivingEntity attacker, LivingEntity target, int level) {
        float chance = level * 0.05f;
        if (attacker.getRandom().nextFloat() < chance) {
            applyAnnihilation(target);
        }
    }

    private static void handleAnnihilationFromPlayer(Player player, LivingEntity target, int level) {
        float chance = level * 0.05f;
        if (player.getRandom().nextFloat() < chance) {
            applyAnnihilation(target);
        }
    }

    private static void applyAnnihilation(LivingEntity target) {
        // Step 1: Remove invulnerable flag
        target.setInvulnerable(false);

        // Step 2: Remove all effects that might prevent death
        target.removeAllEffects();

        // Step 3: Reset max health attribute modifiers and set health to 0
        var maxHealthAttr = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            for (AttributeModifier mod : new ArrayList<>(maxHealthAttr.getModifiers())) {
                maxHealthAttr.removeModifier(mod);
            }
        }
        target.setHealth(0);

        // Step 4: If still alive, use kill() (equivalent to /kill command)
        if (target.isAlive()) {
            target.invulnerableTime = 0;
            target.hurt(target.damageSources().genericKill(), Float.MAX_VALUE);
        }

        // Step 5: Last resort - kill() triggers die() so drops still occur
        if (target.isAlive()) {
            target.kill();
        }

        // Step 6: If STILL alive, manually drop loot and remove
        if (target.isAlive()) {
            dropLootAndRemove(target);
        }

        // Step 7: If entity STILL exists (remove was intercepted), tag it for peaceful despawn
        if (!target.isRemoved()) {
            target.getPersistentData().putBoolean("enchant_mobs_force_despawn", true);
        }
    }

    /**
     * Manually rolls the entity's loot table, spawns drops + experience, then removes the entity.
     * Used as a last resort when all normal kill paths are blocked by other mods' mixins.
     */
    private static void dropLootAndRemove(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        // Find the last player that damaged this entity
        LivingEntity creditEntity = target.getKillCredit();
        Player killer = creditEntity instanceof Player p ? p : null;

        // --- Loot Table ---
        ResourceLocation lootTableId = target.getLootTable();
        if (lootTableId != null) {
            LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(lootTableId);

            DamageSource lastDamage = target.getLastDamageSource();
            if (lastDamage == null) {
                lastDamage = target.damageSources().genericKill();
            }

            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.THIS_ENTITY, target)
                    .withParameter(LootContextParams.ORIGIN, target.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, lastDamage);

            if (killer != null) {
                builder.withOptionalParameter(LootContextParams.KILLER_ENTITY, killer)
                       .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, killer)
                       .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);

                // Apply Looting enchant level
                int lootingLevel = EnchantmentHelper.getMobLooting(killer);
                builder.withLuck(killer.getLuck() + lootingLevel);
            }

            LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
            List<ItemStack> drops = lootTable.getRandomItems(lootParams);

            for (ItemStack stack : drops) {
                ItemEntity itemEntity = new ItemEntity(
                        serverLevel,
                        target.getX(), target.getY(), target.getZ(),
                        stack
                );
                itemEntity.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(itemEntity);
            }
        }

        // --- Experience ---
        if (killer != null) {
            int xp = target.getExperienceReward();
            if (xp > 0) {
                ExperienceOrb.award(serverLevel, target.position(), xp);
            }
        }

        // --- Remove entity ---
        target.remove(Entity.RemovalReason.KILLED);
    }

    /**
     * Overflow Heal: lv% chance to apply Instant Health to target.
     * Heal level increments each application (0→1→...→29→reset).
     * At level 29, the heal amount overflows and causes instant death.
     */
    private static void handleOverflowHeal(LivingEntity target, int level) {
        float chance = level * 0.01f;
        if (target.getRandom().nextFloat() >= chance) return;

        UUID targetId = target.getUUID();
        int currentHealLevel = overflowHealCounters.getOrDefault(targetId, 0);

        // Apply instant health at current level
        target.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, currentHealLevel, false, true));

        // Increment counter, reset at 30
        currentHealLevel++;
        if (currentHealLevel >= 30) {
            overflowHealCounters.remove(targetId);
        } else {
            overflowHealCounters.put(targetId, currentHealLevel);
        }
    }

    /**
     * Enchant Strip: lv% chance to remove a random enchantment from target's equipment.
     */
    private static void handleEnchantStrip(LivingEntity target, int level) {
        float chance = level * 0.01f;
        if (target.getRandom().nextFloat() >= chance) return;

        // Collect all equipment slots with enchanted items
        EquipmentSlot[] allSlots = EquipmentSlot.values();
        List<EquipmentSlot> enchantedSlots = new ArrayList<>();

        for (EquipmentSlot slot : allSlots) {
            ItemStack item = target.getItemBySlot(slot);
            if (!item.isEmpty() && item.isEnchanted()) {
                enchantedSlots.add(slot);
            }
        }

        if (enchantedSlots.isEmpty()) return;

        // Pick random enchanted slot
        EquipmentSlot chosenSlot = enchantedSlots.get(target.getRandom().nextInt(enchantedSlots.size()));
        ItemStack item = target.getItemBySlot(chosenSlot);

        // Get all enchantments on this item
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item);
        if (enchantments.isEmpty()) return;

        // Pick a random enchantment to remove
        List<Enchantment> enchList = new ArrayList<>(enchantments.keySet());
        Enchantment toRemove = enchList.get(target.getRandom().nextInt(enchList.size()));
        enchantments.remove(toRemove);

        // Rewrite enchantments on the item
        EnchantmentHelper.setEnchantments(enchantments, item);
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
