package cn.kejian;
import org.json.*;import java.time.*;
public class LocalTablesTest {
 public static void main(String[] args)throws Exception{
  JSONObject empty=LocalTables.blank();ScheduleRepository r=ScheduleRepository.parse(empty);if(r.weeks.size()!=20||r.courses.size()!=0)throw new AssertionError("preview");
  LocalDate date=r.weeks.get(2).start;JSONObject added=LocalTables.edit(empty,null,date,"本地课程","","",1,2,false);ScheduleRepository one=ScheduleRepository.parse(added);if(one.courses.size()!=1||!one.courses.get(0).date.equals(date))throw new AssertionError("add before login");
  JSONObject changed=LocalTables.edit(added,one.courses.get(0),date.plusDays(1),"改名","A101","",3,4,false);ScheduleRepository edited=ScheduleRepository.parse(changed);if(edited.courses.size()!=1||edited.courses.get(0).first!=3)throw new AssertionError("edit");
  JSONObject second=LocalTables.edit(changed,null,date.plusWeeks(2),"另一课程","","",5,6,false);ScheduleRepository two=ScheduleRepository.parse(second);java.util.List<Integer> visible=LocalTables.visible(two);if(!visible.equals(java.util.Arrays.asList(2,3,4)))throw new AssertionError("middle empty must remain");
  ScheduleRepository deleted=ScheduleRepository.parse(LocalTables.edit(changed,edited.courses.get(0),date.plusDays(1),"改名","","",3,4,true));if(!deleted.courses.isEmpty())throw new AssertionError("delete");
  JSONObject unknown=new JSONObject(second.toString());unknown.getJSONArray("weeks").getJSONObject(0).put("state","unknown");if(LocalTables.visible(ScheduleRepository.parse(unknown)).get(0)!=0)throw new AssertionError("unknown is not empty");
  System.out.println("PASS preview, local add/edit/delete, trim ends, keep middle and unknown weeks");
 }
}
