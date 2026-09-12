from __future__ import annotations

import base64
import io
from html import escape
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path("/Users/brian/Documents/ChatGPT/Coxswain")
ATLAS = ROOT / "work/screen-atlas"
BOARD = ROOT / "work/screen-inventory-overview.png"
HTML = Path("/Users/brian/.codex/visualizations/2026/09/07/01a07dce-ee45-77f1-a36d-c2efbed2db4d/coxswain-screen-inventory.html")

CURRENT = [
    ("Home", "Launch", "01_home.png"),
    ("Quick Duration — None", "Quick Row", "02_quick_duration_none.png"),
    ("Quick Duration — Stroke Rate", "Quick Row", "03_quick_duration_stroke_rate.png"),
    ("Quick Duration — Speed", "Quick Row", "04_quick_duration_speed.png"),
    ("Quick Duration — Power", "Quick Row", "05_quick_duration_power.png"),
    ("Quick Distance", "Quick Row", "06_quick_distance.png"),
    ("Distance — Race Available", "Quick Row", "07_distance_race_available.png"),
    ("Distance — Race Enabled", "Quick Row", "08_distance_race_enabled.png"),
    ("Quick Intervals", "Quick Row", "09_quick_intervals.png"),
    ("Segment — Duration", "Intervals", "10_segment_duration.png"),
    ("Segment — Distance", "Intervals", "11_segment_distance.png"),
    ("Segment — Rest", "Intervals", "12_segment_rest.png"),
    ("Program Builder — Duration", "Programs", "13_program_duration.png"),
    ("Program Builder — Distance", "Programs", "14_program_distance.png"),
    ("Program Builder — Intervals", "Programs", "15_program_intervals.png"),
    ("My Programs", "Programs", "16_my_programs.png"),
    ("Workout Library", "Programs", "17_workout_library.png"),
    ("Live Row — Free", "Session", "18_live_row_free.png"),
    ("Live Row — Race", "Session", "19_live_row_race.png"),
    ("Program Detail — Duration", "Programs", "20_program_detail_duration.png"),
    ("Program Detail — Distance", "Programs", "21_program_detail_distance.png"),
    ("Program Detail — Intervals", "Programs", "22_program_detail_intervals.png"),
    ("Live Target — Duration", "Session", "23_live_target_duration.png"),
    ("Live Target — Distance", "Session", "24_live_target_distance.png"),
    ("Live Target — Intervals", "Session", "25_live_target_intervals.png"),
    ("Live Target — Stroke Rate", "Session", "26_live_target_goal_stroke_rate.png"),
    ("Live Target — Speed", "Session", "27_live_target_goal_speed.png"),
    ("Live Target — Power", "Session", "28_live_target_goal_power.png"),
    ("Live Target — Interval Rest", "Session", "29_live_target_interval_rest.png"),
    ("Race Reference Picker", "Session", "30_race_reference_picker.png"),
    ("Pause / End — Paused", "Session", "31_pause_end_paused.png"),
    ("Pause / End — End", "Session", "32_pause_end_confirm.png"),
    ("Pause / End — Discard", "Session", "33_pause_end_discard.png"),
    ("Complete — Distance", "Results", "34_workout_complete_distance.png"),
    ("Complete — Duration", "Results", "35_workout_complete_duration.png"),
    ("Complete — Intervals", "Results", "36_workout_complete_intervals.png"),
    ("Complete — Free Row", "Results", "37_workout_complete_free.png"),
    ("Complete — Race", "Results", "38_workout_complete_race.png"),
    ("Complete — Ended Early", "Results", "39_workout_complete_ended.png"),
]

PLANNED = [
    ("History", "History", None),
    ("Workout Details", "History", None),
    ("Race Comparison Details", "History", None),
    ("Connect Rower", "Device", None),
    ("Settings", "Settings", None),
    ("Display Customization", "Settings", None),
    ("Heart Rate Source", "Settings", None),
    ("Data / Export", "Settings", None),
    ("More / Diagnostics", "Settings", None),
]

SCREENS = [(name, group, path, "Current") for name, group, path in CURRENT] + [
    (name, group, path, "Placeholder") for name, group, path in PLANNED
]


def font(size: int, bold: bool = False):
    paths = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
    ]
    for path in paths:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            pass
    return ImageFont.load_default()


