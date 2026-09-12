from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
REPLACEMENTS = {
    'Users can replace, add, and remove metric slots, retaining at least one. Preserve saved configurations and separate ordinary-rowing and pace-boat configurations as supported by the existing app.':
        'Users can replace the metric type in each existing slot. Preserve the saved configuration across sessions and use the same live display layout for ordinary rowing and Race Your Best.',
    'States and rules: No bottom navigation. Preserve at least one metric. Connection-loss and missing-sensor states remain to be finalized.':
        'States and rules: No bottom navigation. Metric blocks are read-only until Edit display is selected. Connection-loss and missing-sensor states remain to be finalized.',
}

doc = Document(SPEC)
found = set()
for paragraph in doc.paragraphs:
    source = paragraph.text.strip()
    if source in REPLACEMENTS:
        paragraph.text = REPLACEMENTS[source]
        found.add(source)
missing = set(REPLACEMENTS) - found
if missing:
    raise RuntimeError('Expected paragraphs were not found: ' + repr(sorted(missing)))
doc.save(SPEC)
print(f'removed add/remove behavior from {len(found)} specification paragraphs')
