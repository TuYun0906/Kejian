package cn.kejian;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.time.*;
import java.util.*;
public final class TimetableView extends View {
 public interface Selected { void show(java.util.List<ScheduleLogic.Course> courses); }
 private float touchX,touchY;private boolean dragging=false;private java.util.function.IntConsumer swipe;public void setSwipe(java.util.function.IntConsumer s){swipe=s;}private final CoursePalette palette;private final ScheduleRepository repo;private final ScheduleRepository.Week week;private final Selected selected;private final Paint p=new Paint(3);private final java.util.List<RectF> bounds=new ArrayList<>();private final java.util.List<ScheduleLogic.Course> entries=new ArrayList<>();
 public TimetableView(Context c,ScheduleRepository r,ScheduleRepository.Week w,Selected s){super(c);repo=r;week=w;selected=s;java.util.List<String> names=new ArrayList<>();for(ScheduleLogic.Course course:r.courses)names.add(course.name);palette=new CoursePalette(names);setBackground(UiKit.shape(c,UiKit.SURFACE,20));setClipToOutline(true);setOnClickListener(v->openCourse());setContentDescription("周一至周日完整课表，点击课程查看完整名称和详情");}
 private float d(float x){return x*getResources().getDisplayMetrics().density;}
 private void label(Canvas c,String text,float x,float y,float size,int color){p.setTextSize(size);p.setColor(color);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(text,x,y,p);}
 private void centered(Canvas c,String value,float x,float y,float size,int color){
  p.setTextSize(size);
  p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
  Paint.FontMetrics fm=p.getFontMetrics();
  label(c,value,x,y-(fm.ascent+fm.descent)/2,size,color);
 }
 @Override protected void onDraw(Canvas c){super.onDraw(c);bounds.clear();entries.clear();float gutter=d(28),head=d(38),cw=(getWidth()-gutter)/7f;
  int count=0;
  for(String start:repo.starts)if(!start.isEmpty())count++;
  float rh=Math.max(0,getHeight()-head)/Math.max(1,count);
  float[] lines=new float[repo.starts.length+1];
  lines[0]=head;
  for(int i=0;i<repo.starts.length;i++)lines[i+1]=lines[i]+(repo.starts[i].isEmpty()?0:rh);int ink=UiKit.INK;String[] ds={"一","二","三","四","五","六","日"};
  for(int day=0;day<7;day++){LocalDate date=week.start.plusDays(day);float x=gutter+day*cw;if(date.equals(LocalDate.now(ScheduleLogic.ZONE))){p.setColor(UiKit.tint(UiKit.BRAND));c.drawRoundRect(new RectF(x+2,3,x+cw-2,head-2),d(10),d(10),p);}p.setTextAlign(Paint.Align.CENTER);
   centered(c,"周"+ds[day],x+cw/2,head*.29f,Math.min(d(12),cw/3),ink);
   centered(c,date.getMonthValue()+"/"+date.getDayOfMonth(),x+cw/2,head*.73f,Math.min(d(10),cw/3),UiKit.MUTED);
   p.setTextAlign(Paint.Align.LEFT);}
  p.setStrokeWidth(d(.5f));
  for(int i=0;i<repo.starts.length;i++){
   if(repo.starts[i].isEmpty())continue;
   p.setColor(UiKit.LINE);c.drawLine(gutter,lines[i],getWidth(),lines[i],p);
   if(rh>=d(12)){
    centered(c,""+(i+1),d(5),lines[i]+rh/2,Math.min(d(9),rh*.42f),ink);
    centered(c,repo.starts[i],d(2),lines[i],Math.min(d(7),rh*.42f),UiKit.MUTED);
   }else if(rh>=d(9))centered(c,""+(i+1),d(5),lines[i]+rh/2,d(8),ink);
  }
  for(int day=0;day<7;day++){java.util.List<ScheduleLogic.Course> list=ScheduleLogic.onDate(repo.courses,week.start.plusDays(day));for(ScheduleLogic.Course course:list){java.util.List<ScheduleLogic.Course> overlaps=new ArrayList<>();for(ScheduleLogic.Course other:list)if(course.start().isBefore(other.end())&&other.start().isBefore(course.end()))overlaps.add(other);int lane=overlaps.indexOf(course),lanes=Math.max(1,overlaps.size());float x=gutter+day*cw+lane*cw/lanes;RectF r=new RectF(x+d(1),lines[course.first-1]+d(1),x+cw/lanes-d(1),lines[course.last]-d(1));p.setColor(UiKit.tint(palette.color(course.name)));c.drawRoundRect(r,d(10),d(10),p);p.setColor(palette.color(course.name));c.drawRoundRect(new RectF(r.left,r.top+d(7),r.left+d(2),r.bottom-d(7)),d(1),d(1),p);bounds.add(r);entries.add(course);drawCourseText(c,r,course,ink);}}
 }
 private void drawCourseText(Canvas canvas,RectF rect,ScheduleLogic.Course course,int color){
  if(rect.width()<d(7)||rect.height()<d(8))return;
  int width=Math.max(1,(int)(rect.width()-d(5)));
  float height=rect.height()-d(4);
  String title=course.name.trim();
  String value=title+(course.room.isEmpty()?"":"\n"+course.room);
  android.text.SpannableString styled=new android.text.SpannableString(value);
  if(!course.room.isEmpty()){
   styled.setSpan(new android.text.style.RelativeSizeSpan(.88f),title.length(),value.length(),33);
   styled.setSpan(new android.text.style.ForegroundColorSpan(UiKit.MUTED),title.length(),value.length(),33);
  }
  android.text.TextPaint paint=new android.text.TextPaint(3);
  paint.setColor(color);
  paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  float preferred=Math.min(d(14),Math.max(d(8),width/3.7f));
  android.text.StaticLayout layout=null;
  for(float size=preferred;size>=d(7);size-=d(.5f)){
   paint.setTextSize(size);
   android.text.StaticLayout candidate=layout(styled,paint,width,Integer.MAX_VALUE);
   if(candidate.getHeight()<=height){layout=candidate;break;}
  }
  if(layout==null){
   paint.setTextSize(d(7));
   int max=Math.max(1,(int)(height/paint.getFontSpacing()));
   layout=layout(title,paint,width,max);
  }
  canvas.save();
  canvas.clipRect(rect);
  canvas.translate(rect.centerX()-width/2f,rect.centerY()-layout.getHeight()/2f);
  layout.draw(canvas);
  canvas.restore();
 }
 private android.text.StaticLayout layout(CharSequence value,android.text.TextPaint paint,int width,int maxLines){
  return android.text.StaticLayout.Builder.obtain(value,0,value.length(),paint,width)
   .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setIncludePad(false)
   .setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END).build();
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN){touchX=event.getX();touchY=event.getY();}
  return super.onTouchEvent(event);
 }
 private void openCourse(){
  java.util.List<ScheduleLogic.Course> hit=new ArrayList<>();
  for(int i=0;i<bounds.size();i++)if(bounds.get(i).contains(touchX,touchY))hit.add(entries.get(i));
  if(!hit.isEmpty())selected.show(hit);
 }
}
