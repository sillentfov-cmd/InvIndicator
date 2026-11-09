package com.example.invindicator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Lado servidor: a cada 10 ticks manda o inventario completo (41 slots)
 * dos jogadores proximos para cada cliente que tenha o mod. So reenvia quando muda.
 */
public class InvIndicator implements ModInitializer {
    public static final String MOD_ID = "invindicator";
    public static final Identifier PACKET = new Identifier(MOD_ID, "inventory");
    public static final int SLOTS = 41;          // 36 principal + 4 armadura + 1 offhand
    public static final double RANGE = 24.0;     // blocos
    private static final int INTERVAL = 10;       // ticks

    // viewer -> (target -> ultimo hash enviado)
    private final Map<UUID, Map<UUID, Integer>> sent = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            sent.remove(id);
            sent.values().forEach(m -> m.remove(id));
        });
    }

    private void tick(MinecraftServer server) {
        if (server.getTicks() % INTERVAL != 0) return;
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.size() < 2) return;

        Map<UUID, Integer> hashes = new HashMap<>();
        for (ServerPlayerEntity p : players) hashes.put(p.getUuid(), hash(p));

        for (ServerPlayerEntity viewer : players) {
            if (!ServerPlayNetworking.canSend(viewer, PACKET)) continue; // cliente sem o mod
            Map<UUID, Integer> known = sent.computeIfAbsent(viewer.getUuid(), k -> new HashMap<>());
            Set<UUID> inRange = new HashSet<>();

            for (ServerPlayerEntity target : players) {
                if (target == viewer || target.getWorld() != viewer.getWorld()) continue;
                if (viewer.squaredDistanceTo(target) > RANGE * RANGE) continue;

                inRange.add(target.getUuid());
                int h = hashes.get(target.getUuid());
                Integer prev = known.get(target.getUuid());
                if (prev == null || prev != h) {
                    send(viewer, target);
                    known.put(target.getUuid(), h);
                }
            }
            known.keySet().retainAll(inRange); // saiu do alcance -> reenvia quando voltar
        }
    }

    private static void send(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(target.getUuid());
        for (int i = 0; i < SLOTS; i++) buf.writeItemStack(target.getInventory().getStack(i));
        ServerPlayNetworking.send(viewer, PACKET, buf);
    }

    private static int hash(ServerPlayerEntity p) {
        int h = 1;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = p.getInventory().getStack(i);
            h = 31 * h + (s.isEmpty() ? 0 : Item.getRawId(s.getItem()) * 131 + s.getCount());
            NbtCompound nbt = s.getNbt();
            if (nbt != null) h = 31 * h + nbt.hashCode();
        }
        return h;
    }
}
