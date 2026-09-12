from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path("/Users/brian/Documents/ChatGPT/Coxswain")
SOURCE = ROOT / "work/spec-images/image1.png"
OUTPUT = ROOT / "work/screen-atlas/01_home.png"


def font(size: int, bold: bool = False):
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
    ]
    for candidate in candidates:
        try:
            return ImageFont.truetype(candidate, size)
        except OSError:
            pass
    return ImageFont.load_default()


def centered(draw: ImageDraw.ImageDraw, box, text: str, text_font, fill: str) -> None:
    left, top, right, bottom = box
    bounds = draw.textbbox((0, 0), text, font=text_font)
    width = bounds[2] - bounds[0]
    height = bounds[3] - bounds[1]
    draw.text(
        (left + (right - left - width) / 2, top + (bottom - top - height) / 2 - bounds[1]),
        text,
        font=text_font,
        fill=fill,
    )


image = Image.open(SOURCE).convert("RGB")
draw = ImageDraw.Draw(image)

# Preserve the approved Home layout and spacing. Only update the two labels whose
# product terminology changed from Workouts to Programs.
quick_label = (650, 1400, 835, 1472)
draw.rectangle(quick_label, fill=(3, 28, 43))
centered(draw, quick_label, "Programs", font(26), "#FFFFFF")

nav_label = (250, 1560, 410, 1626)
draw.rectangle(nav_label, fill=(248, 249, 250))
centered(draw, nav_label, "Programs", font(23), "#536985")

image.save(OUTPUT, optimize=True)
print(f"restored approved Home composition at {OUTPUT}")
