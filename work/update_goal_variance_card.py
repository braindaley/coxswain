from pathlib import Path


LIVE = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Live_Row_Free.html')
text = LIVE.read_text(encoding='utf-8')

old_css = "#cs-live .goal-variance{top:12px;right:14px;min-width:0;padding:0;border:0;border-radius:0;background:transparent!important;font-size:10px;line-height:14px}#cs-live .goal-preview[hidden],#cs-live .interval-state-preview[hidden],#cs-live .rest-countdown[hidden],#cs-live .metrics[hidden],#cs-live .customize[hidden]{display:none}#cs-live .metric.has-goal{display:grid;grid-template-columns:1fr 1fr;padding:0}#cs-live .measure-pane,#cs-live .variance-pane{align-self:stretch;display:flex;min-width:0;flex-direction:column;align-items:center;justify-content:center;padding:8px;box-sizing:border-box}#cs-live .variance-pane{border-left:1px solid #31505D}#cs-live .variance-pane.below{color:#FF8A96;background:#482F38}#cs-live .variance-pane.above{color:#4ADE80;background:#16493B}#cs-live .variance-pane.on-target{color:#DCEBFF;background:#31505D}#cs-live .variance-number{font-weight:500;line-height:1.05;font-variant-numeric:tabular-nums;white-space:nowrap}#cs-live .variance-label{margin-top:4px;color:currentColor;font-size:11px;font-weight:500;letter-spacing:.06em}"
new_css = "#cs-live .goal-variance{top:12px;right:14px;min-width:0;padding:0;border:0;border-radius:0;background:transparent!important;font-size:10px;line-height:14px}#cs-live .goal-preview[hidden],#cs-live .interval-state-preview[hidden],#cs-live .rest-countdown[hidden],#cs-live .metrics[hidden],#cs-live .customize[hidden]{display:none}#cs-live .metric.has-goal{padding:8px;color:#FFFFFF}#cs-live .metric.has-goal.goal-below{background:#7A2836}#cs-live .metric.has-goal.goal-above,#cs-live .metric.has-goal.goal-on-target{background:#126B4D}#cs-live .metric.has-goal .label{color:#FFFFFF}#cs-live .variance-number{font-weight:500;line-height:1.05;font-variant-numeric:tabular-nums;white-space:nowrap}"

old_fit = "function fit(){const cells=[...grid.children];if(!cells.length)return;let sharedSize=Infinity;cells.forEach((cell,i)=&gt;{const m=catalog[slots[i]],n=cell.querySelector(&#x27;.number&#x27;),label=cell.querySelector(&#x27;.label&#x27;);const w=(cell.classList.contains(&#x27;has-goal&#x27;)?cell.clientWidth/2:cell.clientWidth)-24,h=cell.clientHeight-label.offsetHeight-24;const family=getComputedStyle(n).fontFamily;ctx.font=&#x27;500 100px &#x27;+family;const stable=m[1].replace(/[0-9]/g,&#x27;8&#x27;);const measure=ctx.measureText(stable).width;sharedSize=Math.min(sharedSize,h/1.05,w*100/measure);});const size=Math.max(12,Math.floor(sharedSize));cells.forEach(cell=&gt;{cell.querySelector(&#x27;.number&#x27;).style.fontSize=size+&#x27;px&#x27;;const variance=cell.querySelector(&#x27;.variance-number&#x27;);if(variance)variance.style.fontSize=size+&#x27;px&#x27;});}"
new_fit = "function fit(){const cells=[...grid.children];if(!cells.length)return;let sharedSize=Infinity;cells.forEach((cell,i)=&gt;{const m=catalog[slots[i]],n=cell.querySelector(&#x27;.number&#x27;),label=cell.querySelector(&#x27;.label&#x27;);const w=cell.clientWidth-24,h=cell.clientHeight-label.offsetHeight-24;const family=getComputedStyle(n).fontFamily;ctx.font=&#x27;500 100px &#x27;+family;const stable=(cell.classList.contains(&#x27;has-goal&#x27;)?n.textContent:m[1]).replace(/[0-9]/g,&#x27;8&#x27;);const measure=ctx.measureText(stable).width;sharedSize=Math.min(sharedSize,h/1.05,w*100/measure);});const size=Math.max(12,Math.floor(sharedSize));cells.forEach(cell=&gt;cell.querySelector(&#x27;.number&#x27;).style.fontSize=size+&#x27;px&#x27;); }"

old_render_piece = "b.className=&#x27;metric&#x27;+(hasGoal?&#x27; has-goal&#x27;:&#x27;&#x27;)+(device.classList.contains(&#x27;editing&#x27;)&amp;&amp;i===active?&#x27; active&#x27;:&#x27;&#x27;);const shownValue=hasGoal?goal.value:m[1]"
new_render_piece = "b.className=&#x27;metric&#x27;+(hasGoal?&#x27; has-goal goal-&#x27;+goal.tone:&#x27;&#x27;)+(device.classList.contains(&#x27;editing&#x27;)&amp;&amp;i===active?&#x27; active&#x27;:&#x27;&#x27;);const shownValue=hasGoal?goal.variance:m[1]"

old_goal_markup = "l.textContent=hasGoal&amp;&amp;m[0]===&#x27;Stroke rate&#x27;?&#x27;STROKE RATE&#x27;:m[0].toUpperCase()+&#x27; · &#x27;+m[2];if(hasGoal){const measure=document.createElement(&#x27;span&#x27;);measure.className=&#x27;measure-pane&#x27;;measure.append(n,l);const variance=document.createElement(&#x27;span&#x27;);variance.className=&#x27;variance-pane &#x27;+goal.tone;const vn=document.createElement(&#x27;span&#x27;);vn.className=&#x27;variance-number&#x27;;vn.textContent=goal.variance;const vl=document.createElement(&#x27;span&#x27;);vl.className=&#x27;variance-label&#x27;;vl.textContent=&#x27;TO TARGET&#x27;;variance.append(vn,vl);b.append(measure,variance)}else{b.append(n,l)}"
new_goal_markup = "l.textContent=hasGoal?m[0].toUpperCase()+&#x27; · TO TARGET&#x27;:m[0].toUpperCase()+&#x27; · &#x27;+m[2];b.append(n,l)"

for old, new, expected in [
    (old_css, new_css, 1),
    (old_fit, new_fit, 1),
    (old_render_piece, new_render_piece, 1),
    (old_goal_markup, new_goal_markup, 1),
]:
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f'Expected {expected} occurrence(s), found {count}: {old[:80]}')
    text = text.replace(old, new)

LIVE.write_text(text, encoding='utf-8')
print('updated goal cards to full-card variance states')
