/* Server-backed personal workspace. Authentication uses stock yudao endpoints. */
'use strict';
const cloudGoogleReturn=location.hash.match(/^#google-(callback|error)\/([A-Za-z0-9_-]+)$/);if(cloudGoogleReturn)history.replaceState(null,'',location.pathname+location.search+'#create');
const cloudConfig=window.STUDIO_CONFIG||{cloudEnabled:true,apiBase:''};
let cloudUser=null,cloudData=null,cloudEpoch=0,cloudLoading=false,cloudRefreshPromise=null,cloudUploadsPending=0;
let cloudTokens;try{cloudTokens=JSON.parse(sessionStorage.getItem('zaomeng-auth')||'null');}catch{cloudTokens=null;}
const cloudURLs=new Map(),draftRevisions=new Map(),draftWrites=new Map();
let draftTimer=null,draftDrain=null,draftError=null;
const studioRender=render,studioSyncComposer=syncComposer,studioMenu=menu,studioPreview=preview;
function cloudEmpty(){return {version:1,current:'guest',credits:0,subjects:[],records:[],voiceLikes:[],editDrafts:[],sessions:[{id:'guest',title:'默认创作',pinned:true,isDefault:true,created:Date.now(),draft:normalizeDraft(defaults())}]};}
function cloudTokenWrite(value){cloudTokens=value;if(value)sessionStorage.setItem('zaomeng-auth',JSON.stringify(value));else sessionStorage.removeItem('zaomeng-auth');}
function cloudRelease(){for(const value of cloudURLs.values())URL.revokeObjectURL(value);cloudURLs.clear();}
function cloudReset(){cloudEpoch++;cloudUser=null;cloudData=null;clearTimeout(draftTimer);draftWrites.clear();draftRevisions.clear();selectedAssets.clear();draftError=null;cloudRelease();state=cloudEmpty();page='create';render();}
function cloudFail(error){if(error?.code===401){cloudTokenWrite(null);cloudReset();}toast(error.message||'操作失败，请稍后重试');}
function cloudAction(fn){return el=>Promise.resolve().then(()=>fn(el)).catch(cloudFail);}
async function cloudRequest(path,options={},retry=true){
 const headers=new Headers(options.headers||{});headers.set('tenant-id','1');
 if(cloudTokens?.accessToken)headers.set('Authorization','Bearer '+cloudTokens.accessToken);
 if(options.body&&!(options.body instanceof FormData))headers.set('Content-Type','application/json');
 const response=await fetch((cloudConfig.apiBase||'')+path,{...options,headers,cache:'no-store',credentials:'same-origin'});
 if(response.status===429)throw Error('操作太频繁，请稍后再试');
 let result;try{result=await response.json();}catch{throw Error('服务暂不可用，请稍后重试');}
 if((response.status===401||result.code===401)&&retry&&cloudTokens?.refreshToken&&!path.includes('/system/auth/')){
  if(!cloudRefreshPromise){const epoch=cloudEpoch,refresh=cloudTokens.refreshToken;cloudRefreshPromise=(async()=>{const r=await cloudRequest('/admin-api/system/auth/refresh-token?refreshToken='+encodeURIComponent(refresh),{method:'POST'},false);if(epoch!==cloudEpoch)throw Error('账号状态已变化，请重新登录');cloudTokenWrite(r);await cloudRequest('/admin-api/aigc/media-session',{method:'POST'},false);})().finally(()=>cloudRefreshPromise=null);}
  await cloudRefreshPromise;return cloudRequest(path,options,false);
 }
 if(!response.ok||result.code!==0){const error=Error(result.msg||'操作失败');error.code=result.code||response.status;throw error;}
 return result.data;
}
async function cloudContent(r){
 const epoch=cloudEpoch;
 if(cloudURLs.has(r.id))return cloudURLs.get(r.id);
 const blob=window.StudioMedia?await window.StudioMedia.bytes(r):await(async()=>{const response=await fetch((cloudConfig.apiBase||'')+r.contentPath,{headers:{'tenant-id':'1',Authorization:'Bearer '+cloudTokens.accessToken},credentials:'same-origin'});if(!response.ok)throw Error('素材读取失败，请刷新登录后重试');const blob=await response.blob();if(blob.type.includes('application/json'))throw Error('素材暂不可用');return blob;})();
 if(epoch!==cloudEpoch)throw Error('账号已切换');
 const url=URL.createObjectURL(blob);cloudURLs.set(r.id,url);return url;
}
function cloudHydrateDraft(d,records){
 Object.assign(d,{...defaults(),...d});normalizeDraft(d);d.model='待配置';
 for(const key of ['refs','attachments'])d[key]=(d[key]||[]).map(r=>({...r,src:records.find(a=>a.id===String(r.assetId))?.src||''}));
 for(const prior of Object.values(d.modeDrafts||{})){normalizeDraft(prior);for(const key of ['refs','attachments'])prior[key]=(prior[key]||[]).map(r=>({...r,src:records.find(a=>a.id===String(r.assetId))?.src||''}));}
 return d;
}
async function cloudLoad(preferred){
 const epoch=cloudEpoch;cloudLoading=true;cloudStatus('正在加载…');
 try{
  const data=await cloudRequest('/admin-api/aigc/bootstrap');if(epoch!==cloudEpoch)return;
  await cloudRequest('/admin-api/aigc/media-session',{method:'POST'});
  const records=data.records;let cursor=data.assetNextId;const cursors=new Set();while(cursor){if(cursors.has(cursor))throw Error('资产分页异常，请刷新');cursors.add(cursor);const next=await cloudRequest('/admin-api/aigc/assets?beforeId='+encodeURIComponent(cursor)+'&limit=500');if(epoch!==cloudEpoch)return;records.push(...next.records);cursor=next.nextId;}for(const r of records)r.src=(cloudConfig.apiBase||'')+r.contentPath;
  if(epoch!==cloudEpoch)return;
  for(const s of data.sessions){cloudHydrateDraft(s.draft,records);for(const [mode,prior] of Object.entries(s.draft.modeDrafts||{}))draftRevisions.set(s.id+':'+mode,prior._revision||0);draftRevisions.set(s.id+':'+s.draft.mode,s.draft._revision||0);}
  cloudUser=data.user;cloudData=data;voices.splice(0,voices.length,...data.voices.map(v=>({...v,mine:v.visibility==='PRIVATE'})));motionTemplates.splice(0,motionTemplates.length,...data.motionTemplates.map(t=>({...t,duration:Math.round((t.duration||0)/1000),emoji:'↶'})));
  const chosen=preferred||state.current;
  state={version:1,sessions:data.sessions,records,subjects:data.subjects.map(s=>({...s,src:records.find(a=>a.id===s.assetId)?.src||''})),credits:data.credits,voiceLikes:data.voiceLikes,editDrafts:[],current:data.sessions.some(s=>s.id===chosen)?chosen:data.sessions[0].id};
  render();window.studioAfterLoad?.();
 }finally{cloudLoading=false;cloudStatus(draftError?'草稿未同步':'已同步');}
}
function cloudStatus(text){const node=$('#cloud-save-status');if(node){node.textContent=text;node.dataset.action=draftError?'cloud-draft-recovery':'';node.title=draftError?draftError.message:'';node.style.cursor=draftError?'pointer':'';}}
save=function(){
 if(!cloudUser||!cloudConfig.cloudEnabled)return true;
 const d=structuredClone(normalizeDraft(draft())),key=state.current+':'+d.mode;delete d.modeDrafts;
 // References are stable asset IDs. Object/data URLs are presentation-only.
 for(const field of ['refs','attachments'])d[field]=(d[field]||[]).map(({src,blobKey,...item})=>item);
 draftWrites.set(key,{conversation:state.current,draft:d});clearTimeout(draftTimer);cloudStatus('正在保存…');draftTimer=setTimeout(()=>cloudFlush().catch(cloudFail),700);return true;
};
async function cloudFlush(){
 clearTimeout(draftTimer);if(draftDrain){await draftDrain;if(draftWrites.size)return cloudFlush();return;}
 if(!cloudUser||!draftWrites.size)return;
 const epoch=cloudEpoch;
 draftDrain=(async()=>{while(draftWrites.size&&epoch===cloudEpoch){const [key,item]=draftWrites.entries().next().value;draftWrites.delete(key);
   try{const result=await cloudRequest('/admin-api/aigc/conversations/'+item.conversation+'/draft',{method:'PUT',body:JSON.stringify({draft:item.draft,revision:draftRevisions.get(key)||0})});if(epoch!==cloudEpoch)return;draftRevisions.set(key,result.revision);draftError=null;}
   catch(error){if(epoch!==cloudEpoch)return;draftError=error;if(!draftWrites.has(key))draftWrites.set(key,item);cloudStatus('草稿未同步 · '+error.message);throw error;}
  }cloudStatus('已同步');})().finally(()=>draftDrain=null);
 return draftDrain;
}
function cloudRequire(){if(cloudUser)return true;cloudLogin();return false;}
function cloudLogin(register=false){
 if(!cloudConfig.cloudEnabled){modal('进入创作工作台',`<p>登录和个人资产已在工作台启用。</p><p>请在同一局域网内打开工作台，继续注册或登录。</p>`,`<a class="primary-btn cloud-link" href="${esc(cloudConfig.studioUrl)}">打开工作台 ↗</a>`);return;}
 const emailFields=`<label>邮箱<input name="email" type="email" autocomplete="email" required maxlength="50" placeholder="输入邮箱地址"></label><label>邮箱验证码<div class="email-code-row"><input name="code" aria-label="邮箱验证码" inputmode="numeric" autocomplete="one-time-code" required pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="6 位验证码"><button class="secondary-btn email-send-code" type="button"><span>获取验证码</span><small data-user-content></small></button></div></label>`;
 modal(register?'创建Safillu账号':'欢迎回到Safillu',`<form id="cloud-auth-form" class="cloud-form ${register?'email-register-form':''}">${register?emailFields:'<label>邮箱 / 原账号<input name="username" autocomplete="username" required maxlength="50" placeholder="输入邮箱或已有账号"></label>'}<label>密码<input name="password" type="password" autocomplete="${register?'new-password':'current-password'}" required minlength="${register?8:4}" maxlength="16" placeholder="${register?'8–16 位密码':'输入密码'}"></label><p id="auth-error" class="cloud-error" role="alert"></p><p id="auth-status" class="email-code-status" role="status"></p><button class="primary-btn" type="submit">${register?'注册并进入空间':'登录'}</button><button class="subtle-button" type="button" data-action="${register?'cloud-login':'cloud-register'}">${register?'已有账号，去登录':'还没有账号？立即注册'}</button></form>`);
 const authForm=$('#cloud-auth-form');
 if(register){
  authForm.closest('.modal').classList.add('email-register');
  const send=authForm.querySelector('.email-send-code'),email=authForm.elements.email,error=authForm.querySelector('#auth-error'),status=authForm.querySelector('#auth-status');
  send.addEventListener('click',async()=>{
   if(!email.reportValidity())return;
   send.disabled=true;error.textContent='';status.textContent='';send.querySelector('span').textContent='发送中…';
   try{
    const result=await cloudRequest('/admin-api/aigc/auth/email/code',{method:'POST',body:JSON.stringify({email:email.value.trim(),locale:window.StudioI18n?.getLocale()||'zh-CN'})},false);
    if(!authForm.isConnected)return;
    status.textContent='验证码已发送，5 分钟内有效，请查收邮件';
    send.querySelector('span').textContent='重新发送';
    const until=Date.now()+Math.max(1,Number(result.retryAfter)||60)*1000;
    const tick=()=>{if(!authForm.isConnected){clearInterval(timer);return;}const left=Math.max(0,Math.ceil((until-Date.now())/1000));send.querySelector('small').textContent=left?` (${left}s)`:'';if(!left){clearInterval(timer);send.disabled=false;}};
    const timer=setInterval(tick,1000);tick();authForm.elements.code.focus();
   }catch(e){if(authForm.isConnected){error.textContent=e.message;send.disabled=false;send.querySelector('span').textContent='获取验证码';}}
  });
 }
 authForm.addEventListener('submit',async event=>{event.preventDefault();const form=event.target;if(!form.reportValidity())return;const values=Object.fromEntries(new FormData(form));const button=form.querySelector('[type="submit"]');button.disabled=true;form.querySelector('#auth-error').textContent='';
  try{const body=register?{email:values.email.trim(),code:values.code,password:values.password}:{account:values.username.trim(),password:values.password};const tokens=await cloudRequest('/admin-api/aigc/auth/email/'+(register?'register':'login'),{method:'POST',body:JSON.stringify(body)},false);if(!form.isConnected)return;const authPage=page,restoreAuthReturn=window.studioCaptureAuthReturn?.();cloudTokenWrite(tokens);cloudReset();page=authPage;restoreAuthReturn?.();await cloudLoad();closeModal();toast(register?'账号已创建，欢迎开始创作':'登录成功');}catch(error){if(form.isConnected)form.querySelector('#auth-error').textContent=error.message;}finally{button.disabled=false;}
 });
}
render=function(){studioRender();const name=cloudUser?.nickname||cloudUser?.username||'登录 / 注册';
 const route='#'+page+(cloudUser?'/'+encodeURIComponent(state.current):'');if(location.hash!==route)history.replaceState(null,'',route);
 const caption=$('.composer-caption');if(caption)caption.textContent=cloudConfig.cloudEnabled?'素材与草稿保存到个人账号 · 生成服务稍后配置':'界面预览 · 登录和个人资产请进入工作台';
 const avatar=name.slice(0,1).toUpperCase();$('.profile .avatar').textContent=avatar;$('.top-avatar').textContent=avatar;
 $('.profile > span:nth-child(2)').innerHTML=(cloudUser?`<span data-user-content>${esc(name)}</span>`:esc(name))+`<small>${cloudUser?'个人空间':'保存你的每一次灵感'}</small>`;
 $('.demo-badge').innerHTML='<i></i>'+(cloudUser?'个人空间':cloudConfig.cloudEnabled?'未登录':'界面预览');$('.credit-label').textContent='可用额度';
 if(!$('#cloud-save-status')){const status=document.createElement('span');status.id='cloud-save-status';status.className='cloud-save-status';$('.composer-topline').append(status);}
 cloudStatus(cloudUser?(cloudLoading?'正在同步…':draftError?'草稿未同步':'已同步'):'登录后自动保存');
 if($('.quote-line'))$('.quote-line').textContent='生成服务稍后配置，素材、主体和创作草稿支持账号保存。';
 if(!cloudConfig.cloudEnabled&&!$('#cloud-entry')){const banner=document.createElement('div');banner.id='cloud-entry';banner.className='cloud-entry';banner.innerHTML=`<span>账号与个人资产已在工作台启用</span><a href="${esc(cloudConfig.studioUrl)}">进入工作台 ↗</a>`;$('#view').prepend(banner);}
};
syncComposer=function(){draft().model='待配置';studioSyncComposer();$('#cost').textContent='—';$('#cost-unit').textContent=' / 待配置';$('#generate').title=cloudUser?'生成服务稍后配置，当前不会扣除额度':'登录后保存草稿';};
menu=function(kind){if(kind==='model'){closeMenu();openMenu='model';$('#popover').hidden=false;$('#popover').innerHTML='<div class="popover-title">生成模型</div><p class="helper cloud-model-empty">模型服务稍后配置。你可以先上传素材、整理主体和保存创作草稿。</p>';return;}studioMenu(kind);};
for(const m of modes){m.models=['待配置'];observedModels[m.id]=m.models;}
newSession=async function(){if(!cloudRequire())return;await cloudFlush();const id=await cloudRequest('/admin-api/aigc/conversations',{method:'POST',body:JSON.stringify({title:'未命名创作'})});page='create';await cloudLoad(id);$('#prompt')?.focus();};actions['new-session']=cloudAction(newSession);
const cloudSwitch=switchSession;switchSession=function(id){if(draftWrites.size)cloudFlush().catch(cloudFail);cloudSwitch(id);};
commitRename=function(input,cancel=false){if(editingSession!==input.dataset.renameId)return;const id=editingSession;editingSession=null;if(cancel){renderSessions();return;}const name=input.value.trim();if(!name){renderSessions();return;}cloudAction(async()=>{if(!cloudRequire())return;await cloudFlush();await cloudRequest('/admin-api/aigc/conversations/'+id,{method:'PATCH',body:JSON.stringify({title:name})});await cloudLoad();})();};
sessionOptions=function(id){const s=state.sessions.find(s=>s.id===id);if(!cloudRequire())return;modal('管理对话',`<p data-user-content>${esc(s.title)}</p><button class="menu-item" data-action="pin" data-id="${id}">${s.pinned?'取消置顶':'置顶对话'}</button><button class="menu-item" data-action="rename" data-id="${id}">重命名</button>${s.isDefault?'':`<button class="menu-item danger" data-action="delete-session" data-id="${id}">删除对话</button>`}`);};actions['session-options']=el=>sessionOptions(el.dataset.id);
actions.pin=cloudAction(async el=>{if(!cloudRequire())return;await cloudFlush();const s=state.sessions.find(s=>s.id===el.dataset.id);await cloudRequest('/admin-api/aigc/conversations/'+s.id,{method:'PATCH',body:JSON.stringify({pinned:!s.pinned})});closeModal();await cloudLoad();});
actions['confirm-delete-session']=cloudAction(async el=>{if(!cloudRequire())return;await cloudFlush();await cloudRequest('/admin-api/aigc/conversations/'+el.dataset.id,{method:'DELETE'});closeModal();await cloudLoad();toast('对话已删除，资产仍保留');});
generate=async function(){if(!cloudRequire())return;if(cloudUploadsPending>0){toast('素材上传中，请等待上传完成');return;}save();try{await cloudFlush();await cloudRequest('/admin-api/aigc/tasks',{method:'POST',body:JSON.stringify({conversationId:state.current,operation:draft().mode.toUpperCase(),clientRequestId:uid(),draft:draft()})});}catch(error){cloudFail(error);}};actions.generate=generate;
actions.regenerate=el=>{reedit(el.dataset.id,false);generate();};
actions.workspace=()=>cloudUser?cloudProfile():cloudLogin();actions['cloud-login']=()=>cloudLogin();actions['cloud-register']=()=>cloudLogin(true);
actions['cloud-draft-recovery']=()=>modal('草稿尚未同步',`<p>${esc(draftError?.message||'请检查网络后重试')}</p><p>重新加载会放弃当前未同步的修改，请先复制需要保留的文字。</p>`,`<button class="secondary-btn" data-action="cloud-draft-retry">重试保存</button><button class="primary-btn" data-action="cloud-draft-reload">放弃修改并重新加载</button>`);
actions['cloud-draft-retry']=cloudAction(async()=>{await cloudFlush();closeModal();toast('草稿已同步');});
actions['cloud-draft-reload']=cloudAction(async()=>{clearTimeout(draftTimer);draftWrites.clear();draftError=null;await cloudLoad();closeModal();});
actions['cloud-logout']=cloudAction(async()=>{await cloudFlush();await cloudRequest('/admin-api/system/auth/logout',{method:'POST'});cloudTokenWrite(null);cloudReset();closeModal();toast('已退出登录');});
actions.credits=()=>{if(!cloudRequire())return;const q=cloudData.quota;modal('创作额度',`<div class="cloud-quota"><div><small>可用</small><strong>${esc(q.available_points)}</strong></div><div><small>处理中</small><strong>${esc(q.reserved_points)}</strong></div><div><small>已使用</small><strong>${esc(q.consumed_points)}</strong></div></div><p>当前尚未启用模型服务。保存草稿、上传素材和整理资产不扣创作额度。</p>`);};delete actions.refill;
async function cloudProfile(){try{const p=await cloudRequest('/admin-api/system/user/profile/get');modal('账号与个人资料',`<form id="cloud-profile-form" class="cloud-form"><label>账号<input value="${esc(p.username)}" disabled></label><label>昵称<input name="nickname" maxlength="30" value="${esc(p.nickname)}" required></label><label>邮箱<input type="email" name="email" maxlength="50" value="${esc(p.email||'')}"></label><p class="cloud-error" id="profile-error" role="alert"></p><button type="submit" class="primary-btn">保存资料</button></form>`,`<button class="secondary-btn" data-action="cloud-password">修改密码</button><button class="secondary-btn" data-action="cloud-logout">退出登录</button>`);$('#cloud-profile-form').onsubmit=async e=>{e.preventDefault();const form=e.target,button=form.querySelector('button');button.disabled=true;try{await cloudRequest('/admin-api/system/user/profile/update',{method:'PUT',body:JSON.stringify(Object.fromEntries(new FormData(form)))});await cloudFlush();await cloudLoad();closeModal();toast('资料已更新');}catch(error){$('#profile-error').textContent=error.message;}finally{button.disabled=false;}};}catch(error){cloudFail(error);}}
actions['cloud-password']=()=>{modal('修改密码',`<form id="cloud-password-form" class="cloud-form"><label>当前密码<input type="password" name="oldPassword" autocomplete="current-password" required minlength="4" maxlength="16"></label><label>新密码<input type="password" name="newPassword" autocomplete="new-password" required minlength="8" maxlength="16"></label><label>确认新密码<input type="password" name="confirmation" autocomplete="new-password" required minlength="8" maxlength="16"></label><p class="cloud-error" id="password-error" role="alert"></p><button type="submit" class="primary-btn">更新密码</button></form>`);$('#cloud-password-form').onsubmit=async e=>{e.preventDefault();const form=e.target,values=Object.fromEntries(new FormData(form));if(values.newPassword!==values.confirmation){$('#password-error').textContent='两次密码不一致';return;}const button=form.querySelector('button');button.disabled=true;try{await cloudRequest('/admin-api/system/user/profile/update-password',{method:'PUT',body:JSON.stringify({oldPassword:values.oldPassword,newPassword:values.newPassword})});closeModal();toast('密码已更新');}catch(error){$('#password-error').textContent=error.message;}finally{button.disabled=false;}};};
actions.like=cloudAction(async el=>{if(!cloudRequire())return;const r=record(el.dataset.id);if(!r)throw Error('请先把素材上传到个人资产');await cloudFlush();await cloudRequest('/admin-api/aigc/favorites/asset/'+r.id,{method:'PUT',body:JSON.stringify({liked:!r.liked})});await cloudLoad();});
actions['bulk-like']=cloudAction(async()=>{if(!cloudRequire())return;await cloudFlush();for(const id of selectedAssets)await cloudRequest('/admin-api/aigc/favorites/asset/'+id,{method:'PUT',body:'{"liked":true}'});await cloudLoad();toast('所选资产已收藏');});
async function cloudDeleteAssets(ids){if(!cloudRequire())return;if(!ids.length||ids.length>200)throw Error('每次请选择 1 至 200 项资产');await cloudFlush();await cloudRequest('/admin-api/aigc/assets/delete',{method:'POST',body:JSON.stringify(ids)});selectedAssets.clear();closeModal();await cloudLoad();toast('资产已移入回收站');}
actions['confirm-delete-record']=cloudAction(el=>cloudDeleteAssets([el.dataset.id]));actions['confirm-bulk-delete']=cloudAction(()=>cloudDeleteAssets([...selectedAssets]));
actions['bulk-delete']=()=>modal('删除所选资产？',`<p>将从当前账号的资产中移除 ${selectedAssets.size} 项，文件按保留策略进入回收站。</p>`,`<button class="secondary-btn" data-action="close-modal">取消</button><button class="primary-btn" data-action="confirm-bulk-delete">确认删除</button>`);
assetBlob=async function(r){if(!r.cloud){const response=await fetch(r.src);return response.blob();}const src=await cloudContent(r);return (await fetch(src)).blob();};
function cloudAddReference(r,slot='reference',subjectVersionId){
 const d=draft(),category=StudioUtils.category(r),mode=d.mode;
 if(category==='document')throw Error('文档支持预览和下载');
 const role=['avatar','motion'].includes(mode);
 if(role){if(category==='image')slot='role';else if(mode==='avatar'&&category==='audio')slot='speech';else if(mode==='motion'&&category==='video')slot='motion';else throw Error('该素材类型不适合当前创作模式');}
 else if(mode==='video'){if(!['image','audio','video'].includes(category))throw Error('请选择图片、音频或视频');}
 else if(mode==='audio'){if(category!=='audio')throw Error('音频模式请引用音频素材');}
 else if(category!=='image')throw Error('当前模式请引用图片素材');
 const field=category==='image'?'refs':'attachments',list=d[field]||[],other=d[field==='refs'?'attachments':'refs']||[];
 if(list.some(x=>String(x.assetId)===r.id&&x.slot===slot))return false;
 const limits=mode==='video'&&typeof liveCapability==='function'?liveCapability()?.limits:null;
 if(limits){const key=category==='image'?'maxRefs':category==='audio'?'maxAudioRefs':'maxVideoRefs',count=[...list,...other].filter(x=>(x.type||'').startsWith(category+'/')).length;if(count>=(limits[key]||0))throw Error(category==='image'?'已达到当前模型的图片参考数量上限':category==='audio'?'已达到当前模型的音频参考数量上限':'已达到当前模型的视频参考数量上限');}
 const kept=role?list.filter(x=>x.slot!==slot):list,max=mode==='video'&&limits?(limits.maxRefs||0)+(limits.maxAudioRefs||0)+(limits.maxVideoRefs||0):mode==='video'?12:role?2:6;
 if(kept.length+other.length>=max)throw Error('当前模式最多引用 '+max+' 个素材');
 d[field]=[...kept,{assetId:r.id,src:r.src,name:r.title||r.prompt,type:r.mime,slot,...(subjectVersionId?{subjectVersionId}: {})}];return true;
}
useAsset=function(id){const r=findRecord(id);if(!cloudRequire())return;if(!r?.cloud){closeModal();navigate('create');draft().prompt=r?.fullPrompt||r?.prompt||'';save();syncComposer();toast('示例描述已载入');return;}
 try{const category=StudioUtils.category(r),mode=draft().mode;if(category==='document')throw Error('文档支持预览和下载');if(!['avatar','motion','video'].includes(mode)&&((category==='image'&&mode==='audio')||category!=='image'))setMode(category==='image'?'image':category==='audio'?'audio':'video');cloudAddReference(r);save();closeModal();navigate('create');syncComposer();}catch(e){cloudFail(e);}
};
actions['reference-from-asset']=el=>{try{cloudAddReference(record(el.dataset.id),referenceSlot);save();closeModal();syncComposer();}catch(e){cloudFail(e);}};
useSubject=function(id){const s=state.subjects.find(s=>s.id===id);if(!s||!cloudRequire())return;try{cloudAddReference(record(s.assetId),'subject',s.versionId);draft().prompt+=(draft().prompt?' ':'')+'@'+s.name+' ';save();closeModal();navigate('create');syncComposer();}catch(e){cloudFail(e);}};
const cloudReedit=reedit;reedit=function(id,notify=true){const r=record(id);if(!r)return;if(StudioUtils.category(r)==='document'){toast('文档支持预览和下载');return;}cloudReedit(id,false);if(r.cloud&&r.source==='UPLOAD'){try{cloudAddReference(r);save();syncComposer();}catch(e){cloudFail(e);}}if(notify)toast('描述、参数和参考素材已载入');};actions.reedit=el=>reedit(el.dataset.id);
const cloudRenderAssets=renderAssets;renderAssets=function(){cloudRenderAssets();$('.asset-header-actions')?.insertAdjacentHTML('afterbegin','<button class="secondary-btn" data-action="cloud-trash">回收站</button>');};
actions['cloud-trash']=cloudAction(async()=>{if(!cloudRequire())return;const rows=await cloudRequest('/admin-api/aigc/assets/trash');modal('资产回收站',rows.length?'<p>恢复后会回到资产库，已移除的草稿引用需要重新添加。</p>'+rows.map(r=>`<div class="menu-item"><span><span data-user-content>${esc(r.title)}</span> · ${esc(r.kind)}</span><button class="secondary-btn" data-action="cloud-restore" data-id="${r.id}">恢复</button></div>`).join(''):'<div class="empty"><p>回收站为空</p></div>');});
actions['cloud-restore']=cloudAction(async el=>{await cloudFlush();await cloudRequest('/admin-api/aigc/assets/restore',{method:'POST',body:JSON.stringify([el.dataset.id])});await cloudLoad();await actions['cloud-trash']();toast('素材已恢复到资产库');});
actions['save-subject-from']=cloudAction(async el=>{if(!cloudRequire())return;const name=$('#subject-from-name').value.trim();if(!name)throw Error('请输入主体名称');await cloudFlush();await cloudRequest('/admin-api/aigc/subjects',{method:'POST',body:JSON.stringify({name,assetId:el.dataset.id})});closeModal();await cloudLoad();toast('主体已保存');});
actions['confirm-delete-subject']=cloudAction(async el=>{if(!cloudRequire())return;await cloudFlush();await cloudRequest('/admin-api/aigc/subjects/'+el.dataset.id,{method:'DELETE'});closeModal();await cloudLoad();});
actions['like-voice']=cloudAction(async el=>{if(!cloudRequire())return;const id=el.dataset.id;await cloudFlush();await cloudRequest('/admin-api/aigc/favorites/voice/'+id,{method:'PUT',body:JSON.stringify({liked:!state.voiceLikes.includes(id)})});await cloudLoad();voiceLibrary();});
const cloudReferenceOptions=actions['reference-options'];actions['reference-options']=()=>{if(!cloudRequire())return;cloudReferenceOptions();const helper=$('.modal .helper');if(helper)helper.textContent='上传素材会保存到当前账号，供你的个人创作使用。';};
const browserVoiceLibrary=voiceLibrary;voiceLibrary=function(){if(!cloudRequire())return;if(!voices.length){modal('音色库','<div class="empty"><h2>音色库待配置</h2><p>模型接入后可在这里筛选、试听和收藏音色。</p></div>');return;}browserVoiceLibrary();const helper=$('.modal .helper');if(helper)helper.textContent='音色来自已配置目录；试听与语音生成服务稍后接入。';};actions['voice-library']=voiceLibrary;actions['preview-voice']=()=>toast('音色试听服务稍后配置');actions['select-voice']=el=>{const v=voices.find(v=>v.id===el.dataset.id);if(!v)return;draft().voice=v.name;draft().voiceId=v.id;save();closeModal();syncComposer();};
actions['pick-motion']=()=>{if(!cloudRequire())return;modal('选择动作素材',`<p>${motionTemplates.length?'选择已配置的动作模板，或上传参考视频。':'动作模板稍后配置，你可以先上传参考视频。'}</p><div class="motion-grid">${motionTemplates.map(t=>`<button data-action="select-motion" data-id="${t.id}">${esc(t.name)}</button>`).join('')}</div>`,`<button class="primary-btn" data-action="upload-motion">上传参考视频</button>`);};
upload=function(kind){if(!cloudRequire())return;closeModal();uploadKind=kind;referenceSlot=['role','speech','motion'].includes(kind)?kind:'reference';mediaInput.value='';mediaInput.multiple=!['role','speech','motion','subject'].includes(kind);mediaInput.accept=kind==='motion'?'video/*':kind==='speech'?'audio/*':kind==='role'||kind==='subject'?'image/*':kind==='asset'?'image/*,audio/*,video/*,.txt,.md,.pdf':draft().mode==='video'?'image/*,audio/*,video/*':draft().mode==='audio'?'audio/*':'image/*';mediaInput.click();};
async function cloudUpload(files,kind){if(!files.length||!cloudRequire())return;const uploadEpoch=cloudEpoch;cloudUploadsPending++;if($('#composer'))syncComposer();try{await cloudFlush();if(uploadEpoch!==cloudEpoch)return;const conversation=state.current;const added=[];for(const file of files){if(uploadEpoch!==cloudEpoch)return;if(file.size>50*1024*1024)throw Error('单个素材不能超过 50MB');const form=new FormData();form.append('file',file);const id=await cloudRequest('/admin-api/aigc/assets/upload',{method:'POST',body:form});added.push(id);}if(uploadEpoch!==cloudEpoch)return;await cloudLoad(conversation);if(uploadEpoch!==cloudEpoch)return;
 if(kind==='subject'){const name=pendingSubjectName||files[0].name.replace(/\.[^.]+$/,'').slice(0,20);await cloudRequest('/admin-api/aigc/subjects',{method:'POST',body:JSON.stringify({name,assetId:added[0]})});await cloudLoad(conversation);pendingSubjectName='';}
 else if(kind!=='asset'){for(const id of added)cloudAddReference(record(id),referenceSlot);save();syncComposer();}
 render();toast('素材已保存到个人账号');
 }finally{cloudUploadsPending=Math.max(0,cloudUploadsPending-1);if($('#composer'))syncComposer();}
}
document.addEventListener('change',event=>{if(!['media-file-input','file-input'].includes(event.target.id))return;event.stopImmediatePropagation();cloudUpload([...event.target.files],uploadKind).catch(cloudFail);},true);
document.addEventListener('drop',event=>{if(!event.target.closest('#composer')||!event.dataTransfer?.files.length)return;event.preventDefault();event.stopImmediatePropagation();referenceSlot='reference';cloudUpload([...event.dataTransfer.files],'reference').catch(cloudFail);},true);
const cloudOldPreview=preview;preview=function(id){const r=findRecord(id);if(r?.cloud&&r.mode==='document'&&!r.src){cloudAction(async()=>{r.src=await cloudContent(r);cloudOldPreview(id);})();return;}cloudOldPreview(id);const helper=$('.detail-sidebar .helper');if(r?.cloud&&helper)helper.textContent='生成及图片处理服务稍后配置，当前可以保存编辑说明。';};actions.preview=el=>preview(el.dataset.id);
const cloudOpenEditor=actions['edit-tool'];actions['edit-tool']=el=>{if(!cloudRequire())return;if(StudioUtils.category(record(el.dataset.id))!=='image'){toast('图片编辑工具仅支持图片素材');return;}cloudOpenEditor(el);const saved=record(el.dataset.id)?.editDrafts?.[el.dataset.tool];const helper=$('.modal .helper');if(helper)helper.textContent='编辑说明和涂抹草稿会保存到当前资产；图片处理服务稍后配置。';if(!saved)return;$('#edit-setting').value=saved.value;$('#edit-instruction').value=saved.instruction||'';if(saved.maskAssetId){const image=new Image(),canvas=$('#edit-mask');image.onload=()=>{if(canvas.isConnected)canvas.getContext('2d').drawImage(image,0,0,canvas.width,canvas.height);};image.src=(cloudConfig.apiBase||'')+'/admin-api/aigc/assets/'+saved.maskAssetId+'/content';}};
actions['save-edit-draft']=cloudAction(async el=>{if(!cloudRequire())return;const r=record(el.dataset.id);if(!r)throw Error('请先上传素材');const tool=el.dataset.tool,value=$('#edit-setting').value,instruction=$('#edit-instruction').value,revision=r.editDrafts?.[tool]?.revision||0;const mask=await new Promise(resolve=>$('#edit-mask').toBlob(resolve,'image/png'));await cloudFlush();const form=new FormData();form.append('file',mask,'edit-mask.png');const maskId=await cloudRequest('/admin-api/aigc/assets/upload',{method:'POST',body:form});await cloudRequest('/admin-api/aigc/assets/'+r.id+'/edit-draft',{method:'PUT',body:JSON.stringify({maskAssetId:maskId,tool,value,instruction,revision})});await cloudLoad();closeModal();toast('编辑说明与涂抹草稿已保存，处理服务稍后配置');});
details=function(id){const r=findRecord(id);modal('素材信息',`<dl class="detail-list"><dt>名称</dt><dd data-user-content>${esc(r.title||r.prompt)}</dd><dt>来源</dt><dd>${r.cloud?'个人账号 · '+(r.source==='UPLOAD'?'上传素材':'生成资产'):'精选示例'}</dd><dt>类型</dt><dd>${esc(r.mime||r.mode)}</dd><dt>比例</dt><dd>${esc(r.ratio)}</dd><dt>创建时间</dt><dd>${dateLabel(r.created)}</dd></dl>`);};
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='hidden'&&cloudUser)cloudFlush().catch(()=>{});});
window.addEventListener('beforeunload',event=>{if(draftWrites.size||draftDrain){event.preventDefault();event.returnValue='';}});
const cloudInitialRoute=location.hash.slice(1).split('/');
state=cloudEmpty();page=['create','assets','explore','tools','subjects','tasks','payments'].includes(cloudInitialRoute[0])?cloudInitialRoute[0]:'create';render();
if(cloudConfig.cloudEnabled&&cloudTokens)cloudLoad(cloudInitialRoute[1]).catch(cloudFail);
