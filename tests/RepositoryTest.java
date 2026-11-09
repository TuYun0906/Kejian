package cn.kejian;
import org.json.*;
import java.time.*;
public class RepositoryTest {
 static JSONObject course()throws Exception{return new JSONObject("{\"KBID\":\"c1\",\"KCM\":\"测试课程\",\"SKXQ\":\"7\",\"KSJC\":\"9\",\"JSJC\":\"10\",\"XNXQDM\":\"2026-2027-1\"}");}
 static JSONObject week(String state)throws Exception {return new JSONObject().put("number",18).put("start","2026-12-28").put("end","2027-01-03").put("state",state).put("courses",new JSONArray());}
 static JSONObject snapshot(JSONObject w)throws Exception{return new JSONObject().put("updated",1).put("weeks",new JSONArray().put(w));}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void reject(JSONObject s,String reason)throws Exception{try{ScheduleRepository.parse(s);}catch(Exception expected){return;}throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception{
  ScheduleRepository unknown=ScheduleRepository.parse(snapshot(week("unknown")));check(!unknown.complete()&&!unknown.known(LocalDate.of(2027,1,1)),"unknown must not mean empty");
  check(ScheduleRepository.parse(snapshot(week("empty"))).complete(),"confirmed empty is known");
  ScheduleRepository good=ScheduleRepository.parse(snapshot(week("ok").put("courses",new JSONArray().put(course()))));
  check(good.courses.get(0).date.equals(LocalDate.of(2027,1,3)),"cross year date");check(good.courses.get(0).room.isEmpty(),"missing room retained");
  reject(snapshot(week("empty").put("courses",new JSONArray().put(course()))),"empty week cannot hide courses");
  reject(snapshot(week("ok").put("end","2027-01-04")),"invalid week rejected");
  reject(snapshot(week("ok").put("courses",new JSONArray().put(course()).put(course()))),"duplicate record accepted");
  reject(snapshot(week("ok").put("courses",JSONObject.NULL)),"malformed successful data accepted as empty");
  reject(snapshot(week("ok").put("courses",new JSONObject())),"object successful data accepted as empty");
  System.out.println("PASS: 9 repository validation checks");
 }
}
