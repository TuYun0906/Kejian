package cn.kejian;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
public final class UiKit {
 public static int BG=0xfff6f7fa,INK=0xff222936,MUTED=0xff838b99,BRAND=0xff397edb,LINE=0xffe9edf3;
 static final String[] ACCENT_NAMES={"蓝色","青色","绿色","青柠绿","黄色","橙色","粉色","洋红色","紫色","白色"};
 static final int[] ACCENTS={0xff3368ea,0xff07aefc,0xff04bb9d,0xffb6d805,0xffffc93e,0xfffd8263,
  0xfff673a9,0xffdc69de,0xffa25bff,0xffffffff};
 static int accentIndex(Context c){
  return Math.max(0,Math.min(ACCENTS.length-1,c.getSharedPreferences("appearance",0).getInt("accent",0)));
 }
 static boolean customAccent(Context c){return c.getSharedPreferences("appearance",0).contains("customAccent");}
 static int accentColor(Context c){
  return c.getSharedPreferences("appearance",0).getInt("customAccent",ACCENTS[accentIndex(c)]);
 }
 static int accentText(){
  if(dark){
   if(BRAND==ACCENTS[0])return 0xff89adff;
   if(Color.luminance(BRAND)<.26f)return Color.rgb((Color.red(BRAND)+255)/2,(Color.green(BRAND)+255)/2,(Color.blue(BRAND)+255)/2);
   return BRAND;
  }
  if(Color.luminance(BRAND)<.17f)return BRAND;
  return Color.rgb(Color.red(BRAND)*42/100,Color.green(BRAND)*42/100,Color.blue(BRAND)*42/100);
 }
 public static boolean dark=false;public static int SURFACE=0xffffffff;
 public static void apply(Context c){int mode=c.getSharedPreferences("appearance",0).getInt("mode",0);dark=mode==2||(mode==0&&(c.getResources().getConfiguration().uiMode&48)==32);BG=dark?0xff080a0e:0xfff6f7fa;SURFACE=dark?0xff171b23:0xffffffff;INK=dark?0xffedf1f7:0xff222936;MUTED=dark?0xff98a3b4:0xff838b99;LINE=dark?0xff2a303b:0xffe9edf3;BRAND=accentColor(c);}
 public static int tint(int color){if(!dark)return CoursePalette.tint(color);return 0xff000000|((((color>>16)&255)+23*3)/4<<16)|((((color>>8)&255)+27*3)/4<<8)|((color&255)+35*3)/4;}
 public static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
 public static GradientDrawable shape(Context c,int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 public static TextView label(Context c,String value,int size,int color){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");return t;}
 public static LinearLayout col(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(1);return l;}
 public static LinearLayout card(Context c){LinearLayout l=col(c);l.setPadding(dp(c,18),dp(c,18),dp(c,18),dp(c,18));l.setBackground(shape(c,SURFACE,24));return l;}
 public static Button action(Context c,String title,boolean selected,Runnable click){Button b=new Button(c);b.setAllCaps(false);b.setText(title);b.setTextSize(14);b.setTextColor(selected?accentText():INK);b.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(c,42));b.setMinimumHeight(dp(c,42));b.setPadding(dp(c,14),dp(c,6),dp(c,14),dp(c,6));b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf((BRAND&0xffffff)|0x12000000),shape(c,selected?tint(BRAND):Color.TRANSPARENT,16),null));b.setOnClickListener(v->click.run());return b;}
 static final String READING_TIMETABLE="正在读取当前学期并逐周核对课表…";
 static LinearLayout.LayoutParams importButtonLayout(Context c){
  LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,dp(c,52));
  layout.topMargin=dp(c,20);
  return layout;
 }
 public static void field(Context c,EditText f){f.setTextColor(INK);f.setTextSize(16);f.setHintTextColor(MUTED);android.graphics.drawable.GradientDrawable cursor=shape(c,BRAND,1);cursor.setSize(dp(c,2),dp(c,22));f.setTextCursorDrawable(cursor);f.setIncludeFontPadding(false);f.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);f.setPadding(dp(c,16),0,dp(c,16),0);f.setBackground(shape(c,BG,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(c,56));p.topMargin=dp(c,10);f.setLayoutParams(p);}
 public static Dialog sheet(Activity a,String title,View body){Dialog d=new Dialog(a);LinearLayout root=card(a);View handle=new View(a);handle.setBackground(shape(a,0xffd6dce5,3));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(a,34),dp(a,4));hp.gravity=Gravity.CENTER;hp.bottomMargin=dp(a,20);root.addView(handle,hp);TextView heading=label(a,title,22,INK);heading.setTypeface(null,Typeface.BOLD);root.addView(heading);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(a,18);root.addView(body,bp);d.setContentView(root);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.22f);}
  d.setOnShowListener(ignored->{
   Window window=d.getWindow();
   if(window==null)return;
   int screen=a.getResources().getDisplayMetrics().heightPixels;
   int available=(int)(screen*.82f);
   root.measure(View.MeasureSpec.makeMeasureSpec(a.getResources().getDisplayMetrics().widthPixels,View.MeasureSpec.EXACTLY),
     View.MeasureSpec.makeMeasureSpec(available,View.MeasureSpec.AT_MOST));
   window.setLayout(-1,Math.min(root.getMeasuredHeight(),available));
  });
  return d;}

 public static LinearLayout settingRow(Context c,String title,View trailing,Runnable action) {
  LinearLayout row=new LinearLayout(c);
  row.setGravity(Gravity.CENTER_VERTICAL);
  row.setMinimumHeight(dp(c,54));
  row.setPadding(dp(c,14),dp(c,6),dp(c,14),dp(c,6));
  row.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf((BRAND&0xffffff)|0x12000000),shape(c,SURFACE,16),null));
  row.addView(label(c,title,16,INK),new LinearLayout.LayoutParams(0,-2,1));
  row.addView(trailing,new LinearLayout.LayoutParams(-2,-2));
  row.setOnClickListener(v->action.run());
  return row;
 }
 public static Dialog prompt(Activity a,String title,View fields,String primary,
                             Runnable save,String secondary,Runnable alternate) {
  LinearLayout body=col(a);
  body.addView(fields,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout actions=new LinearLayout(a);
  actions.setGravity(Gravity.END);
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);
  ap.topMargin=dp(a,12);
  body.addView(actions,ap);
  ScrollView scroll=new ScrollView(a);
  scroll.addView(body);
  Dialog dialog=sheet(a,title,scroll);
  if(secondary!=null)actions.addView(action(a,secondary,false,()->{dialog.dismiss();alternate.run();}));
  actions.addView(action(a,primary==null?"关闭":"取消",false,dialog::dismiss));
  if(primary!=null)actions.addView(action(a,primary,true,()->{try{save.run();dialog.dismiss();}catch(IllegalArgumentException invalid){Toast.makeText(a,invalid.getMessage(),Toast.LENGTH_SHORT).show();}}));
  dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  return dialog;
 }
 public static EditText input(Context c,String hint,String value,int type) {
  EditText input=new EditText(c);
  input.setSingleLine(true);
  input.setInputType(type);
  input.setHint(hint);
  input.setText(value);
  field(c,input);
  return input;
 }
}
