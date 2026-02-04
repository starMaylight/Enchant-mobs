package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.affix.AffixRegistry;
import com.starmaylight.enchant_mobs.affix.AffixSelector;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import com.starmaylight.enchant_mobs.capability.CapabilityRegistry;
import com.starmaylight.enchant_mobs.equipment.AffixEquipmentManager;
import com.starmaylight.enchant_mobs.util.AffixNameGenerator;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class MobSpawnHandler {

    public static final String AFFIX_DATA_KEY = Enchant_mobs.MODID + ":affixes";
    public static final String PROCESSED_KEY = Enchant_mobs.MODID + ":processed";

    @SubscribeEvent
    public static void onMobSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(mob instanceof Monster)) {
            return;
        }

        if (mob.getPersistentData().getBoolean(PROCESSED_KEY)) {
            return;
        }

        mob.getPersistentData().putBoolean(PROCESSED_KEY, true);

        if (!AffixRegistry.isInitialized()) {
            return;
        }

        Player nearestPlayer = event.getLevel().getNearestPlayer(mob, 64.0);
        if (nearestPlayer == null) {
            return;
        }

        nearestPlayer.getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(cap -> {
            int hazardLevel = cap.getHazardLevel();

            if (hazardLevel <= 0) {
                return;
            }

            MobAffixData affixData = AffixSelector.selectAffixes(mob, hazardLevel, mob.getRandom());

            if (affixData.isEmpty()) {
                return;
            }

            mob.getPersistentData().put(AFFIX_DATA_KEY, affixData.serializeNBT());

            mob.setCustomName(AffixNameGenerator.generateName(mob, affixData));
            mob.setCustomNameVisible(true);

            // Apply enchanted equipment to mob
            AffixEquipmentManager.applyAffixEquipment(mob, affixData);
        });
    }

    public static MobAffixData getAffixData(Mob mob) {
        if (!mob.getPersistentData().contains(AFFIX_DATA_KEY)) {
            return MobAffixData.EMPTY;
        }
        return MobAffixData.deserializeNBT(mob.getPersistentData().getCompound(AFFIX_DATA_KEY));
    }

    public static boolean hasAffixes(Mob mob) {
        return mob.getPersistentData().contains(AFFIX_DATA_KEY);
    }
}
