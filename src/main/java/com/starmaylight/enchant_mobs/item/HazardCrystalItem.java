package com.starmaylight.enchant_mobs.item;

import com.starmaylight.enchant_mobs.capability.CapabilityRegistry;
import com.starmaylight.enchant_mobs.network.ModNetworking;
import com.starmaylight.enchant_mobs.network.SyncHazardLevelPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HazardCrystalItem extends Item {

    private final int amount;

    public HazardCrystalItem(Properties properties, int amount) {
        super(properties);
        this.amount = amount;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            player.getCapability(CapabilityRegistry.HAZARD_LEVEL_CAPABILITY).ifPresent(cap -> {
                cap.addHazardLevelBonus(amount);
                int newLevel = cap.getHazardLevel();

                if (amount > 0) {
                    player.sendSystemMessage(Component.translatable("item.enchant_mobs.hazard_crystal.increase_msg", newLevel)
                            .withStyle(ChatFormatting.RED));
                } else {
                    player.sendSystemMessage(Component.translatable("item.enchant_mobs.hazard_crystal.decrease_msg", newLevel)
                            .withStyle(ChatFormatting.GREEN));
                }

                ModNetworking.sendToPlayer(serverPlayer,
                        new SyncHazardLevelPacket(cap.getBossKillCount(), newLevel));
            });
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (amount > 0) {
            tooltip.add(Component.translatable("item.enchant_mobs.hazard_crystal.increase_tooltip", amount)
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.enchant_mobs.hazard_crystal.decrease_tooltip", Math.abs(amount))
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
