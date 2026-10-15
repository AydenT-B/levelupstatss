package com.levelupstats.client;

import com.levelupstats.ModAttachments;
import com.levelupstats.PlayerStats;
import com.levelupstats.StatEffects;
import com.levelupstats.StatType;
import com.levelupstats.network.PrestigePayload;
import com.levelupstats.network.UpgradeStatPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** The stat upgrade menu, opened with the keybind. */
public class StatsScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int HEADER_HEIGHT = 50;
    private static final int ROW_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 36;

    private final List<Button> upgradeButtons = new ArrayList<>();
    private Button prestigeButton;
    private int left;
    private int top;
    private int panelHeight;

    public StatsScreen() {
        super(Component.translatable("screen.levelupstats.title"));
    }

    private PlayerStats stats() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? new PlayerStats() : mc.player.getData(ModAttachments.STATS.get());
    }

    private int playerLevels() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 0 : mc.player.experienceLevel;
    }

    @Override
    protected void init() {
        upgradeButtons.clear();
        StatType[] types = StatType.values();
        panelHeight = HEADER_HEIGHT + types.length * ROW_HEIGHT + FOOTER_HEIGHT;
        left = (width - PANEL_WIDTH) / 2;
        top = (height - panelHeight) / 2;

        for (int i = 0; i < types.length; i++) {
            final StatType type = types[i];
            int y = top + HEADER_HEIGHT + i * ROW_HEIGHT;
            Button button = Button.builder(Component.literal("+"),
                            b -> PacketDistributor.sendToServer(new UpgradeStatPayload(type.ordinal())))
                    .bounds(left + PANEL_WIDTH - 30, y + 4, 20, 20)
                    .build();
            upgradeButtons.add(addRenderableWidget(button));
        }

        prestigeButton = addRenderableWidget(Button.builder(Component.translatable("screen.levelupstats.prestige"),
                        b -> PacketDistributor.sendToServer(PrestigePayload.INSTANCE))
                .bounds(left + 10, top + panelHeight - 28, PANEL_WIDTH - 20, 20)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientModEvents.OPEN_MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + panelHeight + 2, 0xFF000000);
        g.fill(left, top, left + PANEL_WIDTH, top + panelHeight, 0xF0202028);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PlayerStats stats = stats();
        int levels = playerLevels();
        StatType[] types = StatType.values();

        // Update button states before they are drawn.
        for (int i = 0; i < types.length; i++) {
            StatType type = types[i];
            upgradeButtons.get(i).active = !stats.isMaxed(type) && levels >= stats.costFor(type);
        }
        prestigeButton.active = stats.allMaxed();

        super.render(g, mouseX, mouseY, partialTick);

        // Header
        g.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 8, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("screen.levelupstats.levels", levels), left + 10, top + 26, 0xFF80FF20);
        Component capText = Component.translatable("screen.levelupstats.cap", stats.cap(), stats.getPrestige());
        g.drawString(font, capText, left + PANEL_WIDTH - 10 - font.width(capText), top + 26, 0xFFFFD700);

        // Rows
        for (int i = 0; i < types.length; i++) {
            StatType type = types[i];
            int y = top + HEADER_HEIGHT + i * ROW_HEIGHT;
            int level = stats.get(type);

            g.drawString(font, Component.translatable(type.nameKey()), left + 10, y + 2, type.color());

            Component levelText = Component.translatable("screen.levelupstats.level", level, stats.cap());
            g.drawString(font, levelText, left + 105, y + 2, 0xFFFFFFFF);

            // Progress bar toward the current cap
            int barX = left + 150;
            int barY = y + 3;
            int barW = 60;
            g.fill(barX, barY, barX + barW, barY + 6, 0xFF000000);
            int filled = (int) (barW * (level / (float) stats.cap()));
            g.fill(barX, barY, barX + filled, barY + 6, type.color());

            // What the stat currently gives
            Component effect = StatEffects.describe(type, level);
            g.drawString(font, effect, left + 10, y + 15, 0xFFAAAAAA);

            Component right = stats.isMaxed(type)
                    ? Component.translatable("screen.levelupstats.max")
                    : Component.translatable("screen.levelupstats.cost", stats.costFor(type));
            int rightColor = stats.isMaxed(type) ? 0xFFFFD700 : (levels >= stats.costFor(type) ? 0xFF80FF20 : 0xFFFF5555);
            g.drawString(font, right, left + PANEL_WIDTH - 36 - font.width(right), y + 2, rightColor);
        }
    }
}
