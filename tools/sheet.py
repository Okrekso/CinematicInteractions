"""Contact sheets of showcase screenshots, so many frames can be looked at in one image.

    python tools/sheet.py <screenshots dir> --all <out dir>            every shot, 9 per sheet (sheet_1.png, ...)
    python tools/sheet.py <screenshots dir> <out.png> name1 name2 ...  chosen shots (names without showcase_ and .png)
    python tools/sheet.py <screenshots dir> <out.png> --story "name|caption" ...   captioned storyboard, 640x360 frames
"""
import glob
import os
import sys

from PIL import Image, ImageDraw, ImageFont


def font(size):
    try:
        return ImageFont.truetype('arial.ttf', size)
    except OSError:
        return ImageFont.load_default()


def sheet(paths, captions, out, w=533, h=300, label=18, cols=3, text_size=12):
    rows = (len(paths) + cols - 1) // cols
    canvas = Image.new('RGB', (cols * w + (cols + 1) * 8, rows * (h + label) + (rows + 1) * 8), (16, 16, 18))
    draw = ImageDraw.Draw(canvas)
    f = font(text_size)
    for i, (path, caption) in enumerate(zip(paths, captions)):
        x = 8 + (i % cols) * (w + 8)
        y = 8 + (i // cols) * (h + label + 8)
        if os.path.exists(path):
            canvas.paste(Image.open(path).convert('RGB').resize((w, h), Image.LANCZOS), (x, y + label))
        draw.text((x + 3, y + 1), caption, fill=(242, 234, 216), font=f)
    canvas.save(out)
    print('wrote', out)


def main():
    shots, target = sys.argv[1], sys.argv[2]
    rest = sys.argv[3:]
    if target == '--all':
        out_dir = rest[0]
        os.makedirs(out_dir, exist_ok=True)
        files = sorted(glob.glob(os.path.join(shots, 'showcase_*.png')))
        for i in range(0, len(files), 9):
            chunk = files[i:i + 9]
            names = [os.path.basename(p)[9:-4] for p in chunk]
            sheet(chunk, names, os.path.join(out_dir, 'sheet_%d.png' % (i // 9 + 1)))
        print(len(files), 'shots')
    elif rest and rest[0] == '--story':
        pairs = [item.split('|', 1) for item in rest[1:]]
        paths = [os.path.join(shots, 'showcase_%s.png' % name) for name, _ in pairs]
        sheet(paths, [caption for _, caption in pairs], target, w=640, h=360, label=30, text_size=20)
    else:
        paths = [os.path.join(shots, 'showcase_%s.png' % name) for name in rest]
        sheet(paths, rest, target)


if __name__ == '__main__':
    main()
