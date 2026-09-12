from pathlib import Path


path = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Live_Row_Free.html')
text = path.read_text(encoding='utf-8')
broken = 'b.append(n,l)b.onclick'
fixed = 'b.append(n,l);b.onclick'
if text.count(broken) != 1:
    raise RuntimeError(f'Expected one broken render boundary, found {text.count(broken)}')
path.write_text(text.replace(broken, fixed), encoding='utf-8')
print('repaired Live Row render function')
