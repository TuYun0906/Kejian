(async function(){
  var auth=window.__kejianAuth;
  try{if(!auth)auth=JSON.parse(window.sessionStorage.getItem('kejianAuth')||'null');}catch(ignored){}
  window.__kejianAuth=auth;
  function persist(){try{window.sessionStorage.setItem('kejianAuth',JSON.stringify(auth));}catch(ignored){}}
  if(!auth || auth.state!=='success')return;
  auth.state='checking';persist();
  for(var attempt=0;attempt<3;attempt++){
    var controller=new AbortController(),timer=setTimeout(function(){controller.abort();},6000);
    try{
      var response=await fetch('/api_proxy/cplus/index.php?rid=queryUser',{method:'POST',credentials:'same-origin',signal:controller.signal,headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({clientOSType:'H5-android',deviceModel:'Android',clientVerNum:'102',source:'157',osType:'H5'})});
      var user=await response.json();
      if(response.ok && user.code==='0x000000' && user.data && (user.roleInfo || user.moreUserInfo)) {auth.state='authenticated';persist();return;}
    }catch(ignored){}finally{clearTimeout(timer);}
    if(attempt<2)await new Promise(function(resolve){setTimeout(resolve,500);});
  }
  auth.state='error';auth.message='登录响应已返回，但会话未就绪，请重试登录或使用已有登录更新';persist();
})();
