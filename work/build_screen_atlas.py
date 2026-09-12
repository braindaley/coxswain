from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json, runpy, textwrap

OUT = Path('/Users/brian/Documents/ChatGPT/Coxswain/work/screen-atlas')
OUT.mkdir(parents=True, exist_ok=True)
S, W, H = 2, 430, 860
REG = '/System/Library/Fonts/Supplemental/Arial.ttf'
BOLD = '/System/Library/Fonts/Supplemental/Arial Bold.ttf'
C = {'bg':'#F4F7FB','ink':'#10213F','muted':'#53647C','blue':'#0B63F6','pale':'#DCEBFF','line':'#DCE4EE','white':'#FFFFFF','navy':'#042C3D','green':'#19A463','red':'#BA1A1A'}

def sc(v): return int(v*S)
def font(n,b=False): return ImageFont.truetype(BOLD if b else REG, sc(n))
def canvas(bg=None):
    im=Image.new('RGB',(sc(W),sc(H)),bg or C['bg']); return im,ImageDraw.Draw(im)
def rr(d,box,r=16,fill=None,outline=None,width=1): d.rounded_rectangle(tuple(sc(x) for x in box),sc(r),fill=fill,outline=outline,width=sc(width))
def line(d,xy,fill,width=1): d.line(tuple(sc(x) for x in xy),fill=fill,width=sc(width))
def txt(d,xy,s,n=14,b=False,fill=None,anchor=None): d.text((sc(xy[0]),sc(xy[1])),s,font=font(n,b),fill=fill or C['ink'],anchor=anchor)
def centered(d,box,s,n=14,b=False,fill=None):
    x1,y1,x2,y2=box; txt(d,((x1+x2)/2,(y1+y2)/2),s,n,b,fill,'mm')
def goal_icon(d,cx,cy,kind,color):
    cx,cy=sc(cx),sc(cy); w=sc(2)
    if kind=='None':
        d.ellipse((cx-sc(9),cy-sc(9),cx+sc(9),cy+sc(9)),outline=color,width=w)
        d.line((cx-sc(7),cy+sc(7),cx+sc(7),cy-sc(7)),fill=color,width=w)
    elif kind=='Stroke rate':
        pts=[(cx-sc(11),cy),(cx-sc(6),cy),(cx-sc(3),cy-sc(7)),(cx+sc(1),cy+sc(8)),(cx+sc(5),cy-sc(4)),(cx+sc(8),cy),(cx+sc(12),cy)]
        d.line(pts,fill=color,width=w,joint='curve')
    elif kind=='Speed':
        d.arc((cx-sc(11),cy-sc(8),cx+sc(11),cy+sc(14)),180,360,fill=color,width=w)
        d.line((cx,cy+sc(3),cx+sc(7),cy-sc(4)),fill=color,width=w)
        d.ellipse((cx-sc(2),cy+sc(1),cx+sc(2),cy+sc(5)),fill=color)
    else:
        pts=[(cx+sc(2),cy-sc(11)),(cx-sc(7),cy+sc(2)),(cx-sc(1),cy+sc(2)),(cx-sc(3),cy+sc(11)),(cx+sc(8),cy-sc(3)),(cx+sc(2),cy-sc(3))]
        d.polygon(pts,fill=color)
def wrapped(d,xy,s,n=13,width=42,fill=None,spacing=4):
    d.multiline_text((sc(xy[0]),sc(xy[1])),'\n'.join(textwrap.wrap(s,width)),font=font(n),fill=fill or C['ink'],spacing=sc(spacing))
def status(d,dark=False):
    col=C['white'] if dark else C['ink']; txt(d,(18,12),'9:30',12,True,col); txt(d,(375,12),'●  ▲',11,True,col)
def header(d,title,dark=False,subtitle=None):
    status(d,dark); col=C['white'] if dark else C['ink']; txt(d,(18,47),'‹',34,False,col); txt(d,(58,54),title,23,True,col)
    if subtitle: txt(d,(58,82),subtitle,12,False,C['white'] if dark else C['muted'])
def label(d,y,s): txt(d,(18,y),s.upper(),11,True,C['muted'])
def chip(d,x,y,w,s,selected=False): rr(d,(x,y,x+w,y+38),19,C['pale'] if selected else C['white'],C['blue'] if selected else C['line'],1); centered(d,(x,y,x+w,y+38),s,12,True if selected else False,C['blue'] if selected else C['ink'])
def segmented(d,y,items,selected):
    rr(d,(16,y,414,y+46),23,'#E8EEF6'); w=398/len(items)
    for i,s in enumerate(items):
        x=16+i*w
        if s==selected: rr(d,(x+3,y+3,x+w-3,y+43),20,C['white']);
        centered(d,(x,y,x+w,y+46),s,12,s==selected,C['blue'] if s==selected else C['muted'])
def cta(d,y,primary,secondary=None):
    if secondary:
        rr(d,(16,y,168,y+54),27,C['white'],C['blue']); centered(d,(16,y,168,y+54),secondary,13,True,C['blue'])
        rr(d,(176,y,414,y+54),27,C['blue']); centered(d,(176,y,414,y+54),primary,14,True,C['white'])
    else: rr(d,(16,y,414,y+54),27,C['blue']); centered(d,(16,y,414,y+54),primary,14,True,C['white'])
def target_card(d,y,value,unit,presets,selected):
    rr(d,(16,y,414,y+132),18,C['white']); txt(d,(215,y+35),value,44,True,C['ink'],'ma'); txt(d,(215,y+72),unit,12,False,C['muted'],'ma')
    x=23; widths=(384-5*6)/6
    for p in presets:
        rr(d,(x,y+88,x+widths,y+124),9,C['blue'] if p==selected else C['white'],C['blue'] if p==selected else C['line'])
        centered(d,(x,y+88,x+widths,y+124),p,10,True,C['white'] if p==selected else C['ink']); x+=widths+6
