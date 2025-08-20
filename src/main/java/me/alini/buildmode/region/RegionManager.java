package me.alini.buildmode.region;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.alini.buildmode.client.ClientBuildModeManager;
import me.alini.buildmode.network.BuildModePacket;
import me.alini.buildmode.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import me.alini.buildmode.effect.ModEffects;
import net.minecraftforge.fml.loading.FMLPaths;
import me.alini.buildmode.client.BuildModeButtonOverlay;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber
public class RegionManager {
    private static final List<Region> regions = new CopyOnWriteArrayList<>();
    private static Path configFile;
    private static final Gson GSON = new Gson();
    private static int tickCounter = 0;

    public static void init(Path configDir) {
        configFile = configDir.resolve("buildmode/regions.json");
        load();
    }

    public static void load() {
        try (FileReader reader = new FileReader(configFile.toFile())) {
            List<Region> loaded = GSON.fromJson(reader, new TypeToken<List<Region>>(){}.getType());
            regions.clear();
            if (loaded != null) regions.addAll(loaded);
        } catch (Exception e) {
            System.err.println("[RegionManager] 加载区域数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void save() {
        // 获取配置文件路径
        File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "buildmode");
        File regionsFile = new File(configDir, "regions.json");

        try {
            // 关键修复：创建父目录（如果不存在）
            if (!configDir.exists()) {
                configDir.mkdirs(); // 递归创建所有不存在的目录
            }

            // 写入文件（现在目录已确保存在）
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(regionsFile)) {
                gson.toJson(regions, writer);
                // 告诉用户保存成功
                System.out.println("[RegionManager] 区域数据已保存到: " + regionsFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[RegionManager] 保存区域数据失败: " + e.getMessage());
        }
    }

    private static final java.util.Set<java.util.UUID> insidePlayers = new java.util.HashSet<>();

    public static void tickPlayer(ServerPlayer player) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;
        boolean inside = isInsideRegion(player);
        UUID uuid = player.getUUID();
        boolean wasInside = insidePlayers.contains(uuid);

        if (inside) {
            if (!wasInside) {
                player.sendSystemMessage(
                        Component.literal("您已进入建造模式区域！")
                                .withStyle(style -> style.withColor(0xFF69B4))
                );
                insidePlayers.add(uuid);
                // 向客户端发送建造模式状态
                ModMessages.sendToClient(new BuildModePacket(true), player);

            }
        } else {
            if (wasInside) {
                player.sendSystemMessage(
                        Component.literal("您已离开建造模式区域！")
                                .withStyle(style -> style.withColor(0xFF69B4))
                );
                insidePlayers.remove(uuid);
                // 向客户端发送建造模式状态
                ModMessages.sendToClient(new BuildModePacket(false), player);

            }
            if (player.hasEffect(ModEffects.BUILDFLY.get())) {
                player.removeEffect(ModEffects.BUILDFLY.get());
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, true, false, false));
            }
        }
    }

    // 合并后的主方法
    public static boolean isInsideRegion(ServerPlayer player) {
        Vec3 pos = player.position();
        for (Region region : regions) {
            if (region.isInside(pos)) return true;
        }
        return false;
    }

    // 兼容旧接口，直接调用主方法
    public static boolean isInBuildRegion(ServerPlayer player) {
        return isInsideRegion(player);
    }

    public static boolean addRegion(String name, double x1, double y1, double z1, double x2, double y2, double z2) {
        if (regions.stream().anyMatch(r -> r.name.equals(name))) {
            return false;
        }
        regions.add(new Region(name, x1, y1, z1, x2, y2, z2));
        save();
        return true;
    }

    public static List<Region> getRegions() {
        return regions;
    }

    public static class Region {
        public String name;
        public double x1, y1, z1, x2, y2, z2;

        public Region(String name, double x1, double y1, double z1, double x2, double y2, double z2) {
            this.name = name;
            this.x1 = Math.min(x1, x2);
            this.y1 = Math.min(y1, y2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.y2 = Math.max(y1, y2);
            this.z2 = Math.max(z1, z2);
        }

        public boolean isInside(Vec3 pos) {
            return pos.x >= x1 && pos.x <= x2 &&
                    pos.y >= y1 && pos.y <= y2 &&
                    pos.z >= z1 && pos.z <= z2;
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("buildmode")
                        .then(Commands.literal("region")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .then(Commands.argument("x1", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("y1", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("z1", DoubleArgumentType.doubleArg())
                                                                        .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                                                                                .then(Commands.argument("y2", DoubleArgumentType.doubleArg())
                                                                                        .then(Commands.argument("z2", DoubleArgumentType.doubleArg())
                                                                                                .executes(ctx -> {
                                                                                                    String name = StringArgumentType.getString(ctx, "name");
                                                                                                    double x1 = DoubleArgumentType.getDouble(ctx, "x1");
                                                                                                    double y1 = DoubleArgumentType.getDouble(ctx, "y1");
                                                                                                    double z1 = DoubleArgumentType.getDouble(ctx, "z1");
                                                                                                    double x2 = DoubleArgumentType.getDouble(ctx, "x2");
                                                                                                    double y2 = DoubleArgumentType.getDouble(ctx, "y2");
                                                                                                    double z2 = DoubleArgumentType.getDouble(ctx, "z2");
                                                                                                    addRegion(name, x1, y1, z1, x2, y2, z2);
                                                                                                    ctx.getSource().sendSuccess(() -> Component.literal("区域已添加: " + name), false);
                                                                                                    return 1;
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    boolean removed = removeRegion(name);
                                                    if (removed) {
                                                        ctx.getSource().sendSuccess(() -> Component.literal("已移除区域: " + name), false);
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.literal("未找到区域: " + name));
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            if (regions.isEmpty()) {
                                                source.sendSuccess(() -> Component.literal("没有定义任何区域"), false);
                                            } else {
                                                StringBuilder sb = new StringBuilder("已定义区域:\n");
                                                for (Region region : regions) {
                                                    sb.append(region.name).append(": [")
                                                            .append(region.x1).append(", ").append(region.y1).append(", ").append(region.z1).append("] - [")
                                                            .append(region.x2).append(", ").append(region.y2).append(", ").append(region.z2).append("]\n");
                                                }
                                                source.sendSuccess(() -> Component.literal(sb.toString()), false);
                                            }
                                            return 1;
                                        })
                                )
                        )
        );
    }

    public static boolean removeRegion(String name) {
        boolean removed = regions.removeIf(r -> r.name.equals(name));
        if (removed) save();
        return removed;
    }
}