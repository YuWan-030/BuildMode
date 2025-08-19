// 客户端按钮渲染
package me.alini.buildmode.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BuildModeButtonOverlay {
    private static boolean showButton = false;
    private static final ResourceLocation BUTTON_ICON = new ResourceLocation("buildmode", "textures/gui/buildmode_button.png");
    private static final int BUTTON_SIZE = 16;
    private static final int BUTTON_X_OFFSET = 5;
    private static final int BUTTON_Y_OFFSET = 5;

    public static void setShowButton(boolean value) {
        showButton = value;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!showButton) return;
        if (!event.getOverlay().id().toString().equals("minecraft:container")) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int left = event.getWindow().getGuiScaledWidth() / 2 - 176 / 2;
        int top = event.getWindow().getGuiScaledHeight() / 2 - 166 / 2;
        int x = left + BUTTON_X_OFFSET;
        int y = top + BUTTON_Y_OFFSET;

        double mouseX = Minecraft.getInstance().mouseHandler.xpos() * event.getWindow().getGuiScaledWidth() / event.getWindow().getScreenWidth();
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() * event.getWindow().getGuiScaledHeight() / event.getWindow().getScreenHeight();
        boolean hovered = mouseX >= x && mouseX < x + BUTTON_SIZE && mouseY >= y && mouseY < y + BUTTON_SIZE;

        RenderSystem.enableBlend();
        guiGraphics.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, hovered ? 0x80FFFFFF : 0x80000000);
        guiGraphics.blit(BUTTON_ICON, x, y, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);

        // 悬停时显示提示文字（lang 里写 key: buildmode.button.tooltip）
        if (hovered) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("buildmode.button.tooltip"),
                    (int) mouseX, (int) mouseY
            );
        }

        if (hovered && Minecraft.getInstance().mouseHandler.isLeftPressed()) {
            if (Minecraft.getInstance().screen instanceof BuildModeScreen) {
                Minecraft.getInstance().setScreen(null);
            } else {
                Minecraft.getInstance().setScreen(new BuildModeScreen());
            }
        }
    }
}