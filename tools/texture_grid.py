"""Prints a 16x16 block texture as a grid of brightness numbers, to find its openings, grates and ledges.

    python tools/texture_grid.py furnace_front smoker_front_on
    python tools/texture_grid.py path/to/any.png

Names without a path are looked up in refs/assets/minecraft/textures/block (tools/extract_refs.sh).
Dark runs (< ~40) are holes, bright ones ledges. Pixel column c spans u = c/16 .. (c+1)/16 from the
viewer's left; row r spans v = 1 - (r+1)/16 .. 1 - r/16 from the bottom (see furnace/FurnaceLayout.java).
Animated textures (taller than wide) print their first frame.
"""
import os
import sys

from PIL import Image

BLOCKS = os.path.join(os.path.dirname(__file__), '..', 'refs', 'assets', 'minecraft', 'textures', 'block')

for arg in sys.argv[1:]:
    path = arg if arg.lower().endswith('.png') else os.path.join(BLOCKS, arg + '.png')
    image = Image.open(path).convert('RGBA')
    width = image.size[0]
    print('%s  %dx%d' % (os.path.basename(path), image.size[0], image.size[1]))
    print('     ' + ' '.join('%3d' % c for c in range(width)))
    for row in range(min(width, image.size[1])):
        values = []
        for col in range(width):
            r, g, b, a = image.getpixel((col, row))
            values.append('  .' if a == 0 else '%3d' % ((r + g + b) // 3))
        print('%3d  %s' % (row, ' '.join(values)))
    print()
