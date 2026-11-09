package com.example.invindicator;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cache no cliente: inventario dos outros jogadores recebido do servidor. */
public final class InvData {
    private static final Map<UUID, ItemStack[]> DATA = new HashMap<>();

    private InvData() {}

    public static void put(UUID id, ItemStack[] stacks) { DATA.put(id, stacks); }
    public static ItemStack[] get(UUID id) { return DATA.get(id); }
    public static void clear() { DATA.clear(); }
}
