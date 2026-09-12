from __future__ import annotations

import base64
import io
import json
from html import escape
from pathlib import Path

from PIL import Image


ROOT = Path("/Users/brian/Documents/ChatGPT/Coxswain")
DEST = Path("/Users/brian/.codex/visualizations/2026/09/07/01a07dce-ee45-77f1-a36d-c2efbed2db4d/coxswain-interactive-screen-canvas.html")
PROJECT_DEST = ROOT / "outputs/Coxswain_Interactive_Screen_Canvas.html"


def jpeg_data(path: Path, size=(520, 920), quality=72) -> str:
    image = Image.open(path).convert("RGB")
    image.thumbnail(size, Image.Resampling.LANCZOS)
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=quality, optimize=True)
    return "data:image/jpeg;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")


home_image = jpeg_data(ROOT / "work/screen-atlas/01_home.png")

home_source = f'''<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>
html,body{{margin:0;min-height:100%;background:#eef2f7;font-family:Arial,sans-serif}}.home{{position:relative;width:min(430px,100%);margin:auto}}img{{display:block;width:100%;height:auto}}button{{position:absolute;border:0;background:transparent;cursor:pointer;border-radius:18px}}button:focus-visible{{outline:4px solid #76b7ff;outline-offset:-4px}}.free{{left:5%;top:64%;width:86%;height:8%}}.duration{{left:4%;top:75%;width:23%;height:13%}}.distance{{left:27%;top:75%;width:23%;height:13%}}.intervals{{left:50%;top:75%;width:23%;height:13%}}.programs{{left:73%;top:75%;width:23%;height:13%}}.navprograms{{left:23%;top:89%;width:22%;height:10%}}
</style></head><body><div class="home"><img src="{home_image}" alt="Coxswain Home screen"><button class="free" data-open="Live Row: Free" aria-label="Open Free Row"></button><button class="duration" data-open="Duration Setup" aria-label="Open Duration Setup"></button><button class="distance" data-open="Distance Setup" aria-label="Open Distance Setup"></button><button class="intervals" data-open="Quick Intervals" aria-label="Open Quick Intervals"></button><button class="programs" data-open="My Programs" aria-label="Open Programs"></button><button class="navprograms" data-open="My Programs" aria-label="Open Programs navigation"></button></div><script>document.querySelectorAll('[data-open]').forEach(b=>b.onclick=()=>parent.postMessage({{type:'coxswain-open',name:b.dataset.open}},'*'));</script></body></html>'''

base_setup = (ROOT / "outputs/Coxswain_Workout_Setup.html").read_text(encoding="utf-8")
base_programs = (ROOT / "outputs/Coxswain_Programs.html").read_text(encoding="utf-8")
base_live = (ROOT / "outputs/Coxswain_Live_Row_Free.html").read_text(encoding="utf-8")
base_program_detail = (ROOT / "outputs/Coxswain_Program_Detail.html").read_text(encoding="utf-8")


def setup_variant(workout_type: str, mode: str = "quick") -> str:
    result = base_setup
    result = result.replace(
        "state={mode:&#x27;quick&#x27;,type:&#x27;duration&#x27;",
        f"state={{mode:&#x27;{mode}&#x27;,type:&#x27;{workout_type}&#x27;",
        1,
    )
    result = result.replace(
        '&lt;button class=&quot;active&quot; data-type=&quot;duration&quot;&gt;Duration&lt;/button&gt;',
        '&lt;button data-type=&quot;duration&quot;&gt;Duration&lt;/button&gt;',
        1,
    )
    result = result.replace(
        f'&lt;button data-type=&quot;{workout_type}&quot;&gt;{workout_type.title()}&lt;/button&gt;',
        f'&lt;button class=&quot;active&quot; data-type=&quot;{workout_type}&quot;&gt;{workout_type.title()}&lt;/button&gt;',
        1,
    )
    if workout_type == "distance":
        result = result.replace(
            '&lt;input class=&quot;value&quot; value=&quot;60&quot;',
            '&lt;input class=&quot;value&quot; value=&quot;5000&quot;',
            1,
        )
    if mode == "program":
        result = result.replace(
            '&lt;button class=&quot;quickctx active&quot; type=&quot;button&quot;&gt;Quick workout&lt;/button&gt;&lt;button class=&quot;programctx&quot;',
            '&lt;button class=&quot;quickctx&quot; type=&quot;button&quot;&gt;Quick workout&lt;/button&gt;&lt;button class=&quot;programctx active&quot;',
            1,
        )
    return result


