package com.example.invindicator;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Desenha um painel 9x5 com o inventario de cada jogador proximo,
 * sempre virado para a camera e ao lado (direita) do jogador.
 *
 * Linha 0: capacete, peitoral, calca, bota, mao secundaria
 * Linhas 1-3: inventario principal
 * Linha 4: hotbar
 */
public final class InvRenderer {
    // ---- ajustes visuais ----
    private static final double MAX_DISTANCE = 20.0;  // blocos
    private static final float SPACING = 0.14f;       // distancia entre slots
    private static final float ITEM_SCALE = 0.12f;    // tamanho do item
    private static final float SIDE_OFFSET = 1.15f;   // distancia lateral ate o centro do painel
    private static final float HEIGHT_OFFSET = 0.9f;  // altura relativa aos pes do jogador
    private static final int FULL_BRIGHT = 0xF000F0;

    private InvRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!InvIndicatorClient.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;

        VertexConsumerProvider vcp = ctx.consumers();
        if (vcp == null) return;

        Camera cam = ctx.camera();
        Vec3d camPos = cam.getPos();
        MatrixStack m = ctx.matrixStack();
        float td = ctx.tickDelta();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator()) continue;
            ItemStack[] data = InvData.get(p.getUuid());
            if (data == null) continue;
            if (p.getPos().squaredDistanceTo(camPos) > MAX_DISTANCE * MAX_DISTANCE) continue;

            Vec3d pos = p.getLerpedPos(td);

            m.push();
            m.translate(pos.x - camPos.x, pos.y - camPos.y + HEIGHT_OFFSET, pos.z - camPos.z);
            // billboard: eixos alinhados a tela (x direita, y cima, z para o espectador)
            m.multiply(cam.getRotation());
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            m.translate(SIDE_OFFSET, 0, 0);

            drawBackground(m, vcp);
            drawSlots(mc, m, vcp, data);

            m.pop();
        }
    }

    private static void drawBackground(MatrixStack m, VertexConsumerProvider vcp) {
        float halfW = 4.5f * SPACING + 0.02f;
        float halfH = 2.5f * SPACING + 0.02f;
        Matrix4f mat = m.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getTextBackground());
        float z = -0.02f;
        vc.vertex(mat, -halfW, -halfH, z).color(0f, 0f, 0f, 0.45f).light(FULL_BRIGHT).next();
        vc.vertex(mat,  halfW, -halfH, z).color(0f, 0f, 0f, 0.45f).light(FULL_BRIGHT).next();
        vc.vertex(mat,  halfW,  halfH, z).color(0f, 0f, 0f, 0.45f).light(FULL_BRIGHT).next();
        vc.vertex(mat, -halfW,  halfH, z).color(0f, 0f, 0f, 0.45f).light(FULL_BRIGHT).next();
    }

    private static void drawSlots(MinecraftClient mc, MatrixStack m, VertexConsumerProvider vcp, ItemStack[] data) {
        // linha 0: armadura (head=39 ... feet=36) + offhand (40)
        int[] top = {39, 38, 37, 36, 40};
        for (int c = 0; c < top.length; c++) drawStack(mc, m, vcp, data[top[c]], c, 0);
        // linhas 1-3: inventario principal (9..35)
        for (int i = 9; i < 36; i++) drawStack(mc, m, vcp, data[i], (i - 9) % 9, 1 + (i - 9) / 9);
        // linha 4: hotbar (0..8)
        for (int i = 0; i < 9; i++) drawStack(mc, m, vcp, data[i], i, 4);
    }

    private static void drawStack(MinecraftClient mc, MatrixStack m, VertexConsumerProvider vcp,
                                  ItemStack stack, int col, int row) {
        if (stack == null || stack.isEmpty()) return;

        float x = (col - 4) * SPACING;
        float y = (2 - row) * SPACING;

        m.push();
        m.translate(x, y, 0);

        m.push();
        m.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, FULL_BRIGHT,
                OverlayTexture.DEFAULT_UV, m, vcp, mc.world, 0);
        m.pop();

        if (stack.getCount() > 1) {
            String text = String.valueOf(stack.getCount());
            TextRenderer tr = mc.textRenderer;
            float ts = 0.005f;
            m.push();
            m.translate(SPACING * 0.45f, -SPACING * 0.45f, 0.06f);
            m.scale(ts, -ts, ts);
            tr.draw(text, -tr.getWidth(text), -8, 0xFFFFFF, true, m.peek().getPositionMatrix(),
                    vcp, TextRenderer.TextLayerType.NORMAL, 0, FULL_BRIGHT);
            m.pop();
        }

        m.pop();
    }
}
