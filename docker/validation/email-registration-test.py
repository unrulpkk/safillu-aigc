"""Real HTTP/MySQL/Redis/SMTP registration test. ONLY for an isolated qa/smoke deployment.
Uses a local SMTP sink; never sends messages to an external inbox or prints OTPs.
Run from deployment root: python docker/validation/email-registration-test.py .env.local
"""
import base64, concurrent.futures, email, hashlib, hmac, json, re, runpy, secrets, socketserver, subprocess, sys, threading, time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
envfile=sys.argv[1] if len(sys.argv)>1 else '.env.local'
sys.argv=['smoke',envfile]
h=runpy.run_path(str(ROOT/'docker/smoke-test.py'),run_name='helpers')
env=h['env'];runtime=h['runtime'];sql=h['sql'];checks=[]
if not re.search(r'(^|[-_])(qa|smoke)([-_]|$)',env.get('COMPOSE_PROJECT_NAME','')): raise SystemExit('Refusing to run outside an isolated qa/smoke project')
compose=['docker','compose','--env-file',envfile]
def run(*args): return subprocess.run(compose+list(args),cwd=ROOT,text=True,capture_output=True,check=True).stdout.strip()
container=run('ps','-q','backend')
info=json.loads(subprocess.run(['docker','inspect',container],text=True,capture_output=True,check=True).stdout)[0]
network=next(iter(info['NetworkSettings']['Networks'].values()))
BASE='http://'+network['IPAddress']+':48080'
messages={};lock=threading.Lock()
class SMTP(socketserver.StreamRequestHandler):
 def reply(self,line): self.wfile.write((line+'\r\n').encode());self.wfile.flush()
 def handle(self):
  self.reply('220 localhost ESMTP');recipient=''
  while True:
   raw=self.rfile.readline()
   if not raw: break
   line=raw.decode(errors='replace').strip();verb=line.split(' ',1)[0].upper()
   if verb in ('EHLO','HELO'): self.reply('250-localhost');self.reply('250 AUTH PLAIN LOGIN')
   elif verb=='AUTH':
    if line.upper().startswith('AUTH LOGIN'):
     self.reply('334 VXNlcm5hbWU6');self.rfile.readline();self.reply('334 UGFzc3dvcmQ6');self.rfile.readline()
    elif len(line.split())<3: self.reply('334 ');self.rfile.readline()
    self.reply('235 Authentication successful')
   elif verb=='RCPT': recipient=line.split(':',1)[1].strip().strip('<>');self.reply('250 OK')
   elif verb=='DATA':
    self.reply('354 Send message');body=b''
    while True:
     part=self.rfile.readline()
     if part in (b'.\r\n',b'.\n',b''): break
     body+=part[1:] if part.startswith(b'..') else part
    msg=email.message_from_bytes(body)
    text='\n'.join(part.get_payload(decode=True).decode(part.get_content_charset() or 'utf-8') for part in msg.walk() if part.get_content_type()=='text/plain')
    with lock: messages[recipient]=re.search(r'(?<![0-9])([0-9]{6})(?![0-9])',text).group(1)
    self.reply('250 Queued')
   elif verb=='QUIT': self.reply('221 Bye');break
   else: self.reply('250 OK')
class Server(socketserver.ThreadingTCPServer): allow_reuse_address=True;daemon_threads=True
server=Server((network['Gateway'],0),SMTP);threading.Thread(target=server.serve_forever,daemon=True).start()
def check(name,passed):
 checks.append({'name':name,'passed':bool(passed)})
 if not passed: raise AssertionError(name)
def request(path,body,token=None):
 status,raw,_=h['request'](BASE,'/admin-api/'+path,'POST',body,token)
 result=json.loads(raw);return result
prefix='qa'+str(int(time.time()))
def address(suffix): return prefix+suffix+'@example.test'
def send(address):
 result=request('aigc/auth/email/code',{'email':address,'locale':'en'})
 check('SMTP accepted '+address.split('@')[0][-1:],result.get('code')==0)
 with lock: return messages[address]
pepper=(runtime/'secrets/data_key').read_text().strip().encode()
def key(address): return 'safillu:email:register:'+hmac.new(pepper,('safillu:email:v1:'+address).encode(),hashlib.sha256).hexdigest()
def redis(*args):
 return run('exec','-T','redis','sh','-ec','REDISCLI_AUTH="$(cat /run/secrets/redis_password)" exec redis-cli --raw "$@"','sh',*map(str,args))