def programs_variant(source: str) -> str:
    if source == "mine":
        return base_programs
    result = base_programs.replace(
        "const state={empty:false,source:&#x27;mine&#x27;}",
        "const state={empty:false,source:&#x27;library&#x27;}",
        1,
    )
    result = result.replace(
        'class=&quot;active my-tab&quot; role=&quot;tab&quot; aria-selected=&quot;true&quot;',
        'class=&quot;my-tab&quot; role=&quot;tab&quot; aria-selected=&quot;false&quot;',
        1,
    )
    result = result.replace(
        'class=&quot;library-tab&quot; role=&quot;tab&quot; aria-selected=&quot;false&quot;',
        'class=&quot;active library-tab&quot; role=&quot;tab&quot; aria-selected=&quot;true&quot;',
        1,
    )
    return result


def live_variant(race: bool) -> str:
    if not race:
        return base_live
    result = base_live.replace(
        '&lt;div class=&quot;device&quot;&gt;',
        '&lt;div class=&quot;device race&quot;&gt;',
        1,
    )
    result = result.replace(
        '&lt;section class=&quot;race-progress&quot; hidden',
        '&lt;section class=&quot;race-progress&quot;',
        1,
    )
    return result


def live_target_variant() -> str:
    result = base_live.replace(
        '&lt;div class=&quot;target-preview&quot; hidden',
        '&lt;div class=&quot;target-preview&quot;',
        1,
    )
    result = result.replace(
        '&lt;div class=&quot;device&quot;&gt;',
        '&lt;div class=&quot;device target&quot;&gt;',
        1,
    )
    result = result.replace(
        '&lt;section class=&quot;target-progress&quot; hidden',
        '&lt;section class=&quot;target-progress&quot;',
        1,
    )
    result = result.replace(
        '&lt;div class=&quot;goal-preview&quot; hidden',
        '&lt;div class=&quot;goal-preview&quot;',
        1,
    )
    return result


sources = {
    "home": home_source,
    "setup-duration": setup_variant("duration"),
    "setup-distance": setup_variant("distance"),
    "setup-intervals": setup_variant("intervals"),
    "setup-program": setup_variant("duration", "program"),
    "programs-mine": programs_variant("mine"),
    "programs-library": programs_variant("library"),
    "program-detail": base_program_detail,
    "live-free": live_variant(False),
    "live-race": live_variant(True),
    "live-target": live_target_variant(),
}

screens = [
    ("Home", "Interactive", "Launch", "home", "Click Free Row, Duration, Distance, Intervals, or Programs directly on the screen."),
    ("Duration Setup", "Interactive", "Quick Row", "setup-duration", "Opens on Duration with its goal controls. Save as Program and Start Workout are clickable."),
    ("Distance Setup", "Interactive", "Quick Row", "setup-distance", "Opens on Distance with Race Your Best, goal controls, presets, and actions available."),
    ("Quick Intervals", "Interactive", "Quick Row", "setup-intervals", "Opens on Intervals. Add a segment, then edit or delete segments in the live prototype."),
    ("Program Builder", "Interactive", "Programs", "setup-program", "Opens in New Program mode with Duration selected; Distance and Intervals use the same template."),
    ("My Programs", "Interactive", "Programs", "programs-mine", "Opens with My Programs selected. Cards, View, Start, the overflow menu, and Create Program are clickable."),
    ("Workout Library", "Interactive", "Programs", "programs-library", "Opens with Workout Library selected. It uses the same compact cards without Create Program."),
    ("Live Row: Free", "Interactive", "Session", "live-free", "Edit display enables metric blocks; tap a block and choose its replacement from the radio list."),
    ("Live Row: Race", "Interactive", "Session", "live-race", "Uses the Live Row metric grid with two race-progress lines at the bottom: You and the best compatible workout from History."),
    ("Program Detail / Start", "Interactive", "Programs", "program-detail", "Review the saved workout, inspect program history, optionally Race Your Best, or start the workout."),
    ("Live Row: Target", "Interactive", "Session", "live-target", "Preview Duration, Distance, or Intervals. In Intervals, switch between Row and Rest to see the simplified sequence line and full countdown state."),
    ("Race Reference Picker", "Placeholder", "Session", None, "Choose an alternative compatible completed workout."),
    ("Pause / End", "Placeholder", "Session", None, "Pause, resume, end, and discard safeguards."),
    ("Workout Complete", "Placeholder", "Results", None, "Post-workout summary and quick actions."),
    ("History", "Placeholder", "History", None, "Completed workouts, filters, and totals."),
    ("Workout Details", "Placeholder", "History", None, "Metrics, graphs, export, repeat, and Race Your Best."),
    ("Race Comparison Details", "Placeholder", "History", None, "Detailed comparison with the selected prior workout."),
    ("Connect Rower", "Placeholder", "Device", None, "USB and Bluetooth connection and device status."),
    ("Settings", "Placeholder", "Settings", None, "Display, units, defaults, heart rate, data, and advanced options."),
    ("Display Customization", "Placeholder", "Settings", None, "Choose and reorder live-row metrics."),
    ("Heart Rate Source", "Placeholder", "Settings", None, "Select a supported heart-rate source."),
    ("Data / Export", "Placeholder", "Settings", None, "Export, backup, and storage management."),
    ("More / Diagnostics", "Placeholder", "Settings", None, "Diagnostics, protocol information, about, and advanced tools."),
]

