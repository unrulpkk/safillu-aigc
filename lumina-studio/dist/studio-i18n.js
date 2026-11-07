/* UI-only localization. Canonical business values and user content stay untouched. */
(()=>{'use strict';
 const messages=window.STUDIO_MESSAGES, supported=['zh-CN','en','es'], storageKey='zaomeng-locale';
 let locale='zh-CN';try{const saved=localStorage.getItem(storageKey);if(supported.includes(saved))locale=saved;}catch{}
 const originals=new WeakMap(), attributes=new WeakMap(),missing=new Set();let applying=false,queued=false,disposed=false;
 const localeTag=()=>({en:'en-US',es:'es-ES','zh-CN':'zh-CN'})[locale];
 const number=n=>new Intl.NumberFormat(localeTag()).format(Number(String(n).replace(/,/g,'')));
 const pick=(en,es)=>locale==='en'?en:es;
 const unitNames={'张':['image','images','imagen','imágenes'],'条':['video','videos','vídeo','vídeos'],'首':['track','tracks','pista','pistas'],'段':['audio clip','audio clips','fragmento de audio','fragmentos de audio'],'次':['request','requests','solicitud','solicitudes'],'秒':['second','seconds','segundo','segundos'],'项':['item','items','elemento','elementos'],'字':['character','characters','carácter','caracteres']};
 function unit(n,u){const words=unitNames[u];return number(n)+' '+words[(locale==='en'?0:2)+(Number(String(n).replace(/,/g,''))===1?0:1)];}
 const keys=Object.keys(messages).filter(k=>/[\u3400-\u9fff]/.test(k)).sort((a,b)=>b.length-a.length);
 const escaped=s=>s.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');
 const phrasePattern=new RegExp(keys.map(escaped).join('|'),'g');
 function translate(source){
  if(locale==='zh-CN')return source;
  const s=source.trim(),entry=messages[s];if(entry)return source.replace(s,entry[locale==='en'?0:1]);
  let m;
  if((m=s.match(/^共 (\d+) 个作品与素材$/)))return pick(`${number(m[1])} ${m[1]==='1'?'asset':'assets'}`,`${number(m[1])} ${m[1]==='1'?'recurso':'recursos'}`);
  if((m=s.match(/^(\d+) 个上传素材$/)))return pick(`${number(m[1])} ${m[1]==='1'?'upload':'uploads'}`,`${number(m[1])} ${m[1]==='1'?'archivo subido':'archivos subidos'}`);
  if((m=s.match(/^已选择 (\d+) 项$/)))return pick(`${number(m[1])} selected`,`${number(m[1])} seleccionados`);
  if((m=s.match(/^已进入队列，预占 ([\d,.]+) 积分$/)))return pick(`Queued. ${number(m[1])} credits reserved.`,`En cola. Se han reservado ${number(m[1])} créditos.`);
  if((m=s.match(/^结果已保存至个人资产，结算 ([\d,.]+) 积分$/)))return pick(`Results saved to your assets. ${number(m[1])} credits charged.`,`Resultados guardados en tus recursos. Se han cobrado ${number(m[1])} créditos.`);
  if((m=s.match(/^已关联 (.+)，可用 Google 登录当前账号。$/)))return pick(`Linked to ${m[1]}. You can sign in with Google.`,`Vinculada a ${m[1]}. Puedes entrar con Google.`);
  if((m=s.match(/^(移除|选择|打开对话) (.+)$/)))return pick(({移除:'Remove',选择:'Select',打开对话:'Open conversation'})[m[1]],({移除:'Quitar',选择:'Seleccionar',打开对话:'Abrir conversación'})[m[1]])+' '+m[2];
  if((m=s.match(/^(试听|收藏)(.+)$/)))return pick(m[1]==='试听'?'Preview voice: ':'Favorite: ',m[1]==='试听'?'Escuchar voz: ':'Guardar en favoritos: ')+m[2];
  if((m=s.match(/^将从当前账号的资产中移除 (\d+) 项，文件按保留策略进入回收站。$/)))return pick(`Move ${number(m[1])} selected items to the trash. Files follow the retention policy.`,`Se moverán ${number(m[1])} elementos a la papelera según la política de conservación.`);
  if((m=s.match(/^(\d{1,2})月(\d{1,2})日$/)))return new Intl.DateTimeFormat(localeTag(),{month:'long',day:'numeric'}).format(new Date(2026,Number(m[1])-1,Number(m[2])));
  if((m=s.match(/^(\d{1,2})\.(\d{1,2})\s+(\d{1,2}):(\d{2})$/)))return new Intl.DateTimeFormat(localeTag(),{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date(2026,+m[1]-1,+m[2],+m[3],+m[4]));
  if((m=s.match(/^(\d{4})\/(\d+)\/(\d+)\s+(\d+):(\d+)(?::(\d+))?$/)))return new Intl.DateTimeFormat(localeTag(),{dateStyle:'medium',timeStyle:'short'}).format(new Date(+m[1],+m[2]-1,+m[3],+m[4],+m[5],+(m[6]||0)));
  if(/^\d{1,3}(,\d{3})*(\.\d+)?$/.test(s))return source.replace(s,number(s));
  // Protect quantities first so singular/plural remains grammatical in both languages.
  const quantities=[];let result=source.replace(/([\d,]+(?:\.\d+)?)\s*(张|条|首|段|次|秒|项|字)/g,(_,n,u)=>{quantities.push(unit(n,u));return '\uE000'+(quantities.length-1)+'\uE001';});
  result=result.replace(phrasePattern,k=>messages[k][locale==='en'?0:1]);
  result=result.replace(/\uE000(\d+)\uE001/g,(_,i)=>quantities[+i]);
  if(/[\u3400-\u9fff]/.test(result))missing.add(s);return result;
 }
 const protectedSelector='[data-user-content],[data-explore-i18n],script,style,noscript,textarea,input,code,pre,.creation-text,.detail-prompt,.session-title,.ref-name,.subject-pick span,.avatar,.top-avatar,.workspace-icon,.brand-mark,.voice-name,.voice-card strong';
 function protectedText(node){const p=node.parentElement;if(!p||p.closest(protectedSelector))return true;
  if(p.matches('.feed-heading h1')||p.matches('#session-name')&&document.body.classList.contains('creation-page'))return true;
  if(p.closest('.asset-card-info p')&&!p.closest('[data-curated]'))return true;
  if(p.matches('.live-task-card > p,[data-task-detail] > p:first-of-type'))return true;
  return false;
 }
 function textNode(node){if(protectedText(node)||!node.nodeValue.trim())return;let saved=originals.get(node);if(!saved||node.nodeValue!==saved.last)saved={source:node.nodeValue};let next=translate(saved.source);const date=saved.source.trim().match(/^(\d{2})\.(\d{2})$/);if(locale!=='zh-CN'&&date&&node.parentElement.closest('.asset-card-info'))next=new Intl.DateTimeFormat(localeTag(),{month:'short',day:'numeric'}).format(new Date(2026,+date[1]-1,+date[2]));saved.last=next;originals.set(node,saved);if(node.nodeValue!==next)node.nodeValue=next;}
 function attr(el,name){if(el.closest('[data-user-content],script,style')||el.matches('img,video,audio,iframe'))return;let map=attributes.get(el);if(!map){map={};attributes.set(el,map);}const current=el.getAttribute(name);let saved=map[name];if(!saved||current!==saved.last)saved={source:current};const next=translate(saved.source);saved.last=next;map[name]=saved;if(current!==next)el.setAttribute(name,next);}
 function control(parent,modal=false){if(parent.querySelector('[data-locale-select]'))return;const label=document.createElement('label');label.className=modal?'language-setting modal-language':'language-setting';label.innerHTML='<span aria-hidden="true">◎</span><select data-locale-select aria-label="界面语言"><option value="zh-CN">简体中文</option><option value="en">English</option><option value="es">Español</option></select>';label.querySelector('select').addEventListener('change',e=>setLocale(e.target.value));parent.insertBefore(label,modal?parent.lastElementChild:parent.firstChild);}
 function flush(){if(applying||disposed)return;applying=true;observer.disconnect();try{
  document.documentElement.lang=locale;
  const top=document.querySelector('.top-actions');if(top)control(top);
  document.querySelectorAll('.modal-top').forEach(el=>control(el,true));
  document.querySelectorAll('option:not([value])').forEach(el=>el.setAttribute('value',el.textContent));
  document.querySelectorAll('[data-locale-select]').forEach(el=>{el.value=locale;el.setAttribute('aria-label',locale==='zh-CN'?'界面语言':pick('Interface language','Idioma de la interfaz'));});
  const root=document.body;if(root){const walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);let node;while((node=walker.nextNode())){if(node.parentElement?.closest('[data-locale-select]'))continue;textNode(node);}
   root.querySelectorAll('[placeholder],[aria-label],[title]').forEach(el=>{if(el.matches('[data-locale-select]'))return;['placeholder','aria-label','title'].forEach(name=>{if(el.hasAttribute(name))attr(el,name);});});
   root.querySelectorAll('[data-i18n-currency]').forEach(el=>{const currency=el.dataset.i18nCurrency,formatter=new Intl.NumberFormat(localeTag(),{style:'currency',currency});el.textContent=formatter.format(Number(el.dataset.minorAmount)/10**formatter.resolvedOptions().maximumFractionDigits);});
  }
  document.querySelectorAll('[data-explore-i18n]').forEach(el=>{try{const values=JSON.parse(el.dataset.exploreI18n);el.textContent=values[locale]||values['zh-CN']||values.en||values.es||'';}catch{}});
  document.title=locale==='zh-CN'?'Safillu · 创作空间':pick('Safillu · Creative workspace','Safillu · Espacio de creación');
 }finally{applying=false;observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['placeholder','aria-label','title']});}}
 const observer=new MutationObserver(()=>{if(!queued){queued=true;queueMicrotask(()=>{queued=false;flush();});}});
 function setLocale(next){if(!supported.includes(next))return;locale=next;try{localStorage.setItem(storageKey,next);}catch{}flush();window.dispatchEvent(new CustomEvent('studio:localechange',{detail:{locale}}));}
 window.StudioI18n={setLocale,getLocale:()=>locale,t:translate,number,flush,getMissing:()=>[...missing],dispose:()=>{disposed=true;observer.disconnect();}};
 window.addEventListener('storage',e=>{if(e.key===storageKey&&supported.includes(e.newValue)){locale=e.newValue;flush();}});
 document.documentElement.lang=locale;flush();
})();
