package cn.kejian;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class LiveCourseNotification {
 static final String ACTION="cn.kejian.live-course";
 private static final String CHANNEL="live_course";
 private static final int ID=7410;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("live_course",0);}
 static boolean option(Context c,String key,boolean fallback){return prefs(c).getBoolean(key,fallback);}

 static void refresh(Context c,ScheduleRepository repo){
  AlarmManager alarms=c.getSystemService(AlarmManager.class);
  NotificationManager notifications=c.getSystemService(NotificationManager.class);
  PendingIntent boundary=PendingIntent.getBroadcast(c,ID,new Intent(c,Reminders.class).setAction(ACTION),PendingIntent.FLAG_IMMUTABLE);
  alarms.cancel(boundary);
  if(!option(c,"enabled",false)||!notifications.areNotificationsEnabled()||
     (Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)){
   notifications.cancel(ID);return;
  }
  NotificationChannel channel=new NotificationChannel(CHANNEL,"上课状态",NotificationManager.IMPORTANCE_LOW);
  channel.setShowBadge(false);
  notifications.createNotificationChannel(channel);
  if(notifications.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE){
   notifications.cancel(ID);return;
  }

  LocalDateTime now=LocalDateTime.now(ScheduleLogic.ZONE);
  LocalDateTime next=null;
  List<ScheduleLogic.Course> active=new ArrayList<>();
  for(ScheduleLogic.Course course:repo.courses){
   if(!course.end().isAfter(now))continue;
   if(course.start().isAfter(now)){
    next=earlier(next,course.start());continue;
   }
   boolean onBreak="课间休息".equals(ScheduleLogic.status(course,now));
   if(!onBreak||option(c,"break",true))active.add(course);
   for(int i=course.first;i<=course.last;i++){
    if(course.startOf(i).isAfter(now))next=earlier(next,course.startOf(i));
    if(course.endOf(i).isAfter(now))next=earlier(next,course.endOf(i));
   }
  }
  if(next!=null){
   long at=millis(next);
   if(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms())
    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,boundary);
   else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,boundary);
  }
  if(active.isEmpty()){notifications.cancel(ID);return;}

  ScheduleLogic.Course course=active.get(0);
  String status=active.size()>1?"课程时间重叠":ScheduleLogic.status(course,now);
  List<String> fields=new ArrayList<>();
  if(option(c,"room",true)&&course.room!=null&&!course.room.trim().isEmpty())fields.add(course.room);
  if(option(c,"time",true))fields.add(course.time());
  if(option(c,"teacher",false)&&course.teacher!=null&&!course.teacher.trim().isEmpty())fields.add(course.teacher);
  String line=status+(fields.isEmpty()?"":" · "+String.join(" · ",fields));
  String title=active.size()>1?"课程进行中 · "+active.size()+" 门":course.name;
  String detail=line;
  if(active.size()>1){
   List<String> names=new ArrayList<>();
   for(ScheduleLogic.Course item:active)names.add(item.name);
   detail=String.join(" / ",names)+"\n"+line;
  }
  if(option(c,"next",false)){
   ScheduleLogic.Course upcoming=ScheduleLogic.next(ScheduleLogic.onDate(repo.courses,now.toLocalDate()),now);
   if(upcoming!=null)detail+="\n下一门 · "+upcoming.start().toLocalTime()+"  "+upcoming.name;
  }
  Intent open=new Intent(c,MainActivity.class).setAction(ACTION).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  PendingIntent tap=PendingIntent.getActivity(c,ID,open,PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder card=new Notification.Builder(c,CHANNEL)
   .setSmallIcon(c.getResources().getIdentifier("ic_live_course","drawable",c.getPackageName())).setContentTitle(title).setContentText(line)
   .setStyle(new Notification.BigTextStyle().bigText(detail))
   .setColor(option(c,"accent",true)?UiKit.accentColor(c):Notification.COLOR_DEFAULT)
   .setVisibility(option(c,"private",true)?Notification.VISIBILITY_PRIVATE:Notification.VISIBILITY_PUBLIC)
   .setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false).setContentIntent(tap)
   .setTimeoutAfter(Math.max(1,millis(next)-System.currentTimeMillis()));
  if(active.size()==1&&option(c,"countdown",true)){
   LocalDateTime target="课间休息".equals(status)?ScheduleLogic.resume(course,now):course.end();
   card.setWhen(millis(target)).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true);
  }
  notifications.notify(ID,card.build());
 }
 private static long millis(LocalDateTime time){return time.atZone(ScheduleLogic.ZONE).toInstant().toEpochMilli();}
 private static LocalDateTime earlier(LocalDateTime first,LocalDateTime second){return first==null||second.isBefore(first)?second:first;}
}
