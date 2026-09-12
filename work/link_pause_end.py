from pathlib import Path


LIVE = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Live_Row_Free.html')
source = LIVE.read_text(encoding='utf-8')

old = "root.querySelector(&#x27;.pause&#x27;).onclick=()=&gt;{paused=!paused;root.querySelector(&#x27;.pause&#x27;).textContent=paused?&#x27;Resume&#x27;:&#x27;Pause&#x27;;root.querySelector(&#x27;.session&#x27;).textContent=paused?sessionName()+&#x27; · PAUSED&#x27;:sessionName()};"
new = "root.querySelector(&#x27;.pause&#x27;).onclick=()=&gt;top.postMessage({type:&#x27;coxswain-open&#x27;,name:&#x27;Pause / End&#x27;},&#x27;*&#x27;);"

if old not in source and new not in source:
    raise RuntimeError('Pause handler was not found')
source = source.replace(old, new, 1)
LIVE.write_text(source, encoding='utf-8')
print('linked every Live Row variant to Pause / End')
