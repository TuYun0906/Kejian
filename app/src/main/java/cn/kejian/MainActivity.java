package cn.kejian;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
 private boolean preview=false,settings=false,notes=false;private CoursePalette palette;private ScheduleRepository repo;private LinearLayout root,content;private View idleVisual;private TextView summary,heroName,heroStatus;private ProgressBar progress;
 private WeekPager weekPager;private float dockFrom=-1;private boolean transitioning=false;private boolean weekly=false;private int weekIndex=0;private final Handler timer=new Handler(Looper.getMainLooper());private String signature="";
 private int ink=UiKit.INK, muted=UiKit.MUTED, accent=UiKit.accentText();
 private final Runnable tick=new Runnable(){public void run(){if(!weekly&&!settings&&!notes){String s=currentSignature();if(!s.equals(signature))render();else updateHero();}timer.postDelayed(this,1000);}};
 private LocalDateTime now(){return LocalDateTime.now(ScheduleLogic.ZONE);}
 private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
 private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
 private Button button(String title,Runnable action){return UiKit.action(this,title.replace("● ",""),title.contains("●"),action);}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 @Override public void onCreate(Bundle state){super.onCreate(state);if(state!=null){weekly=state.getBoolean("weekly");settings=state.getBoolean("settings");notes=state.getBoolean("notes");weekIndex=state.getInt("weekIndex");}if(LiveCourseNotification.ACTION.equals(getIntent().getAction()))weekly=settings=notes=false;loadRepo();if(state==null)findCurrentWeek();render();}
 private void loadRepo(){repo=new ScheduleRepository(this);preview=repo.raw==null;if(preview)try{repo=ScheduleRepository.parse(LocalTables.blank(School.current(this)));}catch(Exception ignored){}}
 private void findCurrentWeek(){LocalDate d=now().toLocalDate();for(int i=0;i<repo.weeks.size();i++){ScheduleRepository.Week w=repo.weeks.get(i);if(!d.isBefore(w.start)&&!d.isAfter(w.end)){weekIndex=i;return;}}weekIndex=0;}
 @Override protected void onNewIntent(Intent intent){
  super.onNewIntent(intent);
  if(LiveCourseNotification.ACTION.equals(intent.getAction()))switchTab(0,-1);
 }
 @Override protected void onResume(){super.onResume();String oldStart=repo.weeks.isEmpty()?"":repo.weeks.get(0).start.toString();loadRepo();Reminders.schedule(this);String newStart=repo.weeks.isEmpty()?"":repo.weeks.get(0).start.toString();if(!oldStart.equals(newStart))findCurrentWeek();render();timer.removeCallbacks(tick);timer.post(tick);updateResumed=true;checkAppUpdate(false);}
 @Override protected void onPause(){updateResumed=false;timer.removeCallbacks(tick);if(content!=null)content.animate().cancel();transitioning=false;super.onPause();}
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("weekly",weekly);b.putBoolean("settings",settings);b.putBoolean("notes",notes);b.putInt("weekIndex",weekIndex);}
 private String currentSignature(){StringBuilder s=new StringBuilder(now().toLocalDate().toString());for(ScheduleLogic.Course c:ScheduleLogic.onDate(repo.courses,now().toLocalDate()))s.append(c.id).append(ScheduleLogic.status(c,now()));return s.toString();}
 private NotesView currentMemo;
 @Override public void onBackPressed(){
  if(currentMemo==null||!currentMemo.dismissCreateMenu())super.onBackPressed();
 }
 private void render(){
  currentMemo=null;
  weekPager=null;UiKit.apply(this);ink=UiKit.INK;muted=UiKit.MUTED;accent=UiKit.accentText();getWindow().getDecorView().setSystemUiVisibility(UiKit.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);getWindow().setNavigationBarColor(UiKit.BG);getWindow().setStatusBarColor(UiKit.BG);java.util.List<String> names=new ArrayList<>();for(ScheduleLogic.Course c:repo.courses)names.add(c.name);palette=new CoursePalette(names);signature=currentSignature();root=column();root.setBackgroundColor(UiKit.BG);root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(20),dp(14),dp(16),dp(8));LinearLayout titles=column();TextView brand=UiKit.label(this,"课间  /  "+headerDate(),12,muted);titles.addView(brand);TextView title=UiKit.label(this,settings?"更多":(notes?"备忘":(weekly?"课表":"今日")),30,ink);title.setTypeface(null,Typeface.BOLD);title.setPadding(0,dp(6),0,0);titles.addView(title);toolbar.addView(titles,new LinearLayout.LayoutParams(0,-2,1));Button more=button("",this::menu);more.setBackground(new IconTile(this,3));more.setContentDescription("更多选项");Button theme=button("",this::chooseTheme);theme.setBackground(new IconTile(this,getSharedPreferences("appearance",0).getInt("mode",0)==0?2:(UiKit.dark?1:0)));theme.setContentDescription("切换外观模式");toolbar.addView(theme,new LinearLayout.LayoutParams(dp(48),dp(48)));toolbar.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(toolbar);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=column();int side=weekly&&!settings?8:16;content.setPadding(dp(side),0,dp(side),dp(weekly&&!settings?4:20));scroll.addView(content);if(weekly&&!settings){scroll.removeView(content);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));}else if(notes){FrameLayout overlay=new FrameLayout(this);overlay.addView(scroll,new FrameLayout.LayoutParams(-1,-1));root.addView(overlay,new LinearLayout.LayoutParams(-1,0,1));}else root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  DockView nav=new DockView(this,settings?3:(notes?2:(weekly?1:0)),dockFrom,visibleTabs(),dockPressTime,this::switchDockTab);dockFrom=-1;dockPressTime=0;LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(Math.min(dp(352),getResources().getDisplayMetrics().widthPixels-dp(40)),dp(56));int dockPosition=getSharedPreferences("appearance",0).getInt("dockPosition",0);
  np.gravity=dockPosition==2?Gravity.LEFT:dockPosition==3?Gravity.RIGHT:Gravity.CENTER_HORIZONTAL;
  np.setMargins(dp(12),dp(6),dp(12),dp(12));
  if(!getSharedPreferences("appearance",0).getBoolean("dockHidden",false)){
   if(dockPosition==1)root.addView(nav,1,np);else root.addView(nav,np);
  }
  setContentView(root);
  summary=null;progress=null;
  if(settings)renderSettings();else if(notes){NotesView memo=new NotesView(this);currentMemo=memo;content.addView(memo);memo.attachCreateMenu((FrameLayout)scroll.getParent());content.setPadding(dp(side),0,dp(side),dp(96));content.setOnClickListener(v->memo.clearSelection());}else if(weekly)renderWeek();else{renderToday();if(preview){LinearLayout hint=UiKit.card(this);hint.addView(text("连接课表，或先添加本地课程预览",15,ink));hint.addView(button("连接校园课表",this::importCourses));hint.addView(button("添加课程",()->editCourse(null)));content.addView(hint);}}
 }
 private void showScheduleInfo(){
  LinearLayout body=column();
  addScheduleMetadata(body);
  UiKit.sheet(this,"课表信息",body).show();
 }
 private void addScheduleMetadata(LinearLayout body){
  String term=repo.raw.optString("termLabel","");
  body.addView(footerLine(term.isEmpty()?"学期：暂无信息":term));
  String saved=preview||repo.updated<=0?"暂无保存记录":Instant.ofEpochMilli(repo.updated).atZone(ScheduleLogic.ZONE)
   .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
  body.addView(footerLine("本地保存："+saved));
  if(!preview&&!repo.complete())body.addView(footerLine("部分周未获取"));
  if(!preview&&!repo.raw.optString("termEnd","").isEmpty()&&now().toLocalDate().isAfter(repo.weeks.get(repo.weeks.size()-1).end))
   body.addView(footerLine("显示的是旧学期课表，请联网导入当前学期。"));
 }
 private TextView footerLine(String value){
  TextView line=text(value,12,muted);
  line.setPadding(dp(8),dp(2),dp(8),dp(2));
  return line;
 }
 private String headerDate(){
  LocalDate date=now().toLocalDate();
  String label=date.format(DateTimeFormatter.ofPattern("M月d日 EEEE",Locale.CHINA));
  if(!preview){
   for(ScheduleRepository.Week week:repo.weeks)
    if(!date.isBefore(week.start)&&!date.isAfter(week.end))return label+" · 第"+week.number+"周";
  }
  return label;
 }
 private void renderToday(){
  LocalDate day=now().toLocalDate();
  LinearLayout hero=column();hero.setPadding(dp(14),dp(18),dp(14),dp(18));GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xff89763f,0xff77658b});gradient.setCornerRadius(dp(28));hero.setBackground(bg(UiKit.SURFACE,24));LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);heroName=text("",24,ink);heroName.setTypeface(null,Typeface.BOLD);heroName.setMaxLines(2);heroStatus=text("",12,accent);heading.addView(heroName,new LinearLayout.LayoutParams(0,-2,1));heading.addView(heroStatus);hero.addView(heading);progress=new ProgressCircle(this);LinearLayout.LayoutParams ring=new LinearLayout.LayoutParams(dp(142),dp(142));ring.gravity=Gravity.CENTER;ring.topMargin=dp(10);ring.bottomMargin=dp(10);hero.addView(progress,ring);idleVisual=new IdleVisual(this);idleVisual.setVisibility(View.GONE);LinearLayout.LayoutParams idleSize=new LinearLayout.LayoutParams(dp(104),dp(104));idleSize.gravity=Gravity.CENTER;idleSize.topMargin=dp(12);idleSize.bottomMargin=dp(12);hero.addView(idleVisual,idleSize);summary=text("",14,muted);hero.addView(summary);content.addView(hero);updateHero();

  List<ScheduleLogic.Course> today=ScheduleLogic.onDate(repo.courses,day);List<ScheduleLogic.Course> conflict=ScheduleLogic.conflicts(today);if(!conflict.isEmpty()&&today.stream().anyMatch(c->c.end().isAfter(now())))content.addView(text("有时间重叠的课程，请核对安排",15,Color.rgb(162,63,45)));
  LocalDateTime instant=now();
  boolean inClass=today.stream().anyMatch(c->!c.start().isAfter(instant)&&c.end().isAfter(instant));
  ScheduleLogic.Course featured=inClass?null:ScheduleLogic.next(today,instant);
  if(featured!=null){
   ((View)summary.getParent()).setOnClickListener(v->editCourse(featured));
  }
  if(today.stream().anyMatch(c->c.end().isAfter(instant)))
   for(ScheduleLogic.Course c:ScheduleLogic.visibleToday(today,instant))
    if(c!=featured)addCourse(content,c,true);
  if(repo.known(day)&&!today.isEmpty()&&today.stream().noneMatch(c->c.end().isAfter(now()))){TextView empty=text("今天的课程已结束，放松一下吧。",16,muted);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(56),0,dp(40));content.addView(empty);}

 }
 private String remaining(LocalDateTime target){long seconds=Math.max(0,java.time.Duration.between(now(),target).getSeconds());return (seconds/3600>0?seconds/3600+"小时":"")+(seconds%3600/60)+"分"+(seconds>=3600?"":(seconds%60)+"秒");}
 private void setHero(String name,String state,String info,int percent){heroName.setText(CourseText.display(name));heroStatus.setText(state);heroStatus.setVisibility(state.isEmpty()?View.GONE:View.VISIBLE);heroStatus.setTextColor(UiKit.accentText());heroStatus.setBackground(bg(UiKit.tint(UiKit.BRAND),12));((ProgressCircle)progress).setAccent(UiKit.BRAND);boolean idle=name.equals("空闲")||name.equals("今日课程已结束");idleVisual.setVisibility(idle?View.VISIBLE:View.GONE);LinearLayout hero=(LinearLayout)summary.getParent();hero.setOnClickListener(idle?v->showIdleDetails():null);hero.setClickable(idle);if(idle){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{UiKit.tint(UiKit.BRAND),UiKit.SURFACE});g.setCornerRadius(dp(24));hero.setBackground(g);}else hero.setBackground(bg(UiKit.SURFACE,24));heroName.setTextColor(ink);summary.setTextColor(muted);summary.setText(info);progress.setVisibility(percent<0?View.GONE:View.VISIBLE);((ProgressCircle)progress).setCountdown(null);if(percent>=0)progress.setProgress(percent);}
 private void showIdleDetails(){
  LocalDateTime time=now();
  ScheduleLogic.Course next=repo.courses.stream().filter(course->course.start().isAfter(time))
   .min(Comparator.comparing(ScheduleLogic.Course::start)).orElse(null);
  LinearLayout details=column();
  if(next==null)details.addView(text("已保存的课表里暂无后续课程。",15,muted));
  else{
   details.addView(text("下一门 · "+next.name,18,ink));
   details.addView(text(next.date+"  "+next.time()+(room(next).isEmpty()?"":" · "+room(next)),14,muted));
  }
  UiKit.prompt(this,"休息一下",details,"查看课表",()->switchTab(1,-1),null,null).show();
 }
 private void updateHero(){if(summary==null)return;LocalDate day=now().toLocalDate();if(!repo.known(day)){setHero("课表尚未获取","待同步","请更新，或确认当天所在周是否无课",-1);return;}List<ScheduleLogic.Course> today=ScheduleLogic.onDate(repo.courses,day);List<ScheduleLogic.Course> active=new ArrayList<>();for(ScheduleLogic.Course c:today)if(!now().isBefore(c.start())&&now().isBefore(c.end()))active.add(c);
  if(active.size()>1){setHero("课程时间冲突","请核对",String.join(" / ",active.stream().map(c->c.name).toArray(String[]::new)), -1);return;}
  if(active.size()==1){ScheduleLogic.Course c=active.get(0);String state=ScheduleLogic.status(c,now());String info=c.time()+(room(c).isEmpty()?"":"   ·   "+room(c))+"\n"+(state.equals("课间休息")?"距恢复上课 "+remaining(ScheduleLogic.resume(c,now())):"距课程结束 "+remaining(c.end()));ScheduleLogic.Course next=ScheduleLogic.next(today,now());info+="\n"+(next==null?"今日无后续课程":"下一门："+next.name+" · "+next.start().toLocalTime().toString()+(room(next).isEmpty()?"":" · "+room(next)));setHero(c.name,state,info,ScheduleLogic.progress(c,now()));return;}
  ScheduleLogic.Course next=ScheduleLogic.next(today,now());if(next!=null){
   setHero(next.name,"即将上课",next.time()+(room(next).isEmpty()?"":" · "+room(next))+(next.teacher.isEmpty()?"":"\n"+next.teacher),0);
   ((ProgressCircle)progress).setCountdown(remaining(next.start()));
  }else setHero(today.isEmpty()?"空闲":"今日课程已结束",today.isEmpty()?"享受当下":"",today.isEmpty()?"给自己留一点自由的时间。":"今天辛苦了",-1);
 }
 private String room(ScheduleLogic.Course c){return c.room==null||c.room.trim().isEmpty()?"":c.room;}
 private void addCourse(LinearLayout parent,ScheduleLogic.Course c,boolean showStatus){
  LocalDateTime time=now();
  boolean ended=showStatus&&!c.end().isAfter(time);
  boolean active=showStatus&&!ended&&!c.start().isAfter(time);
  int courseColor=palette.color(c.name);
  LinearLayout row=new LinearLayout(this);
  row.setGravity(Gravity.CENTER_VERTICAL);
  View dot=new View(this){
   private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
   @Override protected void onDraw(Canvas canvas){
    float x=getWidth()/2f,y=getHeight()/2f;
    paint.setColor(active?UiKit.tint(UiKit.BRAND):0xffdddcd2);
    paint.setStrokeWidth(dp(2));
    canvas.drawLine(x,0,x,getHeight(),paint);
    if(active)canvas.drawCircle(x,y,dp(9),paint);
    paint.setColor(active?accent:ended?0xffc3c4bc:courseColor);
    canvas.drawCircle(x,y,dp(5),paint);
   }
  };
  row.addView(dot,new LinearLayout.LayoutParams(dp(30),-1));
  LinearLayout card=column();
  card.setPadding(dp(12),dp(12),dp(12),dp(12));
  GradientDrawable surface=bg(UiKit.SURFACE,20);
  if(active){
   surface.setOrientation(GradientDrawable.Orientation.TL_BR);
   surface.setColors(new int[]{UiKit.tint(UiKit.BRAND),UiKit.SURFACE});
  }else if(!ended)surface.setStroke(dp(1),UiKit.tint(courseColor));
  card.setBackground(surface);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.topMargin=dp(12);
  parent.addView(row,lp);
  row.addView(card,new LinearLayout.LayoutParams(0,-2,1));

  if(active){
   TextView live=text("●  "+ScheduleLogic.status(c,time),12,accent);
   live.setTypeface(null,Typeface.BOLD);
   live.setPadding(0,0,0,dp(6));
   card.addView(live);
  }
  LinearLayout line=new LinearLayout(this);
  line.setGravity(Gravity.CENTER_VERTICAL);
  TextView name=text(CourseText.display(c.name),ended?16:20,ink);
  name.setMaxLines(3);
  name.setEllipsize(android.text.TextUtils.TruncateAt.END);
  if(active)name.setTypeface(null,Typeface.BOLD);
  line.addView(name,new LinearLayout.LayoutParams(0,-2,1));
  if(!ended&&!room(c).isEmpty()){
   TextView badge=text("⌖ "+room(c),14,active?accent:courseColor);
   if(!active)badge.setBackground(bg(UiKit.SURFACE,12));
   line.addView(badge);
  }
  card.addView(line);
  if(ended){
   line.addView(text("已上完",13,muted));
   card.setAlpha(.78f);
  }else{
   card.addView(text(c.time(),13,muted));
   if(!c.teacher.isEmpty())card.addView(text(c.teacher,12,muted));
  }
  card.setOnClickListener(v->showCourses(Collections.singletonList(c),false));
 }
 private void renderWeek(){
  if(repo.weeks.isEmpty())return;java.util.List<Integer> allowed=LocalTables.visible(repo);if(!allowed.contains(weekIndex))weekIndex=weekIndex>allowed.get(allowed.size()-1)?allowed.get(allowed.size()-1):allowed.get(0);weekIndex=Math.max(0,Math.min(weekIndex,repo.weeks.size()-1));ScheduleRepository.Week week=repo.weeks.get(weekIndex);
  LinearLayout controls=new LinearLayout(this);controls.addView(button("‹",()->{slideWeek(-1);}));Button weekTitle=button("第"+week.number+"周 ▾",this::chooseWeek);controls.addView(weekTitle,new LinearLayout.LayoutParams(0,-2,1));controls.addView(button("›",()->slideWeek(1)));controls.addView(button("本周",()->{findCurrentWeek();render();}));content.addView(controls);
  TextView dates=text(week.start+" — "+week.end,12,muted);content.addView(dates);
  WeekPager grid=new WeekPager(this,repo,weekIndex,courses->showCourses(courses,true),this::showScheduleInfo,next->{
   weekIndex=next;
   ScheduleRepository.Week shown=repo.weeks.get(next);
   weekTitle.setText("第"+shown.number+"周 ▾");
   dates.setText(shown.start+" — "+shown.end);
  });weekPager=grid;content.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
 }

 private TextView cell(String s,int width,int height){TextView t=text(s,12,ink);t.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,height);lp.setMargins(dp(1),dp(1),dp(1),dp(1));t.setLayoutParams(lp);return t;}
 private void chooseWeek(){LinearLayout body=column();final Dialog[] holder=new Dialog[1];java.util.List<Integer> visible=LocalTables.visible(repo);for(int start=0;start<visible.size();start+=4){LinearLayout row=new LinearLayout(this);for(int j=start;j<Math.min(start+4,visible.size());j++){final int at=visible.get(j);ScheduleRepository.Week w=repo.weeks.get(at);row.addView(UiKit.action(this,"第"+w.number+"周"+(w.known()?"":" ·"),at==weekIndex,()->{weekIndex=at;holder[0].dismiss();render();}),new LinearLayout.LayoutParams(0,dp(44),1));}for(int j=visible.size()-start;j<4;j++)row.addView(new Space(this),new LinearLayout.LayoutParams(0,dp(44),1));body.addView(row);}holder[0]=UiKit.sheet(this,"选择教学周",body);holder[0].show();}
 private long dockPressTime;
 private void switchDockTab(int target,float sliderPosition,long pressTime){
  dockPressTime=pressTime;
  switchTab(target,sliderPosition);
 }
 private void switchTab(int target,float sliderPosition){int current=settings?3:(notes?2:(weekly?1:0));if(target==current)return;dockFrom=sliderPosition;settings=target==3;notes=target==2;weekly=target==1;render();content.setAlpha(0);content.setTranslationX(dp(20)*(target>current?1:-1));content.animate().alpha(1).translationX(0).setDuration(250).start();}
 private void slideWeek(int direction){java.util.List<Integer> visible=LocalTables.visible(repo);int at=visible.indexOf(weekIndex);if(at>=0&&at+direction>=0&&at+direction<visible.size()&&weekPager!=null)weekPager.step(direction);}
 private void chooseTheme(){cycleTheme();} private void importCourses(){
  startActivity(new Intent(this,School.current(this).importActivity()));
 }
 private void chooseSchool(){
  LinearLayout body=column();
  Dialog panel=UiKit.sheet(this,"选择学校",body);
  for(School school:School.values())body.addView(UiKit.action(this,school.label,school==School.current(this),()->{
   panel.dismiss();school.select(this);loadRepo();findCurrentWeek();Reminders.schedule(this);render();
  }));
  panel.show();
 }
 private void showCourses(List<ScheduleLogic.Course> courses, boolean allowAdd) {
  if(courses.isEmpty()) return;
  LinearLayout body=column();
  for(ScheduleLogic.Course c:courses){
   body.addView(text(CourseText.display(c.name),20,ink));
   body.addView(text(c.date+"  "+c.time(),14,muted));
   if(!room(c).isEmpty())body.addView(text("地点："+room(c),14,muted));
   if(!c.teacher.isEmpty())body.addView(text("教师："+c.teacher,14,muted));
  }
  UiKit.prompt(this,"课程详情",body,"编辑",()->editCourse(courses.get(0)),
    allowAdd?"添加课程":null,()->editCourse(null)).show();
 }
 private void menu(){
  LinearLayout body=column();
  body.setPadding(dp(20),dp(36),dp(20),dp(20));
  GradientDrawable surface=bg(UiKit.SURFACE,28);
  surface.setCornerRadii(new float[]{dp(28),dp(28),0,0,0,0,dp(28),dp(28)});
  body.setBackground(surface);
  body.setOnApplyWindowInsetsListener((v,in)->{
   body.setPadding(dp(20),dp(24)+in.getSystemWindowInsetTop(),dp(20),dp(20)+in.getSystemWindowInsetBottom());
   return in;
  });
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);
  SideDrawer panel=new SideDrawer(this,scroll);
  String nickname=profileName();
  String encoded=getSharedPreferences("appearance",0).getString("customAvatar","");
  android.graphics.Bitmap bitmap=AccountAvatar.decode(encoded);
  View avatar;
  if(bitmap==null){
   TextView initial=text(nickname.isEmpty()?"人":nickname.substring(0,nickname.offsetByCodePoints(0,1)),28,UiKit.accentText());
   initial.setGravity(Gravity.CENTER);avatar=initial;
  }else{
   ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar=image;
  }
  avatar.setBackground(bg(UiKit.tint(UiKit.BRAND),36));avatar.setClipToOutline(true);
  LinearLayout.LayoutParams avatarLayout=new LinearLayout.LayoutParams(dp(72),dp(72));avatarLayout.gravity=Gravity.CENTER;
  avatarLayout.topMargin=dp(20);avatarLayout.bottomMargin=dp(10);body.addView(avatar,avatarLayout);
  avatar.setOnClickListener(v->{panel.dismiss();showAvatarOptions();});
  TextView name=text(nickname.isEmpty()?"校园账号":nickname,22,ink);name.setGravity(Gravity.CENTER);body.addView(name);
  Button login=UiKit.action(this,preview?"登录教务 →":"登录 / 更新课表 →",true,()->{panel.dismiss();importCourses();});
  login.setBackground(bg(UiKit.tint(UiKit.BRAND),16));
  login.setTextColor(UiKit.accentText());
  LinearLayout.LayoutParams loginLayout=new LinearLayout.LayoutParams(-1,dp(48));
  loginLayout.setMargins(0,dp(8),0,dp(8));body.addView(login,loginLayout);
  TextView school=text(School.current(this).name,12,muted);school.setGravity(Gravity.CENTER);body.addView(school);
  View divider=new View(this);divider.setBackgroundColor(UiKit.LINE);
  LinearLayout.LayoutParams dividerLayout=new LinearLayout.LayoutParams(-1,dp(1));dividerLayout.setMargins(0,dp(18),0,dp(12));body.addView(divider,dividerLayout);
  LinearLayout entries=column();ScrollView options=new ScrollView(this);options.addView(entries);
  body.addView(options,new LinearLayout.LayoutParams(-1,-2));
  for(int tab:dockOrder()){
   if(!getSharedPreferences("appearance",0).getBoolean("dockHidden",false)&&(dockMask()&(1<<tab))!=0)continue;
   final int target=tab;
   String[] names={"今日","课表","备忘","更多"};
   addDrawerItem(entries,"›",names[tab],()->{panel.dismiss();switchTab(target,-1);});
  }
  addDrawerItem(entries,"↔","导航条设置",()->{panel.dismiss();configureDock();});
  addDrawerItem(entries,"◉","主题颜色",()->{panel.dismiss();chooseAccent();});
  addDrawerItem(entries,"▦","课表管理",()->{panel.dismiss();manageTables();});
  addDrawerItem(entries,"＋","添加课程",()->{panel.dismiss();editCourse(null);});
  addDrawerItem(entries,"⌂","切换学校",()->{panel.dismiss();chooseSchool();});
  addDrawerItem(entries,"↗","通知公告",()->{panel.dismiss();openAnnouncements();});
  addDrawerItem(entries,"ⓘ","关于·更新",()->{panel.dismiss();showAbout();});
  addDrawerItem(entries,"×","清除本地课表",()->{
   panel.dismiss();
   UiKit.prompt(this,"清除本地课表？",text("清除本校课表并退出应用内登录。",15,muted),"清除",()->{
    ScheduleRepository.clear(this);CampusData.clear(this);
    android.webkit.CookieManager.getInstance().removeAllCookies(null);
    android.webkit.CookieManager.getInstance().flush();android.webkit.WebStorage.getInstance().deleteAllData();
    getSharedPreferences("account",0).edit().clear().apply();loadRepo();render();
   },null,null).show();
  });
  body.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));
  TextView footer=text("课间\nTuYun",12,muted);footer.setGravity(Gravity.CENTER);body.addView(footer);
  panel.show();
 }
 private void addDrawerItem(LinearLayout parent,String symbol,String title,Runnable action){
  TextView icon=text(symbol,19,UiKit.accentText());
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
  row.setPadding(0,dp(9),0,dp(9));
  row.setBackground(new android.graphics.drawable.RippleDrawable(
   android.content.res.ColorStateList.valueOf((UiKit.BRAND&0xffffff)|0x22000000),bg(UiKit.SURFACE,14),null));
  row.addView(icon,new LinearLayout.LayoutParams(dp(32),-2));
  row.addView(text(title,15,ink),new LinearLayout.LayoutParams(0,-2,1));
  row.setOnClickListener(v->action.run());parent.addView(row);
 }

 private void cycleTheme(){int next=(getSharedPreferences("appearance",0).getInt("mode",0)+1)%3;getSharedPreferences("appearance",0).edit().putInt("mode",next).apply();render();content.setAlpha(.3f);content.animate().alpha(1).setDuration(220).start();}
 private void persist(org.json.JSONObject data){try{data.put("localEdits",true);ScheduleRepository.parse(data).save(this);loadRepo();Reminders.schedule(this);render();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}
 private void editCourse(ScheduleLogic.Course original) {
  if(repo.starts.length==0){Toast.makeText(this,"请先导入含作息时间的课表",0).show();return;}
  LinearLayout fields=column();
  EditText date=UiKit.input(this,"日期（年-月-日）",(original==null?now().toLocalDate():original.date).toString(),20);
  EditText name=UiKit.input(this,"课程名称",original==null?"":original.name,1);
  EditText room=UiKit.input(this,"地点（可空）",original==null?"":original.room,1);
  EditText teacher=UiKit.input(this,"教师（可空）",original==null?"":original.teacher,1);
  EditText first=UiKit.input(this,"开始节次",String.valueOf(original==null?1:original.first),2);
  EditText last=UiKit.input(this,"结束节次",String.valueOf(original==null?2:original.last),2);
  fields.addView(text("日期（年-月-日）",13,muted));
  fields.addView(date);fields.addView(name);fields.addView(room);fields.addView(teacher);
  LinearLayout periods=new LinearLayout(this);
  for(EditText period:new EditText[]{first,last}){
   LinearLayout cell=column();
   cell.addView(text(period==first?"开始节次":"结束节次",13,muted));
   cell.addView(period);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
   if(period==last)lp.leftMargin=dp(8);
   periods.addView(cell,lp);
  }
  fields.addView(periods);
  UiKit.prompt(this,original==null?"添加课程":"编辑此日课程",fields,"保存",()->{
   try{
    persist(LocalTables.edit(repo.raw,original,LocalDate.parse(date.getText().toString().trim()),
     name.getText().toString().trim(),room.getText().toString().trim(),teacher.getText().toString().trim(),
     Integer.parseInt(first.getText().toString()),Integer.parseInt(last.getText().toString()),false));
   }catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("请输入有效日期，格式为 年-月-日");}
    catch(NumberFormatException e){throw new IllegalArgumentException("请输入有效节次");}
    catch(Exception e){throw new IllegalArgumentException(e.getMessage()==null?"保存失败":e.getMessage());}
  },original==null?null:"删除此日课程",()->{
   try{persist(LocalTables.edit(repo.raw,original,original.date,original.name,original.room,original.teacher,original.first,original.last,true));}
   catch(Exception e){Toast.makeText(this,"删除课程失败",0).show();}
  }).show();
 }
 private LinearLayout moreRow(String title,View trailing,Runnable action,MoreIcon icon){
  LinearLayout row=UiKit.settingRow(this,title,trailing,action);
  View badge=new View(this);
  badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  badge.setBackground(icon.drawable());
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(36),dp(36));
  params.setMarginEnd(dp(12));
  row.addView(badge,0,params);
  return row;
 }
 private void renderSettings() {
  LinearLayout user=UiKit.card(this);
  String nick=profileName();
  android.graphics.Bitmap bitmap=AccountAvatar.decode(getSharedPreferences("appearance",0).getString("customAvatar",""));
  View avatar;
  if(bitmap!=null){
   ImageView image=new ImageView(this);
   image.setImageBitmap(bitmap);
   image.setScaleType(ImageView.ScaleType.CENTER_CROP);
   avatar=image;
  }else{
   String initial=nick.isEmpty()?"人":nick.substring(0,nick.offsetByCodePoints(0,1));
   TextView placeholder=text(initial,26,accent);
   placeholder.setGravity(Gravity.CENTER);
   avatar=placeholder;
  }
  avatar.setBackground(bg(UiKit.tint(accent),48));
  avatar.setClipToOutline(true);
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(96),dp(96));
  ap.gravity=Gravity.CENTER;
  user.addView(avatar,ap);
  TextView username=text(nick.isEmpty()?"未设置昵称":nick,22,ink);
  username.setGravity(Gravity.CENTER);
  user.addView(username);
  username.setOnClickListener(v->editNickname());
  user.addView(moreRow("编辑个人资料",text("›",22,muted),this::showAvatarOptions,MoreIcon.PROFILE));
  avatar.setContentDescription("头像选项");
  avatar.setOnClickListener(v->showAvatarOptions());
  content.addView(user);

  LinearLayout options=addSection("设置");
  options.addView(moreRow("导航条 · 显示与位置",text("›",22,muted),this::configureDock,MoreIcon.DOCK));
  options.addView(moreRow("主题颜色 · "+(UiKit.customAccent(this)?"自定义":UiKit.ACCENT_NAMES[UiKit.accentIndex(this)]),
   text("●",18,UiKit.accentText()),this::chooseAccent,MoreIcon.PALETTE));
  options.addView(moreRow("课表管理",text("›",22,muted),this::manageTables,MoreIcon.TABLE));
  options.addView(moreRow("添加 / 修改课程",text("›",22,muted),()->editCourse(null),MoreIcon.EDIT));
  int minutes=Reminders.prefs(this).getInt("minutes",15);
  Switch reminder=new Switch(this);
  reminder.setContentDescription("课程提醒");
  reminder.setChecked(Reminders.prefs(this).getBoolean("enabled",false));
  int[][] states={{android.R.attr.state_checked},{}};
  reminder.setThumbTintList(new android.content.res.ColorStateList(states,new int[]{accent,muted}));
  reminder.setTrackTintList(new android.content.res.ColorStateList(states,new int[]{UiKit.tint(accent),UiKit.LINE}));
  reminder.setOnCheckedChangeListener((b,on)->Reminders.enable(this,on));
  LinearLayout reminderRow=moreRow("课程提醒 · 提前"+minutes+"分钟",reminder,reminder::toggle,MoreIcon.BELL);
  reminderRow.setOnLongClickListener(v->{editReminder(minutes);return true;});
  reminder.setOnLongClickListener(v->{editReminder(minutes);return true;});
  options.addView(reminderRow);
  options.addView(moreRow("通知栏上课状态",
   text(LiveCourseNotification.option(this,"enabled",false)?"已开启 ›":"已关闭 ›",13,muted),
   this::showLiveCourseSettings,MoreIcon.BELL));
  options.addView(moreRow("关于·更新",text("›",22,muted),this::showAbout,MoreIcon.ABOUT));
  options.addView(text("长按课程提醒可调整时间。",12,muted));
  if(!preview){
   content.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));
   addScheduleMetadata(content);
  }
 }
 private void showLiveCourseSettings(){
  LinearLayout body=column();
  LinearLayout previewCard=UiKit.card(this);
  previewCard.setBackground(bg(UiKit.tint(UiKit.BRAND),20));
  TextView previewTitle=text("示例 · 高等数学",17,ink);
  previewTitle.setTypeface(null,Typeface.BOLD);
  previewCard.addView(previewTitle);
  TextView previewDetails=text("",14,ink);
  previewCard.addView(previewDetails);
  body.addView(previewCard);
  Runnable previewUpdate=()->{
   java.util.List<String> fields=new ArrayList<>();
   fields.add("正在上课");
   if(LiveCourseNotification.option(this,"room",true))fields.add("教学楼 301");
   if(LiveCourseNotification.option(this,"time",true))fields.add("08:00—09:50");
   if(LiveCourseNotification.option(this,"teacher",false))fields.add("授课教师");
   String value=String.join(" · ",fields);
   if(LiveCourseNotification.option(this,"countdown",true))value+="\n距课程结束 · 24:10";
   if(LiveCourseNotification.option(this,"next",false))value+="\n下一门 · 10:10  大学英语";
   previewDetails.setText(value);
   previewTitle.setTextColor(LiveCourseNotification.option(this,"accent",true)?accent:ink);
  };
  addLiveOption(body,"启用通知栏上课状态","enabled",false,previewUpdate);
  addLiveOption(body,"课间继续显示","break",true,previewUpdate);
  addLiveOption(body,"显示教室","room",true,previewUpdate);
  addLiveOption(body,"显示上课时间","time",true,previewUpdate);
  addLiveOption(body,"显示教师","teacher",false,previewUpdate);
  addLiveOption(body,"显示倒计时","countdown",true,previewUpdate);
  addLiveOption(body,"展开后显示下一门课","next",false,previewUpdate);
  addLiveOption(body,"使用应用主题色","accent",true,previewUpdate);
  addLiveOption(body,"锁屏隐藏课程详情","private",true,previewUpdate);
  AlarmManager alarms=getSystemService(AlarmManager.class);
  if(Build.VERSION.SDK_INT>=31&&!alarms.canScheduleExactAlarms()){
   body.addView(text("后台切换可能延迟，可开启准时提醒权限。",13,muted));
   body.addView(button("开启准时提醒权限",()->{
    Intent intent=new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
     android.net.Uri.parse("package:"+getPackageName()));
    try{startActivity(intent);}catch(ActivityNotFoundException ignored){}
   }));
  }
  previewUpdate.run();
  ScrollView scroll=new ScrollView(this);scroll.addView(body);
  Dialog panel=UiKit.sheet(this,"通知栏上课状态",scroll);
  panel.setOnDismissListener(dialog->render());
  panel.show();
 }
 private void addLiveOption(LinearLayout body,String label,String key,boolean fallback,Runnable update){
  Switch toggle=new Switch(this);
  toggle.setContentDescription(label);
  toggle.setChecked(LiveCourseNotification.option(this,key,fallback));
  toggle.setOnCheckedChangeListener((button,on)->{
   LiveCourseNotification.prefs(this).edit().putBoolean(key,on).apply();
   update.run();
   if("enabled".equals(key)&&on){
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
     requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},23);return;
    }
    if(!getSystemService(NotificationManager.class).areNotificationsEnabled()){
     startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));
    }
   }
   Reminders.schedule(this);
  });
  body.addView(UiKit.settingRow(this,label,toggle,toggle::toggle));
 }
 private int dockMask(){
  int mask=getSharedPreferences("appearance",0).getInt("dockTabs",15)&15;
  return mask==0?15:mask;
 }
 private int[] dockOrder(){
  String order=getSharedPreferences("appearance",0).getString("dockOrder","0123");
  if(order.length()!=4||!order.contains("0")||!order.contains("1")||!order.contains("2")||!order.contains("3"))
   return new int[]{0,1,2,3};
  return order.chars().map(ch->ch-'0').toArray();
 }
 private int[] visibleTabs(){
  int mask=dockMask();
  return java.util.Arrays.stream(dockOrder()).filter(tab->(mask&(1<<tab))!=0).toArray();
 }
 private void configureDockTabs(){
  android.content.SharedPreferences appearance=getSharedPreferences("appearance",0);
  LinearLayout body=column();
  String[] names={"今日","课表","备忘","更多"};
  CheckBox[] boxes=new CheckBox[4];
  LinearLayout[] rows=new LinearLayout[4];
  Button[] up=new Button[4],down=new Button[4];
  java.util.List<Integer> order=new java.util.ArrayList<>();
  for(int tab:dockOrder())order.add(tab);
  Runnable layout=()->{
   body.removeAllViews();
   for(int i=0;i<order.size();i++){
    int tab=order.get(i);
    up[tab].setEnabled(i>0);down[tab].setEnabled(i<order.size()-1);
    body.addView(rows[tab]);
   }
  };
  for(int tab:order){
   LinearLayout row=new LinearLayout(this);rows[tab]=row;
   CheckBox box=new CheckBox(this);box.setText(names[tab]);box.setTextColor(ink);
   box.setChecked((dockMask()&(1<<tab))!=0);boxes[tab]=box;
   row.addView(box,new LinearLayout.LayoutParams(0,dp(48),1));
   up[tab]=button("↑",()->{java.util.Collections.swap(order,order.indexOf(tab),order.indexOf(tab)-1);layout.run();});
   down[tab]=button("↓",()->{java.util.Collections.swap(order,order.indexOf(tab),order.indexOf(tab)+1);layout.run();});
   up[tab].setContentDescription("上移"+names[tab]);down[tab].setContentDescription("下移"+names[tab]);
   row.addView(up[tab],new LinearLayout.LayoutParams(dp(48),dp(48)));
   row.addView(down[tab],new LinearLayout.LayoutParams(dp(48),dp(48)));
  }
  layout.run();
  UiKit.prompt(this,"导航条项目与顺序",body,"保存",()->{
   int mask=0;StringBuilder savedOrder=new StringBuilder();
   for(int tab:order){if(boxes[tab].isChecked())mask|=1<<tab;savedOrder.append(tab);}
   if(mask==0)throw new IllegalArgumentException("至少保留一项，或选择隐藏整条导航条");
   appearance.edit().putInt("dockTabs",mask).putString("dockOrder",savedOrder.toString()).apply();render();
  },null,null).show();
 }
 private void configureDock(){
  LinearLayout body=column();
  Dialog panel=UiKit.sheet(this,"导航条",body);
  android.content.SharedPreferences appearance=getSharedPreferences("appearance",0);
  boolean hidden=appearance.getBoolean("dockHidden",false);
  body.addView(UiKit.action(this,hidden?"显示导航条":"隐藏导航条",false,()->{
   appearance.edit().putBoolean("dockHidden",!hidden).apply();panel.dismiss();render();
  }));
  body.addView(UiKit.action(this,"调整项目与顺序",false,()->{panel.dismiss();configureDockTabs();}));
  String[] names={"底部居中","顶部居中","底部靠左","底部靠右"};
  for(int i=0;i<names.length;i++){
   final int position=i;
   body.addView(UiKit.action(this,names[i],i==appearance.getInt("dockPosition",0),()->{
    appearance.edit().putInt("dockPosition",position).putBoolean("dockHidden",false).apply();
    panel.dismiss();render();
   }));
  }
  body.addView(text("隐藏后可从右上角菜单切换页面或恢复导航条。",13,muted));
  panel.show();
 }
 private void chooseAccent(){
  LinearLayout body=column();
  Dialog panel=UiKit.sheet(this,"主题颜色",body);
  for(int start=0;start<UiKit.ACCENTS.length;start+=2){
   LinearLayout row=new LinearLayout(this);
   for(int i=start;i<Math.min(start+2,UiKit.ACCENTS.length);i++){
    final int index=i;
    TextView dot=text("●",18,UiKit.ACCENTS[i]);
    GradientDrawable border=bg(UiKit.SURFACE,12);
    border.setStroke(dp(1),UiKit.LINE);dot.setBackground(border);
    String name=UiKit.ACCENT_NAMES[i]+(!UiKit.customAccent(this)&&i==UiKit.accentIndex(this)?" ✓":"");
    row.addView(UiKit.settingRow(this,name,dot,()->{
     getSharedPreferences("appearance",0).edit().putInt("accent",index).remove("customAccent").apply();
     panel.dismiss();render();
    }),new LinearLayout.LayoutParams(0,-2,1));
   }
   body.addView(row);
  }
  body.addView(UiKit.settingRow(this,"自定义 RGB"+(UiKit.customAccent(this)?" ✓":""),
   text("●",18,UiKit.accentText()),()->{
    panel.dismiss();
    RgbPicker.show(this,UiKit.accentColor(this),color->{
     getSharedPreferences("appearance",0).edit().putInt("customAccent",color).apply();
     Reminders.schedule(this);render();
    });
   }));
  panel.show();
 }
 private LinearLayout addSection(String title) {
  LinearLayout group=UiKit.card(this);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.topMargin=dp(12);
  content.addView(group,lp);
  TextView heading=text(title,16,muted);
  heading.setTypeface(null,Typeface.BOLD);
  group.addView(heading);
  return group;
 }
 private void openAnnouncements() {
  openBrowser(School.current(this)==School.AUST?"https://jwc.aust.edu.cn/":
   "https://e.wfu.edu.cn/h5/#/pages/information-delivery/information-delivery");
 }
 private void openBrowser(String url) {
  try {
   startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));
  } catch(ActivityNotFoundException e){Toast.makeText(this,"找不到浏览器",0).show();}
 }
 private String profileName(){
  School school=School.current(this);
  return getSharedPreferences("appearance",0).getString("customNickname."+school.name(),"");
 }
 private void editNickname(){
  School school=School.current(this);
  EditText input=UiKit.input(this,"昵称",profileName(),1);
  input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
  UiKit.prompt(this,"修改昵称",input,"保存",()->{
   String name=input.getText().toString().trim();
   android.content.SharedPreferences.Editor editor=getSharedPreferences("appearance",0).edit();
   String key="customNickname."+school.name();
   if(name.isEmpty())editor.remove(key);else editor.putString(key,name);
   editor.apply();render();
  },null,null).show();
 }
 private void showAvatarOptions() {
  LinearLayout body=column();
  Dialog dialog=UiKit.sheet(this,"个人资料",body);
  body.addView(button("修改昵称",()->{dialog.dismiss();editNickname();}));
  body.addView(button("自定义头像",()->{
   dialog.dismiss();
   startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
    .addCategory(Intent.CATEGORY_OPENABLE),33);
  }));
  if(getSharedPreferences("appearance",0).contains("customAvatar"))
   body.addView(button("移除自定义头像",()->{
    getSharedPreferences("appearance",0).edit().remove("customAvatar").apply();
    dialog.dismiss();render();
   }));
  dialog.show();
 }
 private void saveCustomAvatar(android.net.Uri uri) {
  try {
   android.graphics.Bitmap bitmap=android.graphics.ImageDecoder.decodeBitmap(
    android.graphics.ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,source)->{
     int width=info.getSize().getWidth(),height=info.getSize().getHeight();
     double scale=Math.min(1.0,512.0/Math.max(width,height));
     decoder.setTargetSize(Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)));
     decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
    });
   java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
   boolean compressed=bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);
   bitmap.recycle();
   if(!compressed)throw new java.io.IOException();
   String encoded=android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP);
   if(AccountAvatar.decode(encoded)==null)throw new java.io.IOException();
   getSharedPreferences("appearance",0).edit().putString("customAvatar",encoded).apply();
   render();
  }catch(Exception e){Toast.makeText(this,"无法使用这张图片，请换一张",0).show();}
 }
 private void editReminder(int minutes) {
  EditText input=UiKit.input(this,"提前分钟数",String.valueOf(minutes),2);
  UiKit.prompt(this,"提前提醒分钟数（0—180）",input,"保存",()->{
   int n;
   try{n=Integer.parseInt(input.getText().toString());}
   catch(NumberFormatException e){throw new IllegalArgumentException("请输入 0—180 分钟");}
   if(n<0||n>180)throw new IllegalArgumentException("请输入 0—180 分钟");
   Reminders.prefs(this).edit().putInt("minutes",n).apply();
   Reminders.schedule(this);render();
  },null,null).show();
 }

 private void showAbout() {
  LinearLayout body = column();
  body.addView(text("当前版本 " + installedVersion(),14,muted));
  body.addView(text("开发者：TuYun",16,ink));
  addCopyRow(body,"QQ","1937747879");
  addCopyRow(body,"QQ群","976368889");
  body.addView(text("加入群聊以获取最新信息",16,ink));
  body.addView(text("操作提示",16,ink));
  body.addView(text("长按课表网格可查看学期和本地保存时间。",14,muted));
  body.addView(text("长按课程提醒可修改提前提醒的分钟数。",14,muted));
  Dialog panel = UiKit.sheet(this, "关于·更新", body);
  body.addView(UiKit.action(this,"检查更新",true,()->{panel.dismiss();checkAppUpdate(true);}));
  panel.show();
 }

 private boolean updateResumed, updateRunning, updateRequested;
 private AppUpdates.Release pendingUpdate;

 private String installedVersion() {
  try {
   return getPackageManager().getPackageInfo(getPackageName(),0).versionName;
  } catch (android.content.pm.PackageManager.NameNotFoundException e) {
   throw new IllegalStateException(e);
  }
 }

 private void checkAppUpdate(boolean manual) {
  if (pendingUpdate != null) {
   showPendingUpdate();
   return;
  }
  if (updateRunning) {
   if (manual) {
    updateRequested = true;
    updateMessage("正在检查更新…");
   }
   return;
  }
  SharedPreferences preferences = getSharedPreferences("app_updates",MODE_PRIVATE);
  long now = System.currentTimeMillis();
  long previous = preferences.getLong("last_check",0);
  if (!manual && now >= previous && now - previous < 86_400_000L) return;
  String repository = AppUpdates.repository(this);
  if (repository.isEmpty()) {
   if (manual) updateMessage("更新源尚未配置");
   return;
  }
  preferences.edit().putLong("last_check",now).apply();
  updateRunning = true;
  updateRequested = manual;
  if (manual) updateMessage("正在检查更新…");
  String installed = installedVersion();
  new Thread(()->{
   try {
    AppUpdates.Release release = AppUpdates.latest(repository,installed);
    runOnUiThread(()->finishUpdateCheck(release,false));
   } catch (Exception e) {
    runOnUiThread(()->finishUpdateCheck(null,true));
   }
  },"app-update-check").start();
 }

 private void finishUpdateCheck(AppUpdates.Release release, boolean failed) {
  updateRunning = false;
  if (isFinishing() || isDestroyed()) return;
  if (release != null) {
   pendingUpdate = release;
   showPendingUpdate();
  } else if (updateRequested && updateResumed) {
   updateMessage(failed ? "暂时无法检查更新，请稍后重试" : "已是最新版本");
  }
 }

 private void showPendingUpdate() {
  if (!updateResumed || pendingUpdate == null) return;
  AppUpdates.Release release = pendingUpdate;
  pendingUpdate = null;
  LinearLayout body = column();
  body.addView(text("当前版本 " + installedVersion() + "  →  " + release.tag(),14,muted));
  if (!release.notes().trim().isEmpty()) {
   ScrollView scroll = new ScrollView(this);
   TextView notes = text(release.notes(),15,ink);
   notes.setMaxLines(7);
   notes.setMaxHeight(dp(180));
   scroll.addView(notes);
   body.addView(scroll);
  }
  Dialog panel = UiKit.sheet(this,"发现新版本",body);
  body.addView(UiKit.action(this,"前往下载",true,()->{
   try {
    startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(release.downloadUrl())));
    panel.dismiss();
   } catch (ActivityNotFoundException e) {
    updateMessage("未找到可以打开下载链接的浏览器");
   }
  }));
  body.addView(UiKit.action(this,"稍后",false,panel::dismiss));
  panel.show();
 }

 private void updateMessage(String message) {
  Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
 }

 private void addCopyRow(LinearLayout body,String label,String number) {
  body.addView(UiKit.settingRow(this,label+"："+number,text("复制",14,accent),()->{
   ClipboardManager clipboard=getSystemService(ClipboardManager.class);
   clipboard.setPrimaryClip(ClipData.newPlainText(label,number));
   if(Build.VERSION.SDK_INT<33)Toast.makeText(this,"已复制"+label,0).show();
  }));
 }
 private void manageTables(){LinearLayout body=column();LinearLayout savedRows=column();final Dialog[] panel=new Dialog[1];body.addView(button("保存当前课表",()->{
  EditText input=UiKit.input(this,"课表名称","",1);
  UiKit.prompt(this,"保存到课表库",input,"保存",()->{
   String title=input.getText().toString().trim();
   try{LocalTables.saveNamed(this,title.isEmpty()?"我的课表":title,repo.raw);}
   catch(Exception e){throw new IllegalArgumentException("保存失败");}
   populateSavedTables(savedRows,panel[0]);
  },null,null).show();
 }));body.addView(button("新建空白课表",()->{try{if(!preview)LocalTables.saveNamed(this,"新建前备份 "+System.currentTimeMillis(),repo.raw);org.json.JSONObject empty=LocalTables.blank(School.current(this));if(repo.raw.has("periods"))empty.put("periods",repo.raw.getJSONArray("periods"));ScheduleRepository.parse(empty).save(this);loadRepo();findCurrentWeek();panel[0].dismiss();render();}catch(Exception e){Toast.makeText(this,"新建失败",0).show();}}));body.addView(button("导出当前课表",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"课间课表.json");startActivityForResult(i,31);}));body.addView(button("导入课表文件",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),32)));body.addView(savedRows);ScrollView scroll=new ScrollView(this);scroll.addView(body);panel[0]=UiKit.sheet(this,"课表管理",scroll);populateSavedTables(savedRows,panel[0]);panel[0].show();}
 private void populateSavedTables(LinearLayout rows,Dialog panel){
  rows.removeAllViews();
  for(Map.Entry<String,?> entry:getSharedPreferences("tables",0).getAll().entrySet()){
   try{
    org.json.JSONObject saved=new org.json.JSONObject(String.valueOf(entry.getValue()));
    String name=saved.getString("name")+"("+School.from(saved.getJSONObject("data")).name+")";
    LinearLayout actions=new LinearLayout(this);
    actions.addView(button("切换",()->{
     try{
      ScheduleRepository incoming=ScheduleRepository.parse(saved.getJSONObject("data"));
      if(!preview)LocalTables.saveNamed(this,"切换前备份 "+System.currentTimeMillis(),repo.raw);
      incoming.save(this);incoming.school.select(this);loadRepo();findCurrentWeek();panel.dismiss();render();
     }catch(Exception e){Toast.makeText(this,"课表无法读取",0).show();}
    }));
    actions.addView(button("删除",()->UiKit.prompt(this,"删除已保存的课表？",
     text("删除「"+name+"」的保存副本。当前使用的课表会保留。",15,muted),"删除",()->{
      LocalTables.deleteNamed(this,entry.getKey());
      populateSavedTables(rows,panel);
     },null,null).show()));
    rows.addView(UiKit.settingRow(this,name,actions,()->actions.getChildAt(0).performClick()));
   }catch(Exception ignored){}
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(result!=RESULT_OK||intent==null||intent.getData()==null)return;if(request==33){saveCustomAvatar(intent.getData());return;}try{if(request==31){org.json.JSONObject copy=new org.json.JSONObject(repo.raw.toString());copy.remove("profile");try(java.io.OutputStream out=getContentResolver().openOutputStream(intent.getData())){if(out==null)throw new Exception();out.write(copy.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}Toast.makeText(this,"已导出",0).show();}else if(request==32){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();try(java.io.InputStream in=getContentResolver().openInputStream(intent.getData())){if(in==null)throw new Exception();byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){out.write(bytes,0,n);if(out.size()>5_000_000)throw new Exception();}}ScheduleRepository incoming=ScheduleRepository.parse(new org.json.JSONObject(out.toString("UTF-8")));if(!preview)LocalTables.saveNamed(this,"文件导入前备份 "+System.currentTimeMillis(),repo.raw);incoming.save(this);incoming.school.select(this);loadRepo();findCurrentWeek();Reminders.schedule(this);render();}}catch(Exception e){Toast.makeText(this,"文件无效或保存失败，原课表已保留",1).show();}}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==23)Reminders.schedule(this);if(request==22){boolean granted=results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED;if(granted)Reminders.enable(this,true);else{Reminders.prefs(this).edit().putBoolean("enabled",false).apply();Reminders.schedule(this);Toast.makeText(this,"未开启通知权限，课程提醒保持关闭",1).show();}render();}}
}
