#!/usr/bin/env python3
"""
Met en page les captures Roborazzi de la fiche Play Store : une accroche en haut, la capture en dessous, au format
1080 x 1920 (9:16, accepté par Google : côtés entre 320 et 3 840 px, le plus grand au plus deux fois le plus petit).

    ./gradlew -Proborazzi.test.record=true \
      :feature:game:testDebugUnitTest --tests '*StoreScreenshotTest' \
      :feature:stats:testDebugUnitTest --tests '*StoreScreenshotTest' \
      :feature:share:testDebugUnitTest --tests '*StoreScreenshotTest' \
      :feature:nearby:testDebugUnitTest --tests '*StoreScreenshotTest'
    python3 -I tools/store/compose_screenshots.py

Les images sont écrites dans docs/store/screenshots/<langue>/, numérotées dans l'ordre de la fiche.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
FONTS = ROOT / "core/designsystem/src/main/res/font"
OUT = ROOT / "docs/store/screenshots"

W, H = 1080, 1920
FOREST = (0x1F, 0x4D, 0x3A)
SAFFRON = (0xF2, 0xA9, 0x3B)
BIRCH = (0xF4, 0xEB, 0xDD)
DARK = (0x14, 0x20, 0x1A)
MARGIN_TOP = 96
CAPTION_GAP = 48
SHOT_BOTTOM = 72
RADIUS = 44

# (fichier source, accroche en deux lignes : la première en safran, la seconde en blanc)
SHOTS = {
    "en": [
        ("feature/game/src/test/screenshots/store/game.png", "Tap the pins.", "The app keeps score."),
        ("feature/nearby/src/test/screenshots/store/invite.png", "Several phones,", "no account, no internet."),
        ("feature/game/src/test/screenshots/store/scoreboard.png", "Live scoreboard,", "on every phone."),
        ("feature/game/src/test/screenshots/store/sunlight.png", "Sunlight mode,", "readable in full sun."),
        ("feature/game/src/test/screenshots/store/end.png", "Every win counts.", "Rematch in one tap."),
        ("feature/stats/src/test/screenshots/store/hall_of_fame.png", "Hall of Fame,", "records and badges."),
        ("feature/share/src/test/screenshots/store/share_card.png", "Share the moment.", "No ads. No trackers."),
        ("feature/game/src/test/screenshots/store/dark.png", "Dark theme,", "for summer evenings."),
    ],
}


def rounded(image: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, *image.size), radius, fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def compose(source: Path, line1: str, line2: str, background: tuple[int, int, int]) -> Image.Image:
    canvas = Image.new("RGB", (W, H), background)
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.truetype(str(FONTS / "bricolage_grotesque_extrabold.ttf"), 76)
    y = MARGIN_TOP
    for text, color in ((line1, SAFFRON), (line2, BIRCH)):
        width = draw.textlength(text, font=font)
        draw.text(((W - width) / 2, y), text, font=font, fill=color)
        y += 92
    top = y + CAPTION_GAP
    shot = Image.open(source).convert("RGB")
    height = H - top - SHOT_BOTTOM
    width = round(shot.width * height / shot.height)
    if width > W - 2 * 72:
        width = W - 2 * 72
        height = round(shot.height * width / shot.width)
    shot = rounded(shot.resize((width, height), Image.LANCZOS), RADIUS)
    canvas.paste(shot, ((W - width) // 2, top), shot)
    return canvas


def main() -> None:
    for lang, shots in SHOTS.items():
        folder = OUT / lang
        folder.mkdir(parents=True, exist_ok=True)
        for old in folder.glob("*.png"):
            old.unlink()
        for index, (source, line1, line2) in enumerate(shots, start=1):
            background = DARK if "dark" in source else FOREST
            image = compose(ROOT / source, line1, line2, background)
            name = f"{index:02d}-{Path(source).stem}.png"
            image.save(folder / name, optimize=True)
            print(folder / name)


if __name__ == "__main__":
    main()
