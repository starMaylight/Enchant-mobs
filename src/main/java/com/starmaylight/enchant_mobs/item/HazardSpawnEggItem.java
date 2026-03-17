package com.starmaylight.enchant_mobs.item;

import com.starmaylight.enchant_mobs.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Spawn-egg styled item that places the Enchant Hazard block on use.
 * Right-click on a block surface to place the Enchant Hazard above it.
 */
public class HazardSpawnEggItem extends Item {

    public HazardSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());

        // Check if the target position is air/replaceable
        BlockState existingState = level.getBlockState(placePos);
        if (!existingState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        // Place the Enchant Hazard block
        level.setBlock(placePos, ModBlocks.ENCHANT_HAZARD.get().defaultBlockState(), 3);

        // Trigger setPlacedBy via block (activates the encounter)
        ModBlocks.ENCHANT_HAZARD.get().setPlacedBy(level, placePos,
                level.getBlockState(placePos), context.getPlayer(), context.getItemInHand());

        // Consume the item
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enchant_mobs.hazard_spawn_egg.tooltip")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.enchant_mobs.hazard_spawn_egg.tooltip2")
                .withStyle(ChatFormatting.GRAY));
    }
}
