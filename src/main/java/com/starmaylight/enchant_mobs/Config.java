package com.starmaylight.enchant_mobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // === Hazard Level Settings ===
    private static final ForgeConfigSpec.IntValue MIN_KILLS_FOR_HAZARD;
    private static final ForgeConfigSpec.DoubleValue HAZARD_SCALE_FACTOR;
    private static final ForgeConfigSpec.IntValue MAX_HAZARD_LEVEL;

    // === Boss Detection ===
    private static final ForgeConfigSpec.BooleanValue ELDER_GUARDIAN_IS_BOSS;
    private static final ForgeConfigSpec.BooleanValue WARDEN_IS_BOSS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_BOSS_ENTITIES;

    // === Affix Settings ===
    private static final ForgeConfigSpec.DoubleValue BASE_AFFIX_CHANCE;
    private static final ForgeConfigSpec.DoubleValue AFFIX_CHANCE_PER_LEVEL;
    private static final ForgeConfigSpec.DoubleValue MAX_AFFIX_CHANCE;
    private static final ForgeConfigSpec.IntValue MAX_AFFIXES_PER_MOB;
    private static final ForgeConfigSpec.DoubleValue BONUS_AFFIX_CHANCE;

    // === Loot Settings ===
    private static final ForgeConfigSpec.DoubleValue BASE_BOOK_DROP_RATE;

    // === Enchantment Blacklist ===
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENCHANTMENT_BLACKLIST;

    // === Persistence ===
    private static final ForgeConfigSpec.BooleanValue PERSIST_ACROSS_DEATH;

    // === Keybind ===
    private static final ForgeConfigSpec.ConfigValue<String> HAZARD_DISPLAY_KEY;

    static {
        BUILDER.push("hazard");
        MIN_KILLS_FOR_HAZARD = BUILDER
                .comment("Minimum boss kills required before hazard level increases")
                .defineInRange("minKillsForHazard", 1, 0, 100);
        HAZARD_SCALE_FACTOR = BUILDER
                .comment("Scaling factor for hazard level calculation (higher = slower progression)")
                .defineInRange("hazardScaleFactor", 2.0, 0.5, 10.0);
        MAX_HAZARD_LEVEL = BUILDER
                .comment("Maximum hazard level achievable")
                .defineInRange("maxHazardLevel", 10, 1, 50);
        BUILDER.pop();

        BUILDER.push("bosses");
        ELDER_GUARDIAN_IS_BOSS = BUILDER
                .comment("Should Elder Guardian count as a boss?")
                .define("elderGuardianIsBoss", true);
        WARDEN_IS_BOSS = BUILDER
                .comment("Should Warden count as a boss?")
                .define("wardenIsBoss", true);
        CUSTOM_BOSS_ENTITIES = BUILDER
                .comment("Additional entity IDs to count as bosses (e.g., 'modid:boss_mob')")
                .defineListAllowEmpty("customBossEntities", List.of(), Config::validateEntityName);
        BUILDER.pop();

        BUILDER.push("affixes");
        BASE_AFFIX_CHANCE = BUILDER
                .comment("Base chance for a mob to spawn with affixes at hazard 1 (0.0 - 1.0)")
                .defineInRange("baseAffixChance", 0.15, 0.0, 1.0);
        AFFIX_CHANCE_PER_LEVEL = BUILDER
                .comment("Additional affix chance per hazard level")
                .defineInRange("affixChancePerLevel", 0.07, 0.0, 0.5);
        MAX_AFFIX_CHANCE = BUILDER
                .comment("Maximum affix spawn chance")
                .defineInRange("maxAffixChance", 0.8, 0.0, 1.0);
        MAX_AFFIXES_PER_MOB = BUILDER
                .comment("Maximum number of affixes a single mob can have")
                .defineInRange("maxAffixesPerMob", 5, 1, 20);
        BONUS_AFFIX_CHANCE = BUILDER
                .comment("Chance for mob to get an extra affix beyond base count")
                .defineInRange("bonusAffixChance", 0.25, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("loot");
        BASE_BOOK_DROP_RATE = BUILDER
                .comment("Base drop rate for enchanted books from affixed mobs (0.0 - 1.0)")
                .defineInRange("baseBookDropRate", 0.05, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("enchantments");
        ENCHANTMENT_BLACKLIST = BUILDER
                .comment("Enchantments to exclude from affix generation")
                .defineListAllowEmpty("blacklist", List.of(
                        "minecraft:mending",
                        "minecraft:unbreaking",
                        "minecraft:silk_touch",
                        "minecraft:fortune",
                        "minecraft:infinity",
                        "minecraft:aqua_affinity",
                        "minecraft:respiration",
                        "minecraft:depth_strider",
                        "minecraft:frost_walker",
                        "minecraft:soul_speed",
                        "minecraft:swift_sneak",
                        "minecraft:efficiency",
                        "minecraft:lure",
                        "minecraft:luck_of_the_sea"
                ), Config::validateEnchantmentName);
        BUILDER.pop();

        BUILDER.push("persistence");
        PERSIST_ACROSS_DEATH = BUILDER
                .comment("Should hazard level persist when player dies?")
                .define("persistAcrossDeath", true);
        BUILDER.pop();

        BUILDER.push("keybind");
        HAZARD_DISPLAY_KEY = BUILDER
                .comment("Default key for displaying hazard level (can be changed in game controls)")
                .define("hazardDisplayKey", "P");
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Resolved config values
    public static int minKillsForHazard;
    public static double hazardScaleFactor;
    public static int maxHazardLevel;
    public static boolean elderGuardianIsBoss;
    public static boolean wardenIsBoss;
    public static Set<String> customBossEntities;
    public static double baseAffixChance;
    public static double affixChancePerLevel;
    public static double maxAffixChance;
    public static int maxAffixesPerMob;
    public static double bonusAffixChance;
    public static double baseBookDropRate;
    public static Set<String> enchantmentBlacklist;
    public static boolean persistAcrossDeath;
    public static String hazardDisplayKey;

    private static boolean validateEntityName(final Object obj) {
        if (!(obj instanceof String)) return false;
        try {
            new ResourceLocation((String) obj);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateEnchantmentName(final Object obj) {
        if (!(obj instanceof String enchantName)) return false;
        try {
            ResourceLocation loc = new ResourceLocation(enchantName);
            return ForgeRegistries.ENCHANTMENTS.containsKey(loc);
        } catch (Exception e) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        minKillsForHazard = MIN_KILLS_FOR_HAZARD.get();
        hazardScaleFactor = HAZARD_SCALE_FACTOR.get();
        maxHazardLevel = MAX_HAZARD_LEVEL.get();
        elderGuardianIsBoss = ELDER_GUARDIAN_IS_BOSS.get();
        wardenIsBoss = WARDEN_IS_BOSS.get();
        customBossEntities = new HashSet<>(CUSTOM_BOSS_ENTITIES.get());
        baseAffixChance = BASE_AFFIX_CHANCE.get();
        affixChancePerLevel = AFFIX_CHANCE_PER_LEVEL.get();
        maxAffixChance = MAX_AFFIX_CHANCE.get();
        maxAffixesPerMob = MAX_AFFIXES_PER_MOB.get();
        bonusAffixChance = BONUS_AFFIX_CHANCE.get();
        baseBookDropRate = BASE_BOOK_DROP_RATE.get();
        enchantmentBlacklist = new HashSet<>(ENCHANTMENT_BLACKLIST.get());
        persistAcrossDeath = PERSIST_ACROSS_DEATH.get();
        hazardDisplayKey = HAZARD_DISPLAY_KEY.get();
    }
}
