const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const path=require('node:path');
const script=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/import.js'),'utf8');
const ok=(data)=>({ok:true,json:async()=>({code:'0x000000',data})});
const weeks=[1,2,3].map(value=>({value,ks:'2026-08-31',js:'2026-09-06'}));
async function run(fn){const window={__kejianDevice:'test'};await vm.runInNewContext(script,{window,URLSearchParams,AbortController,fetch:async(url,opts)=>url.includes('getNowTermWeek')?ok({weeks}):fn(Number(opts.body.get('week'))),setTimeout:(f,t)=>setTimeout(f,t===200?0:t),clearTimeout,Date});return JSON.parse(JSON.stringify(window.__kejian));}
test('network failure preserves completed weeks and cannot be confirmed as empty',async()=>{
 const r=await run(w=>{if(w===2)throw Error('offline');return ok([]);});
 assert.equal(r.state,'done');assert.deepEqual(r.data.weeks.map(w=>w.state),['ok','unknown','ok']);assert.equal(r.data.weeks[1].confirmable,false);
});
test('generic business error needs explicit user confirmation',async()=>{
 const r=await run(()=>({ok:true,json:async()=>({code:'CampusController_class_0x00000600000',data:[]})}));
 assert.equal(r.data.weeks[0].state,'unknown');assert.equal(r.data.weeks[0].confirmable,true);
});
test('malformed success aborts replacement',async()=>{const r=await run(()=>ok(null));assert.equal(r.state,'error');assert.equal(r.data,undefined);});
test('authentication errors stop import',async()=>{const r=await run(()=>({ok:true,json:async()=>({code:'AUTH_EXPIRED'})}));assert.equal(r.state,'error');});
test('student identifiers are not retained in timetable snapshot',async()=>{const r=await run(()=>ok([{KBID:'1',KCM:'测试',XH:'private',SKXQ:'1',KSJC:'1',JSJC:'2',XNXQDM:'2026-2027-1'}]));assert.equal(r.data.weeks[0].courses[0].XH,undefined);assert.equal(r.data.weeks[0].courses[0].KCM,'测试');});
test('account avatar uses HEADIMAGE relative to the official photo directory', async()=>{
 const window={};
 const origin='https://e.wfu.edu.cn';
 const expected=origin+'/cplus/20261007/avatar.jpg';
 let requested;
 class FileReader {
  readAsDataURL(){this.result='data:image/jpeg;base64,dGVzdA==';this.onload();}
 }
 await vm.runInNewContext(script,{
  window,location:{origin},URL,URLSearchParams,AbortController,FileReader,Date,
  setTimeout:(f,t)=>setTimeout(f,t===200?0:t),clearTimeout,
  fetch:async(url)=>{
   if(url.includes('getNowTermWeek'))return ok({weeks});
   if(url.includes('queryUser'))return {ok:true,json:async()=>({
    code:'0x000000',data:{photoUrl:origin+'/cplus/'},
    roleInfo:{NICKNAME:'测试',HEADIMAGE:'/20261007/avatar.jpg'}
   })};
   if(url.startsWith('https:')){
    requested=url;
    return {ok:true,blob:async()=>({size:4,type:'image/jpeg'})};
   }
   return ok([]);
  }
 });
 assert.equal(requested,expected);
 assert.equal(window.__kejian.data.profile.avatar,'dGVzdA==');
});
