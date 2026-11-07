/* Bounded, account-scoped browser cache. IndexedDB also works on LAN HTTP. */
(function(root){
 'use strict';
 function indexedStore(){
  let opening;const memory=new Map();
  async function database(){
   if(!root.indexedDB)return null;
   if(!opening)opening=new Promise(resolve=>{let settled=false;const finish=value=>{if(settled){value?.close();return;}settled=true;clearTimeout(timer);resolve(value);};const timer=setTimeout(()=>finish(null),2000);try{const request=root.indexedDB.open('safillu-media-cache',1);request.onupgradeneeded=()=>request.result.createObjectStore('entries',{keyPath:'key'});request.onsuccess=()=>{request.result.onversionchange=()=>request.result.close();finish(request.result);};request.onerror=()=>finish(null);request.onblocked=()=>finish(null);}catch{finish(null);}});
   return opening;
  }
  async function op(mode,action){const db=await database();if(!db)return undefined;return new Promise((resolve,reject)=>{let value;try{const tx=db.transaction('entries',mode),request=action(tx.objectStore('entries'));if(request)request.onsuccess=()=>{value=request.result;};tx.oncomplete=()=>resolve(value);tx.onerror=tx.onabort=()=>reject(tx.error);}catch(error){reject(error);}});}
  return {get:async key=>(await database())?op('readonly',s=>s.get(key)):memory.get(key),all:async()=>(await database())?(await op('readonly',s=>s.getAll())||[]):[...memory.values()],set:async row=>(await database())?op('readwrite',s=>s.put(row)):memory.set(row.key,row),delete:async key=>(await database())?op('readwrite',s=>s.delete(key)):memory.delete(key)};
 }
 function createCache(options={}){
  const backend=options.backend||indexedStore(),maxBytes=options.maxBytes??64*1024*1024,maxFileBytes=options.maxFileBytes??16*1024*1024,maxEntries=options.maxEntries??128,ttl=options.ttl??14*86400000,now=options.now||Date.now;
  let serial=Promise.resolve();const pending=new Map();
  const key=(owner,kind,record)=>JSON.stringify([String(owner),kind,String(record.id),record.contentPath||'',Number(record.byteSize)||0,record.created||0]);
  function queued(action){const result=serial.then(action);serial=result.catch(()=>{});return result;}
  async function prune(){const rows=await backend.all();let total=0,count=0;rows.sort((a,b)=>b.accessed-a.accessed);for(const row of rows){if(now()-row.saved>ttl||count>=maxEntries||total+row.size>maxBytes)await backend.delete(row.key);else{total+=row.size;count++;}}}
  const get=(owner,kind,record)=>queued(async()=>{try{const id=key(owner,kind,record),row=await backend.get(id);if(!row)return null;if(now()-row.saved>ttl){await backend.delete(id);return null;}await backend.set({...row,accessed:now()});return row.blob;}catch{return null;}});
  const put=(owner,kind,record,blob)=>queued(async()=>{if(!owner||!blob?.size||blob.size>maxFileBytes||blob.size>maxBytes)return false;try{await backend.set({key:key(owner,kind,record),owner:String(owner),blob,size:blob.size,saved:now(),accessed:now()});await prune();return true;}catch{return false;}});
  async function load(owner,kind,record,loader){const id=key(owner,kind,record);if(pending.has(id))return pending.get(id);const work=(async()=>{const cached=await get(owner,kind,record);if(cached)return cached;const blob=await loader();await put(owner,kind,record,blob);return blob;})();pending.set(id,work);try{return await work;}finally{if(pending.get(id)===work)pending.delete(id);}}
  const clear=owner=>queued(async()=>{try{for(const row of await backend.all())if(row.owner===String(owner))await backend.delete(row.key);}catch{}});
  return {key,get,put,load,clear,maxFileBytes};
 }
 root.StudioMediaCache={createCache};
 if(typeof module==='object'&&module.exports)module.exports={createCache};
})(typeof window==='object'?window:globalThis);
