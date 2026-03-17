package com.starmaylight.enchant_mobs.item;

import com.starmaylight.enchant_mobs.block.EnchantHazardBlockEntity;
import com.starmaylight.enchant_mobs.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Right-click on an Enchant Hazard block to disassemble it.
 * The block is removed and a Hazard Spawn Egg is dropped at its location.
 * Also cleans up the boss bar and despawns remaining summoned entities.
 */
public class HazardDisassemblerItem extends Item {

    public HazardDisassemblerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();

        // Check if the clicked block is an Enchant Hazard
        if (!level.getBlockState(pos).is(ModBlocks.ENCHANT_HAZARD.get())) {
            return InteractionResult.PASS;
        }

        // Clean up the block entity (boss bar, summoned entities handled by setRemoved)
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnchantHazardBlockEntity hazardBE) {
            hazardBE.cleanupForDisassembly((net.minecraft.server.level.ServerLevel) level);
        }

        // Remove the block
        level.removeBlock(pos, false);

        // Drop a Hazard Spawn Egg
        ItemStack spawnEgg = new ItemStack(ModItems.HAZARD_SPAWN_EGG.get());
        ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                spawnEgg
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);

        // Notify player
        if (context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(
                    Component.translatable("item.enchant_mobs.hazard_disassembler.msg")
                            .withStyle(ChatFormatting.YELLOW));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enchant_mobs.hazard_disassembler.tooltip")
                .withStyle(ChatFormatting.YELLOW));
    }
}