items = []
for index, (name, status, group, source, note) in enumerate(screens):
    items.append(
        f'<button class="cs-nav-item" type="button" data-index="{index}"><span>{escape(name)}</span><small>{escape(group)} · {escape(status)}</small></button>'
    )

source_nodes = "".join(
    f'<textarea hidden id="cs-source-{key}">{escape(value)}</textarea>' for key, value in sources.items()
)

data_rows = ",\n".join(
    "{" + ",".join([
        f'name:{json.dumps(name)}', f'status:{json.dumps(status)}', f'group:{json.dumps(group)}', f'source:{json.dumps(source)}', f'note:{json.dumps(note)}'
    ]) + "}"
    for name, status, group, source, note in screens
)

source_map_rows = ",".join(
    f'{json.dumps(key)}:root.querySelector({json.dumps("#cs-source-" + key)}).value'
    for key in sources
)

fragment = f'''<div id="coxswain-interactive-canvas">
<style>
#coxswain-interactive-canvas{{font-family:Arial,sans-serif;color:light-dark(#10213f,#eef5ff);height:min(900px,calc(100vh - 24px));min-height:650px;display:grid;grid-template-columns:260px minmax(0,1fr);background:light-dark(#f4f7fb,#0d1726);overflow:hidden;border-radius:18px}}
#coxswain-interactive-canvas *{{box-sizing:border-box}}
#coxswain-interactive-canvas .cs-sidebar{{display:flex;flex-direction:column;min-height:0;padding:16px 10px 12px 14px;border-right:1px solid light-dark(#dce4ee,#26364b)}}
#coxswain-interactive-canvas .cs-sidebar h2{{font-size:19px;margin:0 4px 4px}}#coxswain-interactive-canvas .cs-sidebar p{{font-size:12px;line-height:17px;margin:0 4px 12px;color:light-dark(#607089,#aab8cb)}}
#coxswain-interactive-canvas .cs-list{{display:flex;flex-direction:column;gap:4px;overflow:auto;padding-right:4px}}
#coxswain-interactive-canvas .cs-nav-item{{border:0;border-radius:11px;background:transparent;color:inherit;padding:9px 10px;text-align:left;cursor:pointer}}
#coxswain-interactive-canvas .cs-nav-item:hover{{background:light-dark(#e8eef6,#17263a)}}#coxswain-interactive-canvas .cs-nav-item.active{{background:light-dark(#dcebff,#17375b);color:light-dark(#0759d5,#92c5ff)}}
#coxswain-interactive-canvas .cs-nav-item span,#coxswain-interactive-canvas .cs-nav-item small{{display:block}}#coxswain-interactive-canvas .cs-nav-item span{{font-weight:700;font-size:13px;line-height:17px}}#coxswain-interactive-canvas .cs-nav-item small{{font-size:10px;line-height:14px;margin-top:2px;color:light-dark(#607089,#aab8cb)}}
#coxswain-interactive-canvas .cs-main{{min-width:0;min-height:0;display:grid;grid-template-rows:auto minmax(0,1fr);padding:14px}}
#coxswain-interactive-canvas .cs-toolbar{{display:flex;align-items:flex-start;gap:12px;padding:2px 2px 12px}}#coxswain-interactive-canvas .cs-title{{flex:1;min-width:0}}#coxswain-interactive-canvas .cs-title h3{{margin:0 0 3px;font-size:22px}}#coxswain-interactive-canvas .cs-title p{{margin:0;color:light-dark(#607089,#aab8cb);font-size:12px;line-height:17px}}
#coxswain-interactive-canvas .cs-actions{{display:flex;gap:7px;flex-wrap:wrap;justify-content:flex-end}}#coxswain-interactive-canvas .cs-action{{border:1px solid light-dark(#b9c5d5,#42536a);border-radius:18px;background:light-dark(#fff,#15243a);color:inherit;min-height:36px;padding:0 13px;cursor:pointer;font-weight:700}}#coxswain-interactive-canvas .cs-action.primary{{background:#0b63f6;border-color:#0b63f6;color:#fff}}#coxswain-interactive-canvas .cs-action:disabled{{opacity:.35;cursor:default}}
#coxswain-interactive-canvas .cs-stage{{min-height:0;display:grid;place-items:center;overflow:auto;background:light-dark(#e9eef5,#071322);border-radius:16px;padding:12px}}
#coxswain-interactive-canvas .cs-frame{{width:min(500px,100%);height:100%;min-height:690px;border:0;border-radius:14px;background:#fff}}
#coxswain-interactive-canvas .cs-race{{display:block;max-width:min(430px,100%);max-height:100%;width:auto;height:auto;border-radius:18px}}
#coxswain-interactive-canvas .cs-placeholder{{width:min(430px,92%);min-height:620px;border:2px dashed light-dark(#aab7c8,#53647c);border-radius:24px;display:grid;place-content:center;gap:14px;text-align:center;padding:30px;background:light-dark(#f8fafc,#111f32)}}#coxswain-interactive-canvas .cs-placeholder strong{{font-size:26px}}#coxswain-interactive-canvas .cs-placeholder span{{color:light-dark(#607089,#aab8cb);line-height:1.5}}
#coxswain-interactive-canvas.expanded{{grid-template-columns:1fr}}#coxswain-interactive-canvas.expanded .cs-sidebar{{display:none}}#coxswain-interactive-canvas.expanded .cs-frame{{width:min(620px,100%)}}
@media(max-width:720px){{#coxswain-interactive-canvas{{grid-template-columns:1fr;height:auto}}#coxswain-interactive-canvas .cs-sidebar{{border-right:0;border-bottom:1px solid light-dark(#dce4ee,#26364b);max-height:220px}}#coxswain-interactive-canvas .cs-main{{min-height:760px}}#coxswain-interactive-canvas .cs-toolbar{{flex-direction:column}}#coxswain-interactive-canvas .cs-actions{{justify-content:flex-start}}}}
</style>
{source_nodes}
<div class="cs-sidebar"><h2>Coxswain screens</h2><p>Select a screen to open its working prototype. Planned screens remain visible as placeholders.</p><div class="cs-list">{''.join(items)}</div></div>
<div class="cs-main"><div class="cs-toolbar"><div class="cs-title"><h3></h3><p></p></div><div class="cs-actions"><button class="cs-action cs-prev" type="button">← Previous</button><button class="cs-action cs-next" type="button">Next →</button><button class="cs-action primary cs-expand" type="button">Expand</button></div></div><div class="cs-stage"></div></div>
</div>
<script>
(()=>{{
const root=document.getElementById('coxswain-interactive-canvas');
const screens=[{data_rows}];
const sources={{{source_map_rows}}};
const stage=root.querySelector('.cs-stage');let current=0;
function show(index){{
 current=Math.max(0,Math.min(screens.length-1,index));const s=screens[current];
 root.querySelector('.cs-title h3').textContent=s.name;
 root.querySelector('.cs-title p').textContent=s.group+' · '+s.status+' — '+s.note;
 root.querySelectorAll('.cs-nav-item').forEach((b,i)=>b.classList.toggle('active',i===current));
 stage.replaceChildren();
 if(s.source&&sources[s.source]){{const frame=document.createElement('iframe');frame.className='cs-frame';frame.title=s.name+' interactive prototype';frame.srcdoc=sources[s.source];stage.append(frame);}}
 else {{const box=document.createElement('div');box.className='cs-placeholder';const title=document.createElement('strong');title.textContent=s.name;const state=document.createElement('span');state.textContent='Placeholder — this screen is next in the design queue.';const note=document.createElement('span');note.textContent=s.note;box.append(title,state,note);stage.append(box);}}
 root.querySelector('.cs-prev').disabled=current===0;root.querySelector('.cs-next').disabled=current===screens.length-1;
}}
root.querySelectorAll('.cs-nav-item').forEach((button,index)=>button.addEventListener('click',()=>show(index)));
root.querySelector('.cs-prev').onclick=()=>show(current-1);root.querySelector('.cs-next').onclick=()=>show(current+1);
root.querySelector('.cs-expand').onclick=()=>{{root.classList.toggle('expanded');root.querySelector('.cs-expand').textContent=root.classList.contains('expanded')?'Show list':'Expand';}};
window.addEventListener('message',event=>{{if(event.data?.type==='coxswain-open'){{const index=screens.findIndex(s=>s.name===event.data.name);if(index>=0)show(index);}}}});
show(0);
}})();
</script>
'''

DEST.write_text(fragment, encoding="utf-8")
PROJECT_DEST.write_text(fragment, encoding="utf-8")
print(f"wrote {DEST} ({DEST.stat().st_size} bytes)")
print(f"wrote {PROJECT_DEST} ({PROJECT_DEST.stat().st_size} bytes)")
