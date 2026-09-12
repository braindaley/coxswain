from pathlib import Path

from docx import Document


SPEC = Path("/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx")
ATLAS = Path("/Users/brian/Documents/ChatGPT/Coxswain/work/screen-atlas")


def replace_shape_blob(doc: Document, shape_index: int, image_path: Path) -> None:
    shape = list(doc.inline_shapes)[shape_index]
    blip = shape._inline.graphic.graphicData.pic.blipFill.blip
    doc.part.related_parts[blip.embed]._blob = image_path.read_bytes()


doc = Document(SPEC)
if len(doc.inline_shapes) != 22:
    raise RuntimeError(f"Expected 22 inline images, found {len(doc.inline_shapes)}")

# Shape 0 is the cover. Shapes 1 and 2 are the two legacy setup composites.
replace_shape_blob(doc, 1, ATLAS / "20_duration_goal_states_current.png")
replace_shape_blob(doc, 2, ATLAS / "21_distance_states_current.png")

replacements = {
    "Figure 2. Duration Workout goal states: None, Stroke Rate, Speed, and Power. Selecting a goal reveals its target-value control and updates Workout Summary.":
        "Figure 2. Current Duration Workout goal states: None, Stroke Rate, Speed, and Power. All four use the shared Workout Setup shell; selecting a goal reveals its target control, updates Workout Summary, and preserves the Quick Row option to save the configuration as a program.",
    "Approved Distance Workout setup states. The same layout and interaction language as Duration Setup is preserved.":
        "Figure 3. Current Distance Workout states: standard setup, Race Your Best available, and Race Your Best enabled. Distance uses rowing-specific presets, the shared Workout Setup components, and the same Quick Row save-as-program behavior as Duration and Intervals.",
}

found = set()
for paragraph in doc.paragraphs:
    if paragraph.text in replacements:
        old = paragraph.text
        paragraph.text = replacements[old]
        found.add(old)

missing = set(replacements) - found
if missing:
    raise RuntimeError(f"Could not find expected captions: {sorted(missing)}")

doc.save(SPEC)
print("replaced legacy setup composites and updated captions")
