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


def jpeg_crop_data(path: Path, crop, quality=78) -> str:
    image = Image.open(path).convert("RGB").crop(crop)
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=quality, optimize=True)
    return "data:image/jpeg;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")


# The Home screen is rendered as real HTML controls. Only the scenic rowing
# photograph is raster content, so navigation and states remain fully editable.
home_background = jpeg_crop_data(ROOT / "work/spec-images/image1.png", (0, 390, 941, 820))

home_source = f'''<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>
html,body{{margin:0;min-height:100%;background:#eaf0f6;font-family:Arial,sans-serif;color:#10213f}}*{{box-sizing:border-box}}button{{font:inherit}}.home{{width:min(430px,100%);min-height:860px;margin:auto;display:flex;flex-direction:column;overflow:hidden;background:#052f43;color:#fff;position:relative}}.hero{{position:relative;min-height:650px;padding:18px 16px 14px;background:linear-gradient(180deg,rgba(4,44,61,.18) 0%,rgba(4,44,61,.08) 38%,rgba(2,31,45,.88) 80%,#052f43 100%),url("{home_background}") center/cover no-repeat}}.top{{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}}.brand{{font-size:19px;letter-spacing:.28em;text-transform:uppercase;text-shadow:0 1px 4px #00131f}}.settings{{width:44px;height:44px;border:0;border-radius:22px;background:rgba(255,255,255,.16);color:#fff;font-size:23px;cursor:pointer}}.connection,.last{{width:100%;border:0;border-radius:18px;background:rgba(255,255,255,.96);color:#10213f;box-shadow:0 6px 18px rgba(0,20,35,.18)}}.connection{{min-height:72px;padding:14px 18px;display:grid;grid-template-columns:16px 1fr auto;align-items:center;gap:12px;text-align:left;cursor:pointer}}.dot{{width:14px;height:14px;border-radius:50%;background:#16c66a}}.connection strong,.connection small{{display:block}}.connection strong{{font-size:16px}}.connection small{{margin-top:3px;color:#53647c;font-size:13px}}.chev{{font-size:30px;color:#53647c}}.spacer{{height:202px}}.last{{padding:15px 18px 16px;margin-bottom:14px;text-align:left}}.last-head{{display:flex;align-items:center;justify-content:space-between;gap:8px}}.last-head strong{{font-size:17px}}.again{{min-height:42px;padding:0 15px;border:1px solid #9bc4ff;border-radius:12px;background:#eef6ff;color:#0759d5;font-weight:700;cursor:pointer}}.stats{{margin-top:10px;display:grid;grid-template-columns:1.25fr .8fr .9fr;gap:7px;align-items:end}}.primary-stat{{font-size:30px;font-weight:700;letter-spacing:-.02em}}.stat{{font-size:18px;font-weight:700}}.substats{{margin-top:7px;display:grid;grid-template-columns:1.25fr .8fr .9fr;gap:7px;color:#53647c;font-size:14px}}.date{{margin-top:6px;color:#53647c;font-size:13px}}.free{{width:100%;min-height:62px;border:0;border-radius:32px;background:#0b63f6;color:#fff;font-size:20px;font-weight:700;cursor:pointer;box-shadow:0 8px 18px rgba(0,45,120,.25)}}.free span{{font-size:23px;margin-right:9px}}.quick{{padding:0 16px 17px;background:#052f43}}.quick h2{{margin:0 0 13px;font-size:17px}}.quick-grid{{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px}}.quick button{{min-width:0;border:0;background:transparent;color:#fff;cursor:pointer;padding:0}}.quick .icon{{width:58px;height:58px;margin:0 auto 8px;display:grid;place-items:center;border-radius:50%;background:#f7fbff;color:#10213f}}.quick svg{{width:30px;height:30px;stroke:currentColor;fill:none;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round}}.quick span:last-child{{display:block;font-size:12px;white-space:nowrap}}.nav{{margin-top:auto;min-height:80px;display:grid;grid-template-columns:repeat(4,1fr);align-items:center;background:#fff;color:#53647c;padding:8px 4px 7px}}.nav button{{border:0;background:transparent;color:inherit;min-height:58px;cursor:pointer;font-size:12px}}.nav b{{display:block;margin-bottom:5px;font-size:19px}}.nav .active{{color:#0b63f6}}button:focus-visible{{outline:3px solid #83d7ff;outline-offset:2px}}@media(max-width:380px){{.hero{{padding-left:12px;padding-right:12px}}.quick{{padding-left:12px;padding-right:12px}}.quick .icon{{width:52px;height:52px}}.quick span:last-child{{font-size:11px}}}}
</style></head><body><main class="home"><section class="hero"><div class="top"><div class="brand">Coxswain</div><button class="settings" type="button" aria-label="Settings">⚙</button></div><button class="connection" type="button"><span class="dot"></span><span><strong>WaterRower S4</strong><small>Connected via USB</small></span><span class="chev">›</span></button><div class="spacer"></div><section class="last" aria-label="Last workout"><div class="last-head"><strong>Last Workout</strong><button class="again" type="button" data-open="Live Row: Free">↻&nbsp; Row Again</button></div><div class="stats"><span class="primary-stat">2,000 m</span><span class="stat">18:42</span><span class="chev">›</span></div><div class="substats"><span><b>2:09</b> /500m</span><span><b>26</b> SPM</span><span><b>312</b> cal</span></div><div class="date">Sep 6, 2025</div></section><button class="free" type="button" data-open="Live Row: Free"><span>▶</span>Free Row</button></section><section class="quick"><h2>Quick Start</h2><div class="quick-grid"><button type="button" data-open="Duration Setup"><span class="icon"><svg viewBox="0 0 32 32"><circle cx="16" cy="17" r="11"/><path d="M16 17V10M12 3h8M23 7l3-3"/></svg></span><span>Duration</span></button><button type="button" data-open="Distance Setup"><span class="icon"><svg viewBox="0 0 32 32"><circle cx="8" cy="24" r="3"/><circle cx="24" cy="8" r="3"/><path d="M10 22c3-7 8 1 12-11M21 24h6M24 21v6"/></svg></span><span>Distance</span></button><button type="button" data-open="Quick Intervals"><span class="icon"><svg viewBox="0 0 32 32"><path d="M7 25V16M16 25V7M25 25V12"/></svg></span><span>Intervals</span></button><button type="button" data-open="My Programs"><span class="icon"><svg viewBox="0 0 32 32"><rect x="8" y="5" width="16" height="22" rx="2"/><path d="M12 11h8M12 16h8M12 21h6"/></svg></span><span>Programs</span></button></div></section><nav class="nav"><button class="active" type="button"><b>⌂</b>Home</button><button type="button" data-open="My Programs"><b>▥</b>Programs</button><button type="button"><b>◷</b>History</button><button type="button"><b>•••</b>More</button></nav></main><script>document.querySelectorAll('[data-open]').forEach(b=>b.onclick=()=>parent.postMessage({{type:'coxswain-open',name:b.dataset.open}},'*'));</script></body></html>'''

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
