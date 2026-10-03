#!/usr/bin/env python3
"""Bereitet Roborazzi-Screenshots zum Ansehen auf.

Lange Screens werden mit großer Höhe gerendert (siehe ScreenshotTest). Dieses Skript
1. kürzt Streifen reinen Hintergrunds über 300 px auf 40 px
   (Abstände im Layout bleiben erhalten, nur der leere Rest unter kurzen Screens fällt weg),
2. schneidet das Ergebnis in Abschnitte von höchstens --max-h Pixeln.

Aufruf: scripts/screenshot-sheet.py <eingabe.png> <ausgabe-präfix> [--max-h 1600]
"""
import argparse
from PIL import Image


def collapse_empty(im: Image.Image, keep: int = 40, min_run: int = 300) -> Image.Image:
    w, h = im.size
    px = im.load()
    bg = px[w - 1, h - 1]
    empty = [all(px[x, y] == bg for x in range(0, w, 4)) for y in range(h)]
    rows, y = [], 0
    while y < h:
        if empty[y]:
            run = y
            while run < h and empty[run]:
                run += 1
            n = run - y
            rows.extend(range(y, y + (keep if n > min_run else n)))
            y = run
        else:
            rows.append(y)
            y += 1
    out = Image.new(im.mode, (w, len(rows)), bg)
    for i, src in enumerate(rows):
        out.paste(im.crop((0, src, w, src + 1)), (0, i))
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("src")
    ap.add_argument("prefix")
    ap.add_argument("--max-h", type=int, default=1600)
    a = ap.parse_args()
    im = collapse_empty(Image.open(a.src).convert("RGB"))
    w, h = im.size
    for i, top in enumerate(range(0, h, a.max_h)):
        im.crop((0, top, w, min(h, top + a.max_h))).save(f"{a.prefix}_{i + 1}.png")
        print(f"{a.prefix}_{i + 1}.png")


if __name__ == "__main__":
    main()
