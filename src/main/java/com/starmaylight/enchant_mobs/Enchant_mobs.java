package com.starmaylight.enchant_mobs;

import com.mojang.logging.LogUtils;
import com.starmaylight.enchant_mobs.affix.AffixRegistry;
import com.starmaylight.enchant_mobs.item.ModItems;
import com.starmaylight.enchant_mobs.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Enchant_mobs.MODID)
public class Enchant_mobs {

    public static final String MODID = "enchant_mobs";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Enchant_mobs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // Register ModItems
        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Enchant Mobs: Common Setup");

        ModNetworking.register();
        LOGGER.info("Enchant Mobs: Network registered");

        event.enqueueWork(() -> {
            AffixRegistry.initialize();
            LOGGER.info("Enchant Mobs: Affix Registry initialized");
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Enchant Mobs: Server Starting");

        if (!AffixRegistry.isInitialized()) {
            AffixRegistry.initialize();
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Enchant Mobs: Client Setup");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
