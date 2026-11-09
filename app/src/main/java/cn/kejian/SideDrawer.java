package cn.kejian;
import android.app.*;
import android.content.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;

final class SideDrawer extends Dialog {
 private final View panel;
 private boolean closing;
 private android.animation.ValueAnimator shade;
 private float dim;
 SideDrawer(Activity activity,View content){
  super(activity);
  panel=content;
  setContentView(content);
  Window window=getWindow();
  window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
  window.setGravity(Gravity.RIGHT|Gravity.TOP);
  window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
  window.setDimAmount(0);window.setWindowAnimations(0);
  setCanceledOnTouchOutside(true);
 }
 @Override public void show(){
  super.show();
  int width=Math.min(UiKit.dp(getContext(),360),(int)(getContext().getResources().getDisplayMetrics().widthPixels*.78f));
  getWindow().setLayout(width,-1);
  fade(.38f,280);
  panel.setTranslationX(width);
  panel.animate().translationX(0).setDuration(280).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
 }
 private void fade(float target,int duration){
  if(shade!=null)shade.cancel();
  shade=android.animation.ValueAnimator.ofFloat(dim,target);
  shade.setDuration(duration);
  shade.setInterpolator(new android.view.animation.DecelerateInterpolator());
  shade.addUpdateListener(frame->{
   dim=(float)frame.getAnimatedValue();
   if(getWindow()!=null)getWindow().setDimAmount(dim);
  });
  shade.start();
 }
 @Override public void dismiss(){
  if(closing)return;
  closing=true;
  fade(0,200);
  panel.animate().translationX(Math.max(panel.getWidth(),UiKit.dp(getContext(),320))).setDuration(200)
   .withEndAction(()->{try{super.dismiss();}finally{closing=false;}}).start();
 }
}
