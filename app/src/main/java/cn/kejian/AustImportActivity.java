package cn.kejian;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;

public final class AustImportActivity extends Activity {
 private Button fetch,refreshCaptcha;
 private TextView status;
 private EditText account,password,code;
 private ImageView image;
 private Thread worker;
 private String challengeKey;
 private boolean challengeReady;

 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  LoginForm form=new LoginForm(this,School.AUST,()->worker!=null,this::submit,this::loadChallenge);
  status=form.status;account=form.account;password=form.password;code=form.code;image=form.image;
  fetch=form.submit;refreshCaptcha=form.refresh;
  form.show(this);
  loadChallenge();
 }
 private void loadChallenge(){
  if(worker!=null)return;
  challengeReady=false;
  start(()->{
   try{
    JSONObject result=AustImport.challenge();
    boolean enabled=result.getBoolean("enable");
    String key=enabled?result.getString("key"):null;
    byte[] bytes=enabled?android.util.Base64.decode(result.getString("imageArray"),0):null;
    android.graphics.Bitmap bitmap=bytes==null?null:BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    if(enabled&&(bitmap==null||key.isEmpty()))throw new java.io.IOException();
    done(()->{
     challengeReady=true;challengeKey=key;
     image.setImageBitmap(bitmap);image.setVisibility(enabled?View.VISIBLE:View.GONE);refreshCaptcha.setVisibility(enabled?View.VISIBLE:View.GONE);
     code.setText("");code.setVisibility(enabled?View.VISIBLE:View.GONE);
     fetch.setText("登录并导入课表");
     status.setText(enabled?"请输入学号、密码和图形验证码。":"请输入学号和密码。");
    });
   }catch(Exception e){done(()->{
    fetch.setText("重试连接");
    status.setText("暂时无法获取学校登录状态，请重试。");
   });}
  });
 }
 private void submit(){
  if(worker!=null)return;
  if(!challengeReady){loadChallenge();return;}
  String user=account.getText().toString().trim(), pass=password.getText().toString();
  String answer=code.getText().toString().trim(), key=challengeKey;
  if(user.isEmpty()||pass.isEmpty()||(key!=null&&answer.isEmpty())){
   status.setText("请填写完整的登录信息。");return;
  }
  status.setText("正在登录…");
  start(()->{
   AustImport importer;
   try{importer=AustImport.login(user,pass,answer,key);}
   catch(Exception e){done(()->{
    challengeReady=false;fetch.setText("重新获取登录状态");
    status.setText("图形验证码错误".equals(e.getMessage())?"图形验证码错误，请刷新后重试。":
      "登录失败，请核对账号和密码，或稍后重试。");
   });return;}
   runOnUiThread(()->{
    if(!isFinishing()&&!isDestroyed()){
     password.setText("");
     status.setText(UiKit.READING_TIMETABLE);
    }
   });
   try{ScheduleRepository next=importer.fetch();done(()->save(next));}
   catch(Exception e){done(()->{
    status.setText(e instanceof java.io.IOException&&e.getMessage()!=null&&
     e.getMessage().matches("[\\p{IsHan}，。]+")?e.getMessage()+"，已保留本地课表。":
     "导入失败，请检查网络或重新登录。已保留本地课表。");
   });}
  });
 }
 private void start(Runnable task){
  fetch.setEnabled(false);
  worker=new Thread(task,"aust-import");worker.start();
 }
 private void done(Runnable result){
  runOnUiThread(()->{
   if(isFinishing()||isDestroyed())return;
   worker=null;fetch.setEnabled(true);result.run();
  });
 }
 private void save(ScheduleRepository next){
  try{
   ScheduleRepository old=new ScheduleRepository(this);
   if(old.raw!=null&&old.raw.optBoolean("localEdits"))
    LocalTables.saveNamed(this,"联网更新前备份 "+System.currentTimeMillis(),old.raw);
   next.save(this);
   Toast.makeText(this,"当前学期课表已保存",Toast.LENGTH_SHORT).show();
   finish();
  }catch(Exception e){status.setText("保存失败，已保留本地课表。");}
 }
 @Override public void onDestroy(){
  if(worker!=null)worker.interrupt();
  super.onDestroy();
 }
}
