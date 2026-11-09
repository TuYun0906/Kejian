package cn.kejian;
import android.content.*;import android.view.*;import android.widget.*;import android.animation.*;import java.util.*;
public final class WeekPager extends FrameLayout {
 private final ScheduleRepository repo;private int index;private final int firstIndex,lastIndex;private final java.util.function.IntConsumer changed;private final List<View> pages=new ArrayList<>();private final List<Integer> offsets=new ArrayList<>();private float downX,downY,offset,dragOrigin;private boolean dragging,disposed;private ValueAnimator motion;private VelocityTracker velocity;
 public WeekPager(Context c,ScheduleRepository repo,int index,TimetableView.Selected selected,Runnable details,java.util.function.IntConsumer changed){super(c);this.repo=repo;this.index=index;java.util.List<Integer> allowed=LocalTables.visible(repo);firstIndex=allowed.get(0);lastIndex=allowed.get(allowed.size()-1);this.changed=changed;setClipChildren(true);this.selected=selected;this.details=details;syncPages();}
 private final TimetableView.Selected selected;
 private final Runnable details;
 private void syncPages(){
  for(int i=offsets.size()-1;i>=0;i--){
   int at=offsets.get(i);
   if(Math.abs(at-index)>2){removeView(pages.remove(i));offsets.remove(i);}
  }
  for(int at=Math.max(firstIndex,index-2);at<=Math.min(lastIndex,index+2);at++){
   if(offsets.contains(at))continue;
   ScheduleRepository.Week week=repo.weeks.get(at);
   View page;
   if(week.known())page=new TimetableView(getContext(),repo,week,selected);
   else{TextView t=UiKit.label(getContext(),"本周课表尚未获取",16,UiKit.MUTED);t.setGravity(Gravity.CENTER);page=t;}
   page.setOnLongClickListener(v->{details.run();return true;});
   addView(page,new LayoutParams(-1,-1));pages.add(page);offsets.add(at);
  }
 }
 private void position(float dx){offset=dx;for(int i=0;i<pages.size();i++)pages.get(i).setTranslationX((offsets.get(i)-index)*getWidth()+dx);}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){position(0);}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){boolean interrupted=motion!=null&&motion.isRunning();if(interrupted){
  motion.cancel();
  if(getWidth()>0&&Math.abs(offset)>getWidth()*.5f){
   int next=offset<0?1:-1;
   if(index+next>=firstIndex&&index+next<=lastIndex){
    index+=next;position(offset+next*getWidth());changed.accept(index);
   }
  }
 }dragOrigin=offset;downX=e.getRawX();downY=e.getRawY();dragging=false;if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();velocity.addMovement(e);if(interrupted)return true;}else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){if(Math.abs(e.getRawX()-downX)>UiKit.dp(getContext(),10)&&Math.abs(e.getRawX()-downX)>Math.abs(e.getRawY()-downY)){dragging=true;return true;}}return false;}
 @Override public boolean onTouchEvent(MotionEvent e){if(motion!=null&&motion.isRunning())return true;if(velocity!=null)velocity.addMovement(e);if(e.getActionMasked()==MotionEvent.ACTION_MOVE){float dx=dragOrigin+e.getRawX()-downX;dx=Math.max(-getWidth(),Math.min(getWidth(),dx));boolean edge=(index==firstIndex&&dx>0)||(index==lastIndex&&dx<0);position(edge?dx*.22f:dx);return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){float speed=0;if(velocity!=null){velocity.computeCurrentVelocity(1000);speed=velocity.getXVelocity();velocity.recycle();velocity=null;}int direction=0;if(e.getActionMasked()==MotionEvent.ACTION_UP&&(Math.abs(offset)>getWidth()*.18f||Math.abs(speed)>UiKit.dp(getContext(),650)))direction=offset<0?1:-1;settle(direction);return true;}return true;}
 public void step(int delta){if(motion==null||!motion.isRunning())settle(delta);}
 private void settle(int delta){if(index+delta<firstIndex||index+delta>lastIndex)delta=0;final int next=delta;final float start=offset,end=-delta*getWidth();motion=ValueAnimator.ofFloat(0,1);motion.setDuration(300);motion.setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f));motion.addUpdateListener(a->position(start+(end-start)*a.getAnimatedFraction()));motion.addListener(new AnimatorListenerAdapter(){boolean cancelled=false;@Override public void onAnimationCancel(Animator a){cancelled=true;}@Override public void onAnimationEnd(Animator a){if(cancelled||disposed)return;index+=next;position(0);if(next!=0){changed.accept(index);post(()->{if(!disposed){syncPages();position(offset);}});}}});motion.start();}
 @Override protected void onDetachedFromWindow(){disposed=true;if(motion!=null)motion.cancel();if(velocity!=null){velocity.recycle();velocity=null;}super.onDetachedFromWindow();}
}
