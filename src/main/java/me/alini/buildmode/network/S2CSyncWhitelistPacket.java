
// 同步白名单
package me.alini.buildmode.network;

import me.alini.buildmode.client.BuildModeScreen;
import me.alini.buildmode.client.ClientWhitelist;
import me.alini.buildmode.network.ModMessages;
import me.alini.buildmode.whitelist.WhitelistManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class S2CSyncWhitelistPacket {
    private static final int MAX_PER_PACKET = 256; // 每包最大物品数

    private final Set<ResourceLocation> whitelist;
    private final int totalParts;
    private final int partIndex;
    private final UUID syncId;

    public S2CSyncWhitelistPacket(Set<ResourceLocation> whitelist, int totalParts, int partIndex, UUID syncId) {
        this.whitelist = whitelist;
        this.totalParts = totalParts;
        this.partIndex = partIndex;
        this.syncId = syncId;
    }

    public S2CSyncWhitelistPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        whitelist = new HashSet<>();
        for (int i = 0; i < size; i++) {
            whitelist.add(buf.readResourceLocation());
        }
        totalParts = buf.readInt();
        partIndex = buf.readInt();
        syncId = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(whitelist.size());
        for (ResourceLocation id : whitelist) {
            buf.writeResourceLocation(id);
        }
        buf.writeInt(totalParts);
        buf.writeInt(partIndex);
        buf.writeUUID(syncId);
    }

    // 线程安全的分包缓存
    private static final Map<UUID, List<Set<ResourceLocation>>> partCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> expectedParts = new ConcurrentHashMap<>();

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            partCache.computeIfAbsent(syncId, k -> {
                List<Set<ResourceLocation>> list = new ArrayList<>(Collections.nCopies(totalParts, null));
                expectedParts.put(syncId, totalParts);
                return list;
            });
            List<Set<ResourceLocation>> parts = partCache.get(syncId);
            parts.set(partIndex, whitelist);

            // 检查是否全部收到
            if (parts.stream().allMatch(Objects::nonNull)) {
                Set<ResourceLocation> all = new HashSet<>();
                for (Set<ResourceLocation> part : parts) all.addAll(part);
                ClientWhitelist.updateWhitelist(all);
                if (Minecraft.getInstance().screen instanceof BuildModeScreen screen) {
                    screen.updateFilteredItems();
                }
                partCache.remove(syncId);
                expectedParts.remove(syncId);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // 服务端分批广播
    public static void syncToAll() {
        List<ResourceLocation> all = new ArrayList<>(WhitelistManager.getWhitelist());
        int total = (all.size() + MAX_PER_PACKET - 1) / MAX_PER_PACKET;
        UUID syncId = UUID.randomUUID();
        for (int i = 0; i < total; i++) {
            int from = i * MAX_PER_PACKET;
            int to = Math.min(from + MAX_PER_PACKET, all.size());
            Set<ResourceLocation> part = new HashSet<>(all.subList(from, to));
            ModMessages.getChannel().send(
                    PacketDistributor.ALL.noArg(),
                    new S2CSyncWhitelistPacket(part, total, i, syncId)
            );
        }
    }
}