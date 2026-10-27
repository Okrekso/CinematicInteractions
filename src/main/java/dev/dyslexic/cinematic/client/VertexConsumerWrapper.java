//? if fabric && >=1.21 {
/*package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/^* Delegates the complete vertex format, allowing a single draw to change alpha without changing the model. ^/
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
*///?} else if fabric && <1.21 {
/*package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/^* Delegates the complete vertex format, allowing a single draw to change alpha without changing the model. ^/
public class VertexConsumerWrapper implements VertexConsumer {
    protected final VertexConsumer parent;
    public VertexConsumerWrapper(VertexConsumer parent) { this.parent = parent; }
    @Override public VertexConsumer vertex(double x, double y, double z) { parent.addVertex(x, y, z); return this; }
    @Override public VertexConsumer color(int r, int g, int b, int a) { parent.color(r, g, b, a); return this; }
    @Override public VertexConsumer uv(float u, float v) { parent.setUv(u, v); return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { parent.overlayCoords(u, v); return this; }
    @Override public VertexConsumer uv2(int u, int v) { parent.uv2(u, v); return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { parent.normal(x, y, z); return this; }
    @Override public void endVertex() { parent.endVertex(); }
    @Override public void defaultColor(int r, int g, int b, int a) { parent.defaultColor(r, g, b, a); }
    @Override public void unsetDefaultColor() { parent.unsetDefaultColor(); }
}
*///?}
