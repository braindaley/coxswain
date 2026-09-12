from pathlib import Path


detail = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Program_Detail.html')
text = detail.read_text(encoding='utf-8')

css_old = '#cpd .race-best strong{color:#10213F;text-align:right}'
css_new = '#cpd .race-best strong{color:#10213F;text-align:right}#cpd .choose-reference{min-height:30px;padding:0 4px;background:transparent;color:#0B63F6;font-size:12px}'
html_old = '&lt;div class=&quot;race-best&quot;&gt;&lt;span&gt;Comparison&lt;/span&gt;&lt;strong&gt;&lt;/strong&gt;&lt;/div&gt;'
html_new = '&lt;div class=&quot;race-best&quot;&gt;&lt;span&gt;Comparison&lt;/span&gt;&lt;strong&gt;&lt;/strong&gt;&lt;button class=&quot;choose-reference&quot; type=&quot;button&quot;&gt;Change&lt;/button&gt;&lt;/div&gt;'
js_old = "toggle.onclick=()=&gt;{racing=!racing;race.classList.toggle(&#x27;active&#x27;,racing);toggle.classList.toggle(&#x27;active&#x27;,racing);toggle.setAttribute(&#x27;aria-checked&#x27;,String(racing));start.innerHTML=racing?&#x27;▶&amp;nbsp;&amp;nbsp;Start race&#x27;:&#x27;▶&amp;nbsp;&amp;nbsp;Start workout&#x27;};"
js_new = js_old + "\nroot.querySelector(&#x27;.choose-reference&#x27;).onclick=()=&gt;top.postMessage({type:&#x27;coxswain-open&#x27;,name:&#x27;Race Reference Picker&#x27;},&#x27;*&#x27;);"

for old, new in [(css_old, css_new), (html_old, html_new), (js_old, js_new)]:
    if text.count(old) != 1:
        raise RuntimeError(f'Expected one Program Detail match, found {text.count(old)}')
    text = text.replace(old, new)
text = text.replace('parent.postMessage', 'top.postMessage')
detail.write_text(text, encoding='utf-8')

setup = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Workout_Setup.html')
text = setup.read_text(encoding='utf-8')
old = "root.querySelector(&#x27;.changerace&#x27;).onclick=()=&gt;show(&#x27;Choose a workout&#x27;,&#x27;Select another compatible completed 5,000 m workout from History.&#x27;);"
new = "root.querySelector(&#x27;.changerace&#x27;).onclick=()=&gt;top.postMessage({type:&#x27;coxswain-open&#x27;,name:&#x27;Race Reference Picker&#x27;},&#x27;*&#x27;);"
if text.count(old) != 1:
    raise RuntimeError(f'Expected one Workout Setup match, found {text.count(old)}')
setup.write_text(text.replace(old, new).replace('parent.postMessage', 'top.postMessage'), encoding='utf-8')
print('linked Distance Setup and Program Detail to Race Reference Picker')
