from pathlib import Path
from tempfile import NamedTemporaryFile
from zipfile import ZIP_DEFLATED, ZipFile

from docx import Document


ROOT = Path('/Users/brian/Documents/ChatGPT/Coxswain')
SPEC = ROOT / 'outputs/Coxswain_Product_Design_Spec.docx'

replacements = {
    'Live performance goals: A goal-enabled metric card splits evenly between the live measurement and signed variance. The measurement remains large in the left pane; the right pane shows a large value labeled To Target. Negative variance is red, positive variance is green, and exact target is neutral. Stroke rate omits SPM because the context is explicit; Speed and Power keep their unit in the measurement label.':
        'Live performance goals: When Stroke Rate, Speed, or Power has a goal, its assigned metric card becomes a dedicated variance card. The card shows only the signed difference from the target at the same large size as the other live metrics. Its full background is red while below target and green when the target is met or exceeded. The label identifies the metric and says To Target.',
    'States and rules: The metric grid, Pause, and End session match Live Row Free during Row segments. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. During Rest, the metric grid becomes a large countdown and identifies the next Row segment. When Stroke rate, Speed, or Power has a goal, its card splits evenly between the large live measurement and a large signed variance labeled To Target. Negative variance is red, positive variance is green, and exact target is neutral.':
        'States and rules: The metric grid, Pause, and End session match Live Row Free during Row segments. The progress panel is fixed below the grid. Free-form intervals advance through their saved order and never introduce rounds. During Rest, the metric grid becomes a large countdown and identifies the next Row segment. When Stroke Rate, Speed, or Power has a goal, its assigned card shows only the large signed variance. The full card is red while below target and green when the target is met or exceeded.',
    'The large 24 SPM value is the current stroke rate, not a difference from the goal.':
        'The large -2 value is the signed difference from the stroke-rate target.',
    'The card splits evenly: 24 and Stroke Rate appear on the left; a large red −2 and To Target appear on the right. SPM is omitted because the metric context is explicit.':
        'The entire card uses the below-target red state and is labeled Stroke Rate · To Target. The current reading is not repeated in this card.',
    'The left pane shows current speed with km/h in its label. The right pane shows a large signed variance labeled To Target.':
        'The card shows only the signed speed variance and is labeled Speed · To Target. Its full background communicates whether the target is being met.',
    'The left pane shows current power with W in its label. The right pane shows a large green +8 labeled To Target.':
        'The card shows only the large +8 power variance. Its full green background indicates that the target is being met or exceeded.',
}

doc = Document(SPEC)
found = set()
for paragraph in doc.paragraphs:
    original = paragraph.text.strip()
    if original in replacements:
        paragraph.text = replacements[original]
        found.add(original)

missing = set(replacements) - found
if missing:
    raise RuntimeError('Expected specification text was not found: ' + repr(sorted(missing)))

doc.save(SPEC)

media_replacements = {
    'word/media/image30.png': ROOT / 'work/screen-atlas/26_live_target_goal_stroke_rate.png',
    'word/media/image31.png': ROOT / 'work/screen-atlas/27_live_target_goal_speed.png',
    'word/media/image32.png': ROOT / 'work/screen-atlas/28_live_target_goal_power.png',
}

with NamedTemporaryFile(suffix='.docx', delete=False, dir=SPEC.parent) as handle:
    temp_path = Path(handle.name)

with ZipFile(SPEC, 'r') as source, ZipFile(temp_path, 'w', ZIP_DEFLATED) as target:
    for item in source.infolist():
        payload = media_replacements[item.filename].read_bytes() if item.filename in media_replacements else source.read(item.filename)
        target.writestr(item, payload)

temp_path.replace(SPEC)
print('updated specification text and goal-state figures')
