from pathlib import Path

from PIL import Image
from docx import Document


SPEC = Path("/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx")
HOME = Path("/Users/brian/Documents/ChatGPT/Coxswain/work/screen-atlas/01_home.png")
BOARD = Path("/Users/brian/Documents/ChatGPT/Coxswain/work/screen-inventory-overview.png")


def replace(shape, image_path: Path, preserve_width: bool = True) -> None:
    blip = shape._inline.graphic.graphicData.pic.blipFill.blip
    doc.part.related_parts[blip.embed]._blob = image_path.read_bytes()
    if preserve_width:
        pixel_width, pixel_height = Image.open(image_path).size
        shape.height = int(shape.width * pixel_height / pixel_width)


doc = Document(SPEC)
shapes = list(doc.inline_shapes)
if len(shapes) != 33:
    raise RuntimeError(f"Expected 33 inline images, found {len(shapes)}")

# Cover, Home state in the atlas, and the final all-screen inventory.
replace(shapes[0], HOME)
replace(shapes[3], HOME)
replace(shapes[-1], BOARD)

doc.save(SPEC)
print("updated the cover, Home atlas state, and complete screen inventory")
