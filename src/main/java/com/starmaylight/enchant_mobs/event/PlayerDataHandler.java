package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Config;
import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.capability.CapabilityRegistry;
import com.starmaylight.enchant_mobs.network.ModNetworking;
import com.starmaylight.enchant_mobs.network.SyncHazardLevelPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class PlayerDataHandler {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            copyCapabilityData(event);
            return;
        }

        if (Config.persistAcrossDeath) {
            copyCapabilityData(event);
        }
    }

    private static void copyCapabilityData(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();

        event.getOriginal().getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(oldCap -> {
            event.getEntity().getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(newCap -> {
                newCap.deserializeNBT(oldCap.serializeNBT());
            });
        });

        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(cap -> {
                ModNetworking.sendToPlayer(serverPlayer, new SyncHazardLevelPacket(
                        cap.getBossKillCount(), cap.getHazardLevel()
                ));
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(cap -> {
                ModNetworking.sendToPlayer(serverPlayer, new SyncHazardLevelPacket(
                        cap.getBossKillCount(), cap.getHazardLevel()
                ));
            });
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(cap -> {
                ModNetworking.sendToPlayer(serverPlayer, new SyncHazardLevelPacket(
                        cap.getBossKillCount(), cap.getHazardLevel()
                ));
            });
        }
    }
}
