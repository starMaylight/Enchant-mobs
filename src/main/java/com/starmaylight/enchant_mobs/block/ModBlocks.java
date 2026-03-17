package com.starmaylight.enchant_mobs.block;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Enchant_mobs.MODID);

    public static final RegistryObject<Block> ENCHANT_HAZARD = BLOCKS.register("enchant_hazard",
            EnchantHazardBlock::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
