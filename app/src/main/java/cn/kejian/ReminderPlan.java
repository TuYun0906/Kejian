package cn.kejian;
import java.util.*;
public final class ReminderPlan {public static SortedMap<Long,List<ScheduleLogic.Course>> groups(List<ScheduleLogic.Course> courses,int minutes,long now){SortedMap<Long,List<ScheduleLogic.Course>> out=new TreeMap<>();for(ScheduleLogic.Course c:courses){long time=c.start().atZone(ScheduleLogic.ZONE).toInstant().toEpochMilli()-minutes*60000L;if(time>now)out.computeIfAbsent(time,k->new ArrayList<>()).add(c);}return out;}}
