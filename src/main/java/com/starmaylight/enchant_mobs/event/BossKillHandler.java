package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Config;
import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.capability.CapabilityRegistry;
import com.starmaylight.enchant_mobs.network.ModNetworking;
import com.starmaylight.enchant_mobs.network.SyncHazardLevelPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class BossKillHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        if (!isBoss(entity)) {
            return;
        }

        Entity killer = event.getSource().getEntity();

        if (killer instanceof Player player) {
            incrementBossKills(player);
            return;
        }

        if (killer instanceof TamableAnimal tamed && tamed.getOwner() instanceof Player player) {
            incrementBossKills(player);
            return;
        }

        if (entity.getLastHurtByMob() instanceof Player player) {
            incrementBossKills(player);
        }
    }

    private static void incrementBossKills(Player player) {
        player.getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(cap -> {
            cap.incrementBossKills();

            int newHazard = cap.getHazardLevel();
            int bossKills = cap.getBossKillCount();

            player.sendSystemMessage(Component.literal("Boss defeated! ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal("Hazard Level: " + newHazard)
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                    .append(Component.literal(" (Kills: " + bossKills + ")")
                            .withStyle(ChatFormatting.GRAY)));

            if (player instanceof ServerPlayer serverPlayer) {
                ModNetworking.sendToPlayer(serverPlayer, new SyncHazardLevelPacket(
                        cap.getBossKillCount(), cap.getHazardLevel()
                ));
            }
        });
    }

    public static boolean isBoss(LivingEntity entity) {
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return true;
        }

        if (entity instanceof ElderGuardian && Config.elderGuardianIsBoss) {
            return true;
        }

        if (entity instanceof Warden && Config.wardenIsBoss) {
            return true;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId != null && Config.customBossEntities != null) {
            return Config.customBossEntities.contains(entityId.toString());
        }

        return false;
    }
}
