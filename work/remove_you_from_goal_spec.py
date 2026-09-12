from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
doc = Document(SPEC)

replacements = {
    'A full-width line spans the bottom of the card. The 26 SPM target is fixed at the center and the labeled You marker shows the current position.':
        'A full-width line spans the bottom of the card. The 26 SPM target is fixed at the center and the dot shows the current position.',
    'The full-width line centers the 14.5 km/h target and places the labeled You marker at the current position.':
        'The full-width line centers the 14.5 km/h target and places the current-position dot relative to it.',
    'The full-width line centers the 180 W target and places the labeled You marker at the current position.':
        'The full-width line centers the 180 W target and places the current-position dot relative to it.',
}

found = set()
for paragraph in doc.paragraphs:
    text = paragraph.text.strip()
    if text in replacements:
        paragraph.text = replacements[text]
        found.add(text)

missing = set(replacements) - found
if missing:
    raise RuntimeError('Expected text was not found: ' + repr(sorted(missing)))

doc.save(SPEC)
print('removed You label from live goal specification')
