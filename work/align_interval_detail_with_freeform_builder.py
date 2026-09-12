from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
REPLACEMENTS = {
    'States and rules: Saved definitions are immutable so their History and best result always describe the same workout. Duplicate creates a new editable definition with no History. Delete removes the definition but retains completed workouts in History. Best is type-specific: timed programs use greatest distance; distance programs use fastest completion time; fixed-distance intervals use best average split across work segments; fixed-duration intervals use greatest work distance; mixed intervals use best average split across the identical ordered work segments. Rest is excluded from performance ranking. Race Your Best appears only with compatible completed workouts.':
        'States and rules: Saved definitions are immutable so their History and best result always describe the same workout. Duplicate creates a new editable definition with no History. Delete removes the definition but retains completed workouts in History. Best is type-specific: timed programs use greatest distance; distance programs use fastest completion time. Interval programs preserve the exact free-form ordered segment list from the builder and have no separate rounds model. Time-only work segments rank by greatest work distance; distance-only work segments rank by lowest cumulative work time; mixed work segments use average work pace across the identical ordered structure. Rest is excluded from performance ranking. Race Your Best appears only with compatible completed workouts.',
    'Intervals expose rounds, work, rest, total work, and segment guidance without claiming an exact duration.':
        'Intervals display the exact ordered Row and Rest segments saved by the free-form builder; they do not introduce rounds.',
    'History ranks work-segment performance; Race Your Best requires the same ordered segment structure.':
        'Time-only sequences can show an exact total and rank by greatest work distance. Mixed time-and-distance sequences omit exact duration; Race Your Best requires the identical ordered segment structure.',
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
    raise RuntimeError('Expected text was not found: ' + repr(sorted(missing)))
doc.save(SPEC)
print('aligned interval detail and history rules with the free-form builder')
