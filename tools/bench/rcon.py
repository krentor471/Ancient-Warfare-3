import socket,struct,sys
class R:
  def __init__(s,host='127.0.0.1',port=25575,pw='bench'):
    s.k=socket.create_connection((host,port)); s.i=0; s.cmd(pw,3)
  def _recv(s,n):
    b=b''
    while len(b)<n: b+=s.k.recv(n-len(b))
    return b
  def cmd(s,c,t=2):
    s.i+=1; p=struct.pack('<ii',s.i,t)+c.encode()+b'\0\0'
    s.k.sendall(struct.pack('<i',len(p))+p)
    ln=struct.unpack('<i',s._recv(4))[0]; d=s._recv(ln)
    return d[8:-2].decode('utf-8','replace')
if __name__=='__main__':
  r=R()
  for c in sys.argv[1:]: print(r.cmd(c))