def goal_component(d,y,goal='None'):
    label(d,y,'Set goal (optional)'); y+=22; rr(d,(16,y,414,y+95),17,C['white'])
    goals=[('None','No target'),('Stroke rate','SPM'),('Speed','/500 m'),('Power','Watts')]; w=94
    for i,(name,sub) in enumerate(goals):
        x=20+i*98; sel=name==goal; rr(d,(x,y+8,x+w,y+86),11,'#EEF6FF' if sel else C['white'],C['blue'] if sel else C['line'],2 if sel else 1)
        goal_icon(d,x+w/2,y+27,name,C['blue'] if sel else C['ink']); centered(d,(x,y+41,x+w,y+61),name,9,True,C['blue'] if sel else C['ink']); centered(d,(x,y+61,x+w,y+78),sub,8,False,C['muted'])
    if goal=='None': return y+103
    values={'Stroke rate':('Target Stroke Rate','26','SPM','14','40'),'Speed':('Target Speed','2:10','/500 m','1:30','4:00'),'Power':('Target Power','180','W','50','400')}
    title,val,unit,lo,hi=values[goal]; rr(d,(16,y+103,414,y+184),14,'#F7FAFD',C['line']); txt(d,(29,y+117),title,12,True); txt(d,(370,y+117),val,15,True,anchor='ra'); txt(d,(376,y+119),unit,10,False,C['muted'])
    rr(d,(29,y+143,65,y+179),18,C['pale']); centered(d,(29,y+143,65,y+179),'−',18,False,C['blue']); line(d,(78,y+160,348,y+160),C['blue'],3); rr(d,(346,y+143,382,y+179),18,C['pale']); centered(d,(346,y+143,382,y+179),'+',18,False,C['blue']); txt(d,(78,y+166),lo,8,False,C['muted']); txt(d,(348,y+166),hi,8,False,C['muted'],'ra')
    return y+192
def summary(d,y,kind,target,guide):
    label(d,y,'Workout summary'); y+=22; rr(d,(16,y,414,y+89),17,C['white']); txt(d,(31,y+15),kind.upper(),10,True,C['muted']); txt(d,(31,y+34),target,22,True); txt(d,(31,y+64),guide,11,False,C['muted']); return y+98
def save(im,name): im.save(OUT/name,optimize=True)

def setup(name,kind='Duration',goal='None',program=False,race=None):
    im,d=canvas(); header(d,'New program' if program else kind+' workout'); y=98
    if program: label(d,y,'Program name'); rr(d,(16,y+20,414,y+68),12,C['white'],C['line']); txt(d,(30,y+35),{'Duration':'60-minute row','Distance':'5K benchmark','Intervals':'500 m repeats'}[kind],15); y+=82
    label(d,y,'Program type' if program else 'Workout type'); segmented(d,y+20,['Duration','Distance','Intervals'],kind); y+=79
    if kind=='Duration': label(d,y,'Target'); target_card(d,y+20,'60','minutes',['10','20','30','45','60','90'],'60'); y+=166; target='60 minutes'
    elif kind=='Distance': label(d,y,'Target'); target_card(d,y+20,'5,000','meters',['500','1K','2K','5K','6K','10K'],'5K'); y+=166; target='5,000 m'
    else:
        label(d,y,'Interval segments'); y+=20; rr(d,(16,y,414,y+142),17,C['white']); rows=[('1','Distance','500 m'),('2','Rest','1:00'),('3','Distance','500 m')]
        for i,(n,k,v) in enumerate(rows): txt(d,(31,y+17+i*37),n,11,True,C['muted']); txt(d,(62,y+13+i*37),k,13,True); txt(d,(330,y+13+i*37),v,13,False,C['muted'],'ra'); txt(d,(391,y+13+i*37),'›',20,False,C['muted'],'ra')
        centered(d,(16,y+112,414,y+142),'+  Add segment',12,True,C['blue']); y+=151; target='3 segments · 500 m / 1:00 / 500 m'
    if race is not None and kind=='Distance' and not program:
        label(d,y,'Race your best'); y+=20; rr(d,(16,y,414,y+(112 if race else 78)),17,C['white']); rr(d,(30,y+17,68,y+55),19,C['pale']); centered(d,(30,y+17,68,y+55),'B',15,True,C['blue']); txt(d,(80,y+15),'Race your best time',14,True); txt(d,(80,y+37),'Fastest compatible workout',11,False,C['muted']); rr(d,(356,y+20,400,y+46),13,C['blue'] if race else '#C7D1DE'); rr(d,(377 if race else 359,y+23,397 if race else 379,y+43),10,C['white'])
        if race: line(d,(80,y+64,400,y+64),C['line']); txt(d,(80,y+76),'5,000 m · 20:42',13,True); txt(d,(80,y+95),'Average 2:04 /500 m · Sep 6',10,False,C['muted']); txt(d,(390,y+76),'Change',11,True,C['blue'],'ra')
        y+=120 if race else 88
    if not race: y=goal_component(d,y,goal)
    guide='Race best: 20:42' if race else ('No performance goal' if goal=='None' else {'Stroke rate':'Goal 26 SPM','Speed':'Goal 2:10 /500 m','Power':'Goal 180 W'}[goal])
    y=summary(d,y,kind,target,guide); cta(d,min(792,y+4),'Save program' if program else 'Start workout',None if program else 'Save as program'); save(im,name)