def fit_text(draw: ImageDraw.ImageDraw, value: str, width: int, initial: int = 22):
    size = initial
    while size > 13:
        candidate = font(size, True)
        if draw.textbbox((0, 0), value, font=candidate)[2] <= width:
            return candidate
        size -= 1
    return font(size, True)


def build_board() -> None:
    cols = 10
    tile_w, tile_h = 220, 360
    gap_x, gap_y = 18, 18
    margin = 44
    header = 150
    rows = (len(SCREENS) + cols - 1) // cols
    width = margin * 2 + cols * tile_w + (cols - 1) * gap_x
    height = header + rows * tile_h + (rows - 1) * gap_y + 44
    out = Image.new("RGB", (width, height), "#F5F7FA")
    draw = ImageDraw.Draw(out)
    draw.text((margin, 28), "Coxswain — Complete Screen Inventory", fill="#101828", font=font(38, True))
    draw.text(
        (margin, 82),
        f"{len(CURRENT)} current screens and interaction states  •  {len(PLANNED)} planned placeholders",
        fill="#53647C",
        font=font(22),
    )

    for index, (name, group, filename, status) in enumerate(SCREENS):
        row, col = divmod(index, cols)
        x = margin + col * (tile_w + gap_x)
        y = header + row * (tile_h + gap_y)
        fill = "#FFFFFF" if status == "Current" else "#E9EEF5"
        outline = "#B8C5D6" if status == "Current" else "#CBD5E1"
        draw.rounded_rectangle((x, y, x + tile_w, y + tile_h), radius=18, fill=fill, outline=outline, width=2)

        image_x, image_y = x + 54, y + 14
        image_w, image_h = 136, 272
        if filename:
            source = Image.open(ATLAS / filename).convert("RGB")
            source.thumbnail((image_w, image_h), Image.Resampling.LANCZOS)
            draw.rounded_rectangle((image_x - 3, image_y - 3, image_x + image_w + 3, image_y + image_h + 3), radius=10, fill="#0B1424")
            out.paste(source, (image_x, image_y))
        else:
            draw.rounded_rectangle((image_x, image_y, image_x + image_w, image_y + image_h), radius=18, fill="#F8FAFC", outline="#9EABBD", width=3)
            draw.rounded_rectangle((image_x + 14, image_y + 28, image_x + image_w - 14, image_y + 60), radius=7, fill="#DDE5EF")
            draw.rounded_rectangle((image_x + 14, image_y + 76, image_x + image_w - 14, image_y + 158), radius=10, fill="#E8EDF4")
            draw.rounded_rectangle((image_x + 14, image_y + 174, image_x + image_w - 14, image_y + 214), radius=9, fill="#D6E4FB")
            draw.text((image_x + 28, image_y + 230), "TO DESIGN", fill="#53647C", font=font(15, True))

        draw.text((x + 14, y + 297), name, fill="#16263E", font=fit_text(draw, name, tile_w - 28))
        badge_fill = "#DCE9FF" if status == "Current" else "#DDE3EA"
        badge_text = "#0B63F6" if status == "Current" else "#53647C"
        draw.rounded_rectangle((x + 14, y + 330, x + 92, y + 350), radius=10, fill=badge_fill)
        draw.text((x + 24, y + 331), status.upper(), fill=badge_text, font=font(12, True))
        group_width = draw.textbbox((0, 0), group, font=font(13))[2]
        draw.text((x + tile_w - 14 - group_width, y + 332), group, fill="#66768D", font=font(13))

    out.save(BOARD, optimize=True)


def thumbnail_data(filename: str) -> str:
    image = Image.open(ATLAS / filename).convert("RGB")
    image.thumbnail((320, 640), Image.Resampling.LANCZOS)
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=70, optimize=True)
    return "data:image/jpeg;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")


