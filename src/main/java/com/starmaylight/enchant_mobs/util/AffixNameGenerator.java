package com.starmaylight.enchant_mobs.util;

import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

public class AffixNameGenerator {

    private static final ChatFormatting[] TIER_COLORS = {
            ChatFormatting.WHITE,
            ChatFormatting.GREEN,
            ChatFormatting.BLUE,
            ChatFormatting.DARK_PURPLE,
            ChatFormatting.GOLD
    };

    public static Component generateName(LivingEntity mob, MobAffixData data) {
        int maxTier = data.getMaxTier();
        ChatFormatting color = TIER_COLORS[Math.min(maxTier / 2, TIER_COLORS.length - 1)];

        MutableComponent prefixComponent = Component.empty();
        boolean first = true;
        for (AffixInstance inst : data.getAffixes()) {
            if (!first) {
                prefixComponent.append(Component.literal(", "));
            }
            prefixComponent.append(inst.getAffix().getDisplayComponent());
            first = false;
        }

        MutableComponent nameComponent = prefixComponent
                .append(Component.literal(" "))
                .append(mob.getType().getDescription().copy())
                .withStyle(color)
                .withStyle(ChatFormatting.BOLD);

        return nameComponent;
    }

    public static ChatFormatting getColorForTier(int tier) {
        return TIER_COLORS[Math.min(tier / 2, TIER_COLORS.length - 1)];
    }
}
