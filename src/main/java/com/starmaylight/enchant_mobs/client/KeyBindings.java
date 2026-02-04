package com.starmaylight.enchant_mobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {

    public static final String KEY_CATEGORY = "key.categories." + Enchant_mobs.MODID;
    public static final String KEY_SHOW_HAZARD = "key." + Enchant_mobs.MODID + ".show_hazard";

    public static KeyMapping showHazardKey;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        showHazardKey = new KeyMapping(
                KEY_SHOW_HAZARD,
                KeyConflictContext.IN_GAME,
                InputConstants.getKey(GLFW.GLFW_KEY_P, -1),
                KEY_CATEGORY
        );

        event.register(showHazardKey);
    }
}
