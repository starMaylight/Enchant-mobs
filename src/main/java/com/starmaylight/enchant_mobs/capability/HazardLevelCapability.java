package com.starmaylight.enchant_mobs.capability;

import com.starmaylight.enchant_mobs.Config;
import net.minecraft.nbt.CompoundTag;

public class HazardLevelCapability implements IHazardLevelCapability {

    private int bossKillCount = 0;

    @Override
    public int getBossKillCount() {
        return bossKillCount;
    }

    @Override
    public void setBossKillCount(int count) {
        this.bossKillCount = Math.max(0, count);
    }

    @Override
    public void incrementBossKills() {
        this.bossKillCount++;
    }

    @Override
    public int getHazardLevel() {
        if (bossKillCount < Config.minKillsForHazard) {
            return 0;
        }

        int level = (int) Math.floor(
                Config.hazardScaleFactor * Math.log(bossKillCount + 1)
        );

        return Math.min(level, Config.maxHazardLevel);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("bossKillCount", bossKillCount);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("bossKillCount")) {
            this.bossKillCount = nbt.getInt("bossKillCount");
        }
    }
}
