from pathlib import Path

from docx import Document


SPEC = Path("/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx")
doc = Document(SPEC)

replacements = {
    "Home Quick Start uses Duration, Distance, My Programs, and Library. My Programs opens the saved-template list within Programs. Library opens the curated Workout Library tab. The persistent navigation uses Home, Programs, History, and More.":
        "Home Quick Start uses Duration, Distance, Intervals, and Programs. Programs opens the reusable-template area with My Programs selected; Workout Library remains its second tab. The persistent navigation uses Home, Programs, History, and More.",
    "Quick Start exposes Duration, Distance, My Programs, and Library.":
        "Quick Start exposes Duration, Distance, Intervals, and Programs.",
}

found = set()
for paragraph in doc.paragraphs:
    if paragraph.text in replacements:
        old = paragraph.text
        paragraph.text = replacements[old]
        found.add(old)

missing = set(replacements) - found
if missing:
    raise RuntimeError(f"Missing expected Home navigation text: {sorted(missing)}")

doc.save(SPEC)
print("aligned Home navigation text with the restored approved screen")
