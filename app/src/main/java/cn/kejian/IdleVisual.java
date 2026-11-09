package cn.kejian;
import android.content.*;import android.graphics.*;import android.view.*;
public final class IdleVisual extends View {
 private final Paint p=new Paint(3);public IdleVisual(Context c){super(c);setContentDescription("休息一下");}
 @Override protected void onDraw(Canvas c){float size=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/100,size/100);p.setStyle(Paint.Style.FILL);p.setColor((UiKit.BRAND&0xffffff)|0x33000000);c.drawCircle(50,50,46,p);p.setColor(UiKit.accentText());p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);c.drawRoundRect(28,42,66,67,5,5,p);c.drawArc(57,43,79,61,-90,180,false,p);c.drawLine(26,74,72,74,p);for(int i=0;i<3;i++){Path steam=new Path();steam.moveTo(36+i*10,32);steam.cubicTo(29+i*10,26,43+i*10,23,37+i*10,17);c.drawPath(steam,p);}c.restore();}
}
