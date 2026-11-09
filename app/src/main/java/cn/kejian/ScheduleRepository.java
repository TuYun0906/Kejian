package cn.kejian;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

public final class ScheduleRepository {
 private static final Object FILE_LOCK=new Object();
 public static final String OK="0x000000";
 public final List<ScheduleLogic.Course> courses=new ArrayList<>();
 public final List<Week> weeks=new ArrayList<>();
 School school=School.WFU;
 String[] starts=ScheduleLogic.START,ends=ScheduleLogic.END;
 public long updated; public boolean corrupt=false; public JSONObject raw;
 public static final class Week {
  public int number;public LocalDate start,end; public String state;
  public boolean known(){return "ok".equals(state)||"empty".equals(state);}
 }
 public ScheduleRepository(Context ctx){synchronized(FILE_LOCK){try{AtomicFile f=file(ctx,School.current(ctx));if(f.getBaseFile().exists()){try(InputStream in=f.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>5_000_000)throw new IOException("课表文件过大");}decode(new JSONObject(out.toString("UTF-8")));}}}catch(Exception e){corrupt=true;raw=null;courses.clear();weeks.clear();}}}
 private static AtomicFile file(Context c,School school){return new AtomicFile(new File(c.getFilesDir(),switch(school){
  case WFU -> "schedule.json";
  case AUST -> "schedule-aust.json";
  case GENERAL -> "schedule-general.json";
 }));}
 public static ScheduleRepository parse(JSONObject data)throws Exception {ScheduleRepository r=new ScheduleRepository();r.decode(data);return r;}
 private ScheduleRepository(){}
 public boolean complete(){return !weeks.isEmpty()&&weeks.stream().allMatch(Week::known);}
 public boolean known(LocalDate d){for(Week w:weeks)if(!d.isBefore(w.start)&&!d.isAfter(w.end))return w.known();return false;}
 private void decode(JSONObject data)throws Exception {
  school=School.from(data);
  if(school!=School.WFU){
   JSONArray times=data.getJSONArray("periods");
   if(times.length()>24)throw new JSONException("节次过多");
   starts=new String[times.length()];ends=new String[times.length()];
   LocalTime previous=LocalTime.MIN;
   for(int i=0;i<times.length();i++){
    if(times.isNull(i)){starts[i]="";ends[i]="";continue;}
    JSONObject period=times.getJSONObject(i);
    LocalTime start=LocalTime.parse(period.getString("start")),end=LocalTime.parse(period.getString("end"));
    if(start.isBefore(previous)||!end.isAfter(start))throw new JSONException("作息时间顺序无效");
    starts[i]=start.toString();ends[i]=end.toString();previous=end;
   }
  }
  JSONArray ws=data.getJSONArray("weeks");if(ws.length()<1||ws.length()>60)throw new JSONException("教学周数量异常");
  Set<Integer> seen=new HashSet<>();String term=null;LocalDate previousEnd=null;
  for(int i=0;i<ws.length();i++){
   JSONObject row=ws.getJSONObject(i);Week w=new Week();w.number=row.getInt("number");w.start=LocalDate.parse(row.getString("start"));w.end=LocalDate.parse(row.getString("end"));w.state=row.getString("state");
   if(w.number<1||!seen.add(w.number)||!w.end.equals(w.start.plusDays(6))||w.start.getDayOfWeek()!=DayOfWeek.MONDAY||(previousEnd!=null&&!w.start.equals(previousEnd.plusDays(1))))throw new JSONException("教学周日期不连续或重复");
   previousEnd=w.end;if(!Arrays.asList("ok","empty","unknown").contains(w.state))throw new JSONException("状态无效");
   JSONArray records=row.getJSONArray("courses");if(records.length()>500)throw new JSONException("课程数量异常");
   if(!w.state.equals("ok")&&records.length()!=0)throw new JSONException("课程状态冲突");
   Set<String> weekIds=new HashSet<>();
   for(int j=0;j<records.length();j++){
    JSONObject x=records.getJSONObject(j);String t=x.optString("XNXQDM","");if(t.isEmpty())throw new JSONException("缺少学期信息");if(term==null)term=t;else if(!term.equals(t))throw new JSONException("混入其他学期");
    String recordId=x.getString("KBID");if(recordId.isEmpty()||!weekIds.add(recordId))throw new JSONException("排课标识缺失或重复");
    int weekday=x.getInt("SKXQ");courses.add(new ScheduleLogic.Course(recordId,x.getString("KCM"),x.optString("JSMC",""),x.optString("SKLS",""),ScheduleLogic.dateFor(w.start,weekday),x.getInt("KSJC"),x.getInt("JSJC"),starts,ends));
   }
   weeks.add(w);
  }
  updated=data.getLong("updated");raw=data;courses.sort(Comparator.comparing(ScheduleLogic.Course::start));
 }
 public void save(Context c)throws Exception {
  byte[] bytes=raw.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>5_000_000)throw new IOException("课表过大");
  synchronized(FILE_LOCK){AtomicFile f=file(c,school);FileOutputStream out=null;try{out=f.startWrite();out.write(bytes);f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}}Reminders.schedule(c);
 }
 public static void clear(Context c){synchronized(FILE_LOCK){file(c,School.current(c)).delete();}Reminders.schedule(c);}
}
