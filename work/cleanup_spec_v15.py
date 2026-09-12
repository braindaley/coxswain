from docx import Document

path = '/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Product_Design_Spec.docx'
doc = Document(path)
matches = [p for p in doc.paragraphs if p.text == '12 Workout Library and Program Builder Direction']
if len(matches) > 1:
    duplicate = matches[1]
    body = doc._element.body
    start = list(body).index(duplicate._p)
    for element in list(body)[start:]:
        body.remove(element)
    previous = list(body)[-1] if len(body) else None
    if previous is not None and previous.tag.endswith('}p'):
        body.remove(previous)
doc.save(path)
