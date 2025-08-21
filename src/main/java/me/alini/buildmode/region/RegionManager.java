package me.alini.buildmode.region;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import me.alini.buildmode.network.BuildModePacket;
import me.alini.buildmode.network.ModMessages;
import me.alini.buildmode.network.S2CSyncWhitelistPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


@Mod.EventBusSubscriber
public class RegionManager {
    private static final List<Region> regions = new CopyOnWriteArrayList<>();
    private static Path configFile;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
            .setPrettyPrinting().create();
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
        File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "buildmode");
        File regionsFile = new File(configDir, "regions.json");
        try {
            if (!configDir.exists()) configDir.mkdirs();
            try (FileWriter writer = new FileWriter(regionsFile)) {
                GSON.toJson(regions, writer);
                System.out.println("[RegionManager] 区域数据已保存到: " + regionsFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[RegionManager] 保存区域数据失败: " + e.getMessage());
        }
    }

    private static final Set<UUID> insidePlayers = new HashSet<>();

    public static void tickPlayer(ServerPlayer player) {
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
                // 主动推送白名单
                S2CSyncWhitelistPacket.syncToAll();
                ModMessages.sendToClient(new BuildModePacket(true), player);
            }
        } else {
            if (wasInside) {
                player.sendSystemMessage(
                        Component.literal("您已离开建造模式区域！")
                                .withStyle(style -> style.withColor(0xFF69B4))
                );
                insidePlayers.remove(uuid);
                ModMessages.sendToClient(new BuildModePacket(false), player);
            }
            if (player.hasEffect(ModEffects.BUILDFLY.get())) {
                player.removeEffect(ModEffects.BUILDFLY.get());
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, true, false, false));
            }
        }
    }

    public static boolean isInsideRegion(ServerPlayer player) {
        Vec3 pos = player.position();
        ResourceLocation dim = player.level().dimension().location();
        for (Region region : regions) {
            if (region.dimension.equals(dim) && region.isInside(pos)) return true;
        }
        return false;
    }

    public static boolean isInBuildRegion(ServerPlayer player) {
        return isInsideRegion(player);
    }

    public static boolean addRegion(String name, ResourceLocation dimension, double x1, double y1, double z1, double x2, double y2, double z2) {
        if (regions.stream().anyMatch(r -> r.name.equals(name) && r.dimension.equals(dimension))) {
            return false;
        }
        regions.add(new Region(name, dimension, x1, y1, z1, x2, y2, z2));
        save();
        return true;
    }

    public static List<Region> getRegions() {
        return regions;
    }

    public static class Region {
        public String name;
        public ResourceLocation dimension;
        public double x1, y1, z1, x2, y2, z2;

        public Region(String name, ResourceLocation dimension, double x1, double y1, double z1, double x2, double y2, double z2) {
            this.name = name;
            this.dimension = dimension;
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

    // ResourceLocation 的 Gson 适配器
    public static class ResourceLocationAdapter implements JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {
        @Override
        public JsonElement serialize(ResourceLocation src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        @Override
        public ResourceLocation deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new ResourceLocation(json.getAsString());
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
                                                .then(Commands.argument("dimension", StringArgumentType.word())
                                                        .then(Commands.argument("x1", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("y1", DoubleArgumentType.doubleArg())
                                                                        .then(Commands.argument("z1", DoubleArgumentType.doubleArg())
                                                                                .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                                                                                        .then(Commands.argument("y2", DoubleArgumentType.doubleArg())
                                                                                                .then(Commands.argument("z2", DoubleArgumentType.doubleArg())
                                                                                                        .executes(ctx -> {
                                                                                                            String name = StringArgumentType.getString(ctx, "name");
                                                                                                            String dimStr = StringArgumentType.getString(ctx, "dimension");
                                                                                                            ResourceLocation dim = ResourceLocation.tryParse(dimStr);
                                                                                                            if (dim == null) {
                                                                                                                ctx.getSource().sendFailure(Component.literal("无效的维度: " + dimStr));
                                                                                                                return 0;
                                                                                                            }
                                                                                                            double x1 = DoubleArgumentType.getDouble(ctx, "x1");
                                                                                                            double y1 = DoubleArgumentType.getDouble(ctx, "y1");
                                                                                                            double z1 = DoubleArgumentType.getDouble(ctx, "z1");
                                                                                                            double x2 = DoubleArgumentType.getDouble(ctx, "x2");
                                                                                                            double y2 = DoubleArgumentType.getDouble(ctx, "y2");
                                                                                                            double z2 = DoubleArgumentType.getDouble(ctx, "z2");
                                                                                                            addRegion(name, dim, x1, y1, z1, x2, y2, z2);
                                                                                                            ctx.getSource().sendSuccess(() -> Component.literal("区域已添加: " + name + " (" + dim + ")"), false);
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
                                )
                                .then(Commands.literal("add_here")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("y2", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("z2", DoubleArgumentType.doubleArg())
                                                                        .executes(ctx -> {
                                                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                                            String name = StringArgumentType.getString(ctx, "name");
                                                                            ResourceLocation dim = player.level().dimension().location();
                                                                            Vec3 pos = player.position();
                                                                            double x2 = DoubleArgumentType.getDouble(ctx, "x2");
                                                                            double y2 = DoubleArgumentType.getDouble(ctx, "y2");
                                                                            double z2 = DoubleArgumentType.getDouble(ctx, "z2");
                                                                            addRegion(name, dim, pos.x, pos.y, pos.z, x2, y2, z2);
                                                                            ctx.getSource().sendSuccess(() -> Component.literal("区域已添加: " + name + " (" + dim + ")"), false);
                                                                            return 1;
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .then(Commands.argument("dimension", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            String dimStr = StringArgumentType.getString(ctx, "dimension");
                                                            ResourceLocation dim = ResourceLocation.tryParse(dimStr);
                                                            boolean removed = removeRegion(name, dim);
                                                            if (removed) {
                                                                ctx.getSource().sendSuccess(() -> Component.literal("已移除区域: " + name + " (" + dim + ")"), false);
                                                            } else {
                                                                ctx.getSource().sendFailure(Component.literal("未找到区域: " + name + " (" + dim + ")"));
                                                            }
                                                            return 1;
                                                        })
                                                )
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
                                                    sb.append(region.name).append(" (").append(region.dimension).append("): [")
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

    public static boolean removeRegion(String name, ResourceLocation dimension) {
        boolean removed = regions.removeIf(r -> r.name.equals(name) && r.dimension.equals(dimension));
        if (removed) save();
        return removed;
    }
    public static boolean isPlayerInAnyRegion(ServerPlayer player) {
        return regions.stream().anyMatch(region -> region.dimension.equals(player.level().dimension().location()) && region.isInside(player.position()));
    }
}