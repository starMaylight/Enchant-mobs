package com.starmaylight.enchant_mobs.capability;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class CapabilityRegistry {

    public static final Capability<IHazardLevelCapability> HAZARD_LEVEL_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation HAZARD_LEVEL_KEY =
            new ResourceLocation(Enchant_mobs.MODID, "hazard_level");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(HAZARD_LEVEL_CAPABILITY).isPresent()) {
                event.addCapability(HAZARD_LEVEL_KEY, new HazardLevelProvider());
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Enchant_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(IHazardLevelCapability.class);
        }
    }
}
