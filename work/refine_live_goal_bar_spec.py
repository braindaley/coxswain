from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
doc = Document(SPEC)

replacements = {
    'States and rules: The metric grid, Edit display interaction, Pause, and End session match Live Row Free. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. When Stroke rate, Speed, or Power has a goal, its metric card keeps the current reading as the large value and adds a compact variance bar with a labeled target inside the same card.':
        'States and rules: The metric grid, Edit display interaction, Pause, and End session match Live Row Free. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. When Stroke rate, Speed, or Power has a goal, its metric card keeps the current reading as the large value. A full-width line at the bottom of that card places the target at the center and marks the user’s current position.',
    'The bar places the current reading relative to the target zone and labels the 26 SPM target.':
        'A full-width line spans the bottom of the card. The 26 SPM target is fixed at the center and the labeled You marker shows the current position.',
    'The same compact bar pattern places it relative to the labeled 14.5 km/h target.':
        'The full-width line centers the 14.5 km/h target and places the labeled You marker at the current position.',
    'The same compact bar pattern places it relative to the labeled 180 W target.':
        'The full-width line centers the 180 W target and places the labeled You marker at the current position.',
    'Live performance goals: Stroke rate, Speed, and Power keep their current reading as the large card value. A compact in-card bar shows position relative to the labeled target; separate +/− variance metrics are not used.':
        'Live performance goals: Stroke rate, Speed, and Power keep their current reading as the large card value. A full-width line at the bottom of the card centers the target and marks the user’s current position; separate +/− variance metrics are not used.',
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
print('refined live goal bar specification')
