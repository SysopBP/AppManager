#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Package the approved AMOLED artwork into Android launcher assets."""
from pathlib import Path
from PIL import Image

repo = Path(__file__).resolve().parents[1]
res = repo / "app/src/main/res"
with Image.open(repo / "arts/sphere/amoled-master.png") as source:
    art = source.convert("RGB")
    if art.width != art.height or art.width < 512:
        raise SystemExit("Sphere master must be square and at least 512 pixels")

    def fitted(size, fraction):
        # Padding is packaging for Android masks; leave the approved art intact.
        canvas = Image.new("RGB", (size, size), "#000000")
        px = round(size * fraction)
        thumb = art.resize((px, px), Image.Resampling.LANCZOS)
        canvas.paste(thumb, ((size - px) // 2, (size - px) // 2))
        return canvas

    for density, size in {"mdpi":48, "hdpi":72, "xhdpi":96, "xxhdpi":144, "xxxhdpi":192}.items():
        directory = res / ("mipmap-" + density)
        directory.mkdir(parents=True, exist_ok=True)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            fitted(size, .88).save(directory / name)
    directory = res / "drawable-nodpi"
    directory.mkdir(parents=True, exist_ok=True)
    # Adaptive foreground is 108dp: keep the main artwork within its 72dp safe area.
    fitted(512, .66).save(directory / "appsphere_splash.png")
    fitted(512, .88).save(repo / "app/src/main/ic_launcher-playstore.png")
