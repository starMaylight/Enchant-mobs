package com.starmaylight.enchant_mobs.client;

import com.starmaylight.enchant_mobs.Enchant_mobs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Enchant_mobs.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static int clientBossKills = 0;
    private static int clientHazardLevel = 0;

    public static void handleHazardSync(int bossKills, int hazardLevel) {
        clientBossKills = bossKills;
        clientHazardLevel = hazardLevel;
    }

    public static int getClientBossKills() {
        return clientBossKills;
    }

    public static int getClientHazardLevel() {
        return clientHazardLevel;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null) {
            return;
        }

        if (KeyBindings.showHazardKey != null && KeyBindings.showHazardKey.consumeClick()) {
            displayHazardLevel(mc.player);
        }
    }

    private static void displayHazardLevel(Player player) {
        MutableComponent message = Component.literal("")
                .append(Component.literal("=== Hazard Status ===")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                .append(Component.literal("\n"))
                .append(Component.literal("Boss Kills: ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(clientBossKills))
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\n"))
                .append(Component.literal("Hazard Level: ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(clientHazardLevel))
                        .withStyle(getHazardColor(clientHazardLevel), ChatFormatting.BOLD));

        if (clientHazardLevel > 0) {
            message.append(Component.literal("\n"))
                    .append(Component.literal("Affix mobs are spawning!")
                            .withStyle(ChatFormatting.YELLOW));
        }

        player.sendSystemMessage(message);
    }

    private static ChatFormatting getHazardColor(int hazardLevel) {
        if (hazardLevel <= 0) return ChatFormatting.GRAY;
        if (hazardLevel <= 2) return ChatFormatting.WHITE;
        if (hazardLevel <= 4) return ChatFormatting.GREEN;
        if (hazardLevel <= 6) return ChatFormatting.YELLOW;
        if (hazardLevel <= 8) return ChatFormatting.RED;
        return ChatFormatting.DARK_RED;
    }
}
