package cn.kejian;
import android.content.*;import android.graphics.*;import android.view.*;import android.animation.*;
public final class DockView extends View {
 public interface Selected {void select(int tab,float from,long pressedAt);}
 private final Paint p=new Paint(3);
 private final ArgbEvaluator highlightColors=new ArgbEvaluator();
 private final Selected action;
 private final int selected;
 private final int[] tabs;
 private final int touchSlop;
 private float position,downX,origin,lift;
 private int pointer=MotionEvent.INVALID_POINTER_ID;
 private boolean dragging;
 private ValueAnimator animator,iconMotion;
 private int jumping=-1;
 private float jumpPhase=1;
 private boolean switching;
 private long pressedAt;

 private void jump(int slot,long start,boolean switching){
  if(iconMotion!=null)iconMotion.cancel();
  this.switching=switching;
  jumping=slot;
  long elapsed=android.os.SystemClock.uptimeMillis()-start;
  if(switching&&elapsed>165)elapsed=0;
  float initial=Math.min(1,elapsed/330f);
  jumpPhase=initial;
  iconMotion=ValueAnimator.ofFloat(initial,1);
  iconMotion.setInterpolator(new android.view.animation.LinearInterpolator());
  iconMotion.setDuration(Math.round(330*(1-initial)));
  iconMotion.addUpdateListener(frame->{jumpPhase=(Float)frame.getAnimatedValue();invalidate();});
  iconMotion.start();
 }


