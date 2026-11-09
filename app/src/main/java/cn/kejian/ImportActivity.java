package cn.kejian;
import android.app.*;
import android.os.*;
import android.net.Uri;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class ImportActivity extends Activity {
 private boolean smsMode=false;private long smsUntil=0;private Button modeButton,sendSms,refreshCaptcha;private EditText account,password,captcha;private ImageView captchaImage;private boolean ready=false,authBusy=false;private int authChecks=0;private WebView web;private TextView status;private Button fetch;private final Handler handler=new Handler(Looper.getMainLooper());private boolean busy=false,closed=false;private long deadline;
 private static final String HOST="e.wfu.edu.cn";
 private boolean trusted(String url){try{Uri u=Uri.parse(url);return "https".equals(u.getScheme())&&HOST.equals(u.getHost());}catch(Exception e){return false;}}
 @Override public void onCreate(Bundle state){super.onCreate(state);UiKit.apply(this);getWindow().getDecorView().setSystemUiVisibility(UiKit.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);getWindow().setNavigationBarColor(UiKit.BG);getWindow().setStatusBarColor(UiKit.BG);
  LoginForm form=new LoginForm(this,School.WFU,()->busy||authBusy,this::login,()->authenticate("refresh"));
  LinearLayout root=form.root;
  status=form.status;account=form.account;password=form.password;captcha=form.code;
  captchaImage=form.image;fetch=form.submit;refreshCaptcha=form.refresh;
  modeButton=UiKit.action(this,"",false,this::switchMode);
  sendSms=UiKit.action(this,"",false,()->authenticate("send"));
  web=new WebView(this);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !trusted(r.getUrl().toString());}@Override public void onPageFinished(WebView v,String url){if(!trusted(url))v.stopLoading();else {ready=true;if(!authBusy&&!busy)status.setText("请输入学号和密码。");}}});
  web.setAlpha(0f);web.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);root.addView(web,new LinearLayout.LayoutParams(1,1));form.show(this);web.loadUrl("https://e.wfu.edu.cn/h5/#/pages/user/login?url=%252Fpages%252Fschedule%252Fschedule-new");
 }
 private void switchMode(){if(busy||authBusy)return;smsMode=!smsMode;modeButton.setText(smsMode?"手机验证码 · 切换密码登录":"密码登录 · 切换手机验证码");account.setHint(smsMode?"手机号":"学号 / 手机号");account.setInputType(smsMode?android.text.InputType.TYPE_CLASS_PHONE:android.text.InputType.TYPE_CLASS_TEXT);password.setText("");password.setHint(smsMode?"短信验证码":"密码");password.setInputType(smsMode?android.text.InputType.TYPE_CLASS_NUMBER:129);captcha.setText("");captcha.setVisibility(View.GONE);captchaImage.setVisibility(View.GONE);sendSms.setVisibility(smsMode?View.VISIBLE:View.GONE);refreshCaptcha.setVisibility(smsMode?View.VISIBLE:View.GONE);updateSmsTimer();authenticate("prepare");}
 private void updateSmsTimer(){if(closed)return;long seconds=Math.max(0,(smsUntil-SystemClock.elapsedRealtime()+999)/1000);sendSms.setText(seconds>0?seconds+" 秒后重新获取":"获取短信验证码");sendSms.setEnabled(!authBusy&&!busy&&seconds==0);if(seconds>0)handler.postDelayed(this::updateSmsTimer,1000);}
 private String asset(String name)throws Exception{try(java.io.InputStream in=getAssets().open(name);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8");}}
 private void login(){authenticate("login");}
 private void authenticate(String action){
  if(busy||authBusy)return;
  if(!ready){status.setText("正在连接，请稍后重试");return;}
  String number=account.getText().toString().trim();
  if(!action.equals("prepare")&&!action.equals("refresh")){
   if(smsMode&&!number.matches("1[0-9]{10}")){status.setText("请输入正确的11位手机号");return;}
   if(action.equals("login")&&(number.isEmpty()||password.getText().length()==0)){status.setText(smsMode?"请输入手机号和短信验证码":"请输入账号和密码");return;}
   if(action.equals("send")&&SystemClock.elapsedRealtime()<smsUntil)return;
  }
  if(!trusted(web.getUrl())){status.setText("认证服务不可用，请重试");return;}
  authBusy=true;authChecks=0;fetch.setEnabled(false);sendSms.setEnabled(false);modeButton.setEnabled(false);status.setText(action.equals("send")?"正在校验并请求短信…":action.equals("login")?"正在登录…":"正在加载认证组件…");
  try{JSONObject input=new JSONObject().put("mode",smsMode?"sms":"password").put("action",action).put("account",number).put("password",password.getText().toString()).put("captcha",captcha.getText().toString());
   web.evaluateJavascript("window.__kejianCredentials="+input.toString()+";"+asset("login.js"),result->{if(closed)return;if(!"true".equals(result)){authFinished("认证页面尚未就绪，请稍后重试");}else handler.postDelayed(this::pollLogin,400);});
  }catch(Exception e){authFinished("无法加载认证组件");}
 }
 private void authFinished(String message){authBusy=false;fetch.setEnabled(!busy);modeButton.setEnabled(!busy);updateSmsTimer();status.setText(message);}
 private void showCaptcha(String image){if(image.isEmpty())return;try{byte[] bytes=android.util.Base64.decode(image.contains(",")?image.substring(image.indexOf(',')+1):image,android.util.Base64.DEFAULT);captchaImage.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length));captchaImage.setVisibility(View.VISIBLE);refreshCaptcha.setVisibility(View.VISIBLE);captcha.setVisibility(View.VISIBLE);captcha.setText("");}catch(Exception ignored){status.setText("图形验证码加载失败，点击图片刷新");}}
 private void pollLogin(){if(closed||!authBusy)return;if(!trusted(web.getUrl())){authFinished("认证跳转异常，请重试");return;}
  web.evaluateJavascript("JSON.stringify(window.__kejianAuth||JSON.parse(sessionStorage.getItem('kejianAuth')||'null')||{state:'running'})",value->{if(closed||!authBusy)return;try{JSONObject r=new JSONObject((String)new JSONTokener(value).nextValue());String state=r.optString("state");
   if(state.equals("success")){status.setText("登录成功，正在检查会话…");web.evaluateJavascript(asset("session.js"),null);}
   else if(state.equals("authenticated")){authBusy=false;password.setText("");captcha.setText("");modeButton.setEnabled(false);startFetch();return;}
   else if(state.equals("captcha")){showCaptcha(r.optString("image"));authFinished("请填写图形验证码，再"+(smsMode?"获取短信验证码":"登录"));return;}
   else if(state.equals("smsSent")||state.equals("smsError")){smsUntil=SystemClock.elapsedRealtime()+60000;showCaptcha(r.optString("image"));authFinished(r.optString("message","短信请求已处理，请查看手机"));return;}
   else if(state.equals("error")||state.equals("cooldown")||state.equals("ready")){authFinished(r.optString("message","请输入登录信息"));return;}
   if(++authChecks<100)handler.postDelayed(this::pollLogin,600);else authFinished("登录超时，请检查网络后重试；也可使用已有登录更新");
  }catch(Exception e){authFinished("无法解析认证结果，请重试");}});
 }
 private void startFetch(){
  if(busy||authBusy)return;if(!trusted(web.getUrl())){status.setText("请先打开官方登录页面");return;}busy=true;fetch.setEnabled(false);modeButton.setEnabled(false);sendSms.setEnabled(false);status.setText(UiKit.READING_TIMETABLE);deadline=System.currentTimeMillis()+360000;
  String device=getPreferences(0).getString("device","");if(device.isEmpty()){device=UUID.randomUUID().toString();getPreferences(0).edit().putString("device",device).apply();}
  String js;
  try(java.io.InputStream in=getAssets().open("import.js");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);js="window.__kejianDevice="+JSONObject.quote(device)+";window.__kejianSkipProfile=true;"+out.toString("UTF-8");}catch(Exception e){fail("无法加载导入组件");return;}
  web.evaluateJavascript(js,null);handler.postDelayed(this::poll,600);
 }
 private void poll(){if(closed||!busy)return;if(!trusted(web.getUrl())){fail("页面离开官方站点，已停止导入");return;}if(System.currentTimeMillis()>deadline){fail("获取超时，请重试。原课表已保留。");return;}
  web.evaluateJavascript("JSON.stringify(window.__kejian||{state:'running'})",value->{if(closed||!busy)return;try{
   Object decoded=new JSONTokener(value).nextValue();if(!(decoded instanceof String))throw new JSONException("无法读取页面数据");String text=(String)decoded;if(text.length()>5_000_000)throw new JSONException("数据过大");JSONObject result=new JSONObject(text);String s=result.optString("state");
   if(s.equals("done")){
    busy=false;
    JSONObject data=result.getJSONObject("data");
    authFinished("获取成功，正在保存课表…");confirm(data);
   }
   else if(s.equals("error"))fail(result.optString("message","获取失败"));
   else{handler.postDelayed(this::poll,600);}
  }catch(Exception e){fail("返回内容无法识别，请确认已登录后重试");}});
 }
 private void fail(String message){busy=false;authFinished(message);}
 private void confirm(JSONObject data)throws Exception {
  ScheduleRepository parsed=ScheduleRepository.parse(data);List<Integer> missing=new ArrayList<>();JSONArray weeks=data.getJSONArray("weeks");for(int i=0;i<weeks.length();i++)if(weeks.getJSONObject(i).getString("state").equals("unknown")&&weeks.getJSONObject(i).optBoolean("confirmable",false))missing.add(i);
  if(missing.isEmpty()){commit(data);return;}
  String[] labels=new String[missing.size()];boolean[] checks=new boolean[labels.length];Arrays.fill(checks,true);for(int i=0;i<labels.length;i++)labels[i]="第"+weeks.getJSONObject(missing.get(i)).getInt("number")+"周：我已核实无课";
  status.setText("来源未返回部分周。仅勾选你已核实无课的周；未确认的周保留为未知。");
  LinearLayout fields=UiKit.col(this);
  CheckBox[] boxes=new CheckBox[labels.length];
  for(int i=0;i<labels.length;i++){
   CheckBox box=new CheckBox(this);
   box.setText(labels[i]);
   box.setTextColor(UiKit.INK);
   box.setButtonTintList(android.content.res.ColorStateList.valueOf(UiKit.BRAND));
   box.setChecked(true);
   fields.addView(box);
   boxes[i]=box;
  }
  Dialog prompt=UiKit.prompt(this,"确认无课周",fields,"确认并保存",()->{
   try{
    for(int i=0;i<boxes.length;i++)
     if(boxes[i].isChecked())weeks.getJSONObject(missing.get(i)).put("state","empty");
    commit(data);
   }catch(Exception e){fail("保存失败，原课表已保留");}
  },null,null);
  prompt.setOnCancelListener(d->status.setText("已取消，原课表已保留"));
  prompt.setOnDismissListener(d->{if(!isFinishing()&&status.getText().toString().startsWith("来源未返回"))status.setText("原课表已保留");});
  prompt.show();
 }


 private void commit(JSONObject data)throws Exception {
  ScheduleRepository next=ScheduleRepository.parse(data),old=new ScheduleRepository(this);
  if(!next.complete()||next.courses.isEmpty()){
   status.setText("当前学期课表未发布或未完整获取，已保留本地课表。");
   return;
  }
  if(old.raw!=null&&old.raw.optBoolean("localEdits",false))
   LocalTables.saveNamed(this,"联网更新前备份 "+System.currentTimeMillis(),old.raw);
  finishImport(next);
 }
 private void finishImport(ScheduleRepository next)throws Exception {
  next.save(this);
  Toast.makeText(this,"课表导入成功，已保存到本机",Toast.LENGTH_LONG).show();
  setResult(RESULT_OK);
  finish();
 }
 @Override protected void onDestroy(){closed=true;handler.removeCallbacksAndMessages(null);if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
