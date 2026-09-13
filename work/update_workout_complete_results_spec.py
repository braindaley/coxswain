from pathlib import Path

from docx import Document


ROOT = Path('/Users/brian/Documents/ChatGPT/Coxswain')
SPEC = ROOT / 'outputs/Coxswain_Product_Design_Spec.docx'
ATLAS = ROOT / 'work/screen-atlas'
OVERVIEW = ROOT / 'work/screen-inventory-overview.png'

doc = Document(SPEC)

replacements = {
    'Primary content: Type-specific primary result, program identity, completion status, best or race comparison when applicable, key averages, interval results, and date/time.':
        'Primary content: Type-specific primary result, program identity, completion status, average split time, calories, average stroke rate, average power, interval or race details when applicable, and Split Time, Power, and Stroke Rate graphs by elapsed time.',
    'Primary actions: Row Again; View Details; Done. Race Yourself remains available from Workout Details when the result is eligible.':
        'Primary actions: Done. The results screen contains the complete workout detail and does not offer Row Again or a separate View Details action.',
    'States and rules: Distance leads with completion time; duration leads with distance; intervals use their program-specific work result and exclude Rest; Free Row leads with distance; Race shows the outcome against its fixed reference. Manually ended targets show actual progress and Ended Early. New Best compares only compatible completed workouts.':
        'States and rules: Distance leads with completion time; duration and Free Row lead with distance; intervals use their program-specific work result and exclude Rest; Race shows the outcome against its fixed reference. Manually ended targets show actual progress and Ended Early. New Best compares only compatible completed workouts. Split Time shows average and minimum; Power shows average and maximum; Stroke Rate shows average, total strokes, minimum, and maximum. Every graph has labeled elapsed-time and metric axes.',
    'The summary keeps average split, stroke rate, power, and energy compact beneath the result.':
        'The summary shows average split time, calories, average stroke rate, and average power, followed by taller Split Time, Power, and Stroke Rate graphs with labeled axes.',
    'The complete Row and Rest group appears on one line, followed by individual Row results in the interactive screen.':
        'The complete Row and Rest group appears on one line, followed by individual Row results and the same full set of performance graphs.',
    'The completed workout is saved to History and can be opened or repeated.':
        'The completed workout is saved to History; Done is the only action on this screen.',
    'View Details opens the full Race Comparison Details path through the completed workout.':
        'Race comparison and the complete Split Time, Power, and Stroke Rate graphs remain on this scrollable results screen.',
    'v2.0 - Consolidated all designed screens into an individual-state visual atlas, documented component changes by interaction, unified quick workout and program setup, simplified interval segments to Duration / Distance / Rest, incorporated Race Your Best across setup, reference selection, Live Row, and results, and locked Pause / End and Workout Complete.':
        'v2.0 - Consolidated all designed screens into an individual-state visual atlas, unified quick workout and program setup, simplified interval segments to Duration / Distance / Rest, incorporated Race Your Best across the full flow, locked Pause / End, and expanded Workout Complete into the single detailed results destination with time-series charts and one Done action.',
}

found = set()
for paragraph in doc.paragraphs:
    if paragraph.text in replacements:
        original = paragraph.text
        paragraph.text = replacements[original]
        found.add(original)
missing = set(replacements) - found
if missing:
    raise RuntimeError('Expected specification text was not found: ' + repr(sorted(missing)))

# Refresh the six Workout Complete state images in their existing positions.
files = [
    '34_workout_complete_distance.png',
    '35_workout_complete_duration.png',
    '36_workout_complete_intervals.png',
    '37_workout_complete_free.png',
    '38_workout_complete_race.png',
    '39_workout_complete_ended.png',
]
workout_shapes = list(doc.inline_shapes)[-7:-1]
if len(workout_shapes) != len(files):
    raise RuntimeError('Workout Complete figure count changed unexpectedly')
for shape, filename in zip(workout_shapes, files):
    blip = shape._inline.graphic.graphicData.pic.blipFill.blip
    doc.part.related_parts[blip.embed]._blob = (ATLAS / filename).read_bytes()

# Refresh the final all-screen inventory after the state images change.
inventory = doc.inline_shapes[-1]
inventory_blip = inventory._inline.graphic.graphicData.pic.blipFill.blip
doc.part.related_parts[inventory_blip.embed]._blob = OVERVIEW.read_bytes()

doc.save(SPEC)
print('updated Workout Complete results and graph specifications')
