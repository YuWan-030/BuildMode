package me.alini.buildmode.region;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import me.alini.buildmode.network.BuildModePacket;
import me.alini.buildmode.network.ModMessages;
import me.alini.buildmode.network.S2CSyncWhitelistPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import me.alini.buildmode.effect.ModEffects;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


@Mod.EventBusSubscriber
public class RegionManager {


    // 区域内给予 Buff
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // 每1200 tick（1分钟）检测一次
        if (player.tickCount % 1200 == 0) {
            if (RegionManager.isInsideRegion(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1210, 2, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1210, 3, true, false));
            }
        }
    }

    // 禁止刷怪
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent event) {
        if (!event.isCancelable()) return; // 不可取消就跳过
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Vec3 pos = event.getEntity().position();
        if (RegionManager.getRegions().stream().anyMatch(region ->
                region.dimension.equals(level.dimension().location()) &&
                        region.isInside(pos)
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (RegionManager.getRegions().stream().anyMatch(region ->
                region.dimension.equals(level.dimension().location()) &&
                        region.isInside(event.getEntity().position())
        )) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;

        tickCounter++;
        if (tickCounter < 5) return; // 每5 tick执行一次
        tickCounter = 0;

        for (RegionManager.Region region : RegionManager.getRegions()) {
            if (!region.dimension.equals(level.dimension().location())) continue;

            level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, regionAABB(region))
                    .forEach(mob -> {
                        if (mob instanceof net.minecraft.world.entity.monster.Monster) {
                            mob.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                            return;
                        }
                        ResourceLocation id = mob.getType().builtInRegistryHolder().key().location();
                        if (removeMobs.contains(id)) {
                            mob.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                        }
                    });
        }
    }


    private static net.minecraft.world.phys.AABB regionAABB(RegionManager.Region region) {
        return new net.minecraft.world.phys.AABB(region.x1, region.y1, region.z1, region.x2, region.y2, region.z2);
    }

    @SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player)) return;
        if (!RegionManager.isPlayerInAnyRegion((ServerPlayer) event.getEntity())) return;
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.Mob) {
            event.setCanceled(true);
        }
    }

    private static Set<ResourceLocation> removeMobs = new HashSet<>();

    public static void loadRemoveMobs(Path configDir) {
        File file = configDir.resolve("buildmode/region_mobs.json").toFile();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            List<String> list = gson.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            removeMobs.clear();
            if (list != null) {
                for (String id : list) {
                    removeMobs.add(new ResourceLocation(id));
                }
            }
        } catch (Exception e) {
            System.err.println("[RegionManager] 加载 region_mobs.json 失败: " + e.getMessage());
        }
    }

    private static final List<Region> regions = new CopyOnWriteArrayList<>();
    private static Path configFile;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
            .setPrettyPrinting().create();
    private static int tickCounter = 0;

    public static void init(Path configDir) {
        insidePlayers.clear();
        configFile = configDir.resolve("buildmode/regions.json");
        load();
        loadRemoveMobs(configDir);
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
                // 给玩家添加1分钟10秒的生命恢复2，伤害吸收3
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1210, 2, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1210, 3, true, false));
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

    private static boolean canUseBuildMode(CommandSourceStack source) {
        // 仅服务器玩家才判断
        if (source.isPlayer()) {
            var player = source.getPlayer();
            // LuckPerms 检查（同步，优先OP）
            if (me.alini.buildmode.util.LuckPermsApi.isLuckPermsPresent()) {
                // 这里只能用 OP 检查，因为 LuckPerms 的 API 是异步的
                return player.hasPermissions(2);
            } else {
                return player.hasPermissions(2);
            }
        }
        // 控制台允许
        return true;
    }
    /**
     * 注册建造模式相关命令
     * @param dispatcher 命令分发器
     */

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("buildmode")
                        .requires(RegionManager::canUseBuildMode) // 只有有权限的玩家/控制台可见
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
    // RegionManager.java
    public static void removeInsidePlayer(UUID uuid) {
        insidePlayers.remove(uuid);
    }

    private static int cleanTickCounter = 0;
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        cleanTickCounter++;
        if (cleanTickCounter < 20) return; // 每 20 tick 执行一次（约 1 秒）
        cleanTickCounter = 0;

        insidePlayers.removeIf(uuid ->
                ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid) == null
        );
    }
}