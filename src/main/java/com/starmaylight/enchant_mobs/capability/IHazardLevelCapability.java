package com.starmaylight.enchant_mobs.capability;

import net.minecraft.nbt.CompoundTag;

public interface IHazardLevelCapability {

    int getBossKillCount();

    void setBossKillCount(int count);

    void incrementBossKills();

    int getHazardLevel();

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);
}
