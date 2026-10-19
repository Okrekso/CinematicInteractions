"""Fail if a lectern Showcase spread renders as the black-page regression.

Pass one or more showcase_k7_spread1.png images captured at the default
Showcase camera. This checks actual pixels, unlike the in-game action checks.
"""

import sys
from pathlib import Path

from PIL import Image


def bright_fraction(image: Image.Image, left: float, right: float) -> float:
    width, height = image.size
    page = image.crop((int(width * left), int(height * 0.30),
                       int(width * right), int(height * 0.60))).convert("RGB")
    pixels = list(page.get_flattened_data())
    return sum(sum(pixel) / 3 > 80 for pixel in pixels) / len(pixels)


def main() -> int:
    if len(sys.argv) < 2:
        print("Usage: python tools/check_book_page_frames.py <showcase_k7_spread1.png> [...]", file=sys.stderr)
        return 2
    failed = False
    for argument in sys.argv[1:]:
        path = Path(argument)
        with Image.open(path) as image:
            left = bright_fraction(image, 0.30, 0.40)
            right = bright_fraction(image, 0.60, 0.70)
        passed = left > 0.55 and right > 0.55
        print(f"{'PASS' if passed else 'FAIL'} {path}: bright page pixels left={left:.0%}, right={right:.0%}")
        failed |= not passed
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
