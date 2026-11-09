const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const path=require('node:path');
const script=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/login.js'),'utf8');
test('CAPTCHA remains pending until slow verification and login settle',async()=>{
 let verified,loginCalls=0;const delay=new Promise(r=>verified=r);
 const component={form:{account:'',password:''},picture:{isShow:true,isSuccess:false},verifyCaptcha(){return delay.then(()=>this.picture.isSuccess=true);},verifyWebUser(){loginCalls++;return Promise.resolve();}};
 const window={__kejianCredentials:{account:'test',password:'secret',captcha:'1234'}};
 assert.equal(vm.runInNewContext(script,{window,document:{querySelectorAll:()=>[{__vue__:component}]},Promise}),true);
 assert.equal(window.__kejianAuthPending,true);assert.equal(window.__kejianCredentials,undefined);assert.equal(loginCalls,0);
 verified();await new Promise(r=>setImmediate(r));assert.equal(loginCalls,1);assert.equal(window.__kejianAuthPending,false);
});
test('invalid CAPTCHA does not submit password again',async()=>{
 let calls=0;const component={form:{account:''},picture:{isShow:true,isSuccess:false},verifyCaptcha:async()=>{},verifyWebUser(){calls++;}};
 const window={__kejianCredentials:{account:'test',password:'secret',captcha:'wrong'}};
 vm.runInNewContext(script,{window,document:{querySelectorAll:()=>[{__vue__:component}]},Promise});await new Promise(r=>setImmediate(r));assert.equal(calls,0);assert.equal(window.__kejianAuthPending,false);
});
