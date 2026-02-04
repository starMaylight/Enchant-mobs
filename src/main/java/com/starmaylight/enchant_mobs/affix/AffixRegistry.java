package com.starmaylight.enchant_mobs.affix;

import com.mojang.logging.LogUtils;
import com.starmaylight.enchant_mobs.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

public class AffixRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<Affix> ALL_AFFIXES = new ArrayList<>();
    private static final List<Affix> ARMOR_AFFIXES = new ArrayList<>();
    private static final List<Affix> WEAPON_AFFIXES = new ArrayList<>();
    private static final Map<ResourceLocation, List<Affix>> AFFIXES_BY_ENCHANTMENT = new HashMap<>();

    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        ALL_AFFIXES.clear();
        ARMOR_AFFIXES.clear();
        WEAPON_AFFIXES.clear();
        AFFIXES_BY_ENCHANTMENT.clear();

        LOGGER.info("Initializing Affix Registry...");

        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (id == null) continue;

            if (Config.enchantmentBlacklist != null && Config.enchantmentBlacklist.contains(id.toString())) {
                LOGGER.debug("Skipping blacklisted enchantment: {}", id);
                continue;
            }

            AffixType type = categorizeEnchantment(enchantment);
            if (type == null) {
                continue;
            }

            List<Affix> enchantAffixes = new ArrayList<>();

            for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                int tier = calculateTier(enchantment, level);
                String displayName = generateDisplayName(enchantment, level);

                Affix affix = new Affix(id, level, type, displayName, tier);
                ALL_AFFIXES.add(affix);
                enchantAffixes.add(affix);

                if (type == AffixType.ARMOR) {
                    ARMOR_AFFIXES.add(affix);
                } else {
                    WEAPON_AFFIXES.add(affix);
                }
            }

            AFFIXES_BY_ENCHANTMENT.put(id, enchantAffixes);
        }

        initialized = true;
        LOGGER.info("Affix Registry initialized with {} affixes ({} armor, {} weapon)",
                ALL_AFFIXES.size(), ARMOR_AFFIXES.size(), WEAPON_AFFIXES.size());
    }

    private static AffixType categorizeEnchantment(Enchantment enchantment) {
        EnchantmentCategory category = enchantment.category;

        if (category == EnchantmentCategory.ARMOR ||
                category == EnchantmentCategory.ARMOR_HEAD ||
                category == EnchantmentCategory.ARMOR_CHEST ||
                category == EnchantmentCategory.ARMOR_LEGS ||
                category == EnchantmentCategory.ARMOR_FEET ||
                category == EnchantmentCategory.WEARABLE) {
            return AffixType.ARMOR;
        }

        if (category == EnchantmentCategory.WEAPON ||
                category == EnchantmentCategory.BOW ||
                category == EnchantmentCategory.CROSSBOW ||
                category == EnchantmentCategory.TRIDENT) {
            return AffixType.WEAPON;
        }

        ItemStack testSword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack testArmor = new ItemStack(Items.DIAMOND_CHESTPLATE);

        if (enchantment.canEnchant(testSword)) {
            return AffixType.WEAPON;
        }
        if (enchantment.canEnchant(testArmor)) {
            return AffixType.ARMOR;
        }

        return null;
    }

    private static int calculateTier(Enchantment enchantment, int level) {
        int rarityWeight = switch (enchantment.getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case VERY_RARE -> 4;
        };

        return Math.min(rarityWeight + level - 1, 10);
    }

    private static String generateDisplayName(Enchantment enchantment, int level) {
        String baseName = enchantment.getFullname(level).getString();
        return baseName;
    }

    public static List<Affix> getAllAffixes() {
        return Collections.unmodifiableList(ALL_AFFIXES);
    }

    public static List<Affix> getArmorAffixes() {
        return Collections.unmodifiableList(ARMOR_AFFIXES);
    }

    public static List<Affix> getWeaponAffixes() {
        return Collections.unmodifiableList(WEAPON_AFFIXES);
    }

    public static List<Affix> getAffixesUpToTier(int maxTier) {
        return ALL_AFFIXES.stream()
                .filter(a -> a.getTier() <= maxTier)
                .toList();
    }

    public static List<Affix> getAffixesByEnchantment(ResourceLocation enchantmentId) {
        return AFFIXES_BY_ENCHANTMENT.getOrDefault(enchantmentId, Collections.emptyList());
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
