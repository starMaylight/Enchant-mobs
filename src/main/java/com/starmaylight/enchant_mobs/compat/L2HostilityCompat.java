package com.starmaylight.enchant_mobs.compat;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

public class L2HostilityCompat {

    public static boolean isLoaded() {
        return ModList.get().isLoaded("l2hostility");
    }

    public static void clearTraits(LivingEntity target) {
        if (!isLoaded()) return;

        target.getCapability(MobTraitCap.CAPABILITY).ifPresent(cap -> {
            cap.deinit();
            cap.traits.clear();
            cap.lv = 0;
            cap.syncToClient(target);
        });
    }
}
