package com.levelupstats;

import com.levelupstats.network.NetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(LevelUpStats.MODID)
public class LevelUpStats {
    public static final String MODID = "levelupstats";

    public LevelUpStats(IEventBus modEventBus) {
        ModAttachments.ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(NetworkHandler::register);
    }
}
