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

Les images sont écrites dans docs/store/screenshots/<langue>/ (fr, en), numérotées dans l'ordre de la fiche, avec
l'image de présentation (feature-graphic.png, 1024 x 500).
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

# (capture, accroche en deux lignes : la première en safran, la seconde en bouleau), dans l'ordre de la fiche.
# Les captures sont dans feature/<module>/src/test/screenshots/store/<nom>_<langue>.png.
SHOTS = [
    ("game", "game"),
    ("nearby", "invite"),
    ("game", "scoreboard"),
    ("game", "sunlight"),
    ("game", "end"),
    ("stats", "hall_of_fame"),
    ("share", "share_card"),
    ("game", "dark"),
]
CAPTIONS = {
    "fr": [
        ("Touchez les quilles,", "l'app compte les points."),
        ("Plusieurs téléphones,", "sans compte ni Internet."),
        ("Les scores en direct,", "sur chaque téléphone."),
        ("Mode Plein soleil,", "lisible en plein jour."),
        ("Chaque victoire compte.", "Revanche en un tap."),
        ("Hall of Fame,", "records et badges."),
        ("Partagez le moment.", "Aucune pub, aucun traceur."),
        ("Thème sombre,", "pour les soirées d'été."),
    ],
    "en": [
        ("Tap the pins.", "The app keeps score."),
        ("Several phones,", "no account, no internet."),
        ("Live scoreboard,", "on every phone."),
        ("Sunlight mode,", "readable in full sun."),
        ("Every win counts.", "Rematch in one tap."),
        ("Hall of Fame,", "records and badges."),
        ("Share the moment.", "No ads. No trackers."),
        ("Dark theme,", "for summer evenings."),
    ],
}
# Image de présentation (1024 x 500) : logotype, accroche, et le haut de l'écran de jeu.
TAGLINES = {
    "fr": ("Le compteur de Mölkky,", "à plusieurs téléphones."),
    "en": ("Mölkky scorekeeper,", "across several phones."),
}


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONTS / name), size)


def fitted(draw: ImageDraw.ImageDraw, lines: tuple[str, str], size: int, width: int) -> ImageFont.FreeTypeFont:
    """La police la plus grande (jusqu'à `size`) qui fait tenir chaque ligne dans `width`."""
    while size > 40:
        caption = font("bricolage_grotesque_extrabold.ttf", size)
        if all(draw.textlength(line, font=caption) <= width for line in lines):
            return caption
        size -= 2
    return caption


def rounded(image: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, *image.size), radius, fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def compose(source: Path, line1: str, line2: str, background: tuple[int, int, int]) -> Image.Image:
    canvas = Image.new("RGB", (W, H), background)
    draw = ImageDraw.Draw(canvas)
    caption = fitted(draw, (line1, line2), 76, W - 2 * 56)
    y = MARGIN_TOP
    for text, color in ((line1, SAFFRON), (line2, BIRCH)):
        width = draw.textlength(text, font=caption)
        draw.text(((W - width) / 2, y), text, font=caption, fill=color)
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


def feature_graphic(lang: str) -> Image.Image:
    fw, fh = 1024, 500
    canvas = Image.new("RGB", (fw, fh), BIRCH)
    draw = ImageDraw.Draw(canvas)
    logo = font("bricolage_grotesque_extrabold.ttf", 76)
    x, y = 56, 130
    draw.text((x, y), "Molkky", font=logo, fill=FOREST)
    draw.text((x + draw.textlength("Molkky ", font=logo), y), "Board", font=logo, fill=SAFFRON)
    tagline = font("inter_semibold.ttf", 34)
    for i, line in enumerate(TAGLINES[lang]):
        draw.text((x + 4, y + 124 + i * 48), line, font=tagline, fill=FOREST)
    # Le haut de l'écran de jeu (score et quilles), dans un cadre arrondi qui sort par le bas.
    shot = Image.open(ROOT / f"feature/game/src/test/screenshots/store/game_{lang}.png").convert("RGB")
    shot = shot.crop((0, 0, shot.width, round(shot.width * 1.25)))
    width = 330
    shot = rounded(shot.resize((width, round(shot.height * width / shot.width)), Image.LANCZOS), 36)
    frame = Image.new("RGBA", (width + 24, shot.height + 24), (*FOREST, 255))
    frame = rounded(frame.convert("RGB"), 48)
    canvas.paste(frame, (fw - width - 24 - 64, 56), frame)
    canvas.paste(shot, (fw - width - 12 - 64, 68), shot)
    return canvas


def main() -> None:
    for lang, captions in CAPTIONS.items():
        folder = OUT / lang
        folder.mkdir(parents=True, exist_ok=True)
        for old in folder.glob("*.png"):
            old.unlink()
        for index, ((module, name), (line1, line2)) in enumerate(zip(SHOTS, captions, strict=True), start=1):
            source = ROOT / f"feature/{module}/src/test/screenshots/store/{name}_{lang}.png"
            background = DARK if name == "dark" else FOREST
            target = folder / f"{index:02d}-{name}.png"
            compose(source, line1, line2, background).save(target, optimize=True)
            print(target)
        target = folder / "feature-graphic.png"
        feature_graphic(lang).save(target, optimize=True)
        print(target)


if __name__ == "__main__":
    main()
