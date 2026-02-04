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

        StringBuilder prefixBuilder = new StringBuilder();
        for (AffixInstance inst : data.getAffixes()) {
            if (prefixBuilder.length() > 0) {
                prefixBuilder.append(", ");
            }
            prefixBuilder.append(inst.getAffix().getDisplayName());
        }

        String baseName = mob.getType().getDescription().getString();

        MutableComponent nameComponent = Component.literal(prefixBuilder + " " + baseName)
                .withStyle(color)
                .withStyle(ChatFormatting.BOLD);

        return nameComponent;
    }

    public static ChatFormatting getColorForTier(int tier) {
        return TIER_COLORS[Math.min(tier / 2, TIER_COLORS.length - 1)];
    }
}
