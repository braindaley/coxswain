from pathlib import Path

from docx import Document
from docx.enum.section import WD_ORIENT
from docx.shared import Inches
from PIL import Image


SPEC = Path("/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx")
BOARD = Path("/Users/brian/Documents/ChatGPT/Coxswain/work/screen-inventory-overview.png")
doc = Document(SPEC)
if len(doc.sections) < 2:
    raise RuntimeError("Expected a separate inventory section")

section = doc.sections[-1]
section.orientation = WD_ORIENT.LANDSCAPE
section.page_width = Inches(11.69)
section.page_height = Inches(8.27)
section.top_margin = Inches(0.36)
section.bottom_margin = Inches(0.36)
section.left_margin = Inches(0.42)
section.right_margin = Inches(0.42)

# The final inline image is the complete inventory board.
shape = list(doc.inline_shapes)[-1]
blip = shape._inline.graphic.graphicData.pic.blipFill.blip
doc.part.related_parts[blip.embed]._blob = BOARD.read_bytes()
pixel_width, pixel_height = Image.open(BOARD).size
shape.width = Inches(8.1)
shape.height = int(shape.width * pixel_height / pixel_width)

doc.save(SPEC)
print("set the inventory section to explicit A4 landscape dimensions")
