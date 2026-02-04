package com.starmaylight.enchant_mobs.affix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public class Affix {

    private final ResourceLocation enchantmentId;
    private final int level;
    private final AffixType type;
    private final String displayName;
    private final int tier;

    public Affix(ResourceLocation enchantmentId, int level, AffixType type, String displayName, int tier) {
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.type = type;
        this.displayName = displayName;
        this.tier = tier;
    }

    public ResourceLocation getEnchantmentId() {
        return enchantmentId;
    }

    public int getLevel() {
        return level;
    }

    public AffixType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }

    public Enchantment getEnchantment() {
        return ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("enchantmentId", enchantmentId.toString());
        tag.putInt("level", level);
        tag.putString("type", type.name());
        tag.putString("displayName", displayName);
        tag.putInt("tier", tier);
        return tag;
    }

    public static Affix deserializeNBT(CompoundTag tag) {
        ResourceLocation enchantmentId = new ResourceLocation(tag.getString("enchantmentId"));
        int level = tag.getInt("level");
        AffixType type = AffixType.valueOf(tag.getString("type"));
        String displayName = tag.getString("displayName");
        int tier = tag.getInt("tier");
        return new Affix(enchantmentId, level, type, displayName, tier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Affix affix = (Affix) o;
        return level == affix.level && Objects.equals(enchantmentId, affix.enchantmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enchantmentId, level);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
