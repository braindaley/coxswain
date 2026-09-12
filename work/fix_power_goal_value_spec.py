from pathlib import Path

from docx import Document


SPEC = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx')
doc = Document(SPEC)
old = 'The large 172 W value is current power.'
new = 'The large 188 W value is current power.'
matches = [p for p in doc.paragraphs if p.text.strip() == old]
if len(matches) != 1:
    raise RuntimeError(f'Expected one matching paragraph, found {len(matches)}')
matches[0].text = new
doc.save(SPEC)
print('corrected power goal example value')
