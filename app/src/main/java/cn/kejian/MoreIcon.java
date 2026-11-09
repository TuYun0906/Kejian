package cn.kejian;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

enum MoreIcon {
 PROFILE(0xff668bd0), SCHOOL(0xff557be0), NOTICE(0xffce784c),
 DOCK(0xff8b71cb), PALETTE(0xffc2669a), TABLE(0xff408d86),
 EDIT(0xff5384b1), BELL(0xffb78335), ABOUT(0xff6585a2);

 private final int color;

 MoreIcon(int color){this.color=color;}

 Drawable drawable(){
  return new Drawable(){
   private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

   @Override public void draw(Canvas canvas){
    Rect bounds=getBounds();
    float diameter=Math.min(bounds.width(),bounds.height());
    paint.setColor(UiKit.tint(color));
    paint.setStyle(Paint.Style.FILL);
    canvas.drawCircle(bounds.exactCenterX(),bounds.exactCenterY(),diameter/2,paint);
    canvas.save();
    float scale=diameter/40;
    canvas.translate(bounds.exactCenterX()-12*scale,bounds.exactCenterY()-12*scale);
    canvas.scale(scale,scale);
    paint.setColor(ink());
    paint.setStrokeWidth(1.7f);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStyle(Paint.Style.STROKE);
    drawSymbol(canvas,paint);
    canvas.restore();
   }

   @Override public void setAlpha(int alpha){}
   @Override public void setColorFilter(ColorFilter filter){}
   @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  };
 }

 private int ink(){
  float weight=UiKit.dark?.50f:.56f;
  int toward=UiKit.dark?255:0;
  return android.graphics.Color.rgb(
   Math.round(android.graphics.Color.red(color)*weight+toward*(1-weight)),
   Math.round(android.graphics.Color.green(color)*weight+toward*(1-weight)),
   Math.round(android.graphics.Color.blue(color)*weight+toward*(1-weight)));
 }

 private void drawSymbol(Canvas c,Paint p){
  switch(this){
   case PROFILE:
    c.drawCircle(12,8,3.5f,p);
    c.drawArc(5,14,19,24,180,180,false,p);
    break;
   case SCHOOL:
    c.drawLines(new float[]{3,9,12,4,12,4,21,9,21,9,12,14,12,14,3,9},p);
    c.drawLines(new float[]{6,12,6,17,6,17,12,20,12,20,18,17,18,17,18,12},p);
    break;
   case NOTICE:
    c.drawRoundRect(5,3,19,21,2,2,p);
    c.drawLine(8,8,16,8,p);
    c.drawLine(8,12,16,12,p);
    c.drawLine(8,16,13,16,p);
    break;
   case DOCK:
    c.drawRoundRect(2,7,22,17,5,5,p);
    p.setStyle(Paint.Style.FILL);
    for(int x=7;x<=17;x+=5)c.drawCircle(x,12,1.2f,p);
    break;
   case PALETTE:
    c.drawCircle(12,12,9,p);
    p.setStyle(Paint.Style.FILL);
    c.drawCircle(8,10,1.4f,p);
    c.drawCircle(12,7,1.4f,p);
    c.drawCircle(16,9,1.4f,p);
    c.drawCircle(15,15,1.4f,p);
    break;
   case TABLE:
    c.drawRoundRect(4,5,20,20,2,2,p);
    c.drawLine(4,10,20,10,p);
    c.drawLine(9,10,9,20,p);
    c.drawLine(15,10,15,20,p);
    c.drawLine(4,15,20,15,p);
    break;
   case EDIT:
    c.drawLines(new float[]{4,20,5,15,5,15,16,4,16,4,20,8,20,8,9,19,9,19,4,20,13,7,17,11},p);
    break;
   case BELL:
    c.drawArc(7,4,17,16,180,180,false,p);
    c.drawLines(new float[]{7,10,7,14,7,14,5,17,5,17,19,17,19,17,17,14,17,14,17,10},p);
    c.drawArc(10,18,14,21,0,180,false,p);
    break;
   case ABOUT:
    c.drawCircle(12,12,9,p);
    c.drawPoint(12,8,p);
    c.drawLine(12,11,12,16,p);
    break;
  }
 }
}
