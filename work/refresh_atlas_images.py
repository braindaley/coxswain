from pathlib import Path
import json
from docx import Document

SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
ATLAS = Path('/Users/brian/Documents/ChatGPT/Coxswain/work/screen-atlas')
doc = Document(SPEC)
items = json.loads((ATLAS / 'manifest.json').read_text(encoding='utf-8'))

def replace_shape(shape, image_path):
    blip = shape._inline.graphic.graphicData.pic.blipFill.blip
    doc.part.related_parts[blip.embed]._blob = Path(image_path).read_bytes()

if len(doc.inline_shapes) < len(items):
    raise RuntimeError('The document does not contain the expected atlas images.')

shapes = list(doc.inline_shapes)
# The atlas follows the cover and the two setup composites. The final shape is
# the full inventory board, so indexing from the end would shift every image.
atlas_shapes = shapes[3:3 + len(items)]
if len(atlas_shapes) != len(items):
    raise RuntimeError('The document does not contain the expected atlas image range.')
for shape, (_, filename, _) in zip(atlas_shapes, items):
    replace_shape(shape, ATLAS / filename)

replace_shape(shapes[0], ATLAS / '01_home.png')
doc.save(SPEC)
print(f'refreshed {len(items)} atlas images and the cover')
