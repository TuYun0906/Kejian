package cn.kejian;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.Locale;
import java.util.function.IntConsumer;

final class RgbPicker {
 static void show(Activity activity,int initial,IntConsumer apply){
  LinearLayout body=UiKit.col(activity);
  TextView sample=UiKit.label(activity,"",17,UiKit.INK);
  sample.setGravity(Gravity.CENTER);
  body.addView(sample,new LinearLayout.LayoutParams(-1,UiKit.dp(activity,56)));
  android.view.View spectrum=new android.view.View(activity);
  spectrum.setContentDescription("色谱，滑动选取色相");
  spectrum.setBackground(new android.graphics.drawable.GradientDrawable(
   android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
   new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED}));
  LinearLayout.LayoutParams strip=new LinearLayout.LayoutParams(-1,UiKit.dp(activity,36));
  strip.topMargin=UiKit.dp(activity,10);body.addView(spectrum,strip);
  SeekBar[] sliders=new SeekBar[3];
  int[] rgb={Color.red(initial),Color.green(initial),Color.blue(initial)};
  TextView[] labels=new TextView[3];
  String[] names={"红","绿","蓝"};
  Runnable update=()->{
   int color=Color.rgb(rgb[0],rgb[1],rgb[2]);
   sample.setText(String.format(Locale.ROOT,"#%06X",color&0xffffff));
   sample.setBackground(UiKit.shape(activity,color,14));
   sample.setTextColor(Color.luminance(color)>.179f?Color.BLACK:Color.WHITE);
   for(int i=0;i<3;i++)labels[i].setText(names[i]+" · "+rgb[i]);
  };
  for(int i=0;i<3;i++){
   int channel=i;
   labels[i]=UiKit.label(activity,"",14,UiKit.INK);body.addView(labels[i]);
   SeekBar slider=new SeekBar(activity);sliders[i]=slider;slider.setMax(255);slider.setProgress(rgb[i]);
   slider.setContentDescription(names[i]);body.addView(slider);
   slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
    public void onProgressChanged(SeekBar bar,int value,boolean user){rgb[channel]=value;update.run();}
    public void onStartTrackingTouch(SeekBar bar){}
    public void onStopTrackingTouch(SeekBar bar){}
   });
  }
  spectrum.setOnTouchListener((view,event)->{
   int action=event.getActionMasked();
   if(action==android.view.MotionEvent.ACTION_DOWN||action==android.view.MotionEvent.ACTION_MOVE||action==android.view.MotionEvent.ACTION_UP){
    view.getParent().requestDisallowInterceptTouchEvent(action!=android.view.MotionEvent.ACTION_UP);
    float hue=359.99f*Math.max(0,Math.min(1,event.getX()/Math.max(1,view.getWidth())));
    int color=Color.HSVToColor(new float[]{hue,1,1});
    sliders[0].setProgress(Color.red(color));sliders[1].setProgress(Color.green(color));sliders[2].setProgress(Color.blue(color));
   }
   return true;
  });
  update.run();
  UiKit.prompt(activity,"自定义颜色",body,"使用颜色",()->apply.accept(Color.rgb(rgb[0],rgb[1],rgb[2])),null,null).show();
 }
}
