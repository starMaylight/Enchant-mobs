package com.starmaylight.enchant_mobs.item;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Enchant_mobs.MODID);

    public static final RegistryObject<Item> INVISIBLE_HELMET = ITEMS.register("invisible_helmet",
            () -> new InvisibleArmorItem(InvisibleArmorMaterial.INVISIBLE, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INVISIBLE_CHESTPLATE = ITEMS.register("invisible_chestplate",
            () -> new InvisibleArmorItem(InvisibleArmorMaterial.INVISIBLE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INVISIBLE_LEGGINGS = ITEMS.register("invisible_leggings",
            () -> new InvisibleArmorItem(InvisibleArmorMaterial.INVISIBLE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INVISIBLE_BOOTS = ITEMS.register("invisible_boots",
            () -> new InvisibleArmorItem(InvisibleArmorMaterial.INVISIBLE, ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INVISIBLE_SWORD = ITEMS.register("invisible_sword",
            () -> new InvisibleSwordItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
