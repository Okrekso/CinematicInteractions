"""Regenerates the effect textures in src/main/resources/assets/cinematic/textures/fx.

    python tools/gen_fx_textures.py

magic_circle.png  rune circle drawn on the enchanting table and behind the offer sigils
glow.png          soft round glow (sigil halo, sparks)
beam.png          vertical shaft of light (enchanting rite, the beacon's flare)
ring.png          soft ring of light (the beacon's shock wave over the pyramid, the payment spot)
ray.png           streak of light, bright at its root (rays bursting out of a lit beacon)

They are drawn as added light (RenderType.eyes), so black means "no light". Alpha is set equal to the
brightness as well, so shader packs that alpha-blend instead of adding still show no dark background.
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'cinematic', 'textures', 'fx')


def light(lum):
    return Image.merge('RGBA', [lum, lum, lum, lum])


def magic_circle():
    random.seed(1337)
    size = 1024  # supersampled, saved at 256
    img = Image.new('L', (size, size), 0)
    d = ImageDraw.Draw(img)
    c = size / 2

    def ring(r, w):
        d.ellipse([c - r, c - r, c + r, c + r], outline=255, width=w)

    ring(488, 14)
    ring(452, 6)
    ring(372, 8)
    ring(352, 4)
    ring(190, 6)
    ring(60, 6)
    # rune band between the rings: 28 glyphs of short strokes
    n = 28
    for i in range(n):
        a = i / n * 2 * math.pi
        cx, cy = c + math.cos(a) * 412, c + math.sin(a) * 412
        t = (-math.sin(a), math.cos(a))
        nr = (math.cos(a), math.sin(a))

        def p(x, y):
            return cx + t[0] * x + nr[0] * y, cy + t[1] * x + nr[1] * y

        pts = [(-14, -22), (0, -22), (14, -22), (-14, 0), (0, 0), (14, 0), (-14, 22), (0, 22), (14, 22)]
        for _ in range(random.randint(2, 4)):
            a1, b1 = random.sample(pts, 2)
            d.line([p(*a1), p(*b1)], fill=255, width=7)
        if random.random() < 0.5:
            px, py = p(*random.choice(pts))
            d.ellipse([px - 6, py - 6, px + 6, py + 6], fill=255)
    for i in range(56):
        a = (i + 0.5) / 56 * 2 * math.pi
        px, py = c + math.cos(a) * 470, c + math.sin(a) * 470
        d.ellipse([px - 4, py - 4, px + 4, py + 4], fill=255)
    # hexagram
    for k in range(2):
        off = 0 if k == 0 else math.pi
        tri = [(c + math.cos(-math.pi / 2 + off + j * 2 * math.pi / 3) * 340,
                c + math.sin(-math.pi / 2 + off + j * 2 * math.pi / 3) * 340) for j in range(3)]
        d.line(tri + [tri[0]], fill=255, width=7)
    for j in range(6):
        a = -math.pi / 2 + j * math.pi / 3
        px, py = c + math.cos(a) * 340, c + math.sin(a) * 340
        d.ellipse([px - 22, py - 22, px + 22, py + 22], outline=255, width=6)
        d.line([(c + math.cos(a) * 60, c + math.sin(a) * 60), (c + math.cos(a) * 190, c + math.sin(a) * 190)], fill=255, width=5)
    for i in range(12):
        a = i / 12 * 2 * math.pi + 0.26
        px, py = c + math.cos(a) * 128, c + math.sin(a) * 128
        d.line([(px - 10, py - 14), (px + 10, py + 14)], fill=255, width=6)
        d.line([(px + 10, py - 14), (px - 2, py + 2)], fill=255, width=6)
    small = img.resize((256, 256), Image.LANCZOS)
    glow = small.filter(ImageFilter.GaussianBlur(3))
    out = Image.blend(glow, small, 0.65)
    out = Image.eval(out, lambda v: min(255, int(v * 1.25)))
    return light(out)


def soft_glow():
    g = Image.new('L', (64, 64))
    for y in range(64):
        for x in range(64):
            r = math.hypot(x - 31.5, y - 31.5) / 32
            g.putpixel((x, y), int(255 * max(0.0, 1 - r) ** 2.2))
    return light(g)


def beam():
    b = Image.new('L', (32, 128))
    for y in range(128):
        for x in range(32):
            across = max(0.0, 1 - abs(x - 15.5) / 16) ** 2.5
            along = (y / 127) ** 0.8  # brightest at the foot (bottom of the texture)
            b.putpixel((x, y), int(255 * across * along))
    return light(b)


def ring():
    n = 128
    g = Image.new('L', (n, n))
    for y in range(n):
        for x in range(n):
            r = math.hypot((x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1)
            # a soft ring with a brighter inner edge and a faint glow inside it
            v = math.exp(-((r - 0.86) / 0.06) ** 2) + 0.25 * math.exp(-((r - 0.8) / 0.16) ** 2)
            g.putpixel((x, y), int(round(255 * min(1.0, v))) if r < 1 else 0)
    return light(g)


def ray():
    w, h = 128, 32
    g = Image.new('L', (w, h))
    for y in range(h):
        for x in range(w):
            u, v = (x + 0.5) / w, (y + 0.5) / h * 2 - 1
            # bright at the root, fading along its length and narrowing towards the tip
            width = 0.55 * (1 - 0.7 * u) + 0.05
            g.putpixel((x, y), int(round(255 * min(1.0, (1 - u) ** 1.6 * math.exp(-(v / width) ** 2 * 2.2)))))
    return light(g)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, image in (('magic_circle.png', magic_circle()), ('glow.png', soft_glow()), ('beam.png', beam()), ('ring.png', ring()),
                        ('ray.png', ray())):
        image.save(os.path.join(OUT, name))
        print('wrote', name)
