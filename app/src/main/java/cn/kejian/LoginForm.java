package cn.kejian;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.function.BooleanSupplier;

final class LoginForm {
 final LinearLayout root,card;
 final EditText account,password,code;
 final ImageView image;
 final TextView status;
 final Button submit,refresh;
 LoginForm(Activity activity,School school,BooleanSupplier busy,Runnable login,Runnable captcha){
  UiKit.apply(activity);
  activity.getWindow().setStatusBarColor(UiKit.BG);
  activity.getWindow().setNavigationBarColor(UiKit.BG);
  activity.getWindow().getDecorView().setSystemUiVisibility(UiKit.dark?0:
   View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  root=UiKit.col(activity);root.setBackgroundColor(UiKit.BG);
  root.setOnApplyWindowInsetsListener((v,in)->{
   int pad=UiKit.dp(activity,20);
   v.setPadding(in.getSystemWindowInsetLeft()+pad,in.getSystemWindowInsetTop()+pad,
    in.getSystemWindowInsetRight()+pad,in.getSystemWindowInsetBottom()+pad);return in;
  });
  root.addView(UiKit.action(activity,"‹ 返回",false,activity::finish),new LinearLayout.LayoutParams(-2,-2));
  TextView heading=UiKit.label(activity,"连接你的课表",30,UiKit.INK);
  heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,UiKit.dp(activity,8),0,UiKit.dp(activity,8));
  root.addView(heading);
  root.addView(UiKit.label(activity,"当前学期 · 本地保存 · 离线可用",13,UiKit.MUTED));
  root.addView(School.loginSelector(activity,school,busy));
  status=UiKit.label(activity,"正在连接学校教务系统…",14,UiKit.MUTED);
  status.setPadding(0,UiKit.dp(activity,16),0,UiKit.dp(activity,16));root.addView(status);
  card=UiKit.card(activity);root.addView(card);
  account=UiKit.input(activity,"学号 / 手机号","",1);account.setSaveEnabled(false);card.addView(account);
  password=UiKit.input(activity,"密码","",129);password.setSaveEnabled(false);card.addView(password);
  image=new ImageView(activity);image.setVisibility(View.GONE);image.setContentDescription("图形验证码，点击刷新");
  image.setOnClickListener(v->captcha.run());card.addView(image,new LinearLayout.LayoutParams(-1,UiKit.dp(activity,64)));
  code=UiKit.input(activity,"图形验证码","",1);code.setVisibility(View.GONE);code.setSaveEnabled(false);card.addView(code);
  refresh=UiKit.action(activity,"看不清？刷新图形验证码",false,captcha);refresh.setVisibility(View.GONE);card.addView(refresh);
  submit=UiKit.action(activity,"登录并导入课表",false,login);
  submit.setTextColor(UiKit.accentText());
  submit.setBackground(UiKit.shape(activity,UiKit.tint(UiKit.BRAND),16));
  card.addView(submit,UiKit.importButtonLayout(activity));
  TextView note=UiKit.label(activity,"密码及验证码仅用于官方认证，不保存在课表中。",12,UiKit.MUTED);
  note.setGravity(Gravity.CENTER);note.setPadding(0,UiKit.dp(activity,20),0,0);root.addView(note);
 }
 void show(Activity activity){
  ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.addView(root);activity.setContentView(scroll);
 }
}
