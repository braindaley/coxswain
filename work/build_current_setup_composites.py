from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path("/Users/brian/Documents/ChatGPT/Coxswain/work/screen-atlas")


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
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


def create_board(output: str, title: str, subtitle: str, screens: list[tuple[str, str]]) -> None:
    phone_width = 250
    phone_height = 500
    gap = 28
    margin = 42
    header_height = 126
    label_height = 42
    width = margin * 2 + phone_width * len(screens) + gap * (len(screens) - 1)
    height = header_height + label_height + phone_height + 38

    canvas = Image.new("RGB", (width, height), "#F5F7FA")
    draw = ImageDraw.Draw(canvas)
    draw.text((margin, 30), title, fill="#101828", font=font(30, True))
    draw.text((margin, 72), subtitle, fill="#53647C", font=font(17))

    for index, (label, filename) in enumerate(screens):
        x = margin + index * (phone_width + gap)
        draw.rounded_rectangle(
            (x - 7, header_height - 2, x + phone_width + 7, height - 25),
            radius=18,
            fill="#FFFFFF",
            outline="#D6DEE9",
            width=2,
        )
        label_box = draw.textbbox((0, 0), label, font=font(18, True))
        label_width = label_box[2] - label_box[0]
        draw.text(
            (x + (phone_width - label_width) / 2, header_height + 7),
            label,
            fill="#1F3B5B",
            font=font(18, True),
        )
        source = Image.open(ROOT / filename).convert("RGB")
        source.thumbnail((phone_width, phone_height), Image.Resampling.LANCZOS)
        y = header_height + label_height
        canvas.paste(source, (x, y))

    canvas.save(ROOT / output, quality=95)


create_board(
    "20_duration_goal_states_current.png",
    "Duration Workout — Current Goal States",
    "One shared setup shell; the selected goal reveals the matching target control.",
    [
        ("None", "02_quick_duration_none.png"),
        ("Stroke Rate", "03_quick_duration_stroke_rate.png"),
        ("Speed", "04_quick_duration_speed.png"),
        ("Power", "05_quick_duration_power.png"),
    ],
)

create_board(
    "21_distance_states_current.png",
    "Distance Workout — Current States",
    "Rowing presets, shared setup controls, and Race Your Best when a result exists.",
    [
        ("Standard", "06_quick_distance.png"),
        ("Race available", "07_distance_race_available.png"),
        ("Race enabled", "08_distance_race_enabled.png"),
    ],
)

print("built current Duration and Distance composite images")
