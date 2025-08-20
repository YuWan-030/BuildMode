package me.alini.buildmode.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "1.0";
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("buildmode", "messages"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        // 注册所有网络包
        INSTANCE.registerMessage(id(), C2SRequestItemPacket.class, C2SRequestItemPacket::toBytes, C2SRequestItemPacket::new, C2SRequestItemPacket::handle);
        INSTANCE.registerMessage(id(), C2SWhitelistEditPacket.class, C2SWhitelistEditPacket::toBytes, C2SWhitelistEditPacket::new, C2SWhitelistEditPacket::handle);
        INSTANCE.registerMessage(id(), S2CSyncWhitelistPacket.class, S2CSyncWhitelistPacket::toBytes, S2CSyncWhitelistPacket::new, S2CSyncWhitelistPacket::handle);
        INSTANCE.registerMessage(id(), C2SRequestToggleBuildModePacket.class, C2SRequestToggleBuildModePacket::toBytes, C2SRequestToggleBuildModePacket::fromBytes, C2SRequestToggleBuildModePacket::handle);
        INSTANCE.registerMessage(id(), S2CUpdateBuildModePacket.class, S2CUpdateBuildModePacket::toBytes, S2CUpdateBuildModePacket::fromBytes, S2CUpdateBuildModePacket::handle);

        // 注册 BuildModePacket
        INSTANCE.registerMessage(id(), BuildModePacket.class, BuildModePacket::encode, BuildModePacket::decode, BuildModePacket::handle);
    }

    public static SimpleChannel getChannel() {
        return INSTANCE;
    }

    public static void sendToClient(BuildModePacket msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}