 public DockView(Context c,int tab,float initial,int[] tabs,long pressTime,Selected action){
  super(c);
  this.action=action;
  this.tabs=tabs;
  int index=-1;for(int i=0;i<tabs.length;i++)if(tabs[i]==tab)index=i;
  selected=index;
  touchSlop=ViewConfiguration.get(c).getScaledTouchSlop();
  position=initial<0?Math.max(0,selected):clamp(initial);
  setContentDescription("底部导航：今日、课表、备忘、更多，可拖动切换");
  settle();
  if(pressTime>0&&selected>=0)jump(selected,pressTime,true);
 }
 private float clamp(float value){return Math.max(0,Math.min(tabs.length-1,value));}
 private float inset(){return UiKit.dp(getContext(),6);}
 private float cellWidth(){return Math.max(1,(getWidth()-2*inset())/tabs.length);}
 private void settle(){
  if(animator!=null)animator.cancel();
  if(selected<0){lift=0;invalidate();return;}
  float start=position,initialLift=lift;
  if(start==selected&&initialLift==0)return;
  animator=ValueAnimator.ofFloat(0,1);
  animator.setDuration(360);
  animator.addUpdateListener(frame->{
   float fraction=(float)frame.getAnimatedValue();
   position=fraction>=1?selected:clamp(start+(selected-start)*SpringCurve.value(158,.68,fraction*.52f));
   lift=initialLift*(1-fraction);
   invalidate();
  });
  animator.start();
 }
 @Override protected void onDraw(Canvas c){
  float density=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),cell=cellWidth();
  p.setColor(UiKit.SURFACE);c.drawRoundRect(0,0,w,h,h/2,h/2,p);
  float x=inset()+position*cell;
  p.setColor(UiKit.tint(UiKit.BRAND));
  boolean showHighlight=selected>=0||pointer!=MotionEvent.INVALID_POINTER_ID;
  if(showHighlight)c.drawRoundRect(x,5*density,x+cell,h-5*density,h/2,h/2,p);
  for(int slot=0;slot<tabs.length;slot++){
   int i=tabs[slot];
   float cx=inset()+(slot+.5f)*cell;
   float phase=slot==jumping?jumpPhase:1;
   float altitude=(float)Math.pow(Math.sin(Math.PI*phase),1.25);
   float cy=(19-7*altitude)*density;
   float coverage=showHighlight?Math.max(0,1-Math.abs(position-slot)):0;
   float blend=coverage*coverage*(3-2*coverage);
   int color=(Integer)highlightColors.evaluate(blend,UiKit.MUTED,UiKit.accentText());
   float morph=slot!=selected?0:slot==jumping&&switching?Math.max(0,Math.min(1,(phase-.20f)/.49f)):1;
   if(morph<1)drawIconLayer(c,i,cx,cy,density,color,1-morph,false,.82f+.18f*(1-morph));
   if(morph>0)drawIconLayer(c,i,cx,cy,density,color,morph,true,.82f+.18f*morph);
   p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(11*density);
   c.drawText(i==0?"今日":i==1?"课表":i==2?"备忘":"更多",cx,45*density,p);
  }
 }
 private void drawIconLayer(Canvas c,int i,float cx,float cy,float density,int color,float opacity,boolean active,float scale){
  c.save();c.scale(scale,scale,cx,cy);
  p.setColor(color);p.setAlpha(Math.round(255*opacity));
  p.setStrokeWidth(1.8f*density);p.setStyle(Paint.Style.STROKE);
  if(active){
   p.setStyle(Paint.Style.FILL);
   if(i==0){
    Path sand=new Path();
    sand.moveTo(cx-6*density,cy-7*density);sand.lineTo(cx+6*density,cy-7*density);
    sand.lineTo(cx+2*density,cy);sand.lineTo(cx+6*density,cy+7*density);
    sand.lineTo(cx-6*density,cy+7*density);sand.lineTo(cx-2*density,cy);sand.close();c.drawPath(sand,p);
   }else if(i==3){
    for(int corner=0;corner<4;corner++)
     c.drawCircle(cx+(corner%2==0?-4:4)*density,cy+(corner<2?-4:4)*density,3*density,p);
   }else{
    float width=(i==1?8:6)*density;
    c.drawRoundRect(cx-width,cy-7*density,cx+width,cy+7*density,2*density,2*density,p);
    p.setColor(UiKit.tint(UiKit.BRAND));p.setAlpha(Math.round(255*opacity));
    p.setStrokeWidth(1.4f*density);
    if(i==1){
     c.drawLine(cx-width,cy-2*density,cx+width,cy-2*density,p);
     c.drawCircle(cx,cy+2*density,1.3f*density,p);
    }else{
     c.drawLine(cx-3*density,cy-2*density,cx+3*density,cy-2*density,p);
     c.drawLine(cx-3*density,cy+2*density,cx+2*density,cy+2*density,p);
    }
   }
  }else{if(i==0){c.drawLine(cx-5*density,cy-6*density,cx+5*density,cy-6*density,p);c.drawLine(cx-5*density,cy+6*density,cx+5*density,cy+6*density,p);c.drawLine(cx-5*density,cy-6*density,cx+5*density,cy+6*density,p);c.drawLine(cx+5*density,cy-6*density,cx-5*density,cy+6*density,p);}else if(i==3){p.setStyle(Paint.Style.FILL);for(int tile=0;tile<4;tile++){float tx=cx+((tile%2)*2-1)*4.4f*density,ty=cy+((tile/2)*2-1)*4.4f*density;float side=3.3f*density;c.save();if(tile==1)c.rotate(45,tx,ty);c.drawRoundRect(tx-side,ty-side,tx+side,ty+side,density,density,p);c.restore();}}else if(i==2){c.drawRoundRect(cx-6*density,cy-7*density,cx+6*density,cy+7*density,2*density,2*density,p);c.drawLine(cx-3*density,cy,cx+3*density,cy,p);c.drawLine(cx-3*density,cy+3*density,cx+3*density,cy+3*density,p);}else{c.drawRoundRect(cx-7*density,cy-6*density,cx+7*density,cy+7*density,2*density,2*density,p);c.drawLine(cx-7*density,cy-2*density,cx+7*density,cy-2*density,p);}}
  p.setAlpha(255);p.setStyle(Paint.Style.FILL);c.restore();
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  int kind=event.getActionMasked();
  if(kind==MotionEvent.ACTION_DOWN){
   if(animator!=null)animator.cancel();
   pointer=event.getPointerId(0);
   downX=event.getX();
   pressedAt=android.os.SystemClock.uptimeMillis();
   jump((int)clamp((float)Math.floor((downX-inset())/cellWidth())),pressedAt,false);
   origin=position;
   dragging=false;
   lift=1;
   invalidate();
   return true;
  }
  if(kind==MotionEvent.ACTION_CANCEL||
     (kind==MotionEvent.ACTION_POINTER_UP&&event.getPointerId(event.getActionIndex())==pointer)){
   pointer=MotionEvent.INVALID_POINTER_ID;
   dragging=false;
   settle();
   return true;
  }
  int index=event.findPointerIndex(pointer);
  if(index<0)return true;
  float x=event.getX(index);
  if(kind==MotionEvent.ACTION_MOVE||kind==MotionEvent.ACTION_UP){
   if(Math.abs(x-downX)>touchSlop)dragging=true;
   if(dragging){
    position=clamp(origin+(x-downX)/cellWidth());
    invalidate();
   }
  }
  if(kind==MotionEvent.ACTION_UP){
   int target=dragging?Math.round(position):(int)clamp((float)Math.floor((x-inset())/cellWidth()));
   pointer=MotionEvent.INVALID_POINTER_ID;
   if(!dragging)performClick();
   long press=dragging?android.os.SystemClock.uptimeMillis():pressedAt;
   dragging=false;
   if(target==selected)settle();else action.select(tabs[target],position,press);
  }
  return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDetachedFromWindow(){
  if(animator!=null)animator.cancel();
  if(iconMotion!=null)iconMotion.cancel();
  super.onDetachedFromWindow();
 }
}
