package com.starmaylight.enchant_mobs.affix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MobAffixData {

    public static final MobAffixData EMPTY = new MobAffixData(Collections.emptyList(), 0, 0xFFFFFF);

    private final List<AffixInstance> affixes;
    private final int sourceHazardLevel;
    private final int colorCode;

    public MobAffixData(List<AffixInstance> affixes, int sourceHazardLevel, int colorCode) {
        this.affixes = new ArrayList<>(affixes);
        this.sourceHazardLevel = sourceHazardLevel;
        this.colorCode = colorCode;
    }

    public List<AffixInstance> getAffixes() {
        return Collections.unmodifiableList(affixes);
    }

    public int getSourceHazardLevel() {
        return sourceHazardLevel;
    }

    public int getColorCode() {
        return colorCode;
    }

    public boolean isEmpty() {
        return affixes.isEmpty();
    }

    public int getMaxTier() {
        return affixes.stream()
                .mapToInt(a -> a.getAffix().getTier())
                .max()
                .orElse(0);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag affixList = new ListTag();
        for (AffixInstance instance : affixes) {
            affixList.add(instance.serializeNBT());
        }
        tag.put("affixes", affixList);
        tag.putInt("sourceHazardLevel", sourceHazardLevel);
        tag.putInt("colorCode", colorCode);

        return tag;
    }

    public static MobAffixData deserializeNBT(CompoundTag tag) {
        if (tag.isEmpty()) {
            return EMPTY;
        }

        List<AffixInstance> affixes = new ArrayList<>();
        ListTag affixList = tag.getList("affixes", Tag.TAG_COMPOUND);
        for (int i = 0; i < affixList.size(); i++) {
            affixes.add(AffixInstance.deserializeNBT(affixList.getCompound(i)));
        }

        int sourceHazardLevel = tag.getInt("sourceHazardLevel");
        int colorCode = tag.getInt("colorCode");

        return new MobAffixData(affixes, sourceHazardLevel, colorCode);
    }
}
