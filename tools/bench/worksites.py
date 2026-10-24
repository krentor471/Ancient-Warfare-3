import sys,time,re,math
# Workers walking to worksites in awkward spots: a roofed hut, a raised floor, a fenced pen with a gate,
# a narrow alcove and a field with a water channel. Prints when each worker got within reach of its site.
sys.path.insert(0,'.')
from rcon import R
r=R()
for _ in range(3): r.cmd('kill @e[type=!player]'); time.sleep(1)
r.cmd('time set day'); r.cmd('gamerule doDaylightCycle false')
r.cmd('fill -5 -60 -10 60 -54 30 air'); r.cmd('fill -5 -61 -10 60 -61 30 grass_block')
def pk(x,y,z): return ((x & 0x3ffffff)<<38)|((z & 0x3ffffff)<<12)|(y & 0xfff)
sites={}
# A: roofed hut, site in the middle, door on the far side
r.cmd('fill 7 -60 -2 11 -58 2 stone hollow'); r.cmd('fill 8 -60 -1 10 -59 1 air'); r.cmd('fill 11 -60 0 11 -59 0 air')
sites['A hut']=(9,-60,0)
# C: raised floor reached by steps
r.cmd('fill 20 -60 -2 24 -59 2 stone'); r.cmd('setblock 18 -60 0 stone_stairs[facing=east]'); r.cmd('setblock 19 -60 0 stone'); r.cmd('setblock 19 -59 0 stone_stairs[facing=east]')
sites['C raised']=(22,-58,0)
# E: fenced pen with a gate on the far side
r.cmd('fill 31 -60 -2 35 -60 2 oak_fence'); r.cmd('fill 32 -60 -1 34 -60 1 air'); r.cmd('setblock 35 -60 0 oak_fence_gate[facing=east]')
sites['E pen']=(33,-60,0)
# F: site in a narrow alcove between two tall blocks with a low ceiling right above
r.cmd('fill 44 -60 -1 46 -58 1 stone'); r.cmd('fill 44 -60 0 45 -59 0 air'); r.cmd('setblock 45 -60 0 air')
sites['F alcove']=(46,-60,0)
# G: site beside farmland with a water channel between
r.cmd('fill 52 -61 -3 58 -61 3 farmland'); r.cmd('fill 55 -61 -3 55 -61 3 water')
sites['G field']=(57,-60,0)
for name,(x,y,z) in sites.items():
  r.cmd(f'setblock {x} {y} {z} ancientwarfare:quarry')
  sx=x-3
  wo='{id:"ancientwarfare:work_order",Count:1b,tag:{orders:{dim:"minecraft:overworld",entryList:[{pos:%dL,dim:"minecraft:overworld",length:0}]}}}'%pk(x,y,z)
  r.cmd(f'summon ancientwarfare:worker_npc {sx}.5 -60 -8.5 {{Owner:[I;1,2,3,4],PersistenceRequired:1b,Tags:["w{name[0]}"],HandItems:[{{id:"minecraft:iron_pickaxe",Count:1b}},{{}}],OrdersStack:{wo}}}')
reached={}
T=int(sys.argv[1]) if len(sys.argv)>1 else 40
for t in range(T):
  time.sleep(1)
  for name,(x,y,z) in sites.items():
    if name in reached: continue
    out=r.cmd(f'data get entity @e[tag=w{name[0]},limit=1] Pos')
    m=re.findall(r'(-?[\d.]+)d',out)
    if len(m)<3: continue
    px,py,pz=map(float,m[:3])
    d=math.sqrt((px-x-0.5)**2+(py-y)**2+(pz-z-0.5)**2)
    if d<=2.0: reached[name]=(t+1,round(px,1),round(py,1),round(pz,1))
for name,(x,y,z) in sites.items():
  out=r.cmd(f'data get entity @e[tag=w{name[0]},limit=1] Pos'); st=r.cmd(f'data get entity @e[tag=w{name[0]},limit=1] WorkState')
  print(name, 'reached at', reached.get(name), '| now', re.sub(r'.*data: ','',out.strip()), re.sub(r'.*data: ','',st.strip()))
