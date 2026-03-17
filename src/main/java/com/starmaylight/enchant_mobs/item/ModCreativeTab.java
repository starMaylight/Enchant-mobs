package com.starmaylight.enchant_mobs.item;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Enchant_mobs.MODID);

    public static final RegistryObject<CreativeModeTab> ENCHANT_MOBS_TAB = TABS.register("enchant_mobs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.enchant_mobs"))
                    .icon(() -> new ItemStack(ModItems.HAZARD_CRYSTAL_UP.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.HAZARD_CRYSTAL_UP.get());
                        output.accept(ModItems.HAZARD_CRYSTAL_DOWN.get());
                        output.accept(ModItems.ENCHANT_HAZARD_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
