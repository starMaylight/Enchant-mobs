package com.starmaylight.enchant_mobs.affix;

import net.minecraft.nbt.CompoundTag;

public class AffixInstance {

    private final Affix affix;
    private final float effectMultiplier;

    public AffixInstance(Affix affix, float effectMultiplier) {
        this.affix = affix;
        this.effectMultiplier = effectMultiplier;
    }

    public Affix getAffix() {
        return affix;
    }

    public float getEffectMultiplier() {
        return effectMultiplier;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("affix", affix.serializeNBT());
        tag.putFloat("effectMultiplier", effectMultiplier);
        return tag;
    }

    public static AffixInstance deserializeNBT(CompoundTag tag) {
        Affix affix = Affix.deserializeNBT(tag.getCompound("affix"));
        float effectMultiplier = tag.getFloat("effectMultiplier");
        return new AffixInstance(affix, effectMultiplier);
    }
}
