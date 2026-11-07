const http=require('node:http');
const fs=require('node:fs');
const path=require('node:path');
const https=require('node:https');
const production=process.argv.includes('--prod')||process.env.NODE_ENV==='production';
const envFile=path.join(__dirname,production?'.env.prod':'.env.local');
if(fs.existsSync(envFile))for(const line of fs.readFileSync(envFile,'utf8').split(/\r?\n/)){
 const match=line.match(/^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);if(!match||process.env[match[1]]!==undefined)continue;
 process.env[match[1]]=match[2].replace(/^(['"])(.*)\1$/,'$2');
}
const apiUpstream=new URL(process.env.API_UPSTREAM||'http://127.0.0.1:48080');
if(!['http:','https:'].includes(apiUpstream.protocol))throw new Error('API_UPSTREAM must use HTTP or HTTPS');
const port=Number(process.env.PORT||4173),host=process.env.HOST||'127.0.0.1';
const root=path.join(__dirname,'dist');
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.png':'image/png','.jpg':'image/jpeg','.svg':'image/svg+xml','.webp':'image/webp'};
http.createServer((req,res)=>{
 const pathname=new URL(req.url,'http://localhost').pathname;
 if(pathname==='/studio-config.js'){res.writeHead(200,{'Content-Type':'application/javascript','Cache-Control':'no-store'});return res.end('window.STUDIO_CONFIG='+JSON.stringify({cloudEnabled:process.env.CLOUD_ENABLED!=='false',apiBase:'',studioUrl:''})+';');}
 if(pathname.startsWith('/admin-api/')){
  const upstream=(apiUpstream.protocol==='https:'?https:http).request(new URL(req.url,apiUpstream),{method:req.method,headers:{...req.headers,host:apiUpstream.host,'tenant-id':'1'}},reply=>{res.writeHead(reply.statusCode,reply.headers);reply.pipe(res);});
  upstream.on('error',()=>{res.writeHead(503,{'Content-Type':'application/json'});res.end(JSON.stringify({code:503,msg:'后台服务暂不可用'}));});
  req.pipe(upstream);return;
 }
 let filename;try{filename=decodeURIComponent(pathname);}catch{res.writeHead(400);return res.end();}
 const file=path.resolve(root,'.'+(filename==='/'?'/index.html':filename));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(err,data)=>{if(err){res.writeHead(404);res.end('Not found');}else{res.writeHead(200,{'Content-Type':types[path.extname(file)]||'application/octet-stream'});res.end(data);}});
}).listen(port,host,()=>console.log(`Safillu: http://${host}:${port}`));
