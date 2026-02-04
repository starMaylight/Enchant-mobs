package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.affix.Affix;
import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Combat handler for affix effects.
 *
 * Most enchantment effects are now handled automatically through the equipped
 * invisible armor/weapons with enchantments. This handler only processes effects
 * that don't work properly through equipment (like Thorns on mobs).
 */
@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class MobCombatHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        // Handle Thorns effect when affixed mob takes damage
        // Thorns may not trigger properly through equipment on mobs, so we handle it manually
        if (target instanceof Mob mob && MobSpawnHandler.hasAffixes(mob)) {
            handleThornsEffect(mob, attacker, event);
        }
    }

    /**
     * Handle Thorns enchantment effect manually.
     * Equipment-based Thorns may not always trigger on mobs, so we ensure it works.
     */
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

                // Vanilla Thorns: 15% chance per level, 1-4 damage (level/2 rounded up to level)
                // We simplify to always trigger with scaled damage
                float thornsDamage = (1.0f + level * 0.5f) * multiplier;

                // Only trigger if not already damaged by thorns this tick
                if (!livingAttacker.hurtMarked) {
                    livingAttacker.hurt(mob.damageSources().thorns(mob), thornsDamage);
                }
                break; // Only apply thorns once even if multiple thorns affixes
            }
        }
    }
}
