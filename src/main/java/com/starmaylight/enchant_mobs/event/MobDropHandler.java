package com.starmaylight.enchant_mobs.event;

import com.starmaylight.enchant_mobs.Config;
import com.starmaylight.enchant_mobs.Enchant_mobs;
import com.starmaylight.enchant_mobs.affix.Affix;
import com.starmaylight.enchant_mobs.affix.AffixInstance;
import com.starmaylight.enchant_mobs.affix.MobAffixData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID)
public class MobDropHandler {

    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }

        if (!MobSpawnHandler.hasAffixes(mob)) {
            return;
        }

        MobAffixData data = MobSpawnHandler.getAffixData(mob);
        RandomSource random = mob.getRandom();

        for (AffixInstance inst : data.getAffixes()) {
            if (shouldDropEnchantedBook(inst, data.getSourceHazardLevel(), random)) {
                ItemStack book = createEnchantedBook(inst.getAffix());

                if (!book.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(
                            mob.level(),
                            mob.getX(),
                            mob.getY(),
                            mob.getZ(),
                            book
                    );
                    itemEntity.setDefaultPickUpDelay();

                    event.getDrops().add(itemEntity);
                }
            }
        }
    }

    private static boolean shouldDropEnchantedBook(AffixInstance inst, int hazardLevel, RandomSource random) {
        double baseRate = Config.baseBookDropRate;

        double tierModifier = 1.0 / inst.getAffix().getTier();

        double hazardBonus = 1.0 + (hazardLevel * 0.02);

        double finalRate = baseRate * tierModifier * hazardBonus;

        return random.nextDouble() < finalRate;
    }

    private static ItemStack createEnchantedBook(Affix affix) {
        Enchantment enchantment = affix.getEnchantment();

        if (enchantment == null) {
            return ItemStack.EMPTY;
        }

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, affix.getLevel()));

        return book;
    }
}
