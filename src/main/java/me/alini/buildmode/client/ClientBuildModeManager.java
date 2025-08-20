package me.alini.buildmode.client;

import net.minecraft.client.Minecraft;

public class ClientBuildModeManager {
    private static boolean isInBuildMode = false;

    public static boolean isInBuildMode() {
        return isInBuildMode;
    }

    // 只控制状态和按钮显示，不直接切换 BuildModeScreen
    public static void setBuildMode(boolean state) {
        isInBuildMode = state;
        BuildModeButtonOverlay.setShowButton(state);
    }
    // 关闭建造模式界面
    public static void closeBuildMode() {
        // 关闭BuildModeScreen渲染的伪创造界面
        if (Minecraft.getInstance().screen instanceof BuildModeScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }
}