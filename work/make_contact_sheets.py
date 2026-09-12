from pathlib import Path
from PIL import Image, ImageOps, ImageDraw
import sys

source = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('/Users/brian/Documents/ChatGPT/Coxswain/work/spec-v1-4-render')
pages = sorted(source.glob('page-*.png'), key=lambda p: int(p.stem.split('-')[1]))
for group_index in range(0, len(pages), 8):
    subset = pages[group_index:group_index + 8]
    thumb_w = 340
    thumb_h = 440
    sheet = Image.new('RGB', (thumb_w * 2, thumb_h * 4), 'white')
    draw = ImageDraw.Draw(sheet)
    for offset, path in enumerate(subset):
        image = Image.open(path).convert('RGB')
        image.thumbnail((thumb_w - 16, thumb_h - 30))
        x = (offset % 2) * thumb_w + (thumb_w - image.width) // 2
        y = (offset // 2) * thumb_h + 24
        sheet.paste(image, (x, y))
        draw.text(((offset % 2) * thumb_w + 8, (offset // 2) * thumb_h + 6), path.stem, fill='black')
    sheet.save(source / f'contact-{group_index // 8 + 1}.png')
