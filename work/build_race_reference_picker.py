from pathlib import Path


DEST = Path('/Users/brian/Documents/ChatGPT/Coxswain/outputs/Coxswain_Race_Reference_Picker.html')

html = r'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Coxswain Race Reference Picker</title>
<style>
html,body{margin:0;min-height:100%;background:#eef2f7;font-family:Arial,sans-serif;color:#10213f}*{box-sizing:border-box}button{font:inherit}.preview{width:min(430px,100%);margin:0 auto 10px;padding:4px;display:grid;grid-template-columns:repeat(3,1fr);background:#e8eef6;border-radius:24px}.preview button{border:0;min-height:40px;border-radius:20px;background:transparent;color:#53647c;font-weight:700;cursor:pointer}.preview button.active{background:#fff;color:#0b63f6;box-shadow:0 1px 3px #10213f1a}.phone{width:min(430px,100%);height:800px;margin:auto;display:flex;flex-direction:column;overflow:hidden;border-radius:24px;background:#f4f7fb}.phone header{height:64px;display:grid;grid-template-columns:48px 1fr 48px;align-items:center;padding:8px 12px}.phone header button{height:48px;border:0;border-radius:24px;background:transparent;color:#10213f;font-size:28px;cursor:pointer}.phone header strong{text-align:center;font-size:18px}.phone main{flex:1;min-height:0;overflow:auto;padding:2px 16px 20px}.context{padding:16px;border-radius:20px;background:#fff;box-shadow:0 1px 3px #10213f12}.context small,.section{display:block;color:#53647c;font-size:11px;font-weight:700;letter-spacing:.08em}.context h1{margin:7px 0 4px;font-size:23px}.context p{margin:0;color:#53647c;font-size:13px;line-height:18px}.section{margin:18px 4px 8px}.list{display:grid;gap:8px}.option{width:100%;min-height:92px;padding:13px 14px;display:grid;grid-template-columns:24px 1fr auto;gap:11px;align-items:center;border:2px solid transparent;border-radius:18px;background:#fff;color:#10213f;text-align:left;cursor:pointer;box-shadow:0 1px 3px #10213f10}.option.active{border-color:#0b63f6;background:#eef6ff}.radio{width:21px;height:21px;border:2px solid #aab7c8;border-radius:50%;display:grid;place-items:center}.option.active .radio{border-color:#0b63f6}.option.active .radio:after{content:'';width:11px;height:11px;border-radius:50%;background:#0b63f6}.copy strong,.copy span{display:block}.copy strong{font-size:14px}.copy span{margin-top:4px;color:#53647c;font-size:12px}.result{text-align:right}.result strong,.result span{display:block;white-space:nowrap}.result strong{font-size:20px}.result span{margin-top:4px;color:#53647c;font-size:11px}.best{display:inline-flex!important;width:max-content;margin-top:6px!important;padding:3px 7px;border-radius:10px;background:#dcebff;color:#0b63f6!important;font-size:9px!important;font-weight:700;letter-spacing:.06em}.unavailable-toggle{width:100%;min-height:46px;margin-top:12px;padding:0 4px;display:flex;justify-content:space-between;align-items:center;border:0;background:transparent;color:#53647c;font-weight:700;cursor:pointer}.unavailable[hidden]{display:none}.unavailable{display:grid;gap:7px}.disabled{min-height:72px;padding:11px 14px;border-radius:16px;background:#e8eef6;color:#53647c;display:grid;grid-template-columns:1fr auto;gap:8px}.disabled strong,.disabled span{display:block}.disabled strong{font-size:13px}.disabled span{margin-top:4px;font-size:11px}.why{align-self:center;font-size:11px;font-weight:700;color:#8a4b54}.phone footer{flex:0 0 auto;padding:10px 16px 16px;display:grid;grid-template-columns:120px 1fr;gap:8px;border-top:1px solid #e0e7f0;background:#f4f7fb}.phone footer button{min-height:56px;border-radius:28px;font-weight:700;cursor:pointer}.cancel{border:1px solid #0b63f6;background:#fff;color:#0b63f6}.start{border:0;background:#0b63f6;color:#fff}.selection{grid-column:1/-1;margin:0 4px;color:#53647c;font-size:11px;text-align:center}.notice{min-height:18px;margin:10px 4px 0;color:#53647c;font-size:12px;text-align:center}button:focus-visible{outline:3px solid #83d7ff;outline-offset:2px}@media(max-width:360px){.phone footer{grid-template-columns:100px 1fr}.option{padding-left:10px;padding-right:10px}.result strong{font-size:18px}}
</style>
</head>
<body>
<div class="preview" aria-label="Program type preview"><button class="active" data-type="distance">Distance</button><button data-type="duration">Duration</button><button data-type="intervals">Intervals</button></div>
<div class="phone">
 <header><button class="back" type="button" aria-label="Back">‹</button><strong>Choose race</strong><span></span></header>
 <main>
  <section class="context"><small>RACING PROGRAM</small><h1></h1><p></p></section>
  <span class="section">COMPATIBLE WORKOUTS</span>
  <div class="list" role="radiogroup" aria-label="Compatible completed workouts"></div>
  <button class="unavailable-toggle" type="button" aria-expanded="false"><span>Unavailable history</span><span>Show 2</span></button>
  <div class="unavailable" hidden></div>
  <p class="notice" aria-live="polite"></p>
 </main>
 <footer><p class="selection"></p><button class="cancel" type="button">Cancel</button><button class="start" type="button">Start race</button></footer>
</div>
<script>
(()=>{
const models={
 distance:{title:'5,000-meter row',rule:'Only completed 5,000 m workouts can be compared.',unit:'time',items:[['Sep 6, 2025','20:42','2:04 /500 m','BEST'],['Aug 29, 2025','21:06','2:07 /500 m',''],['Aug 18, 2025','21:31','2:09 /500 m','']],unavailable:[['2,000-meter row','Different target distance'],['5K interval workout','Different workout structure']]},
 duration:{title:'30-minute row',rule:'Only completed 30-minute workouts can be compared.',unit:'distance',items:[['Sep 8, 2025','7,214 m','2:04 /500 m','BEST'],['Aug 31, 2025','7,085 m','2:07 /500 m',''],['Aug 20, 2025','6,944 m','2:10 /500 m','']],unavailable:[['60-minute row','Different target duration'],['Open row · 30:00','Free rows are not fixed-duration programs']]},
 intervals:{title:'Timed intervals',rule:'Only workouts with the exact same ordered Row and Rest segments can be compared.',unit:'work distance',items:[['Sep 7, 2025','2,430 m','5:00 row · 1:00 rest · 5:00 row','BEST'],['Aug 27, 2025','2,382 m','Same segment order',''],['Aug 14, 2025','2,341 m','Same segment order','']],unavailable:[['4 × 500 m','Different segment types'],['5:00 row · 2:00 rest · 5:00 row','Different rest duration']]}
};
let type='distance',selected=0;const list=document.querySelector('.list'),unavailable=document.querySelector('.unavailable'),toggle=document.querySelector('.unavailable-toggle');
function render(){const model=models[type];document.querySelector('.context h1').textContent=model.title;document.querySelector('.context p').textContent=model.rule;document.querySelectorAll('.preview button').forEach(button=>button.classList.toggle('active',button.dataset.type===type));list.replaceChildren();model.items.forEach((item,index)=>{const button=document.createElement('button');button.type='button';button.className='option'+(index===selected?' active':'');button.setAttribute('role','radio');button.setAttribute('aria-checked',String(index===selected));button.innerHTML='<span class="radio"></span><span class="copy"><strong>'+item[0]+'</strong><span>'+item[2]+'</span>'+(item[3]?'<span class="best">'+item[3]+'</span>':'')+'</span><span class="result"><strong>'+item[1]+'</strong><span>'+model.unit+'</span></span>';button.onclick=()=>{selected=index;render()};list.append(button)});unavailable.replaceChildren();model.unavailable.forEach(item=>{const row=document.createElement('div');row.className='disabled';row.innerHTML='<span><strong>'+item[0]+'</strong><span>'+item[1]+'</span></span><span class="why">Not comparable</span>';unavailable.append(row)});document.querySelector('.selection').textContent='Selected · '+model.items[selected][0]+' · '+model.items[selected][1];}
document.querySelectorAll('.preview button').forEach(button=>button.onclick=()=>{type=button.dataset.type;selected=0;render()});toggle.onclick=()=>{const open=toggle.getAttribute('aria-expanded')==='true';toggle.setAttribute('aria-expanded',String(!open));toggle.lastElementChild.textContent=open?'Show 2':'Hide';unavailable.hidden=open};document.querySelector('.back').onclick=document.querySelector('.cancel').onclick=()=>top.postMessage({type:'coxswain-open',name:'Program Detail / Start'},'*');document.querySelector('.start').onclick=()=>top.postMessage({type:'coxswain-open',name:'Live Row: Race'},'*');render();
})();
</script>
</body>
</html>'''

DEST.write_text(html, encoding='utf-8')
print(f'wrote {DEST}')
