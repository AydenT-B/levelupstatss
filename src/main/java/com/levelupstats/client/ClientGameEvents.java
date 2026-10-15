package com.levelupstats.client;

import com.levelupstats.LevelUpStats;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Game-bus client events: opens the stats menu when the keybind is pressed. */
@EventBusSubscriber(modid = LevelUpStats.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN_MENU.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new StatsScreen());
            }
        }
    }
}
