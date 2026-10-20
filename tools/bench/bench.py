import sys,time,re,random
sys.path.insert(0,'.')
from rcon import R
r=R()
r.cmd('save-off')
def tps():
  out=r.cmd('forge tps'); m=re.search(r'overworld\): Mean tick time: ([\d.]+) ms',out); return float(m.group(1))
def clear():
  for _ in range(20):
    r.cmd('kill @e[type=!player]'); time.sleep(3)
    if 'failed' in r.cmd('execute if entity @e[type=!player]'): return
  raise SystemExit('could not clear entities')
def summon(eid,n,x0,z0,spread,nbt):
  for i in range(n):
    x=x0+random.uniform(-spread,spread); z=z0+random.uniform(-spread,spread)
    r.cmd(f'summon ancientwarfare:{eid} {x:.1f} -60 {z:.1f} {nbt}')
def measure(label,warm=10,reps=8):
  time.sleep(warm); v=[]
  for _ in range(reps): time.sleep(5); v.append(tps())
  time.sleep(HOLD)
  v.sort(); med=(v[reps//2-1]+v[reps//2])/2 if reps%2==0 else v[reps//2]
  cnt=r.cmd('execute if entity @e[type=!player]')
  print(f'{label}: median mspt {med:.1f} (min {v[0]:.1f}, max {v[-1]:.1f}) | {cnt.strip()}',flush=True)
import os
HOLD=float(os.environ.get('BENCH_HOLD','0'))
random.seed(1)
mode=sys.argv[1]; ns=[int(x) for x in sys.argv[2:]]
for n in ns:
  clear()
  if mode=='idle':
    summon('faction_civilian_male_npc',n,0,0,24,'{FactionName:"empire",PersistenceRequired:1b}')
    measure(f'idle civilians x{n}')
  elif mode=='soldiers':
    summon('faction_soldier_npc',n,0,0,24,'{FactionName:"empire",PersistenceRequired:1b}')
    measure(f'idle faction soldiers x{n}')
  elif mode=='battle':
    nb='{FactionName:"%s",PersistenceRequired:1b,Invulnerable:1b}'
    summon('faction_soldier_npc',n//2,-12,0,6,nb%'empire')
    summon('faction_soldier_npc',n//2,12,0,6,nb%'norska')
    measure(f'battle {n//2}v{n//2} (invulnerable)',warm=8)
  elif mode=='army':
    own='{Owner:[I;1,2,3,4],PersistenceRequired:1b,Invulnerable:1b,HandItems:[{id:"minecraft:iron_sword",Count:1b},{}]}'
    summon('combat_npc',n//2,-12,0,6,own)
    summon('faction_soldier_npc',n//2,12,0,6,'{FactionName:"empire",PersistenceRequired:1b,Invulnerable:1b}')
    measure(f'player army {n//2} vs empire {n//2} (invulnerable)',warm=8)
  elif mode=='workers':
    summon('worker_npc',n,0,0,24,'{PersistenceRequired:1b}')
    measure(f'unowned workers x{n}')
clear()
