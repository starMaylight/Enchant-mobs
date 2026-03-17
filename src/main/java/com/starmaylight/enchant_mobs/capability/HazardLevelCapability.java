package com.starmaylight.enchant_mobs.capability;

import com.starmaylight.enchant_mobs.Config;
import net.minecraft.nbt.CompoundTag;

public class HazardLevelCapability implements IHazardLevelCapability {

    private int bossKillCount = 0;
    private int hazardLevelBonus = 0;

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
        int calculatedLevel = 0;
        if (bossKillCount >= Config.minKillsForHazard) {
            calculatedLevel = (int) Math.floor(
                    Config.hazardScaleFactor * Math.log(bossKillCount + 1)
            );
        }

        return Math.min(calculatedLevel + hazardLevelBonus, Config.maxHazardLevel);
    }

    @Override
    public int getHazardLevelBonus() {
        return hazardLevelBonus;
    }

    @Override
    public void setHazardLevelBonus(int bonus) {
        this.hazardLevelBonus = Math.max(0, bonus);
    }

    @Override
    public void addHazardLevelBonus(int amount) {
        this.hazardLevelBonus = Math.max(0, this.hazardLevelBonus + amount);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("bossKillCount", bossKillCount);
        tag.putInt("hazardLevelBonus", hazardLevelBonus);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("bossKillCount")) {
            this.bossKillCount = nbt.getInt("bossKillCount");
        }
        if (nbt.contains("hazardLevelBonus")) {
            this.hazardLevelBonus = nbt.getInt("hazardLevelBonus");
        }
    }
}
