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

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BuildModeButtonOverlay {
    private static boolean showButton = false;
    private static final ResourceLocation BUTTON_ICON = ResourceLocation.fromNamespaceAndPath("buildmode", "textures/gui/buildmode_button.png");
    private static final int BUTTON_SIZE = 16;
    private static final int BUTTON_X_OFFSET = 15;
    private static final int BUTTON_Y_OFFSET = 15;
    private boolean isMiddleDragging = false;
    private Set<Integer> dragSelectedSlots = new HashSet<>();

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

        int guiWidth = event.getWindow().getGuiScaledWidth();
        int guiHeight = event.getWindow().getGuiScaledHeight();
        int left = guiWidth / 2 - 176 / 2;
        int top = guiHeight / 2 - 166 / 2;

        // 按钮尺寸
        final int BUTTON_WIDTH = 20;
        final int BUTTON_HEIGHT = 20;
        final ResourceLocation WIDGETS = new ResourceLocation("minecraft", "textures/gui/widgets.png");

        int x = left; // 按钮左上角
        int y = top - 20;

        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        double scale = window.getGuiScale();
        double mouseX = mc.mouseHandler.xpos() / scale;
        double mouseY = mc.mouseHandler.ypos() / scale;
        boolean hovered = mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= y && mouseY < y + BUTTON_HEIGHT;

        cachedX = x;
        cachedY = y;
        cachedHovered = hovered;

        RenderSystem.enableBlend();

        // 使用九宫格绘制按钮背景
        int u = 0;
        int v = hovered ? 86 : 66; // hover 贴图
        int corner = 2; // 四角大小
        int edge = BUTTON_WIDTH - 2 * corner;

        var g = event.getGuiGraphics();

        // 四角
        g.blit(WIDGETS, x, y, u, v, corner, corner, 256, 256); // 左上
        g.blit(WIDGETS, x + BUTTON_WIDTH - corner, y, u + BUTTON_WIDTH - corner, v, corner, corner, 256, 256); // 右上
        g.blit(WIDGETS, x, y + BUTTON_HEIGHT - corner, u, v + BUTTON_HEIGHT - corner, corner, corner, 256, 256); // 左下
        g.blit(WIDGETS, x + BUTTON_WIDTH - corner, y + BUTTON_HEIGHT - corner, u + BUTTON_WIDTH - corner, v + BUTTON_HEIGHT - corner, corner, corner, 256, 256); // 右下

        // 上下边
        g.blit(WIDGETS, x + corner, y, u + corner, v, edge, corner, 256, 256); // 上
        g.blit(WIDGETS, x + corner, y + BUTTON_HEIGHT - corner, u + corner, v + BUTTON_HEIGHT - corner, edge, corner, 256, 256); // 下

        // 左右边
        g.blit(WIDGETS, x, y + corner, u, v + corner, corner, edge, 256, 256); // 左
        g.blit(WIDGETS, x + BUTTON_WIDTH - corner, y + corner, u + (200 - corner), v + corner, corner, edge, 256, 256); // 右

        // 中间
        g.blit(WIDGETS, x + corner, y + corner, u + corner, v + corner, edge, edge, 256, 256);

        // 绘制自定义图标
        int iconSize = 16;
        int iconX = x + (BUTTON_WIDTH - iconSize) / 2;
        int iconY = y + (BUTTON_HEIGHT - iconSize) / 2;
        g.blit(BUTTON_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

        // tooltip
        if (hovered) {
            g.renderTooltip(mc.font, Component.translatable("buildmode.button.tooltip"), (int) mouseX, (int) mouseY);
        }
    }

    // onMouseClick 事件中，使用 BUTTON_WIDTH/BUTTON_HEIGHT 替换 BUTTON_SIZE
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!showButton) return;
        if (!(Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        if (event.getButton() != 0) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        // 匹配渲染时的按钮尺寸
        final int BUTTON_WIDTH = 20;
        final int BUTTON_HEIGHT = 20;
        if (mouseX >= cachedX && mouseX < cachedX + BUTTON_WIDTH && mouseY >= cachedY && mouseY < cachedY + BUTTON_HEIGHT) {
            if (Minecraft.getInstance().screen instanceof BuildModeScreen) {
                Minecraft.getInstance().setScreen(null);
            } else {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    Minecraft.getInstance().setScreen(new BuildModeScreen());
                }
            }
            event.setCanceled(true);
        }
    }
}