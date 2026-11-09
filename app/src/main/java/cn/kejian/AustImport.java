package cn.kejian;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

final class AustImport {
 private static final String API="https://jwglyd.aust.edu.cn/app-ws/ws/app-service";
 private final String token,secret,identity;
 AustImport(JSONObject session)throws JSONException{
  token=session.getString("token");secret=session.getString("dynamicSecret");identity=String.valueOf(session.get("identity"));
  if(!"0".equals(identity)||token.isEmpty()||secret.isEmpty())throw new JSONException("登录已失效");
 }
 private static final String LOGIN_SIGNER="supwisdom_eams_app_secret";
 static JSONObject challenge()throws Exception{
  return (JSONObject)post("/verify-code",LOGIN_SIGNER,"");
 }
 static AustImport login(String account,String password,String code,String key)throws Exception{
  String encoded=Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
  JSONObject response=(JSONObject)(key==null
   ?post("/login",LOGIN_SIGNER,"","user_code",account,"passwd",encoded)
   :post("/login-with-verify-code",null,"","user_code",account,"passwd",encoded,"verify_code",code,"verify_code_key",key));
  return new AustImport(new JSONObject().put("token",response.getString("token"))
   .put("dynamicSecret",response.getString("secret")).put("identity",response.get("cur_identity")));
 }
 private Object request(String path,String... fields)throws Exception{
  return post(path,secret,token,fields);
 }
 private static Object post(String path,String secret,String token,String... fields)throws Exception{
  if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
  TreeMap<String,String> params=new TreeMap<>();
  if(!token.isEmpty())params.put("token",token);
  for(int i=0;i<fields.length;i+=2)params.put(fields[i],fields[i+1]);
  String timestamp=Long.toString(System.currentTimeMillis());
  String random=Integer.toString(new java.security.SecureRandom().nextInt(100000));
  String plain=secret+"|"+String.join("|",params.values())+"|"+timestamp+"|"+random;
  byte[] digest=MessageDigest.getInstance("MD5").digest(plain.getBytes(StandardCharsets.UTF_8));
  StringBuilder sign=new StringBuilder();
  for(byte b:digest)sign.append(String.format(Locale.ROOT,"%02X",b&255));
  params.put("sign",sign.toString());params.put("timestamp",timestamp);params.put("random",random);
  StringJoiner form=new StringJoiner("&");
  for(Map.Entry<String,String> e:params.entrySet())form.add(URLEncoder.encode(e.getKey(),"UTF-8")+"="+URLEncoder.encode(e.getValue(),"UTF-8"));
  HttpURLConnection c=(HttpURLConnection)new URL(API+path).openConnection();
  try{
   c.setRequestMethod("POST");c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);
   c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");c.setRequestProperty("token",token);
   c.setDoOutput(true);
   try(OutputStream out=c.getOutputStream()){out.write(form.toString().getBytes(StandardCharsets.UTF_8));}
   if(c.getResponseCode()!=200)throw new IOException("教务请求失败");
   try(InputStream in=c.getInputStream()){
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
    while((count=in.read(buffer))!=-1){if(bytes.size()+count>4_000_000)throw new IOException("课表响应过大");bytes.write(buffer,0,count);}
    JSONObject response=new JSONObject(bytes.toString("UTF-8"));
    String code=response.optString("err_code");
    if("03010".equals(code))throw new IOException("图形验证码错误");
    if(!"00000".equals(code))throw new IOException("教务未接受请求，请核对账号、密码或登录状态");
    String payload=response.optString("business_data","");
    if(payload.isEmpty())throw new IOException("教务未返回数据，请确认仍处于登录状态");
    return new JSONTokener(new String(Base64.getDecoder().decode(payload),StandardCharsets.UTF_8)).nextValue();
   }
  }finally{c.disconnect();}
 }
 ScheduleRepository fetch()throws Exception{
  JSONArray types=(JSONArray)request("/common/get-biz-types","identity",identity);
  String biz=String.valueOf(types.getJSONObject(0).get("id"));
  JSONObject meta=(JSONObject)request("/common/get-semester","biz_type_id",biz);
  String current=String.valueOf(meta.get("cur_semester_id"));
  JSONArray terms=meta.getJSONArray("semesters");
  JSONObject term=null;
  for(int i=0;i<terms.length();i++)if(current.equals(terms.getJSONObject(i).optString("id")))term=terms.getJSONObject(i);
  if(term==null)throw new IOException("教务未返回当前学期");
  JSONObject selected=term;
  return normalize(current,selected,(start,end)->(JSONArray)request("/student/course/schedule/get-course-tables",
   "semester_id",current,"biz_type_id",biz,"start_date",start.toString(),"end_date",end.toString()));
 }
 interface WeekSource {JSONArray load(LocalDate start,LocalDate end)throws Exception;}
 static ScheduleRepository normalize(String current,JSONObject term,WeekSource source)throws Exception{
  LocalDate first=LocalDate.parse(term.getString("start_date"));
  LocalDate last=LocalDate.parse(term.getString("end_date"));
  LocalDate monday=first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  long count=java.time.temporal.ChronoUnit.WEEKS.between(monday,last.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))+1;
  if(count<1||count>60||last.isBefore(first))throw new IOException("当前学期日期无效");
  JSONArray weeks=new JSONArray(),periods=new JSONArray();
  LocalTime[] starts=new LocalTime[24],ends=new LocalTime[24];
  int max=0,total=0;
  for(int i=0;i<count;i++){
   LocalDate from=monday.plusWeeks(i),to=from.plusDays(6);
   JSONArray rows=source.load(from,to),courses=new JSONArray();
   Set<String> seen=new HashSet<>();
   for(int j=0;j<rows.length();j++){
    JSONObject r=rows.getJSONObject(j);
    LocalDate date=LocalDate.parse(r.getString("date"));
    if(date.isBefore(from)||date.isAfter(to))throw new IOException("课程日期与查询周不符");
    int a=r.getInt("start_unit"),b=r.getInt("end_unit");
    LocalTime start=LocalTime.parse(r.getString("start_time")),end=LocalTime.parse(r.getString("end_time"));
    if(a<1||b<a||b>24||!end.isAfter(start))throw new IOException("课程节次无效");
    if(a==b){put(starts,ends,a,start,end);}
    else{
     long minutes=Duration.between(start,end).toMinutes();
     if(minutes!=(b-a+1)*45L+(b-a)*10L)throw new IOException("发现未确认的连堂作息");
     for(int unit=a;unit<=b;unit++){
      LocalTime time=start.plusMinutes((unit-a)*55L);
      put(starts,ends,unit,time,time.plusMinutes(45));
     }
    }
    max=Math.max(max,b);
    String id=String.valueOf(r.get("lesson_id"))+"-"+date+"-"+a+"-"+b;
    if(!seen.add(id))continue;
    courses.put(new JSONObject().put("KBID",id).put("XNXQDM",current).put("KCM",r.getString("course_name"))
      .put("SKLS",names(r.optJSONArray("teachers"))).put("JSMC",names(r.optJSONArray("rooms")))
      .put("SKXQ",date.getDayOfWeek().getValue()).put("KSJC",a).put("JSJC",b));
    total++;
   }
   weeks.put(new JSONObject().put("number",i+1).put("start",from.toString()).put("end",to.toString())
    .put("state",courses.length()==0?"empty":"ok").put("courses",courses));
  }
  if(total==0)throw new IOException("当前学期课表尚未发布或为空");
  for(int i=0;i<max;i++)periods.put(starts[i]==null?JSONObject.NULL:
   new JSONObject().put("start",starts[i].toString()).put("end",ends[i].toString()));
  return ScheduleRepository.parse(new JSONObject().put("school","AUST").put("term",current)
   .put("termLabel",term.optString("year")+" 第"+term.optString("name")+"学期").put("termEnd",last.toString())
   .put("updated",System.currentTimeMillis()).put("periods",periods).put("weeks",weeks));
 }
 private static void put(LocalTime[] starts,LocalTime[] ends,int unit,LocalTime start,LocalTime end)throws IOException{
  int i=unit-1;
  if(starts[i]!=null&&(!starts[i].equals(start)||!ends[i].equals(end)))throw new IOException("同一节次存在不同作息");
  starts[i]=start;ends[i]=end;
 }
 private static String names(JSONArray values)throws JSONException{
  StringJoiner names=new StringJoiner(" / ");
  if(values!=null)for(int i=0;i<values.length();i++){
   String name=values.getJSONObject(i).optString("name");
   if(!name.isEmpty())names.add(name);
  }
  return names.toString();
 }
}