def build_html() -> None:
    tiles = []
    for index, (name, group, filename, status) in enumerate(SCREENS, 1):
        if filename:
            visual = f'<img src="{thumbnail_data(filename)}" alt="{escape(name)} screen">'
        else:
            visual = '<span class="cs-placeholder" aria-hidden="true"><span></span><span></span><span></span><b>TO DESIGN</b></span>'
        tiles.append(
            f'<button class="btn viz-tile cs-screen" type="button" data-status="{status.lower()}" data-index="{index}" '
            f'data-name="{escape(name)}" data-group="{escape(group)}">'
            f'{visual}<strong>{escape(name)}</strong><small>{escape(group)} · {status}</small></button>'
        )

    fragment = f'''<div id="coxswain-screen-inventory">
<style>
#coxswain-screen-inventory {{ color: light-dark(#101828,#eef4ff); }}
#coxswain-screen-inventory .cs-heading {{ display:flex; justify-content:space-between; align-items:flex-end; gap:16px; flex-wrap:wrap; margin-bottom:14px; }}
#coxswain-screen-inventory .cs-heading h2 {{ margin:0; }}
#coxswain-screen-inventory .cs-counts {{ display:flex; gap:8px; flex-wrap:wrap; }}
#coxswain-screen-inventory .cs-grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(152px,1fr)); gap:14px; margin-top:16px; }}
#coxswain-screen-inventory .cs-screen {{ display:flex; flex-direction:column; align-items:stretch; gap:7px; min-height:356px; padding:10px; text-align:left; overflow:hidden; }}
#coxswain-screen-inventory .cs-screen[hidden] {{ display:none; }}
#coxswain-screen-inventory .cs-screen img {{ display:block; width:132px; height:264px; object-fit:contain; object-position:top; margin:0 auto; border-radius:14px; background:#071322; }}
#coxswain-screen-inventory .cs-screen strong {{ line-height:1.2; }}
#coxswain-screen-inventory .cs-screen small {{ color:light-dark(#607089,#aab8cb); line-height:1.2; }}
#coxswain-screen-inventory .cs-placeholder {{ display:flex; flex-direction:column; gap:14px; justify-content:center; width:132px; height:264px; padding:15px; margin:0 auto; border:2px solid light-dark(#aab6c7,#53647c); border-radius:14px; background:light-dark(#eef2f7,#182438); color:light-dark(#607089,#aab8cb); text-align:center; }}
#coxswain-screen-inventory .cs-placeholder span {{ display:block; height:26px; border-radius:7px; background:light-dark(#dbe3ed,#2b3a51); }}
#coxswain-screen-inventory .cs-placeholder span:nth-child(2) {{ height:74px; }}
#coxswain-screen-inventory .cs-placeholder b {{ font-size:12px; letter-spacing:.08em; }}
#coxswain-screen-inventory .cs-dialog {{ width:min(760px,calc(100vw - 32px)); max-height:calc(100vh - 32px); padding:18px; border:0; border-radius:24px; background:light-dark(#fff,#111b2b); color:light-dark(#101828,#eef4ff); box-shadow:0 24px 70px #07132255; }}
#coxswain-screen-inventory .cs-dialog::backdrop {{ background:#071322b8; }}
#coxswain-screen-inventory .cs-dialog-head {{ display:flex; align-items:flex-start; justify-content:space-between; gap:16px; margin-bottom:12px; }}
#coxswain-screen-inventory .cs-dialog-head h3 {{ margin:0 0 3px; }}
#coxswain-screen-inventory .cs-dialog-stage {{ min-height:420px; display:grid; place-items:center; overflow:auto; background:light-dark(#eef2f7,#071322); border-radius:18px; padding:14px; }}
#coxswain-screen-inventory .cs-dialog-stage img {{ display:block; max-width:min(330px,100%); max-height:64vh; width:auto; height:auto; border-radius:18px; background:#071322; }}
#coxswain-screen-inventory .cs-dialog-stage .cs-placeholder {{ width:240px; height:480px; }}
#coxswain-screen-inventory .cs-dialog-actions {{ display:grid; grid-template-columns:1fr auto 1fr; align-items:center; gap:10px; margin-top:12px; }}
#coxswain-screen-inventory .cs-dialog-actions .cs-next {{ justify-self:end; }}
#coxswain-screen-inventory .cs-position {{ color:light-dark(#607089,#aab8cb); font-variant-numeric:tabular-nums; }}
@media (max-width:560px) {{ #coxswain-screen-inventory .cs-grid {{ grid-template-columns:repeat(2,minmax(0,1fr)); }} #coxswain-screen-inventory .cs-screen {{ min-height:325px; }} #coxswain-screen-inventory .cs-screen img,#coxswain-screen-inventory .cs-placeholder {{ width:116px; height:232px; }} }}
</style>
<div class="cs-heading"><div><h2>Complete screen inventory</h2><div class="text-muted">Every current screen and state, followed by placeholders for the remaining product flow.</div></div><div class="cs-counts"><span class="viz-badge">{len(CURRENT)} current</span><span class="viz-badge">{len(PLANNED)} placeholders</span></div></div>
<div class="viz-controls" aria-label="Filter screens"><button class="btn btn-primary" type="button" data-filter="all">All {len(SCREENS)}</button><button class="btn" type="button" data-filter="current">Current {len(CURRENT)}</button><button class="btn" type="button" data-filter="placeholder">Placeholders {len(PLANNED)}</button></div>
<div class="cs-grid">{''.join(tiles)}</div>
<dialog class="cs-dialog" aria-labelledby="cs-dialog-title">
  <div class="cs-dialog-head"><div><h3 id="cs-dialog-title"></h3><div class="text-muted cs-dialog-meta"></div></div><button class="btn btn-ghost cs-close" type="button" aria-label="Close enlarged screen">Close</button></div>
  <div class="cs-dialog-stage"></div>
  <div class="cs-dialog-actions"><button class="btn cs-prev" type="button">← Previous</button><span class="cs-position" aria-live="polite"></span><button class="btn btn-primary cs-next" type="button">Next →</button></div>
</dialog>
</div>
<script>
(()=>{{
const root=document.getElementById('coxswain-screen-inventory');
const filters=[...root.querySelectorAll('[data-filter]')];
const dialog=root.querySelector('.cs-dialog');
const stage=root.querySelector('.cs-dialog-stage');
let activeIndex=0;
const visibleTiles=()=>[...root.querySelectorAll('.cs-screen:not([hidden])')];
function openTile(tile){{
  const tiles=visibleTiles();
  activeIndex=Math.max(0,tiles.indexOf(tile));
  const selected=tiles[activeIndex];
  root.querySelector('#cs-dialog-title').textContent=selected.dataset.name;
  root.querySelector('.cs-dialog-meta').textContent=selected.dataset.group+' · '+(selected.dataset.status==='current'?'Current design':'Placeholder — still to design');
  stage.replaceChildren();
  const image=selected.querySelector('img');
  if(image){{const large=image.cloneNode(true);large.alt=selected.dataset.name+' enlarged screen';stage.append(large);}}
  else {{stage.append(selected.querySelector('.cs-placeholder').cloneNode(true));}}
  root.querySelector('.cs-position').textContent=(activeIndex+1)+' of '+tiles.length;
  root.querySelector('.cs-prev').disabled=activeIndex===0;
  root.querySelector('.cs-next').disabled=activeIndex===tiles.length-1;
  if(!dialog.open)dialog.showModal();
}}
root.querySelectorAll('.cs-screen').forEach(tile=>tile.addEventListener('click',()=>openTile(tile)));
filters.forEach(button=>button.addEventListener('click',()=>{{
  const wanted=button.dataset.filter;
  root.querySelectorAll('.cs-screen').forEach(tile=>tile.hidden=wanted!=='all'&&tile.dataset.status!==wanted);
  filters.forEach(item=>item.classList.toggle('btn-primary',item===button));
}}));
root.querySelector('.cs-close').addEventListener('click',()=>dialog.close());
root.querySelector('.cs-prev').addEventListener('click',()=>{{const tiles=visibleTiles();if(activeIndex>0)openTile(tiles[activeIndex-1]);}});
root.querySelector('.cs-next').addEventListener('click',()=>{{const tiles=visibleTiles();if(activeIndex<tiles.length-1)openTile(tiles[activeIndex+1]);}});
dialog.addEventListener('click',event=>{{if(event.target===dialog)dialog.close();}});
dialog.addEventListener('keydown',event=>{{
  const tiles=visibleTiles();
  if(event.key==='ArrowLeft'&&activeIndex>0)openTile(tiles[activeIndex-1]);
  if(event.key==='ArrowRight'&&activeIndex<tiles.length-1)openTile(tiles[activeIndex+1]);
}});
}})();
</script>
'''
    HTML.write_text(fragment, encoding="utf-8")
    print(f"wrote {HTML} ({HTML.stat().st_size} bytes)")


build_board()
build_html()
print(f"wrote {BOARD}")
