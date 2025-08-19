// Packet 通道注册

package me.alini.buildmode.network;


import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("buildmode", "messages"),
                () -> "1.0",
                s -> true,
                s -> true
        );

        // 注册包
        INSTANCE.registerMessage(id(), C2SRequestItemPacket.class, C2SRequestItemPacket::toBytes, C2SRequestItemPacket::new, C2SRequestItemPacket::handle);
        INSTANCE.registerMessage(id(), C2SWhitelistEditPacket.class, C2SWhitelistEditPacket::toBytes, C2SWhitelistEditPacket::new, C2SWhitelistEditPacket::handle);
        INSTANCE.registerMessage(id(), S2CSyncWhitelistPacket.class, S2CSyncWhitelistPacket::toBytes, S2CSyncWhitelistPacket::new, S2CSyncWhitelistPacket::handle);
    }

    public static SimpleChannel getChannel() {
        return INSTANCE;
    }
}