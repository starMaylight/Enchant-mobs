package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class MobTickHandler {

    private static final String SLOWNESS_AURA_ID = Enchant_mobs.MODID + ":slowness_aura";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!MobSpawnHandler.hasAffixes(mob)) return;

        // Only process every 20 ticks (1 second) for performance
        if (mob.tickCount % 20 != 0) return;

        MobAffixData data = MobSpawnHandler.getAffixData(mob);

        for (AffixInstance inst : data.getAffixes()) {
            if (inst.getAffix().getEnchantmentId().toString().equals(SLOWNESS_AURA_ID)) {
                applySlownessAura(mob, inst.getAffix().getLevel());
                break;
            }
        }
    }

    private static void applySlownessAura(Mob mob, int level) {
        AABB area = mob.getBoundingBox().inflate(10.0);
        List<Player> nearbyPlayers = mob.level().getEntitiesOfClass(Player.class, area);

        for (Player player : nearbyPlayers) {
            // Apply slowness for 2 seconds (40 ticks), refreshed every second
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    40,
                    level - 1,
                    false,
                    true
            ));
        }
    }
}
