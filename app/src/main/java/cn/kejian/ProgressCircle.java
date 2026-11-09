package cn.kejian;
import android.content.Context;
import android.graphics.*;
import android.widget.ProgressBar;
public final class ProgressCircle extends ProgressBar {
 private String countdown;
 public void setCountdown(String text){countdown=text;setContentDescription(text==null?null:"距上课 "+text);invalidate();}
 private int accent=UiKit.BRAND;public void setAccent(int value){accent=value;invalidate();}private final Paint p=new Paint(3);
 public ProgressCircle(Context c){super(c,null,android.R.attr.progressBarStyleHorizontal);setIndeterminate(false);setMax(100);}
 @Override protected synchronized void onDraw(Canvas c){float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-10;p.setStyle(Paint.Style.FILL);p.setColor(UiKit.tint(accent));c.drawCircle(cx,cy,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(UiKit.LINE);c.drawCircle(cx,cy,r,p);p.setColor(accent);p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(cx-r,cy-r,cx+r,cy+r,-90,3.6f*getProgress(),false,p);p.setStyle(Paint.Style.FILL);p.setColor(UiKit.INK);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(r*.43f);p.setTextAlign(Paint.Align.CENTER);if(countdown==null){c.drawText(getProgress()+"%",cx,cy-(p.ascent()+p.descent())/2,p);}
 else {
  p.setTextSize(r*.22f);
  c.drawText("距上课",cx,cy-r*.12f,p);
  p.setTextSize(r*.31f);
  float width=p.measureText(countdown);
  if(width>r*1.65f)p.setTextSize(p.getTextSize()*r*1.65f/width);
  c.drawText(countdown,cx,cy+r*.33f,p);
 }}
}