def segment_editor(name,kind):
    im,d=canvas(); header(d,'Add segment'); y=105; label(d,y,'Segment type'); segmented(d,y+20,['Duration','Distance','Rest'],kind); y+=83; label(d,y,'Target'); y+=21
    if kind=='Distance': target_card(d,y,'500','meters',['100','250','500','1K','2K','5K'],'500'); value='500 m'
    elif kind=='Rest': target_card(d,y,'1:00','min:sec',['0:15','0:30','0:45','1:00','1:30','2:00'],'1:00'); value='Rest · 1:00'
    else: target_card(d,y,'5:00','min:sec',['0:30','1:00','2:00','3:00','5:00','10:00'],'5:00'); value='Duration · 5:00'
    y+=166; rr(d,(16,y,414,y+76),17,C['white']); txt(d,(30,y+15),'SEGMENT SUMMARY',10,True,C['muted']); txt(d,(30,y+39),value,19,True); cta(d,786,'Save segment','Cancel'); save(im,name)

def programs(name,library=False):
    im,d=canvas(); header(d,'Programs'); segmented(d,98,['My Programs','Workout Library'],'Workout Library' if library else 'My Programs'); y=158
    if not library: cta(d,y,'+  Create program'); y+=70
    label(d,y,'Workout library' if library else 'My programs'); y+=22
    rows=[('Steady endurance','60 min · No goal'),('5K benchmark','5,000 m · Goal 26 SPM'),('Power intervals','500 m row · 1:00 rest · 500 m row')] if library else [('60-minute row','60 min · No goal'),('5,000-meter row','5,000 m · Goal 26 SPM'),('Timed intervals','5:00 row · 1:00 rest · 5:00 row')]
    for title,sub in rows:
        rr(d,(16,y,414,y+112),18,C['white']); txt(d,(30,y+14),title,16,True); txt(d,(30,y+40),sub,12,False,C['muted']); txt(d,(389,y+15),'...' if not library else '',15,True,C['muted'],'ra'); txt(d,(30,y+75),'View program',12,True,C['blue']); rr(d,(319,y+62,400,y+104),21,C['blue']); centered(d,(319,y+62,400,y+104),'Start',12,True,C['white']); y+=122
    nav(d,'Programs'); save(im,name)
def nav(d,active):
    rr(d,(0,798,430,860),0,C['white']); items=['Home','Programs','History','More'];
    for i,s in enumerate(items): x=54+i*107; txt(d,(x,817),'●',11,False,C['blue'] if s==active else C['muted'],'ma'); txt(d,(x,840),s,10,s==active,C['blue'] if s==active else C['muted'],'ma')
def home(name):
    im,d=canvas('#073348'); status(d,True); txt(d,(24,58),'COXSWAIN',20,True,C['white']); rr(d,(16,102,414,174),18,C['white']); txt(d,(33,121),'●',16,True,C['green']); txt(d,(62,116),'WaterRower S4',16,True); txt(d,(62,141),'Connected via USB',12,False,C['muted']); rr(d,(16,195,414,410),20,'#1A5265'); centered(d,(16,195,414,410),'ROWER IMAGE',15,True,'#A6C8D4'); rr(d,(28,330,402,402),16,C['white']); txt(d,(43,344),'Last Workout',13,True); txt(d,(43,366),'2,000 m   18:42',18,True); txt(d,(315,354),'Row Again',12,True,C['blue']); rr(d,(16,430,414,490),30,C['blue']); centered(d,(16,430,414,490),'Free Row',18,True,C['white']); txt(d,(18,515),'Quick Start',14,True,C['white']);
    for i,s in enumerate(['Duration','Distance','My Programs','Library']): x=54+i*107; rr(d,(30+i*107,545,78+i*107,593),24,C['white']); centered(d,(30+i*107,545,78+i*107,593),str(i+1),13,True,C['ink']); txt(d,(x,612),s,10,False,C['white'],'ma')
    nav(d,'Home'); save(im,name)
