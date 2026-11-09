package cn.kejian;
import java.time.*;
import java.util.*;
public final class ScheduleLogic {
 public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 public static final String[] START={"08:00","09:00","10:10","11:10","14:10","15:10","16:10","17:10","19:00","20:00"};
 public static final String[] END={"08:50","09:50","11:00","12:00","15:00","16:00","17:00","18:00","19:50","20:50"};
 public static final class Course {
  public final String id,name,room,teacher;public final LocalDate date;public final int first,last;
  private final String[] starts,ends;
  public Course(String id,String name,String room,String teacher,LocalDate date,int first,int last){
   this(id,name,room,teacher,date,first,last,START,END);
  }
  Course(String id,String name,String room,String teacher,LocalDate date,int first,int last,String[] starts,String[] ends){
   this.starts=starts;this.ends=ends;
   if(first<1||last>starts.length||last<first||date==null||name==null||name.trim().isEmpty())throw new IllegalArgumentException("课程日期或节次无效");
   for(int unit=first-1;unit<last;unit++)if(starts[unit].isEmpty()||ends[unit].isEmpty())throw new IllegalArgumentException("缺少节次时间");
   this.id=id;this.name=name;this.room=room;this.teacher=teacher;this.date=date;this.first=first;this.last=last;
  }
  LocalDateTime startOf(int i){return date.atTime(LocalTime.parse(starts[i-1]));}
  LocalDateTime endOf(int i){return date.atTime(LocalTime.parse(ends[i-1]));}
  public LocalDateTime start(){return startOf(first);}
  public LocalDateTime end(){return endOf(last);}
  public String time(){return starts[first-1]+"—"+ends[last-1];}
 }
 public static LocalDate dateFor(LocalDate monday,int weekday){if(weekday<1||weekday>7)throw new IllegalArgumentException("星期无效");return monday.plusDays(weekday-1);}
 public static String status(Course c,LocalDateTime now){
  if(now.isBefore(c.start()))return "未开始";if(!now.isBefore(c.end()))return "已结束";
  for(int i=c.first;i<=c.last;i++)if(!now.isBefore(c.startOf(i))&&now.isBefore(c.endOf(i)))return "正在上课";
  return "课间休息";
 }
 public static int progress(Course c,LocalDateTime now){return (int)Math.max(0,Math.min(100,Duration.between(c.start(),now).toMillis()*100/Duration.between(c.start(),c.end()).toMillis()));}
 public static LocalDateTime resume(Course c,LocalDateTime now){for(int i=c.first;i<=c.last;i++){LocalDateTime t=c.startOf(i);if(t.isAfter(now))return t;}return c.end();}
 public static List<Course> onDate(List<Course> all,LocalDate d){List<Course> out=new ArrayList<>();for(Course c:all)if(c.date.equals(d))out.add(c);out.sort(Comparator.comparing(Course::start));return out;}
 public static Course next(List<Course> all,LocalDateTime now){return all.stream().filter(c->c.date.equals(now.toLocalDate())&&c.start().isAfter(now)).min(Comparator.comparing(Course::start)).orElse(null);}
 public static List<Course> conflicts(List<Course> courses){Set<Integer> indices=new HashSet<>();for(int i=0;i<courses.size();i++)for(int j=i+1;j<courses.size();j++)if(courses.get(i).start().isBefore(courses.get(j).end())&&courses.get(j).start().isBefore(courses.get(i).end())){indices.add(i);indices.add(j);}List<Course> out=new ArrayList<>();for(int i=0;i<courses.size();i++)if(indices.contains(i))out.add(courses.get(i));return out;}
 public static List<Course> visibleToday(List<Course> all,LocalDateTime now){List<Course> out=new ArrayList<>();Course previous=null;for(Course c:onDate(all,now.toLocalDate())){if(!c.end().isAfter(now)){if(previous==null||c.end().isAfter(previous.end()))previous=c;}else out.add(c);}if(previous!=null)out.add(0,previous);return out;}
}
