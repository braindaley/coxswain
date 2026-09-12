from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
doc = Document(SPEC)

replacements = {
    'Live performance goals: Stroke rate, Speed, and Power keep their current reading as the large card value. A small signed number appears in the upper-right of that same card: red below target, green above target, and neutral on target. Variance is never a separate metric card.':
        'Live performance goals: A goal-enabled metric card splits evenly between the live measurement and signed variance. The measurement remains large in the left pane; the right pane shows a large value labeled To Target. Negative variance is red, positive variance is green, and exact target is neutral. Stroke rate omits SPM because the context is explicit; Speed and Power keep their unit in the measurement label.',
    'States and rules: The metric grid, Pause, and End session match Live Row Free during Row segments. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. During Rest, the metric grid becomes a large countdown and identifies the next Row segment. When Stroke rate, Speed, or Power has a goal, its metric card keeps the current reading as the large value and shows a small signed variance in the upper-right. Negative variance is red, positive variance is green, and exact target is neutral.':
        'States and rules: The metric grid, Pause, and End session match Live Row Free during Row segments. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. During Rest, the metric grid becomes a large countdown and identifies the next Row segment. When Stroke rate, Speed, or Power has a goal, its card splits evenly between the large live measurement and a large signed variance labeled To Target. Negative variance is red, positive variance is green, and exact target is neutral.',
    'A small red −2 SPM value in the upper-right shows that the current reading is below target.':
        'The card splits evenly: 24 and Stroke Rate appear on the left; a large red −2 and To Target appear on the right. SPM is omitted because the metric context is explicit.',
    'A small signed variance uses the same upper-right position; −0.7 is red because current speed is below target.':
        'The left pane shows current speed with km/h in its label. The right pane shows a large signed variance labeled To Target.',
    'A small green +8 W value in the upper-right shows that current power is above target.':
        'The left pane shows current power with W in its label. The right pane shows a large green +8 labeled To Target.',
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
print('updated split measurement and variance specification')
