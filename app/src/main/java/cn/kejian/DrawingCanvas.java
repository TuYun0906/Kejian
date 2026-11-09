package cn.kejian;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import org.json.*;
import java.util.ArrayList;
import java.util.List;

final class DrawingCanvas extends View {
 private static final class Stroke {
  final Path path=new Path();
  final JSONArray points=new JSONArray();
  final int color;
  final boolean erase,clear;
  Stroke(int color,boolean erase,boolean clear){this.color=color;this.erase=erase;this.clear=clear;}
  void add(float x,float y){
   if(points.length()==0){path.moveTo(x,y);path.lineTo(x+.0001f,y);}else path.lineTo(x,y);
   try{points.put((double)x).put((double)y);}catch(JSONException e){throw new IllegalArgumentException(e);}
  }
 }
 private final List<Stroke> strokes=new ArrayList<>();
 private final Bitmap base;
 private final String baseName;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final PorterDuffXfermode eraser=new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
 private Stroke current;
 int color=UiKit.INK;
 boolean erase;

 DrawingCanvas(Context context,Bitmap base,String baseName,JSONArray history)throws JSONException{
  super(context);this.base=base;this.baseName=baseName;
  setBackgroundColor(UiKit.SURFACE);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
  for(int i=0;i<history.length();i++){
   JSONObject item=history.getJSONObject(i);
   Stroke stroke=new Stroke(item.getInt("color"),item.optBoolean("erase"),item.optBoolean("clear"));
   JSONArray points=item.getJSONArray("points");
   for(int j=0;j<points.length();j+=2)stroke.add((float)points.getDouble(j),(float)points.getDouble(j+1));
   strokes.add(stroke);
  }
 }
 void undo(){if(!strokes.isEmpty()){strokes.remove(strokes.size()-1);current=null;invalidate();}}
 void clear(){current=null;strokes.add(new Stroke(0,false,true));invalidate();}
 JSONObject history()throws JSONException{
  JSONArray entries=new JSONArray();
  for(Stroke stroke:strokes)entries.put(new JSONObject().put("color",stroke.color).put("erase",stroke.erase)
   .put("clear",stroke.clear).put("points",stroke.points));
  return new JSONObject().put("base",baseName).put("strokes",entries);
 }
 Bitmap bitmap(){
  Bitmap bitmap=Bitmap.createBitmap(Math.max(1,getWidth()),Math.max(1,getHeight()),Bitmap.Config.ARGB_8888);
  Canvas target=new Canvas(bitmap);target.drawColor(UiKit.SURFACE);drawInk(target);return bitmap;
 }
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);drawInk(canvas);}
 private void drawInk(Canvas canvas){
  int width=getWidth(),height=getHeight();
  if(width<=0||height<=0)return;
  int layer=canvas.saveLayer(0,0,width,height,null);
  if(base!=null)canvas.drawBitmap(base,null,new RectF(0,0,width,height),null);
  canvas.scale(width,height);
  for(Stroke stroke:strokes){
   if(stroke.clear){canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);continue;}
   paint.setColor(stroke.color);
   paint.setStrokeWidth(stroke.erase?.045f:.012f);
   paint.setXfermode(stroke.erase?eraser:null);
   canvas.drawPath(stroke.path,paint);
  }
  paint.setXfermode(null);
  canvas.restoreToCount(layer);
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(getWidth()==0||getHeight()==0)return false;
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    getParent().requestDisallowInterceptTouchEvent(true);
    current=new Stroke(color,erase,false);strokes.add(current);
    break;
   case MotionEvent.ACTION_CANCEL:
    if(current!=null)undo();
    return true;
  }
  if(current!=null){
   if(e.getActionMasked()==MotionEvent.ACTION_MOVE)
    for(int i=0;i<e.getHistorySize();i++)current.add(e.getHistoricalX(i)/getWidth(),e.getHistoricalY(i)/getHeight());
   current.add(e.getX()/getWidth(),e.getY()/getHeight());
   if(e.getActionMasked()==MotionEvent.ACTION_UP)current=null;
  }
  invalidate();return true;
 }
}
