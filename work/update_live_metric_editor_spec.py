from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')

REPLACEMENTS = {
    'Edit display enters an explicit layout mode. The user taps a metric position, selects the metric that should appear there, and can add or remove positions before choosing Done editing. The selected position receives a strong outline and every position displays a Tap to change cue.':
        'Edit display enters an explicit edit mode. Outside that mode, metric blocks are read-only. In edit mode, the user taps a metric position and chooses its replacement from visible radio options, then selects Done editing. The number and arrangement of blocks do not change on this screen.',
    'Primary actions: Edit display; replace/add/remove metrics; Pause/Resume; End session with confirmation.':
        'Primary actions: Edit display; tap a block and replace its metric with a radio choice; Done editing; Pause/Resume; End session with confirmation.',
}

doc = Document(SPEC)
counts = {source: 0 for source in REPLACEMENTS}
for paragraph in doc.paragraphs:
    source = paragraph.text.strip()
    if source in REPLACEMENTS:
        paragraph.text = REPLACEMENTS[source]
        counts[source] += 1

missing = [source for source, count in counts.items() if count == 0]
if missing:
    raise RuntimeError('Expected paragraphs were not found: ' + repr(missing))

doc.save(SPEC)
print(f'updated {sum(counts.values())} metric editor specification paragraphs')
