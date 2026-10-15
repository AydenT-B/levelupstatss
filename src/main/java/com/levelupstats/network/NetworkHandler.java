package com.levelupstats.network;

import com.levelupstats.ModAttachments;
import com.levelupstats.StatManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(UpgradeStatPayload.TYPE, UpgradeStatPayload.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) {
                StatManager.tryUpgrade(player, payload.stat());
            }
        });

        registrar.playToServer(PrestigePayload.TYPE, PrestigePayload.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) {
                StatManager.tryPrestige(player);
            }
        });

        registrar.playToClient(SyncStatsPayload.TYPE, SyncStatsPayload.CODEC, (payload, ctx) -> {
            // On the client, ctx.player() is the local player.
            ctx.player().setData(ModAttachments.STATS.get(), payload.stats());
        });
    }
}
