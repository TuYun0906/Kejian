package cn.kejian;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.webkit.CookieManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class CampusData {
 enum Module {
  REPAIR("随手拍维修"), ROOMS("空教室"), CALENDAR("校历"), SCORES("考试成绩查询"), EXAMS("考试列表");
  final String title;
  Module(String title){this.title=title;}
 }
 private static final String HOST="https://e.wfu.edu.cn";
 private static final String API="/api_proxy/cplus/index.php?rid=";
 private static final ExecutorService WORK=Executors.newFixedThreadPool(4);
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static final Map<Module,Snapshot> cache=new EnumMap<>(Module.class);
 private static int generation;
 private static String device="";
 private static final class Snapshot {
  final JSONArray rows;
  final long at=System.currentTimeMillis();
  Snapshot(JSONArray rows){this.rows=rows;}
 }
 static void clear(Context context){
  generation++;
  cache.clear();
  device="";
  context.getSharedPreferences("campus",0).edit().clear().apply();
 }
 static void afterImport(Context context,String id){
  clear(context);
  device=id;
  context.getSharedPreferences("campus",0).edit().putString("device",id).apply();
  for(Module module:Module.values())if(module!=Module.ROOMS) refresh(context,module,null);
 }
 static void show(Activity activity,Module module) {
  LinearLayout body=UiKit.col(activity);
  ScrollView scroll=new ScrollView(activity);
  scroll.addView(body);
  Dialog sheet=UiKit.sheet(activity,module.title,scroll);
  Runnable load=()->{
   draw(activity,body,module,"正在查询…",null);
   refresh(activity,module,error->{
    if(sheet.isShowing())draw(activity,body,module,error,error==null?null:
     ()->refreshView(activity,module,body,sheet));
   });
  };
  sheet.show();
  if(module==Module.ROOMS){
   draw(activity,body,module,"当前未确认智慧潍苑的空教室查询接口。课表中的空档不能判断教室是否空闲。",null);
  }else load.run();
 }
 private static void refreshView(Activity a,Module module,LinearLayout body,Dialog sheet){
  draw(a,body,module,"正在查询…",null);
  refresh(a,module,error->{
   if(sheet.isShowing())draw(a,body,module,error,()->refreshView(a,module,body,sheet));
  });
 }
 private static void draw(Activity a,LinearLayout body,Module module,String message,Runnable retry){
  body.removeAllViews();
  if(message!=null)body.addView(UiKit.label(a,message,14,UiKit.MUTED));
  Snapshot snapshot=cache.get(module);
  if(snapshot!=null){
   String updated=new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(snapshot.at));
   body.addView(UiKit.label(a,"数据获取于 "+updated,12,UiKit.MUTED));
   if(snapshot.rows.length()==0)body.addView(UiKit.label(a,"暂无记录",16,UiKit.INK));
   for(int i=0;i<snapshot.rows.length();i++){
    JSONObject row=snapshot.rows.optJSONObject(i);
    if(row==null)continue;
    LinearLayout card=UiKit.card(a);
    card.setBackground(UiKit.shape(a,UiKit.BG,16));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
    lp.topMargin=UiKit.dp(a,8);
    body.addView(card,lp);
    card.addView(UiKit.label(a,row.optString("title"),16,UiKit.INK));
    String info=row.optString("info");
    if(!info.isEmpty())card.addView(UiKit.label(a,info,14,UiKit.MUTED));
   }
  }
  if(retry!=null)body.addView(UiKit.action(a,"重新查询",true,retry));
 }
 private interface Result {void done(String error);}
 private static void refresh(Context context,Module module,Result result){
  String id=device.isEmpty()?context.getSharedPreferences("campus",0).getString("device",""):device;
  if(id.isEmpty()){
   if(result!=null)result.done("请先登录智慧潍苑并导入课表");
   return;
  }
  int ticket=generation;
  WORK.execute(()->{
   JSONArray rows=null;
   try{rows=fetch(module,id);}catch(Exception ignored){}
   JSONArray data=rows;
   MAIN.post(()->{
    if(ticket!=generation)return;
    if(data!=null)cache.put(module,new Snapshot(data));
    if(result!=null)result.done(data==null?"暂未获取到数据，请检查网络或重新登录":null);
   });
  });
 }
 private static JSONArray fetch(Module module,String device)throws Exception{
  JSONArray rows=new JSONArray();
  switch(module){
   case CALENDAR:
    JSONArray calendars=api(device,"getSchoolBasicsInfo","type","1").getJSONArray("data");
    for(int i=0;i<Math.min(calendars.length(),15);i++){
     JSONObject item=calendars.getJSONObject(i);
     String html=api(device,"showSchoolBasicsInfoById","uid",item.getString("CIID"))
      .getJSONObject("data").optString("cicontent");
     String content=Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY).toString().trim();
     row(rows,item.optString("CITITLE","校历"),content.replace("\uFFFC",""));
    }
    break;
   case SCORES:
    JSONArray terms=api(device,"getTermsAboutScore").getJSONArray("data");
    if(terms.length()==0)break;
    String term=terms.getJSONObject(0).getString("XNXQDM");
    JSONArray scores=api(device,"getScoreByInput","term",term,"isPass","").getJSONArray("data");
    for(int i=0;i<Math.min(scores.length(),200);i++){
     JSONObject score=scores.getJSONObject(i);
     row(rows,score.optString("KCM"),term+"  ·  成绩 "+score.optString("ZCJ")+"  ·  学分 "+score.optString("XF"));
    }
    break;
   case EXAMS:
    JSONArray exams=api(device,"getStudentExamInformation","term","2017-2018-1").getJSONArray("data");
    for(int i=0;i<Math.min(exams.length(),200);i++){
     JSONObject exam=exams.getJSONObject(i);
     row(rows,exam.optString("KCM"),exam.optString("KSRQ")+" "+exam.optString("KSSJMS")+"\n"+exam.optString("JSMC"));
    }
    break;
   case REPAIR:
    String role=api(device,"queryUser").getJSONObject("roleInfo").getString("ROLE_ID");
    String school="cloudtop";
    String salt=md5(md5(""));
    JSONObject conditions=postWithDevice(device,"/api_proxy/repairApi/getCondition","scode",school,"login_yhbh",role,
      "md5sign",md5(school+"+"+role+salt));
    if(!"OK".equals(conditions.optString("status")))throw new IOException();
    JSONArray statuses=conditions.getJSONArray("content").getJSONObject(0).getJSONArray("data");
    String status=String.valueOf(statuses.getJSONObject(0).getInt("ra_id"));
    for(int page=0;page<10;page++){
     String offset=String.valueOf(page);
     JSONObject repair=postWithDevice(device,"/api_proxy/repairApi/getMyRepairAll","scode",school,"begin_num",offset,"num","10",
       "login_yhbh",role,"status",status,"md5sign",md5(String.join("+",school,offset,"10",role,status)+salt));
     if(!"OK".equals(repair.optString("status")))throw new IOException();
     JSONArray records=repair.getJSONObject("content").getJSONArray("data");
     for(int i=0;i<records.length();i++){
      JSONObject item=records.getJSONObject(i);
      row(rows,item.optString("ri_title","维修记录"),item.optString("ra_name")+"\n"+item.optString("ri_content"));
     }
     if(records.length()<10)break;
    }
    break;
   default: throw new IOException();
  }
  return rows;
 }
 private static void row(JSONArray rows,String title,String info)throws JSONException {
  rows.put(new JSONObject().put("title",title).put("info",info.trim()));
 }
 private static String md5(String value)throws Exception{
  byte[] digest=MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
  StringBuilder hex=new StringBuilder();
  for(byte b:digest)hex.append(String.format(Locale.ROOT,"%02x",b&255));
  return hex.toString();
 }
 private static JSONObject api(String device,String rid,String... params)throws Exception{
  JSONObject result=postWithDevice(device,API+rid,params);
  if(!"0x000000".equals(result.optString("code")))throw new IOException();
  return result;
 }
 private static JSONObject postWithDevice(String device,String path,String... params)throws Exception{
  String[] base={"clientOSType","H5-android","deviceModel","Android","clientVerNum","102","source","157",
   "osType","H5","deviceId",device};
  String[] fields=Arrays.copyOf(base,base.length+params.length);
  System.arraycopy(params,0,fields,base.length,params.length);
  return post(path,fields);
 }
 private static JSONObject post(String path,String... fields)throws Exception{
  String address=HOST+path;
  HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
  try{
   connection.setConnectTimeout(6000);
   connection.setReadTimeout(6000);
   connection.setInstanceFollowRedirects(false);
   connection.setRequestMethod("POST");
   connection.setDoOutput(true);
   connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");
   connection.setRequestProperty("Origin",HOST);
   connection.setRequestProperty("Accept","application/json, text/plain, */*");
   String cookies=CookieManager.getInstance().getCookie(address);
   if(cookies!=null)connection.setRequestProperty("Cookie",cookies);
   StringJoiner body=new StringJoiner("&");
   for(int i=0;i<fields.length;i+=2)
    body.add(URLEncoder.encode(fields[i],"UTF-8")+"="+URLEncoder.encode(fields[i+1],"UTF-8"));
   try(OutputStream stream=connection.getOutputStream()){stream.write(body.toString().getBytes(StandardCharsets.UTF_8));}
   if(connection.getResponseCode()!=200)throw new IOException();
   try(InputStream input=connection.getInputStream()){
    ByteArrayOutputStream output=new ByteArrayOutputStream();
    byte[] buffer=new byte[8192];
    for(int n;(n=input.read(buffer))!=-1;){
     if(output.size()+n>2_000_000)throw new IOException();
     output.write(buffer,0,n);
    }
    return new JSONObject(output.toString("UTF-8"));
   }
  }finally{connection.disconnect();}
 }
}
