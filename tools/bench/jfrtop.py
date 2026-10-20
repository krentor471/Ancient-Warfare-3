import json,sys,collections,subprocess
d=json.loads(subprocess.run(['jfr','print','--json','--stack-depth','64','--events','jdk.ExecutionSample',sys.argv[1]],capture_output=True,text=True).stdout)
ev=[e for e in d['recording']['events'] if e['values']['sampledThread']['javaName']=='Server thread']
print('samples',len(ev))
selfc=collections.Counter(); incl=collections.Counter(); awself=collections.Counter()
def nm(f): m=f['method']; return m['type']['name'].split('.')[-1]+'.'+m['name']
for e in ev:
  fr=e['values']['stackTrace']['frames']
  selfc[nm(fr[0])]+=1
  seen=set()
  for f in fr:
    n=nm(f)
    if 'shadowmage' in f['method']['type']['name'] and n not in seen: incl[n]+=1; seen.add(n)
  for f in fr:
    if 'shadowmage' in f['method']['type']['name']: awself[nm(f)]+=1; break
N=len(ev)
print('--- top self'); [print(f'{c*100/N:5.1f}% {n}') for n,c in selfc.most_common(int(sys.argv[2]) if len(sys.argv)>2 else 15)]
print('--- AW inclusive'); [print(f'{c*100/N:5.1f}% {n}') for n,c in incl.most_common(25)]
print('--- deepest AW frame'); [print(f'{c*100/N:5.1f}% {n}') for n,c in awself.most_common(15)]
