package com.starmaylight.enchant_mobs.block;

import com.mojang.logging.LogUtils;
import com.starmaylight.enchant_mobs.Config;
import com.starmaylight.enchant_mobs.affix.AffixRegistry;
import com.starmaylight.enchant_mobs.affix.AffixSelector;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import com.starmaylight.enchant_mobs.enchantment.ModEnchantments;
import com.starmaylight.enchant_mobs.equipment.AffixEquipmentManager;
import com.starmaylight.enchant_mobs.event.MobSpawnHandler;
import com.starmaylight.enchant_mobs.util.AffixNameGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

public class EnchantHazardBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_BOSS_KILLS = 100;
    private static final int BOSSES_PER_WAVE = 5;
    private static final int MINIONS_PER_WAVE = 30;
    private static final int SPAWN_RADIUS = 15;
    private static final double BOSS_BAR_RANGE = 64.0;
    private static final int FORCED_HAZARD_LEVEL = 200;

    private ServerBossEvent bossEvent;
    private int totalBossKills;
    private int currentWaveBosses;
    private final List<UUID> summonedBosses = new ArrayList<>();
    private final List<UUID> summonedMinions = new ArrayList<>();
    private boolean active;
    private int tickCounter;

    // Cached boss type list
    private List<EntityType<?>> cachedBossTypes;

    public EnchantHazardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANT_HAZARD_BE.get(), pos, state);
    }

    public void activate() {
        this.active = true;
        this.totalBossKills = 0;
        this.tickCounter = 0;
        this.cachedBossTypes = null;
        setChanged();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || !active) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }

        // Initialize boss bar lazily
        if (bossEvent == null) {
            bossEvent = new ServerBossEvent(
                    Component.translatable("block.enchant_mobs.enchant_hazard.boss_bar"),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS
            );
        }

        // Track nearby players for boss bar
        updateBossBarPlayers(serverLevel, pos);

        // Update boss bar progress
        float progress = (MAX_BOSS_KILLS - totalBossKills) / (float) MAX_BOSS_KILLS;
        bossEvent.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));

        // Check summoned bosses - remove dead ones and count kills
        checkBossStatus(serverLevel);

        // If no living bosses remain and we haven't finished, spawn a new wave
        if (summonedBosses.isEmpty() && totalBossKills < MAX_BOSS_KILLS) {
            spawnWave(serverLevel, pos);
        }

        // Check completion
        if (totalBossKills >= MAX_BOSS_KILLS) {
            completeEncounter(serverLevel, pos);
        }
    }

    private void updateBossBarPlayers(ServerLevel serverLevel, BlockPos pos) {
        if (bossEvent == null) return;

        List<ServerPlayer> nearbyPlayers = serverLevel.getPlayers(
                player -> player.blockPosition().closerThan(pos, BOSS_BAR_RANGE)
        );

        // Remove players who are no longer nearby
        Set<ServerPlayer> currentPlayers = new HashSet<>(bossEvent.getPlayers());
        for (ServerPlayer player : currentPlayers) {
            if (!nearbyPlayers.contains(player)) {
                bossEvent.removePlayer(player);
            }
        }

        // Add new nearby players
        for (ServerPlayer player : nearbyPlayers) {
            bossEvent.addPlayer(player);
        }
    }

    private void checkBossStatus(ServerLevel serverLevel) {
        Iterator<UUID> it = summonedBosses.iterator();
        while (it.hasNext()) {
            UUID bossId = it.next();
            Entity entity = serverLevel.getEntity(bossId);

            if (entity == null || !entity.isAlive()) {
                it.remove();
                totalBossKills++;
                currentWaveBosses--;
                setChanged();
                LOGGER.debug("Enchant Hazard: Boss killed. Total kills: {}/{}", totalBossKills, MAX_BOSS_KILLS);
            }
        }

        // Clean up dead minions from the list
        summonedMinions.removeIf(minionId -> {
            Entity entity = serverLevel.getEntity(minionId);
            return entity == null || !entity.isAlive();
        });
    }

    private void spawnWave(ServerLevel serverLevel, BlockPos pos) {
        LOGGER.info("Enchant Hazard: Spawning new wave at {} (kills: {}/{})", pos, totalBossKills, MAX_BOSS_KILLS);

        // Spawn bosses
        List<EntityType<?>> bossTypes = collectBossTypes(serverLevel);
        if (bossTypes.isEmpty()) {
            LOGGER.warn("Enchant Hazard: No boss types available to spawn!");
            return;
        }

        RandomSource random = serverLevel.getRandom();
        int bossesToSpawn = Math.min(BOSSES_PER_WAVE, MAX_BOSS_KILLS - totalBossKills);
        currentWaveBosses = 0;

        for (int i = 0; i < bossesToSpawn; i++) {
            EntityType<?> bossType = bossTypes.get(random.nextInt(bossTypes.size()));
            BlockPos spawnPos = findSpawnPosition(pos, random, serverLevel);

            Entity entity = bossType.create(serverLevel);
            if (entity instanceof Mob mob) {
                mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                        random.nextFloat() * 360.0F, 0.0F);
                mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null, null);
                mob.setPersistenceRequired();
                serverLevel.addFreshEntity(mob);
                summonedBosses.add(mob.getUUID());
                currentWaveBosses++;
            } else if (entity != null) {
                // For non-Mob entities like Ender Dragon
                entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 5, spawnPos.getZ() + 0.5,
                        random.nextFloat() * 360.0F, 0.0F);
                serverLevel.addFreshEntity(entity);
                summonedBosses.add(entity.getUUID());
                currentWaveBosses++;
            }
        }

        // Spawn minions with affixes
        spawnMinions(serverLevel, pos, random);

        setChanged();
    }

    private void spawnMinions(ServerLevel serverLevel, BlockPos pos, RandomSource random) {
        List<EntityType<?>> monsterTypes = collectMonsterTypes();
        if (monsterTypes.isEmpty()) return;

        for (int i = 0; i < MINIONS_PER_WAVE; i++) {
            EntityType<?> monsterType = monsterTypes.get(random.nextInt(monsterTypes.size()));
            BlockPos spawnPos = findSpawnPosition(pos, random, serverLevel);

            Entity entity = monsterType.create(serverLevel);
            if (entity instanceof Mob mob && mob instanceof Monster) {
                mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                        random.nextFloat() * 360.0F, 0.0F);
                mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null, null);
                mob.setPersistenceRequired();

                // Apply affixes at forced high hazard level
                applyForcedAffixes(mob, random);

                serverLevel.addFreshEntity(mob);
                summonedMinions.add(mob.getUUID());
            } else if (entity != null) {
                entity.discard();
            }
        }
    }

    private void applyForcedAffixes(Mob mob, RandomSource random) {
        if (!AffixRegistry.isInitialized()) {
            return;
        }

        mob.getPersistentData().putBoolean(MobSpawnHandler.PROCESSED_KEY, true);

        MobAffixData affixData = AffixSelector.selectAffixes(mob, FORCED_HAZARD_LEVEL, random);
        if (affixData.isEmpty()) {
            return;
        }

        mob.getPersistentData().put(MobSpawnHandler.AFFIX_DATA_KEY, affixData.serializeNBT());
        mob.setCustomName(AffixNameGenerator.generateName(mob, affixData));
        mob.setCustomNameVisible(true);

        AffixEquipmentManager.applyAffixEquipment(mob, affixData);
    }

    private List<EntityType<?>> collectBossTypes(ServerLevel level) {
        if (cachedBossTypes != null) {
            return cachedBossTypes;
        }

        Set<String> blacklist = Config.hazardBossBlacklist != null
                ? Config.hazardBossBlacklist : Set.of();

        List<EntityType<?>> bosses = new ArrayList<>();

        // Hardcoded vanilla bosses (filtered by blacklist)
        addIfNotBlacklisted(bosses, EntityType.WITHER, blacklist);
        addIfNotBlacklisted(bosses, EntityType.ENDER_DRAGON, blacklist);
        addIfNotBlacklisted(bosses, EntityType.ELDER_GUARDIAN, blacklist);
        addIfNotBlacklisted(bosses, EntityType.WARDEN, blacklist);

        // From config (filtered by blacklist)
        if (Config.customBossEntities != null) {
            for (String id : Config.customBossEntities) {
                if (blacklist.contains(id)) continue;
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                if (type != null && !bosses.contains(type)) {
                    bosses.add(type);
                }
            }
        }

        // Scan registry for anything with "boss" in name (filtered by blacklist)
        for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            String entityId = entry.getKey().location().toString();
            if (blacklist.contains(entityId)) continue;
            if (entry.getKey().location().getPath().contains("boss") && !bosses.contains(entry.getValue())) {
                bosses.add(entry.getValue());
            }
        }

        // Remove duplicates while preserving order
        cachedBossTypes = new ArrayList<>(new LinkedHashSet<>(bosses));
        LOGGER.info("Enchant Hazard: Collected {} boss types (blacklisted: {})", cachedBossTypes.size(), blacklist);
        return cachedBossTypes;
    }

    private void addIfNotBlacklisted(List<EntityType<?>> list, EntityType<?> type, Set<String> blacklist) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null && !blacklist.contains(id.toString()) && !list.contains(type)) {
            list.add(type);
        }
    }

    private List<EntityType<?>> collectMonsterTypes() {
        List<EntityType<?>> monsters = new ArrayList<>();
        for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            EntityType<?> type = entry.getValue();
            // Filter to monster-category entities
            if (type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
                monsters.add(type);
            }
        }
        return monsters;
    }

    private BlockPos findSpawnPosition(BlockPos center, RandomSource random, ServerLevel level) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = random.nextIntBetweenInclusive(-SPAWN_RADIUS, SPAWN_RADIUS);
            int dz = random.nextIntBetweenInclusive(-SPAWN_RADIUS, SPAWN_RADIUS);
            BlockPos candidate = center.offset(dx, 0, dz);

            // Find suitable Y position
            BlockPos surfacePos = level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            if (surfacePos.getY() > level.getMinBuildHeight()) {
                return surfacePos;
            }
        }
        // Fallback: spawn above the block
        return center.above(2);
    }

    private void completeEncounter(ServerLevel serverLevel, BlockPos pos) {
        LOGGER.info("Enchant Hazard: Encounter completed at {}! Dropping Annihilation V book.", pos);

        // Drop Annihilation enchanted book level 5
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book,
                new EnchantmentInstance(ModEnchantments.ANNIHILATION.get(), 5));

        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                serverLevel,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                book
        );
        itemEntity.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(itemEntity);

        // Clean up boss bar
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
            bossEvent = null;
        }

        // Kill remaining minions
        for (UUID minionId : summonedMinions) {
            Entity entity = serverLevel.getEntity(minionId);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        summonedMinions.clear();

        // Self-destruct the block
        active = false;
        serverLevel.destroyBlock(pos, false);
    }

    /**
     * Called by HazardDisassemblerItem to cleanly stop the encounter.
     * Despawns all summoned bosses and minions, cleans up boss bar.
     */
    public void cleanupForDisassembly(ServerLevel serverLevel) {
        // Despawn all summoned bosses
        for (UUID bossId : summonedBosses) {
            Entity entity = serverLevel.getEntity(bossId);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        summonedBosses.clear();

        // Despawn all summoned minions
        for (UUID minionId : summonedMinions) {
            Entity entity = serverLevel.getEntity(minionId);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        summonedMinions.clear();

        active = false;

        // Clean up boss bar
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
            bossEvent = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clean up boss bar when block entity is removed
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
            bossEvent = null;
        }
    }

    // === NBT Save/Load ===

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("totalBossKills", totalBossKills);
        tag.putBoolean("active", active);
        tag.putInt("currentWaveBosses", currentWaveBosses);

        ListTag bossUuids = new ListTag();
        for (UUID uuid : summonedBosses) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("uuid", uuid);
            bossUuids.add(uuidTag);
        }
        tag.put("summonedBosses", bossUuids);

        ListTag minionUuids = new ListTag();
        for (UUID uuid : summonedMinions) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("uuid", uuid);
            minionUuids.add(uuidTag);
        }
        tag.put("summonedMinions", minionUuids);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.totalBossKills = tag.getInt("totalBossKills");
        this.active = tag.getBoolean("active");
        this.currentWaveBosses = tag.getInt("currentWaveBosses");

        summonedBosses.clear();
        ListTag bossUuids = tag.getList("summonedBosses", Tag.TAG_COMPOUND);
        for (int i = 0; i < bossUuids.size(); i++) {
            summonedBosses.add(bossUuids.getCompound(i).getUUID("uuid"));
        }

        summonedMinions.clear();
        ListTag minionUuids = tag.getList("summonedMinions", Tag.TAG_COMPOUND);
        for (int i = 0; i < minionUuids.size(); i++) {
            summonedMinions.add(minionUuids.getCompound(i).getUUID("uuid"));
        }
    }
}