def live(name,race=False,target=None,goal=None):
    target_titles={'Duration':'Duration workout','Distance':'Distance workout','Intervals':'Timed intervals'}
    title='Race your best' if race else target_titles.get(target,'Free Row')
    im,d=canvas(C['navy']); status(d,True); txt(d,(20,55),title,22,True,C['white']); txt(d,(247,60),'● Connected',11,True,C['green']); txt(d,(402,58),'Edit display',11,True,'#DCEBFF','ra');
    goal_values={'Stroke rate':('Stroke rate','24','SPM','−2','below','Target 26 SPM'),'Speed':('Speed','13.8','km/h','−0.7','below','Target 14.5 km/h'),'Power':('Power','188','W','+8','above','Target 180 W')}
    vals=[('Distance','2,450','m'),('Duration','12:34','elapsed'),('Split','2:08','/500 m'),goal_values.get(goal,('Stroke rate','26','SPM',None,None))]
    y=100
    compact=race or target is not None; metric_h=112 if compact else 132
    metric_gap=10 if compact else 12
    for i,metric in enumerate(vals):
        lab,val,unit=metric[:3]; variance=metric[3] if len(metric)>3 else None; tone=metric[4] if len(metric)>4 else None
        rr(d,(16,y,414,y+metric_h),16,'#0B3B4E','#1C566B')
        if variance:
            tone_fill='#7A2836' if tone=='below' else '#126B4D'
            rr(d,(16,y,414,y+metric_h),16,tone_fill)
            centered(d,(16,y+14,414,y+78),variance,56,True,C['white'])
            centered(d,(16,y+78,414,y+105),f'{lab.upper()} · TO TARGET',10,True,C['white'])
        else:
            txt(d,(32,y+14),lab.upper(),11,True,'#A6C8D4'); txt(d,(215,y+metric_h//2+6),val,46 if race else 52,True,C['white'],'mm'); txt(d,(386,y+metric_h-26),unit,11,False,'#A6C8D4','ra')
        y+=metric_h+metric_gap
    if race:
        rr(d,(16,600,414,704),17,'#123F51'); txt(d,(30,615),'RACE PROGRESS',11,True,C['white']); txt(d,(400,615),'24 m ahead',11,True,'#83D7FF','ra')
        for lane_y,lane,label_text,progress,color,value_text in [(650,'You','You',.62,C['blue'],'2,450 m'),(680,'Best','Best',.59,'#83D7FF','History')]:
            txt(d,(30,lane_y-8),label_text,11,True,C['white'] if lane=='You' else '#BFEAFF'); rr(d,(88,lane_y,328,lane_y+4),2,'#53647C'); rr(d,(88,lane_y,88+int(240*progress),lane_y+4),2,color); rr(d,(82+int(240*progress),lane_y-5,94+int(240*progress),lane_y+7),6,color,C['white']); txt(d,(400,lane_y-9),value_text,10,False,'#CAD4E1','ra')
        footer_y=720
    elif target:
        models={'Duration':('21%','47:26 remaining','12:34 elapsed','60:00 target',.21),'Distance':('49%','2,550 m remaining','2,450 m complete','5,000 m target',.49),'Intervals':('2:46 / 5:00','Row 1 · 2:14 remaining','','',.55)}
        percent,primary,left,right,progress=models[target]; rr(d,(16,600,414,704),17,'#123F51'); txt(d,(30,614),'WORKOUT PROGRESS',11,True,C['white']); txt(d,(400,614),percent,11,True,'#83D7FF','ra'); txt(d,(30,638),primary,17,True,C['white']); rr(d,(30,668,400,674),3,'#53647C'); rr(d,(30,668,30+int(370*progress),674),3,C['blue']); txt(d,(30,682),left,10,False,'#CAD4E1'); txt(d,(400,682),right,10,False,'#CAD4E1','ra'); footer_y=720
        if target == 'Intervals':
            # Replace detailed segment chips with one color-coded sequence line.
            rr(d,(30,661,400,704),0,'#123F51')
            bar_left,bar_right,bar_y=30,400,674; gap=3; usable=(bar_right-bar_left)-gap*3; cursor=bar_left
            for kind,weight in [('row',5),('rest',1),('row',5),('rest',1)]:
                width=round(usable*weight/12); fill=C['blue'] if kind=='row' else '#83D7FF'; rr(d,(cursor,bar_y,cursor+width,bar_y+10),5,fill); cursor+=width+gap
            marker_x=bar_left+int((bar_right-bar_left)*.23); rr(d,(marker_x-1,bar_y-4,marker_x+2,bar_y+14),1,C['white'])
    else: footer_y=704
    rr(d,(16,footer_y,204,footer_y+56),28,C['blue']); centered(d,(16,footer_y,204,footer_y+56),'Pause',14,True,C['white']); rr(d,(212,footer_y,414,footer_y+56),28,'#DCEBFF'); centered(d,(212,footer_y,414,footer_y+56),'End session',14,True,C['ink']); save(im,name)

def live_interval_rest(name):
    im,d=canvas(C['navy']); status(d,True); txt(d,(20,55),'Rest interval',22,True,C['white']); txt(d,(400,60),'● Connected',11,True,C['green'],'ra')
    rr(d,(16,100,414,582),18,'#0B3B4E','#1C566B'); centered(d,(16,205,414,390),'0:42',92,True,C['white']); centered(d,(16,390,414,425),'REST REMAINING',14,True,'#CAD4E1'); centered(d,(16,442,414,475),'Next · Row 5:00',13,True,'#83D7FF')
    rr(d,(16,600,414,704),17,'#123F51'); txt(d,(30,614),'WORKOUT PROGRESS',11,True,C['white']); txt(d,(400,614),'0:18 / 1:00',11,True,'#83D7FF','ra'); txt(d,(30,638),'Rest 1 · 0:42 remaining',17,True,C['white'])
    bar_left,bar_right,bar_y=30,400,674; gap=3; usable=(bar_right-bar_left)-gap*3; cursor=bar_left
    for kind,weight in [('row',5),('rest',1),('row',5),('rest',1)]:
        width=round(usable*weight/12); fill=C['blue'] if kind=='row' else '#83D7FF'; rr(d,(cursor,bar_y,cursor+width,bar_y+10),5,fill); cursor+=width+gap
    marker_x=bar_left+int((bar_right-bar_left)*.44); rr(d,(marker_x-1,bar_y-4,marker_x+2,bar_y+14),1,C['white'])
    rr(d,(16,720,204,776),28,C['blue']); centered(d,(16,720,204,776),'Pause',14,True,C['white']); rr(d,(212,720,414,776),28,'#DCEBFF'); centered(d,(212,720,414,776),'End session',14,True,C['ink']); save(im,name)

def race_reference_picker(name):
    im,d=canvas(); status(d); txt(d,(18,54),'‹',34,False,C['ink']); centered(d,(52,43,378,80),'Choose race',18,True,C['ink'])
    rr(d,(16,96,414,180),18,C['white']); txt(d,(30,111),'RACING PROGRAM',10,True,C['muted']); txt(d,(30,132),'5,000-meter row',21,True,C['ink']); txt(d,(30,158),'Only completed 5,000 m workouts can be compared.',10,False,C['muted'])
    label(d,202,'Compatible workouts'); y=226
    workouts=[('Sep 6, 2025','2:04 /500 m','20:42','BEST'),('Aug 29, 2025','2:07 /500 m','21:06',''),('Aug 18, 2025','2:09 /500 m','21:31','')]
    for index,(date,pace,result,best) in enumerate(workouts):
        selected=index==0; rr(d,(16,y,414,y+96),18,'#EEF6FF' if selected else C['white'],C['blue'] if selected else C['line'],2 if selected else 1)
        d.ellipse((sc(30),sc(y+35),sc(50),sc(y+55)),outline=C['blue'] if selected else '#AAB7C8',width=sc(2))
        if selected: d.ellipse((sc(35),sc(y+40),sc(45),sc(y+50)),fill=C['blue'])
        txt(d,(64,y+16),date,13,True,C['ink']); txt(d,(64,y+42),pace,11,False,C['muted'])
        if best: rr(d,(64,y+63,105,y+83),10,C['pale']); centered(d,(64,y+63,105,y+83),best,8,True,C['blue'])
        txt(d,(396,y+21),result,19,True,C['ink'],'ra'); txt(d,(396,y+50),'time',9,False,C['muted'],'ra'); y+=106
    txt(d,(22,y+6),'Unavailable history',12,True,C['muted']); txt(d,(404,y+6),'Show 2',11,True,C['blue'],'ra')
    txt(d,(215,711),'Selected · Sep 6, 2025 · 20:42',10,False,C['muted'],'ma')
    rr(d,(16,730,134,786),28,C['white'],C['blue']); centered(d,(16,730,134,786),'Cancel',13,True,C['blue']); rr(d,(142,730,414,786),28,C['blue']); centered(d,(142,730,414,786),'Start race',14,True,C['white']); save(im,name)

def pause_end(name,state='paused'):
    im,d=canvas(C['navy']); status(d,True); txt(d,(20,54),'●  WaterRower S4',11,True,C['green']); rr(d,(337,42,414,74),16,C['navy'],'#F3CF67'); centered(d,(337,42,414,74),'PAUSED',10,True,'#FFE081')
    centered(d,(16,88,414,130),'Workout paused',25,True,C['white']); centered(d,(16,126,414,150),'Metrics and workout progress are frozen.',11,False,'#B5D3DC')
    values=[('2,450','DISTANCE · M'),('12:34','DURATION'),('2:08','SPLIT · /500 M'),('26','STROKE RATE'),('188','POWER · W'),('142','HEART RATE · BPM')]
    top=164; cell_h=174
    for i,(value,label_text) in enumerate(values):
        row,col=divmod(i,2); x1=16+col*199; x2=215+col*199; y1=top+row*cell_h; y2=y1+cell_h
        rr(d,(x1,y1,x2,y2),0,C['navy'],'#31505D'); centered(d,(x1,y1+28,x2,y1+112),value,40,False,C['white']); centered(d,(x1,y1+111,x2,y1+143),label_text,9,True,'#CAD4E1')
    rr(d,(16,704,204,760),28,C['blue']); centered(d,(16,704,204,760),'Resume',14,True,C['white']); rr(d,(212,704,414,760),28,C['pale']); centered(d,(212,704,414,760),'End session',14,True,C['ink'])
    if state!='paused':
        overlay=Image.new('RGBA',im.size,(0,16,25,185)); im=Image.alpha_composite(im.convert('RGBA'),overlay).convert('RGB'); d=ImageDraw.Draw(im)
        if state=='confirm':
            rr(d,(0,468,430,860),28,C['bg']); rr(d,(193,480,237,485),3,'#CAD4E1'); txt(d,(20,510),'End this workout?',25,True,C['ink']); wrapped(d,(20,550),'Your completed distance and time will be saved to History.',13,48,C['muted']); rr(d,(20,614,209,684),16,'#E8EEF6'); txt(d,(34,629),'2,450 m',20,True); txt(d,(34,657),'DISTANCE',9,True,C['muted']); rr(d,(217,614,410,684),16,'#E8EEF6'); txt(d,(231,629),'12:34',20,True); txt(d,(231,657),'DURATION',9,True,C['muted']); rr(d,(20,704,410,760),28,C['blue']); centered(d,(20,704,410,760),'End and save',14,True,C['white']); rr(d,(20,770,410,826),28,C['white'],C['blue']); centered(d,(20,770,410,826),'Stay paused',14,True,C['blue']); centered(d,(20,827,410,853),'Discard workout',11,True,'#A52A37')
        else:
            rr(d,(0,566,430,860),28,C['bg']); rr(d,(193,578,237,583),3,'#CAD4E1'); txt(d,(20,608),'Discard this workout?',25,True,C['ink']); wrapped(d,(20,648),'This workout will not be saved to History. This cannot be undone.',13,46,C['muted']); rr(d,(20,718,410,774),28,C['red']); centered(d,(20,718,410,774),'Discard workout',14,True,C['white']); rr(d,(20,784,410,840),28,C['white'],C['blue']); centered(d,(20,784,410,840),'Go back',14,True,C['blue'])
    save(im,name)

def workout_complete(name,kind='distance'):
    models={
        'distance':('5,000-METER ROW','20:42','','TOTAL TIME','NEW BEST','18 sec faster than your previous best',[('2:04','AVERAGE · /500 M'),('26','AVERAGE STROKE RATE'),('188','AVERAGE POWER · W'),('312','ENERGY · KCAL')]),
        'duration':('30-MINUTE ROW','7,214','m','DISTANCE','NEW BEST','129 m farther than your previous best',[('2:05','AVERAGE · /500 M'),('27','AVERAGE STROKE RATE'),('192','AVERAGE POWER · W'),('428','ENERGY · KCAL')]),
        'intervals':('TIMED INTERVALS','2,430','m','WORK DISTANCE','3 OF 3 SEGMENTS','Rest is excluded from performance results.',[('2:03','AVERAGE WORK · /500 M'),('28','AVERAGE WORK STROKE RATE'),('204','AVERAGE WORK POWER · W'),('11:00','TOTAL ELAPSED')]),
        'free':('FREE ROW','2,450','m','DISTANCE','SAVED TO HISTORY','Ended by you after 12:34',[('12:34','DURATION'),('2:08','AVERAGE · /500 M'),('26','AVERAGE STROKE RATE'),('312','ENERGY · KCAL')]),
        'race':('5,000-METER RACE','20:42','','TOTAL TIME','WON BY 24 M','Reference · Sep 6, 2025 · 20:46',[('2:04','AVERAGE · /500 M'),('26','AVERAGE STROKE RATE'),('+24 m','FINISH MARGIN'),('−4 sec','TIME VS BEST')]),
        'ended':('5,000-METER ROW','2,450','m','DISTANCE COMPLETED','ENDED EARLY','Saved at 12:34 of the workout',[('12:34','DURATION'),('2:08','AVERAGE · /500 M'),('26','AVERAGE STROKE RATE'),('49%','TARGET COMPLETE')]),
    }
    program,result,unit,result_label,badge,comparison,metrics=models[kind]; im,d=canvas(); status(d); d.ellipse((sc(187),sc(43),sc(243),sc(99)),fill='#D9F6E6'); line(d,(202,71,211,81),'#168752',3); line(d,(211,81,230,59),'#168752',3); centered(d,(16,105,414,145),'Workout complete',26,True,C['ink']); centered(d,(16,140,414,162),'Today · 9:42 AM',10,False,C['muted'])
    rr(d,(16,178,414,350),20,C['white']); centered(d,(30,192,400,216),program,10,True,C['muted']); centered(d,(30,216,400,279),result,42,True,C['ink']);
    if unit: txt(d,(306 if len(result)>4 else 274,248),unit,20,True,C['ink'])
    centered(d,(30,278,400,300),result_label,10,True,C['muted']); badge_fill='#D9F6E6' if kind in ('distance','duration','race') else '#E8EEF6'; badge_text='#168752' if kind in ('distance','duration','race') else C['muted']; badge_w=max(80,10+len(badge)*7); rr(d,(215-badge_w/2,306,215+badge_w/2,332),13,badge_fill); centered(d,(215-badge_w/2,306,215+badge_w/2,332),badge,9,True,badge_text); centered(d,(30,334,400,350),comparison,9,False,C['muted'])
    label(d,370,'Workout summary'); top=394
    for i,(value,label_text) in enumerate(metrics):
        row,col=divmod(i,2); x1=16+col*203; x2=211+col*203; y1=top+row*96; rr(d,(x1,y1,x2,y1+88),16,C['white']); txt(d,(x1+14,y1+14),value,23,True,C['ink']); txt(d,(x1+14,y1+52),label_text,8,True,C['muted'])
    footer_y=724
    if kind=='intervals':
        label(d,596,'Segment results'); rr(d,(16,618,414,710),16,C['white']); x=28
        for label_text,value,width,fill in [('ROW','5:00',142,C['pale']),('REST','1:00',72,'#EDF4FC'),('ROW','5:00',142,C['pale'])]: rr(d,(x,632,x+width,676),10,fill); centered(d,(x,632,x+width,652),label_text,9,True,C['blue'] if label_text=='ROW' else C['muted']); centered(d,(x,650,x+width,674),value,8,False,C['muted']); x+=width+5
    elif kind=='race':
        label(d,596,'Race result'); rr(d,(16,618,414,704),16,C['white']); txt(d,(30,635),'You',11,False,C['muted']); txt(d,(400,635),'20:42',13,True,C['ink'],'ra'); line(d,(30,660,400,660),C['line']); txt(d,(30,675),'Saved reference',11,False,C['muted']); txt(d,(400,675),'20:46',13,True,C['ink'],'ra')
    else: footer_y=626
    rr(d,(16,footer_y,204,footer_y+54),27,C['white'],C['blue']); centered(d,(16,footer_y,204,footer_y+54),'Row again',13,True,C['blue']); rr(d,(212,footer_y,414,footer_y+54),27,C['blue']); centered(d,(212,footer_y,414,footer_y+54),'View details',13,True,C['white']); centered(d,(16,footer_y+60,414,footer_y+100),'Done',12,True,C['muted']); save(im,name)

def program_detail(name,kind='Distance'):
    models={
        'Duration':('60-minute row','A repeatable duration workout saved to My Programs.',[('TYPE','Duration'),('TARGET','60 minutes'),('COMPLETION','When 60:00 is reached')],('—','No performance goal','Row at your own pace','No target'),('5 completed workouts','Best distance 13,720 m · Sep 2','13,720 m at 60:00')),
        'Distance':('5,000-meter row','A repeatable distance workout saved to My Programs.',[('TYPE','Distance'),('TARGET','5,000 meters'),('ESTIMATED TIME','20–24 minutes')],('26','Stroke rate','Live guidance throughout the row','26 SPM'),('7 completed workouts','Best time 20:42 · Sep 6','20:42 · 2:04 /500 m')),
        'Intervals':('Timed intervals','A free-form sequence saved exactly as it was built.',[('TYPE','Intervals'),('1 · ROW','5:00'),('2 · REST','1:00'),('3 · ROW','5:00'),('TOTAL TIME','11:00')],('—','No performance goal','Segments may carry individual targets','No target'),('4 completed workouts','Best work distance 2,430 m','2,430 m · same segment order')),
    }
    title,subtitle,rows,goal,history=models[kind]; im,d=canvas(); header(d,'Program details'); txt(d,(400,54),'...',18,True,C['ink'],'ra')
    rr(d,(18,100,112,128),14,C['pale']); centered(d,(18,100,112,128),'MY PROGRAM',10,True,C['blue']); txt(d,(18,143),title,25,True); txt(d,(18,177),subtitle,12,False,C['muted'])
    label(d,210,'Workout'); card_top=232; card_h=26+24*len(rows); rr(d,(16,card_top,414,card_top+card_h),18,C['white'])
    for index,(key,value) in enumerate(rows):
        row_y=card_top+16+index*24; txt(d,(30,row_y),key,10,True,C['ink']); txt(d,(400,row_y),value,11,False,C['muted'],'ra')
    y=card_top+card_h+18; label(d,y,'Performance goal'); y+=22; rr(d,(16,y,414,y+76),18,C['white']); rr(d,(30,y+17,70,y+57),20,'#EAF2FF'); centered(d,(30,y+17,70,y+57),goal[0],12,True,C['blue']); txt(d,(82,y+14),goal[1],14,True); txt(d,(82,y+37),goal[2],9,False,C['muted']); txt(d,(400,y+25),goal[3],14,True,C['ink'],'ra')
    y+=96; label(d,y,'Program history'); y+=22; rr(d,(16,y,414,y+76),18,C['white']); rr(d,(30,y+17,70,y+57),20,'#E8EEF6'); centered(d,(30,y+17,70,y+57),'H',15,True,C['muted']); txt(d,(82,y+14),history[0],14,True); txt(d,(82,y+37),history[1],10,False,C['muted']); txt(d,(400,y+25),'View',11,True,C['blue'],'ra')
    y+=96; label(d,y,'Session option'); y+=22; rr(d,(16,y,414,y+110),18,C['white']); rr(d,(30,y+17,70,y+57),20,C['pale']); centered(d,(30,y+17,70,y+57),'B',15,True,C['blue']); txt(d,(82,y+14),'Race your best',14,True); txt(d,(82,y+37),'Compare with a compatible completion',9,False,C['muted']); rr(d,(350,y+21,400,y+51),15,C['blue']); rr(d,(371,y+23,398,y+50),14,C['white']); line(d,(82,y+66,400,y+66),C['line']); txt(d,(82,y+80),'Comparison',10,False,C['muted']); txt(d,(400,y+80),history[2],10,True,C['ink'],'ra')
    cta(d,786,'Start race'); save(im,name)

home('01_home.png')
# The approved photographic Home is maintained separately from the generated
# atlas screens. Restore it after every atlas build so regeneration cannot
# silently replace the current design with the legacy placeholder composition.
runpy.run_path('/Users/brian/Documents/ChatGPT/Coxswain/work/restore_approved_home.py')
for idx,g in enumerate(['None','Stroke rate','Speed','Power'],2): setup(f'{idx:02d}_quick_duration_{g.lower().replace(" ","_")}.png','Duration',g)
setup('06_quick_distance.png','Distance','None',race=None)
setup('07_distance_race_available.png','Distance','None',race=False)
setup('08_distance_race_enabled.png','Distance','None',race=True)
setup('09_quick_intervals.png','Intervals','None')
segment_editor('10_segment_duration.png','Duration'); segment_editor('11_segment_distance.png','Distance'); segment_editor('12_segment_rest.png','Rest')
setup('13_program_duration.png','Duration','None',program=True); setup('14_program_distance.png','Distance','Stroke rate',program=True); setup('15_program_intervals.png','Intervals','None',program=True)
programs('16_my_programs.png'); programs('17_workout_library.png',True); live('18_live_row_free.png'); live('19_live_row_race.png',True); program_detail('20_program_detail_duration.png','Duration'); program_detail('21_program_detail_distance.png','Distance'); program_detail('22_program_detail_intervals.png','Intervals'); live('23_live_target_duration.png',target='Duration'); live('24_live_target_distance.png',target='Distance'); live('25_live_target_intervals.png',target='Intervals')
live('26_live_target_goal_stroke_rate.png',target='Distance',goal='Stroke rate'); live('27_live_target_goal_speed.png',target='Distance',goal='Speed'); live('28_live_target_goal_power.png',target='Distance',goal='Power')
live_interval_rest('29_live_target_interval_rest.png')
race_reference_picker('30_race_reference_picker.png')
pause_end('31_pause_end_paused.png','paused'); pause_end('32_pause_end_confirm.png','confirm'); pause_end('33_pause_end_discard.png','discard')
for index,kind in enumerate(['distance','duration','intervals','free','race','ended'],34): workout_complete(f'{index:02d}_workout_complete_{kind}.png',kind)

items=[
('Home','01_home.png',['Quick Start exposes Duration, Distance, My Programs, and Library.','Free Row remains the dominant action.']),
('Quick Duration — no goal','02_quick_duration_none.png',['Duration is preselected from Home.','No target-value panel is shown while Goal is None.','Start Workout is primary; Save as Program preserves the setup.']),
('Quick Duration — stroke rate','03_quick_duration_stroke_rate.png',['Selecting Stroke Rate reveals its target controls.','Summary changes immediately to Goal 26 SPM.']),
('Quick Duration — speed','04_quick_duration_speed.png',['Selecting Speed reveals pace controls in the configured /500 m representation.','Summary changes immediately to Goal 2:10 /500 m.']),
('Quick Duration — power','05_quick_duration_power.png',['Selecting Power reveals watt controls.','Summary changes immediately to Goal 180 W.']),
('Quick Distance','06_quick_distance.png',['Distance uses rowing presets: 500 m, 1K, 2K, 5K, 6K, and 10K.','Direct entry supports any distance.']),
('Distance — race available','07_distance_race_available.png',['Race Your Best appears only when a compatible result exists.','Set Goal remains available while racing is disabled.']),
('Distance — race enabled','08_distance_race_enabled.png',['The best compatible result and comparison pace are shown.','Race mode replaces Set Goal to avoid competing guidance.']),
('Quick Intervals','09_quick_intervals.png',['Segments are compact, ordered, and individually editable.','Add Segment appends a new Duration, Distance, or Rest segment.']),
('Add Segment — Duration','10_segment_duration.png',['Duration uses direct min:sec entry and duration presets.','Save adds or updates the segment and refreshes the summary.']),
('Add Segment — Distance','11_segment_distance.png',['Distance uses direct meter entry and interval-distance presets.','Edit mode adds a Delete action.']),
('Add Segment — Rest','12_segment_rest.png',['Rest is a first-class segment type and always uses time.','Rest presets favor short recovery periods.']),
('New Program — Duration','13_program_duration.png',['Program context adds Program Name.','Save Program replaces Start Workout.']),
('New Program — Distance','14_program_distance.png',['Program Builder reuses the same target, goal, and summary components.','Race Your Best is session guidance and is not saved into a program.']),
('New Program — Intervals','15_program_intervals.png',['The type selector swaps only the configuration area.','Set Goal and Summary keep stable positions.']),
('My Programs','16_my_programs.png',['Owned programs include View, Start, and an overflow management menu.','Create Program appears only on My Programs.']),
('Workout Library','17_workout_library.png',['Library uses the same compact cards.','Curated items are read-only and omit Create Program and management actions.']),
('Live Row — Free','18_live_row_free.png',['All metric values use the same maximum readable size.','Edit Display, Pause, and End remain available.']),
('Live Row — Race','19_live_row_race.png',['The metric grid matches Live Row Free.','Two progress lines compare You with the best compatible workout from History.']),
('Program Detail — Duration','20_program_detail_duration.png',['Duration ranks History by greatest distance at the fixed elapsed time.','A compatible previous run can compare distance at the same elapsed time.']),
('Program Detail — Distance','21_program_detail_distance.png',['Distance ranks History by fastest completion time.','A compatible previous result enables Race Your Best.']),
('Program Detail — Intervals','22_program_detail_intervals.png',['Intervals show the exact free-form order of Row and Rest segments saved by the builder.','Time-only sequences can show a total; mixed sequences omit an exact duration.']),
('Live Row Target — Duration','23_live_target_duration.png',['The approved metric grid remains unchanged.','Progress shows elapsed time, time remaining, and the fixed duration target.']),
('Live Row Target — Distance','24_live_target_distance.png',['Progress shows completed meters, remaining meters, and the fixed distance target.','Metric values retain the same shared maximum size.']),
('Live Row Target — Intervals','25_live_target_intervals.png',['One continuous segmented line represents the complete saved sequence.','Blue sections are Row, lighter sections are Rest, and a small marker shows current progress.']),
('Live Row Target — Stroke-rate Goal','26_live_target_goal_stroke_rate.png',['The goal card shows only the signed variance so it stays readable at rowing distance.','A red background makes the below-target state immediately visible.']),
('Live Row Target — Speed Goal','27_live_target_goal_speed.png',['The goal card shows only the signed speed variance.','Its background communicates whether the rower is below or meeting the target.']),
('Live Row Target — Power Goal','28_live_target_goal_power.png',['The goal card shows only the signed power variance.','A green background makes the at-or-above-target state immediately visible.']),
('Live Row Target — Interval Rest','29_live_target_interval_rest.png',['Rest replaces the metric grid with a large countdown.','The next Row segment and complete color-coded sequence remain visible.']),
('Race Reference Picker','30_race_reference_picker.png',['Only completed workouts with a compatible target and structure can be selected.','The best result is selected by default; unavailable History explains why other workouts cannot be compared.','Start Race locks the chosen reference snapshot for the session.']),
('Pause / End — Paused','31_pause_end_paused.png',['Pause freezes every metric and all program progress.','Resume returns to the exact Live Row mode and session state that opened the screen.']),
('Pause / End — End confirmation','32_pause_end_confirm.png',['End and Save preserves the completed portion in History and proceeds to Workout Complete.','Stay Paused closes the sheet without advancing timers or progress.']),
('Pause / End — Discard confirmation','33_pause_end_discard.png',['Discard is separated from ending and saving.','A second explicit confirmation protects the irreversible removal of the active workout.']),
('Workout Complete — Distance','34_workout_complete_distance.png',['Distance results lead with completion time and identify a new best when applicable.','Summary metrics remain compact and Row Again, View Details, and Done stay available.']),
('Workout Complete — Duration','35_workout_complete_duration.png',['Duration results lead with distance achieved during the fixed time.','Best-result comparison uses distance rather than completion time.']),
('Workout Complete — Intervals','36_workout_complete_intervals.png',['Intervals lead with the program-specific work result and exclude Rest from performance ranking.','The complete ordered segment group and individual Row results remain available.']),
('Workout Complete — Free Row','37_workout_complete_free.png',['Free Row leads with distance and records that the user ended the open session.','The workout is saved to History without target-completion language.']),
('Workout Complete — Race','38_workout_complete_race.png',['Race results show the finish outcome and retain the fixed saved reference.','The comparison uses both distance and time language.']),
('Workout Complete — Ended Early','39_workout_complete_ended.png',['A manually ended target workout states that it ended early and shows partial target progress.','The partial result is still saved accurately to History.']),
]
(OUT/'manifest.json').write_text(json.dumps(items,indent=2),encoding='utf-8')
print(f'generated {len(items)} screens in {OUT}')
