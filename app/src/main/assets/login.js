(function () {
  var credentials = window.__kejianCredentials || {};
  delete window.__kejianCredentials;
  window.__kejianAuth = {state:'running'};
  function persist(){try{window.sessionStorage.setItem('kejianAuth',JSON.stringify(window.__kejianAuth));}catch(ignored){}}
  persist();
  window.__kejianAuthPending = true;
  function nodes() { return Array.prototype.map.call(document.querySelectorAll('*'),function(n){return n.__vue__;}).filter(Boolean); }
  function find() { return nodes().find(function(v){return v.form && (credentials.mode==='sms' ? typeof v.verifyPhone==='function' && 'smsCode' in v.form : typeof v.verifyWebUser==='function' && 'account' in v.form);}); }
  function result(state,message) {window.__kejianAuth={state:state,message:message||''};persist();}
  function graphic(v) {window.__kejianAuth={state:'captcha',image:v.picture.base64||'',message:'请填写图形验证码'};persist();}
  function finish() {window.__kejianAuthPending=false;}
  async function waitPictures(v,operation){
    var original=v.getPictureVerifyCode,pending;
    if(typeof original==='function')v.getPictureVerifyCode=function(){var task=original.apply(v,arguments);pending=Promise.resolve(task);return task;};
    try{await operation();if(pending)await pending;}finally{if(typeof original==='function')v.getPictureVerifyCode=original;}
  }
  async function execute(v) {
    if(!v) {result('error','认证组件尚未就绪，请稍后重试');return;}
    var sms=credentials.mode==='sms', action=credentials.action||'login';
    if(action==='prepare') {if(sms){if(!v.picture.base64)await v.getPictureVerifyCode();graphic(v);}else result('ready');return;}
    if(action==='refresh') {await v.getPictureVerifyCode();graphic(v);return;}
    if(sms)v.form.phone=credentials.account;else {v.form.account=credentials.account;v.form.password=credentials.password;}
    if(action==='send' && !v.canSendCaptcha) {result('cooldown','请等待倒计时结束');return;}
    if(action==='send' || (!sms && v.picture && v.picture.isShow)) {
      if(!credentials.captcha) {graphic(v);return;}
      v.picture.pictureCode=credentials.captcha;
      await waitPictures(v,function(){return v.verifyCaptcha();});
      if(!v.picture.isSuccess) {graphic(v);return;}
    }
    if(action==='send') {
      // Observe the official component's own toast and cooldown. No extra SMS requests.
      var toast=v.showToast, message='';
      v.showToast=function(m){message=String(m||'');if(toast)toast.call(v,m);};
      try {await waitPictures(v,function(){return v.sendCaptcha();});} finally {v.showToast=toast;}
      var failed=message && !/成功|已发送/.test(message);
      result(failed?'smsError':'smsSent',message||'短信请求已处理，请查看手机');
      if(failed && v.picture)window.__kejianAuth.image=v.picture.base64||'';
      return;
    }
    var listener=function(r) {
      if(r.code==='0x000000') {
        if(r.data && ['secondary','changePwd','phone','bindUser'].indexOf(r.data.action)>=0)result('error','该账号需要额外验证或初始化，请先在智慧潍苑官方应用完成后重试');
        else result('success','登录成功，正在检查会话…');
      } else if(v.picture && v.picture.isShow && !v.picture.isSuccess)graphic(v);
      else result('error',String(r.msg||'登录失败，请检查输入后重试'));
    };
    if(v.$on)v.$on('loginChange',listener);
    try {
      if(sms){v.form.smsCode=credentials.password;await v.verifyPhone();}else await v.verifyWebUser();
    } finally {if(v.$off)v.$off('loginChange',listener);}
    if(window.__kejianAuth.state==='running' && v.picture && v.picture.isShow && !v.picture.isSuccess)graphic(v);
  }
  var v=find();
  if(v){execute(v).catch(function(){result('error','认证请求失败，请检查网络后重试');}).then(finish);return true;}
  var parent=nodes().find(function(v){return typeof v.toLoginSuccess==='function' && typeof v.type==='number';});
  if(!parent){result('error','认证页面尚未就绪，请稍后重试');finish();return false;}
  if(credentials.mode==='sms' && !parent.isPhoneLoginType){result('error','官方暂未开放手机验证码登录');finish();return true;}
  parent.type=credentials.mode==='sms'?2:1;
  parent.$nextTick(function(){execute(find()).catch(function(){result('error','认证请求失败，请重试');}).then(finish);});
  return true;
})();
