package cn.kejian;
import java.time.*;
import java.util.*;
public class ScheduleLogicTest {
 public static void main(String[] args) {
  LocalDate day=LocalDate.of(2026,11,5);
  ScheduleLogic.Course math=new ScheduleLogic.Course("1","高数","A509","",day,1,2);
  check(ScheduleLogic.status(math,day.atTime(8,55)).equals("课间休息"),"08:55 break");
  check(ScheduleLogic.progress(math,day.atTime(8,55))==50,"block progress includes break");
  check(ScheduleLogic.status(math,day.atTime(9,50)).equals("已结束"),"end exclusive");
  check(ScheduleLogic.status(math,day.atTime(8,0)).equals("正在上课"),"start inclusive");
  ScheduleLogic.Course pm=new ScheduleLogic.Course("2","高数","","",day,5,6);
  check(ScheduleLogic.status(pm,day.atTime(15,5)).equals("课间休息"),"confirmed afternoon break");
  ScheduleLogic.Course night=new ScheduleLogic.Course("3","晚课","","",day,9,10);
  check(ScheduleLogic.status(night,day.atTime(20,50)).equals("已结束"),"confirmed night end");
  check(ScheduleLogic.dateFor(LocalDate.of(2026,12,28),7).equals(LocalDate.of(2027,1,3)),"year crossing");
  check(ScheduleLogic.conflicts(Arrays.asList(math,math)).size()==2,"keep conflicting records");
  check(ScheduleLogic.next(Arrays.asList(math,pm),day.atTime(8,55))==pm,"next course not next small period");
  try {new ScheduleLogic.Course("x","bad","","",day,0,2);throw new AssertionError("invalid period accepted");}catch(IllegalArgumentException expected){}
  ScheduleLogic.Course early=new ScheduleLogic.Course("early","体育","","",day,1,2);
  check(ScheduleLogic.visibleToday(Arrays.asList(early,pm,night),day.atTime(18,0)).size()==2,"only last finished and future shown");
  check(ScheduleLogic.visibleToday(Arrays.asList(early,pm,night),day.atTime(18,0)).get(0)==pm,"last completed course retained");
  System.out.println("PASS: 12 schedule behavior checks");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
