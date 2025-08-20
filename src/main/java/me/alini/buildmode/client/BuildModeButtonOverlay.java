// 客户端按钮渲染与点击
package me.alini.buildmode.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BuildModeButtonOverlay {
    private static boolean showButton = false;
    private static final ResourceLocation BUTTON_ICON = ResourceLocation.fromNamespaceAndPath("buildmode", "textures/gui/buildmode_button.png");
    private static final int BUTTON_SIZE = 16;
    private static final int BUTTON_X_OFFSET = 15;
    private static final int BUTTON_Y_OFFSET = 15;

    // 按钮实际像素区域缓存
    private static int cachedX = 0;
    private static int cachedY = 0;
    private static boolean cachedHovered = false;

    public static void setShowButton(boolean value) {
        showButton = value;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {

        if (!showButton) return;
        if (!(Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        Minecraft.getInstance().player.sendSystemMessage(
                Component.literal("已打开背包，可点击建造模式按钮！")
        );
        // 获取 GUI 缩放后的窗口宽高
        int guiWidth = event.getWindow().getGuiScaledWidth();
        int guiHeight = event.getWindow().getGuiScaledHeight();

        // 背包界面左上角坐标（176x166为原版背包尺寸）
        int left = guiWidth / 2 - 176 / 2;
        int top = guiHeight / 2 - 166 / 2;
        int x = 50;
        int y = 40;
        // 获取鼠标 GUI 坐标
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        double scale = window.getGuiScale();
        double mouseX = mc.mouseHandler.xpos() / scale;
        double mouseY = mc.mouseHandler.ypos() / scale;
        boolean hovered = mouseX >= x && mouseX < x + BUTTON_SIZE && mouseY >= y && mouseY < y + BUTTON_SIZE;

        cachedX = x;
        cachedY = y;
        cachedHovered = hovered;

        RenderSystem.enableBlend();
        event.getGuiGraphics().fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, hovered ? 0x80FFFFFF : 0x80000000);
        event.getGuiGraphics().blit(BUTTON_ICON, x, y, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        System.out.println("渲染按钮: " + x + "," + y + " hovered=" + hovered);
        if (hovered) {
            event.getGuiGraphics().renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("buildmode.button.tooltip"),
                    (int) mouseX, (int) mouseY
            );
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!showButton) return;
        if (!(Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        if (event.getButton() != 0) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (mouseX >= cachedX && mouseX < cachedX + BUTTON_SIZE && mouseY >= cachedY && mouseY < cachedY + BUTTON_SIZE) {
            if (Minecraft.getInstance().screen instanceof BuildModeScreen) {
                Minecraft.getInstance().setScreen(null);
            } else {
                Minecraft.getInstance().setScreen(new BuildModeScreen());
            }
            event.setCanceled(true);
        }
    }
}