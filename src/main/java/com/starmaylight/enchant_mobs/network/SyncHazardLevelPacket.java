package com.starmaylight.enchant_mobs.network;

import com.starmaylight.enchant_mobs.client.ClientEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncHazardLevelPacket {

    private final int bossKills;
    private final int hazardLevel;

    public SyncHazardLevelPacket(int bossKills, int hazardLevel) {
        this.bossKills = bossKills;
        this.hazardLevel = hazardLevel;
    }

    public SyncHazardLevelPacket(FriendlyByteBuf buf) {
        this.bossKills = buf.readInt();
        this.hazardLevel = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(bossKills);
        buf.writeInt(hazardLevel);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientEventHandler.handleHazardSync(bossKills, hazardLevel);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
