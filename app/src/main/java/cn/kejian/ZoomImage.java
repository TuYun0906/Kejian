package cn.kejian;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

final class ZoomImage extends View {
 private final Bitmap bitmap;
 private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
 private final RectF bounds=new RectF();
 private final GestureDetector gesture;
 private final ScaleGestureDetector pinch;
 private float fit=1,zoom=1,x,y;

 ZoomImage(Context context,Bitmap bitmap){
  super(context);
  this.bitmap=bitmap;
  setContentDescription("图片预览，双指缩放，双击放大或复位");
  pinch=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
   @Override public boolean onScale(ScaleGestureDetector detector){
    scale(zoom*detector.getScaleFactor(),detector.getFocusX(),detector.getFocusY());
    return true;
   }
  });
  pinch.setQuickScaleEnabled(false);
  gesture=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(MotionEvent e){return true;}
   @Override public boolean onDoubleTap(MotionEvent e){
    scale(zoom>1.01f?1:2.5f,e.getX(),e.getY());
    return true;
   }
   @Override public boolean onScroll(MotionEvent start,MotionEvent current,float dx,float dy){
    if(!pinch.isInProgress()&&current.getPointerCount()==1){x-=dx;y-=dy;constrain();}
    return true;
   }
  });
 }
 @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
  fit=Math.min((float)w/bitmap.getWidth(),(float)h/bitmap.getHeight());
  zoom=1;constrain();
 }
 private void scale(float target,float focusX,float focusY){
  target=Math.max(1,Math.min(5,target));
  float ratio=target/zoom;
  x=focusX-(focusX-x)*ratio;
  y=focusY-(focusY-y)*ratio;
  zoom=target;constrain();
 }
 private void constrain(){
  float w=bitmap.getWidth()*fit*zoom,h=bitmap.getHeight()*fit*zoom;
  x=limit(x,getWidth(),w);y=limit(y,getHeight(),h);
  bounds.set(x,y,x+w,y+h);invalidate();
 }
 private float limit(float position,float viewport,float image){
  return image<=viewport?(viewport-image)/2:Math.max(viewport-image,Math.min(0,position));
 }
 @Override protected void onDraw(Canvas canvas){canvas.drawBitmap(bitmap,null,bounds,paint);}
 @Override public boolean onTouchEvent(MotionEvent event){
  pinch.onTouchEvent(event);gesture.onTouchEvent(event);return true;
 }
}
