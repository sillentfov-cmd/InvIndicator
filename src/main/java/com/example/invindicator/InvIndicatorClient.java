package com.example.invindicator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class InvIndicatorClient implements ClientModInitializer {
    public static boolean enabled = true;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.invindicator.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K,
                "key.categories.invindicator"));

        ClientPlayNetworking.registerGlobalReceiver(InvIndicator.PACKET, (client, handler, buf, sender) -> {
            UUID id = buf.readUuid();
            ItemStack[] stacks = new ItemStack[InvIndicator.SLOTS];
            for (int i = 0; i < stacks.length; i++) stacks[i] = buf.readItemStack();
            client.execute(() -> InvData.put(id, stacks));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> InvData.clear());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Inventory Indicator: " + (enabled ? "ON" : "OFF")), true);
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(InvRenderer::render);
    }
}
