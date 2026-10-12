package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Delegates the complete vertex format, allowing a single draw to change alpha without changing the model. */
public class VertexConsumerWrapper implements VertexConsumer {
    protected final VertexConsumer parent;
    public VertexConsumerWrapper(VertexConsumer parent) { this.parent = parent; }
    @Override public VertexConsumer addVertex(float x, float y, float z) { parent.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { parent.setColor(r, g, b, a); return this; }
    @Override public VertexConsumer setUv(float u, float v) { parent.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { parent.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { parent.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { parent.setNormal(x, y, z); return this; }
}
