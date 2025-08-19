package me.alini.buildmode.region;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public class RegionManager {
    private static final List<Region> regions = new ArrayList<>();
    private static Path configFile;
    private static final Gson GSON = new Gson();

    public static void init(Path configDir) {
        configFile = configDir.resolve("buildmode/regions.json");
        load();
    }

    public static void load() {
        try (FileReader reader = new FileReader(configFile.toFile())) {
            List<Region> loaded = GSON.fromJson(reader, new TypeToken<List<Region>>(){}.getType());
            regions.clear();
            if (loaded != null) regions.addAll(loaded);
        } catch (Exception ignored) {}
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            GSON.toJson(regions, writer);
        } catch (Exception ignored) {}
    }

    private static final java.util.Set<java.util.UUID> insidePlayers = new java.util.HashSet<>();

    public static void tickPlayer(ServerPlayer player) {
        boolean inside = isInsideRegion(player);
        UUID uuid = player.getUUID();
        boolean wasInside = insidePlayers.contains(uuid);

        if (inside) {
            if (!wasInside) {
                // 首次进入区域，发送提示
                player.sendSystemMessage(
                        Component.literal("您已进入建造模式区域！")
                                .withStyle(style -> style.withColor(0xFF69B4)) // 0xFF69B4 是亮粉色（Hot Pink）
                );
                insidePlayers.add(uuid);
            }
            // 区域内：允许玩家自行获得飞行，不自动赋予飞行能力
            return;
        } else {
            if (wasInside) {
                // 离开区域，移除记录
                player.sendSystemMessage(
                        Component.literal("您已离开建造模式区域！")
                                .withStyle(style -> style.withColor(0xFF69B4)) // 0xFF69B4 是亮粉色（Hot Pink）
                );
                insidePlayers.remove(uuid);
            }
            // 区域外：如果有漂浮效果则移除并给予缓降
            if (player.hasEffect(ModEffects.BUILDFLY.get())) {
                player.removeEffect(ModEffects.BUILDFLY.get());
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, true, false, false));
            }
        }
    }

    private static boolean isInsideRegion(ServerPlayer player) {
        Vec3 pos = player.position();
        for (Region region : regions) {
            if (region.isInside(pos)) return true;
        }
        return false;
    }

    public static void addRegion(String name, double x1, double y1, double z1, double x2, double y2, double z2) {
        regions.add(new Region(name, x1, y1, z1, x2, y2, z2));
        save();
    }

    public static List<Region> getRegions() {
        return regions;
    }

    // 区域数据结构：长方体
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

    // 命令注册
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    // 只需替换 registerCommands 方法，其它部分不变
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("buildmode")
                        .then(Commands.literal("region")
                                // 添加区域
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
                                // 移除区域
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
                                // 列出区域
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

    // 新增方法：移除区域
    public static boolean removeRegion(String name) {
        boolean removed = regions.removeIf(r -> r.name.equals(name));
        if (removed) save();
        return removed;
    }
}