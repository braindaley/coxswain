from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')


REPLACEMENTS = {
    'Race Yourself mode should visually prioritize distance delta and time delta over decorative charts.':
        'Race Yourself uses the same configurable metric grid as Live Row Free. A compact panel below the grid compares two progress lines: You and the best compatible result from saved program History.',
    'Edit display opens metric configuration. The approved interactive mockup also allows selecting a metric area to replace its metric. Preserve access to configuration during a session.':
        'Edit display enters an explicit layout mode. The user taps a metric position, selects the metric that should appear there, and can add or remove positions before choosing Done editing. The selected position receives a strong outline and every position displays a Tap to change cue.',
    'Primary content: Configurable metrics with prominent distance and time deltas.':
        'Primary content: The same configurable metric grid as Live Row Free, followed by two labeled progress lines for You and Best from History.',
    'States and rules: Use separate saved metric configuration. Ahead/behind meaning must use text or sign as well as color.':
        'States and rules: The user’s live metric layout is shared with Live Row Free. The race panel is fixed below it. The best compatible saved workout remains fixed for the session; the You marker updates live. Ahead or behind status uses text and distance as well as color.',
    'Ahead/behind distance and time replace ordinary slots as configured.':
        'The ordinary configurable metric slots remain unchanged. A compact panel below them shows two progress lines, You and Best, plus a text distance indicating ahead or behind.',
}


doc = Document(SPEC)
found = set()
for paragraph in doc.paragraphs:
    text = paragraph.text.strip()
    if text in REPLACEMENTS:
        paragraph.text = REPLACEMENTS[text]
        found.add(text)

missing = set(REPLACEMENTS) - found
if missing:
    raise RuntimeError('Expected paragraphs were not found: ' + repr(sorted(missing)))

doc.save(SPEC)
print(f'updated {len(found)} Live Row and Race specification paragraphs')
