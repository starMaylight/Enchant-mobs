package com.starmaylight.enchant_mobs.affix;

import com.starmaylight.enchant_mobs.Config;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

public class AffixSelector {

    public static MobAffixData selectAffixes(Mob mob, int hazardLevel, RandomSource random) {
        if (hazardLevel <= 0) {
            return MobAffixData.EMPTY;
        }

        if (!shouldApplyAffixes(hazardLevel, random)) {
            return MobAffixData.EMPTY;
        }

        int numAffixes = calculateAffixCount(hazardLevel, random);
        int maxTier = calculateMaxTier(hazardLevel);

        List<Affix> eligible = new ArrayList<>(AffixRegistry.getAffixesUpToTier(maxTier));
        if (eligible.isEmpty()) {
            return MobAffixData.EMPTY;
        }

        List<AffixInstance> selected = new ArrayList<>();
        for (int i = 0; i < numAffixes && !eligible.isEmpty(); i++) {
            Affix chosen = weightedRandomSelect(eligible, hazardLevel, random);
            if (chosen == null) break;

            float multiplier = calculateMultiplier(hazardLevel);
            selected.add(new AffixInstance(chosen, multiplier));

            final Affix finalChosen = chosen;
            eligible.removeIf(a -> a.getEnchantmentId().equals(finalChosen.getEnchantmentId()));
        }

        if (selected.isEmpty()) {
            return MobAffixData.EMPTY;
        }

        int colorCode = generateColorCode(selected, random);
        return new MobAffixData(selected, hazardLevel, colorCode);
    }

    private static boolean shouldApplyAffixes(int hazardLevel, RandomSource random) {
        double chance = Config.baseAffixChance + (hazardLevel * Config.affixChancePerLevel);
        chance = Math.min(chance, Config.maxAffixChance);
        return random.nextDouble() < chance;
    }

    private static int calculateAffixCount(int hazardLevel, RandomSource random) {
        int baseCount = 1 + (hazardLevel / 3);

        if (random.nextDouble() < Config.bonusAffixChance) {
            baseCount++;
        }

        return Math.min(baseCount, Config.maxAffixesPerMob);
    }

    private static int calculateMaxTier(int hazardLevel) {
        return Math.min(hazardLevel + 1, 10);
    }

    private static float calculateMultiplier(int hazardLevel) {
        return 1.0f + (hazardLevel * 0.1f);
    }

    private static Affix weightedRandomSelect(List<Affix> affixes, int hazardLevel, RandomSource random) {
        if (affixes.isEmpty()) return null;

        double totalWeight = 0;
        for (Affix a : affixes) {
            totalWeight += 1.0 / (a.getTier() * a.getTier());
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;

        for (Affix a : affixes) {
            cumulative += 1.0 / (a.getTier() * a.getTier());
            if (roll <= cumulative) {
                return a;
            }
        }

        return affixes.get(affixes.size() - 1);
    }

    private static int generateColorCode(List<AffixInstance> affixes, RandomSource random) {
        int maxTier = affixes.stream()
                .mapToInt(a -> a.getAffix().getTier())
                .max()
                .orElse(1);

        return switch (maxTier / 2) {
            case 0 -> 0xFFFFFF;
            case 1 -> 0x55FF55;
            case 2 -> 0x5555FF;
            case 3 -> 0xAA00AA;
            default -> 0xFFAA00;
        };
    }
}
