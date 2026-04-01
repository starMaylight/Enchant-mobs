package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import com.starmaylight.enchant_mobs.enchantment.ModEnchantments;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class MobTickHandler {

    private static final String SLOWNESS_AURA_ID = Enchant_mobs.MODID + ":slowness_aura";
    private static final String DEBUFF_AURA_ID = Enchant_mobs.MODID + ":debuff_aura";

    // All negative effects for Debuff Aura random selection
    private static final MobEffect[] DEBUFF_EFFECTS = {
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.BLINDNESS,
            MobEffects.HUNGER,
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.LEVITATION,
            MobEffects.UNLUCK,
            MobEffects.DARKNESS
    };

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity entity = event.getEntity();

        // --- Annihilation force-despawn: entity tagged but survived all kill attempts ---
        if (entity.getPersistentData().getBoolean("enchant_mobs_force_despawn")) {
            handleForceDespawn(entity);
            return; // Don't process other tick effects for a doomed entity
        }

        // --- Affix mob tick effects ---
        if (entity instanceof Mob mob && MobSpawnHandler.hasAffixes(mob)) {
            // Only process every 10 ticks for performance
            if (mob.tickCount % 10 != 0) return;

            MobAffixData data = MobSpawnHandler.getAffixData(mob);

            for (AffixInstance inst : data.getAffixes()) {
                String enchId = inst.getAffix().getEnchantmentId().toString();
                int level = inst.getAffix().getLevel();

                switch (enchId) {
                    case SLOWNESS_AURA_ID -> {
                        if (mob.tickCount % 20 == 0) applySlownessAura(mob, level);
                    }
                    case DEBUFF_AURA_ID -> applyDebuffAura(mob, level);
                }
            }
        }

        // --- Player armor enchantment tick effects ---
        if (entity instanceof Player player) {
            if (player.tickCount % 10 != 0) return;
            handlePlayerArmorTick(player);
        }
    }

    private static void handlePlayerArmorTick(Player player) {
        // Debuff Aura from player armor
        int debuffAuraLv = getMaxArmorEnchantLevel(player, ModEnchantments.DEBUFF_AURA.get());
        if (debuffAuraLv > 0) {
            applyDebuffAuraFromPlayer(player, debuffAuraLv);
        }

        // Creative Flight from player armor
        int creativeFlightLv = getMaxArmorEnchantLevel(player, ModEnchantments.CREATIVE_FLIGHT.get());
        if (creativeFlightLv > 0) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            // Remove flight if no longer wearing the enchantment (and not in creative/spectator)
            if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    private static int getMaxArmorEnchantLevel(Player player, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        int maxLevel = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                int lv = EnchantmentHelper.getItemEnchantmentLevel(enchantment, armor);
                if (lv > maxLevel) maxLevel = lv;
            }
        }
        return maxLevel;
    }

    /**
     * Force-despawn an entity that survived all Annihilation kill attempts.
     * Simulates Peaceful difficulty despawn behavior: hostile mobs (Enemy) are discarded.
     * For non-hostile entities, force checkDespawn() which respects Peaceful logic.
     * As a final fallback, directly discard the entity.
     */
    private static void handleForceDespawn(LivingEntity entity) {
        // Try 1: If it's a Mob, call checkDespawn with Peaceful-like conditions
        if (entity instanceof Mob mob) {
            // Directly invoke the peaceful despawn path
            // In vanilla, Mob.checkDespawn() calls this.discard() when difficulty == PEACEFUL for Enemy mobs
            if (mob instanceof Enemy) {
                mob.discard();
                if (mob.isRemoved()) return;
            }

            // Try checkDespawn - some mods override this
            mob.checkDespawn();
            if (mob.isRemoved()) return;
        }

        // Try 2: setHealth(0) + kill() again each tick to overwhelm any cooldown-based protection
        entity.setInvulnerable(false);
        entity.invulnerableTime = 0;
        entity.setHealth(0);
        if (entity.isAlive()) {
            entity.kill();
        }

        // Try 3: Hard discard - bypasses all death logic
        if (!entity.isRemoved()) {
            entity.discard();
        }
    }

    private static void applySlownessAura(Mob mob, int level) {
        AABB area = mob.getBoundingBox().inflate(10.0);
        List<Player> nearbyPlayers = mob.level().getEntitiesOfClass(Player.class, area);

        for (Player player : nearbyPlayers) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 40, level - 1, false, true));
        }
    }

    /**
     * Debuff Aura (mob): every 10 ticks, lv*10% chance to apply a random debuff at lv strength
     * to all players within 10 blocks.
     */
    private static void applyDebuffAura(Mob mob, int level) {
        float chance = level * 0.10f;
        if (mob.getRandom().nextFloat() >= chance) return;

        AABB area = mob.getBoundingBox().inflate(10.0);
        List<Player> nearbyPlayers = mob.level().getEntitiesOfClass(Player.class, area);

        MobEffect debuff = DEBUFF_EFFECTS[mob.getRandom().nextInt(DEBUFF_EFFECTS.length)];

        for (Player player : nearbyPlayers) {
            player.addEffect(new MobEffectInstance(debuff, 100, level - 1, false, true));
        }
    }

    /**
     * Debuff Aura (player armor): every 10 ticks, lv*10% chance to apply a random debuff
     * to all mobs within 10 blocks.
     */
    private static void applyDebuffAuraFromPlayer(Player player, int level) {
        float chance = level * 0.10f;
        if (player.getRandom().nextFloat() >= chance) return;

        AABB area = player.getBoundingBox().inflate(10.0);
        List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, area);

        MobEffect debuff = DEBUFF_EFFECTS[player.getRandom().nextInt(DEBUFF_EFFECTS.length)];

        for (Mob mob : nearbyMobs) {
            mob.addEffect(new MobEffectInstance(debuff, 100, level - 1, false, true));
        }
    }
}
