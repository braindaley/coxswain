from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
doc = Document(SPEC)

replacements = {
    'States and rules: The metric grid, Edit display interaction, Pause, and End session match Live Row Free. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. When Stroke rate, Speed, or Power has a goal, its metric card keeps the current reading as the large value. A full-width line at the bottom of that card places the target at the center and marks the user’s current position.':
        'States and rules: The metric grid, Edit display interaction, Pause, and End session match Live Row Free. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. When Stroke rate, Speed, or Power has a goal, its metric card keeps the current reading as the large value and shows a compact signed variance in the upper-right. Negative variance is red, positive variance is green, and exact target is neutral.',
    'A full-width line spans the bottom of the card. The 26 SPM target is fixed at the center and the dot shows the current position.':
        'A small red −2 SPM badge in the upper-right shows that the current reading is below target.',
    'The full-width line centers the 14.5 km/h target and places the current-position dot relative to it.':
        'A small signed variance badge uses the same upper-right position; −0.7 is red because current speed is below target.',
    'The full-width line centers the 180 W target and places the current-position dot relative to it.':
        'A green +8 W badge in the upper-right shows that current power is above target.',
    'Live performance goals: Stroke rate, Speed, and Power keep their current reading as the large card value. A full-width line at the bottom of the card centers the target and marks the user’s current position; separate +/− variance metrics are not used.':
        'Live performance goals: Stroke rate, Speed, and Power keep their current reading as the large card value. A compact signed variance appears in the upper-right of that same card: red below target, green above target, and neutral on target. Variance is never a separate metric card.',
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
print('replaced goal line with signed variance badges')
