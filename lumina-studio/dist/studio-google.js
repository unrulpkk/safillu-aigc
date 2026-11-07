/* Google OIDC authorization code flow; application tokens never travel in URLs. */
'use strict';
let googleSubmitting=false;
const googlePendingKey='zaomeng-google-login';
async function renderGoogleOption(){const mount=$('#google-auth-entry');if(!mount)return;try{const config=await cloudRequest('/admin-api/aigc/auth/google/config',{},false);if(!mount.isConnected)return;mount.innerHTML=`<button type="button" class="google-login-btn" data-action="google-login" ${config.enabled?'':'disabled'}>使用 Google 登录 / 注册</button><p class="helper google-help">${config.enabled?'首次授权将自动创建账号。已有账号请先登录，再在个人资料中关联 Google。':esc(config.reason||'Google 登录尚未配置。')}</p><div class="auth-divider">或使用账号密码</div>`;}catch(error){if(mount.isConnected)mount.innerHTML='<p class="helper">Google 登录暂不可用，请使用账号密码。</p>';}}
const googleCloudLogin=cloudLogin;cloudLogin=function(register=false){googleCloudLogin(register);if(register)return;const form=$('#cloud-auth-form');if(form){form.insertAdjacentHTML('beforebegin','<div id="google-auth-entry"><p class="helper">正在检查 Google 登录…</p></div>');renderGoogleOption();}};
async function startGoogle(action='LOGIN'){
 if(googleSubmitting)return;if(action==='BIND'&&!cloudRequire())return;googleSubmitting=true;
 try{await cloudFlush();const bytes=new Uint8Array(32);crypto.getRandomValues(bytes);const proof=btoa(String.fromCharCode(...bytes)).replaceAll('+','-').replaceAll('/','_').replaceAll('=','');
 sessionStorage.setItem(googlePendingKey,JSON.stringify({proof,action,page,current:state.current,time:Date.now()}));
 const response=await cloudRequest('/admin-api/aigc/auth/google/start',{method:'POST',body:JSON.stringify({proof,action})});const url=new URL(response.authorizeUrl);if(url.protocol!=='https:'||url.hostname!=='accounts.google.com'||url.pathname!=='/o/oauth2/v2/auth')throw Error('Google 授权地址校验失败');location.assign(url.href);
 }catch(error){sessionStorage.removeItem(googlePendingKey);cloudFail(error);}finally{googleSubmitting=false;}
}
actions['google-login']=()=>startGoogle();actions['google-bind']=()=>startGoogle('BIND');
const googleCloudProfile=cloudProfile;cloudProfile=async function(){await googleCloudProfile();const form=$('#cloud-profile-form');if(!form)return;try{const linked=await cloudRequest('/admin-api/aigc/auth/google/linked');if(!form.isConnected)return;form.insertAdjacentHTML('afterend',`<div class="google-account"><strong>Google 账号</strong>${linked.linked?`<p class="helper">已关联 ${esc(linked.name)}，可用 Google 登录当前账号。</p>`:'<p class="helper">关联后可用 Google 登录，保留当前资产和积分。</p><button class="google-login-btn" data-action="google-bind">关联 Google 账号</button>'}</div>`);}catch(error){if(form.isConnected)form.insertAdjacentHTML('afterend','<p class="helper">Google 关联状态暂不可用。</p>');}};
async function completeGoogleLogin(){
 if(!cloudGoogleReturn)return;history.replaceState(null,'',location.pathname+location.search+'#create');
 let pending;try{pending=JSON.parse(sessionStorage.getItem(googlePendingKey)||'null');}catch{}
 sessionStorage.removeItem(googlePendingKey);
 if(cloudGoogleReturn[1]==='error'){toast(cloudGoogleReturn[2]==='cancelled'?'Google 授权已取消':'Google 登录验证失败，请重新尝试');return;}
 if(!pending||Date.now()-pending.time>600000){toast('Google 登录流程已失效，请从当前浏览器重新发起');return;}
 try{const response=await cloudRequest('/admin-api/aigc/auth/google/exchange',{method:'POST',body:JSON.stringify({ticket:cloudGoogleReturn[2],proof:pending.proof})});
  if(!response.bound){cloudTokenWrite(response.tokens);cloudReset();}page=['create','assets','tasks','payments','subjects','explore','tools'].includes(pending.page)?pending.page:'create';await cloudLoad(pending.current);closeModal();render();toast(response.bound?'Google 已关联当前账号，资产和积分保持不变':'Google 登录成功');
 }catch(error){cloudFail(error);cloudLogin();const message=$('#auth-error');if(message)message.textContent=error.message;}
}
const googleReturnPromise=completeGoogleLogin();