account_id=None
try:
 check('Isolated SMTP configuration starts empty',sql('SELECT COUNT(*) FROM system_mail_account WHERE deleted=0;')=='0')
 sql("INSERT INTO system_mail_account(mail,username,password,host,port,ssl_enable,starttls_enable,creator) VALUES('fixture@example.test','fixture','fixture','"+network['Gateway']+"',"+str(server.server_address[1])+",0,0,'email-smoke');")
 account_id=int(sql("SELECT id FROM system_mail_account WHERE deleted=0 AND creator='email-smoke' ORDER BY id DESC LIMIT 1;"))
 check('Malformed registration rejected',request('aigc/auth/email/register',{'email':'invalid','code':'123456','password':'short'}).get('code')!=0)
 check('Legacy unverified signup blocked',request('system/auth/register',{'username':prefix,'nickname':'QA','password':'Example123'}).get('code')!=0)
 a=address('a');code=send(a)
 check('Code lifetime is 5 minutes',0<int(redis('TTL',key(a)))<=300)
 check('Redis stores digest, not OTP',code not in redis('HGETALL',key(a)))
 check('Resend cooldown enforced',request('aigc/auth/email/code',{'email':a,'locale':'en'}).get('code')==429)
 wrong='000000' if code!='000000' else '111111'
 for i in range(5): check('Incorrect code attempt '+str(i+1),request('aigc/auth/email/register',{'email':a,'code':wrong,'password':'Example123'}).get('code')!=0)
 check('Five incorrect attempts invalidate even the correct code',request('aigc/auth/email/register',{'email':a,'code':code,'password':'Example123'}).get('code')!=0)
 check('Failed validation did not create user',sql("SELECT COUNT(*) FROM aigc_email_identity WHERE email='"+a+"';")=='0')
 b=address('b');expired=send(b);redis('EXPIRE',key(b),1);time.sleep(1.1)
 check('Expired code rejected',request('aigc/auth/email/register',{'email':b,'code':expired,'password':'Example123'}).get('code')!=0)
 c=address('c');code=send(c);password=secrets.token_urlsafe(9)
 body={'email':c,'code':code,'password':password}
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: results=list(pool.map(lambda _:request('aigc/auth/email/register',body),range(2)))
 check('Concurrent code consumption creates exactly one account',sum(x.get('code')==0 for x in results)==1)
 tokens=next(x['data'] for x in results if x.get('code')==0);token=tokens['accessToken']
 identity=sql("SELECT user_id FROM aigc_email_identity WHERE email='"+c+"';");uid=int(identity)
 check('Identity maps to a native Yudao user with hashed password',sql('SELECT COUNT(*) FROM system_users WHERE id='+str(uid)+" AND tenant_id=1 AND email='"+c+"' AND password LIKE '$2%';")=='1')
 check('Used code removed',redis('EXISTS',key(c))=='0')
 check('Registered email cannot register again',request('aigc/auth/email/register',body).get('code')==409)
 check('Registered email cannot request signup code',request('aigc/auth/email/code',{'email':c}).get('code')==409)
 login=request('aigc/auth/email/login',{'account':c.upper(),'password':password})
 check('Case-insensitive email login uses native tokens',login.get('code')==0)
 check('Wrong password rejected',request('aigc/auth/email/login',{'account':c,'password':'Wrong1234'}).get('code')!=0)
 bootstrap=h['api'](BASE,'/admin-api/aigc/bootstrap',token=token)
 check('New account opens its own persistent workspace',str(bootstrap['user']['id'])==str(uid) and len(bootstrap['sessions'])>=1)
 sql('UPDATE system_users SET status=1 WHERE id='+str(uid)+';')
 check('Disabled native account cannot log in',request('aigc/auth/email/login',{'account':c,'password':password}).get('code')!=0)
 sql('UPDATE system_users SET status=0 WHERE id='+str(uid)+';')
 # Break only the isolated fixture's SMTP transport; failure must revoke the challenge.
 sql("UPDATE system_mail_account SET host='127.0.0.1',port=1 WHERE id="+str(account_id)+';')
 d=address('d');failure=request('aigc/auth/email/code',{'email':d})
 check('SMTP failure is not reported as successful delivery',failure.get('code')==503)
 check('Failed delivery leaves no usable code or cooldown',redis('EXISTS',key(d),key(d)+':cooldown')=='0')
finally:
 if account_id is not None: sql('UPDATE system_mail_account SET deleted=1 WHERE id='+str(account_id)+';')
 server.shutdown();server.server_close()
 report={'checks':checks,'externalEmailSent':False,'smtp':'isolated real SMTP sink','database':env.get('DB_NAME')}
 target=runtime/'email-registration-verification.json';target.write_text(json.dumps(report,indent=2))
print(json.dumps({'passed':len(checks),'report':str(target),'externalEmailSent':False}))
