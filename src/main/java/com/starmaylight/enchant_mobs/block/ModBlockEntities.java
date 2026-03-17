package com.starmaylight.enchant_mobs.block;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Enchant_mobs.MODID);

    public static final RegistryObject<BlockEntityType<EnchantHazardBlockEntity>> ENCHANT_HAZARD_BE =
            BLOCK_ENTITIES.register("enchant_hazard",
                    () -> BlockEntityType.Builder.of(EnchantHazardBlockEntity::new,
                            ModBlocks.ENCHANT_HAZARD.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
