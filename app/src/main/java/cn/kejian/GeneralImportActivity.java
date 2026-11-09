package cn.kejian;

import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public final class GeneralImportActivity extends Activity {
 private WebView web;

 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  UiKit.apply(this);
  LinearLayout body = UiKit.col(this);
  body.setBackgroundColor(UiKit.BG);
  body.addView(UiKit.action(this,"‹ 返回",false,this::finish));
  EditText address = field("学校官方教务网址（HTTPS）");
  address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
  body.addView(address);
  body.addView(UiKit.action(this,"打开教务",false,()->open(address.getText().toString().trim())));
  body.addView(UiKit.label(this,"在官方页面登录后，打开当前学期的完整课表。",14,UiKit.MUTED));
  body.addView(UiKit.action(this,"读取当前页面的课表",true,this::read));
  web = new WebView(this);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(false);
  web.setWebViewClient(new WebViewClient() {
   @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
    address.setText(url);
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
    return !"https".equalsIgnoreCase(request.getUrl().getScheme());
   }
  });
  body.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(body);
 }

 private EditText field(String hint) {
  EditText input = new EditText(this);
  input.setHint(hint);
  input.setTextColor(UiKit.INK);
  input.setHintTextColor(UiKit.MUTED);
  return input;
 }

 private void open(String value) {
  Uri url = Uri.parse(value);
  if (!"https".equalsIgnoreCase(url.getScheme()) || url.getHost() == null
    || url.getUserInfo() != null) {
   message("请输入学校的 HTTPS 教务网址");
   return;
  }
  web.loadUrl(value);
 }

 private void read() {
  String page = web.getUrl();
  if (page == null || !"https".equalsIgnoreCase(Uri.parse(page).getScheme())) {
   message("请先打开学校的课表页面");
   return;
  }
  try (InputStream input = getAssets().open("general-timetable.js")) {
   ByteArrayOutputStream script = new ByteArrayOutputStream();
   byte[] bytes = new byte[4096];
   for (int n; (n = input.read(bytes)) != -1;) script.write(bytes,0,n);
   web.evaluateJavascript(new String(script.toByteArray(),StandardCharsets.UTF_8),value->{
    if (isFinishing() || !page.equals(web.getUrl())) return;
    try {
     JSONObject result = new JSONObject(value);
     JSONArray records = result.getJSONArray("records");
     int unrecognized = result.getJSONArray("errors").length();
     if (unrecognized > 0) {
      message("有 " + unrecognized + " 个课程块未识别，无法安全保存整学期课表");
     } else if (records.length() == 0) {
      message("未识别到课表，请确认已打开完整学期课表");
     } else {
      askCalendar(records);
     }
    } catch (Exception e) {
     message("无法读取当前页面的课表");
    }
   });
  } catch (Exception e) {
   message("无法加载课表读取器");
  }
 }

 private void askCalendar(JSONArray records) {
  LinearLayout body = UiKit.col(this);
  EditText start = field("第 1 周周一（YYYY-MM-DD）");
  EditText weeks = field("学期总周数");
  weeks.setInputType(InputType.TYPE_CLASS_NUMBER);
  EditText times = field("每行填写一节，如 08:00-08:45");
  times.setMinLines(3);
  times.setMaxLines(5);
  ScheduleRepository previous = new ScheduleRepository(this);
  if (previous.raw != null) {
   start.setText(previous.weeks.get(0).start.toString());
   weeks.setText(Integer.toString(previous.weeks.size()));
   StringBuilder lines = new StringBuilder();
   for (int i = 0; i < previous.starts.length; i++) {
    if (i > 0) lines.append('\n');
    lines.append(previous.starts[i].isEmpty() ? "-" : previous.starts[i] + "-" + previous.ends[i]);
   }
   times.setText(lines);
  }
  body.addView(UiKit.label(this,"请核对当前学期的校历和节次时间。无对应节次可填一行「-」。",14,UiKit.MUTED));
  body.addView(start);
  body.addView(weeks);
  body.addView(times);
  Dialog panel = UiKit.sheet(this,"填写校历与作息",body);
  body.addView(UiKit.action(this,"预览课表",true,()->{
   try {
    ScheduleRepository parsed = ScheduleRepository.parse(makeSchedule(records,
      LocalDate.parse(start.getText().toString().trim()),
      Integer.parseInt(weeks.getText().toString().trim()),times.getText().toString()));
    panel.dismiss();
    preview(parsed);
   } catch (Exception e) {
    message("请核对开学日期、总周数和节次时间");
   }
  }));
  panel.show();
 }

 static JSONObject makeSchedule(JSONArray records, LocalDate start, int count, String times) throws Exception {
  if (start.getDayOfWeek() != DayOfWeek.MONDAY || count < 1 || count > 60)
   throw new IllegalArgumentException("Invalid term");
  JSONArray periods = new JSONArray();
  for (String line : times.trim().split("\\r?\\n")) {
   if (line.trim().equals("-")) {
    periods.put(JSONObject.NULL);
    continue;
   }
   String[] range = line.trim().split("-", -1);
   if (range.length != 2) throw new IllegalArgumentException("Invalid period");
   periods.put(new JSONObject().put("start",LocalTime.parse(range[0].trim()).toString())
     .put("end",LocalTime.parse(range[1].trim()).toString()));
  }
  JSONArray weeks = new JSONArray();
  for (int i = 0; i < count; i++) {
   LocalDate monday = start.plusWeeks(i);
   weeks.put(new JSONObject().put("number",i+1).put("start",monday.toString())
     .put("end",monday.plusDays(6).toString()).put("state","empty").put("courses",new JSONArray()));
  }
  for (int i = 0; i < records.length(); i++) {
   JSONObject row = records.getJSONObject(i);
   Set<Integer> active = expandWeeks(row.getString("weeks"),row.getString("parity"),count);
   int first = row.getInt("first"), last = row.getInt("last");
   if (first < 1 || first > last || last > periods.length())
    throw new IllegalArgumentException("Period outside timetable");
   for (int period = first; period <= last; period++) {
    if (periods.isNull(period-1)) throw new IllegalArgumentException("Missing period");
   }
   JSONObject course = new JSONObject().put("KBID",UUID.randomUUID().toString())
     .put("XNXQDM","LOCAL").put("KCM",row.getString("title")).put("JSMC",row.optString("room"))
     .put("SKLS",row.optString("teacher")).put("SKXQ",row.getInt("day"))
     .put("KSJC",first).put("JSJC",last);
   for (int number : active) {
    JSONObject week = weeks.getJSONObject(number-1);
    week.put("state","ok");
    week.getJSONArray("courses").put(course);
   }
  }
  return new JSONObject().put("school",School.GENERAL.name()).put("periods",periods)
    .put("updated",System.currentTimeMillis()).put("weeks",weeks);
 }

 static Set<Integer> expandWeeks(String expression, String parity, int count) {
  Set<Integer> weeks = new TreeSet<>();
  String normalized = expression.replace('－','-').replace('至','-').replaceAll("\\s+","");
  for (String part : normalized.split("[,，、]",-1)) {
   if (!part.matches("[0-9]{1,2}(-[0-9]{1,2})?")) throw new IllegalArgumentException("Invalid weeks");
   String[] limits = part.split("-");
   int first = Integer.parseInt(limits[0]);
   int last = Integer.parseInt(limits[limits.length-1]);
   if (first < 1 || last > count || first > last) throw new IllegalArgumentException("Week outside term");
   for (int week = first; week <= last; week++) {
    if (parity.isEmpty() || (week % 2 == 1 ? "单" : "双").equals(parity)) weeks.add(week);
   }
  }
  if (weeks.isEmpty()) throw new IllegalArgumentException("Empty weeks");
  return weeks;
 }

 private void preview(ScheduleRepository schedule) {
  LinearLayout body = UiKit.col(this);
  body.addView(UiKit.label(this,"共 " + schedule.weeks.size() + " 周，"
    + schedule.courses.size() + " 次上课。请与官网核对后保存。",15,UiKit.INK));
  StringBuilder details = new StringBuilder();
  for (ScheduleLogic.Course course : schedule.courses) {
   details.append(course.date).append("  ").append(course.start().toLocalTime()).append("–")
     .append(course.end().toLocalTime()).append("  ").append(course.name);
   if (!course.room.isEmpty()) details.append("  ").append(course.room);
   details.append('\n');
  }
  ScrollView scroll = new ScrollView(this);
  scroll.addView(UiKit.label(this,details.toString(),14,UiKit.MUTED));
  body.addView(scroll,new LinearLayout.LayoutParams(-1,UiKit.dp(this,220)));
  Dialog panel = UiKit.sheet(this,"预览通用课表",body);
  body.addView(UiKit.action(this,"保存当前学期",true,()->{
   try {
    ScheduleRepository existing = new ScheduleRepository(this);
    if (existing.raw != null) LocalTables.saveNamed(this,"通用导入前备份",existing.raw);
    schedule.save(this);
    panel.dismiss();
    message("课表导入成功");
    finish();
   } catch (Exception e) {
    message("无法保存课表，请重试");
   }
  }));
  body.addView(UiKit.action(this,"取消",false,panel::dismiss));
  panel.show();
 }

 private void message(String message) {
  Toast.makeText(this,message,Toast.LENGTH_LONG).show();
 }

 @Override protected void onDestroy() {
  if (web != null) {
   ((android.view.ViewGroup) web.getParent()).removeView(web);
   web.destroy();
  }
  super.onDestroy();
 }
}
