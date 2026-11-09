package cn.kejian;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

final class NotesView extends LinearLayout {
 private final Activity activity;
 private final SharedPreferences prefs;
 private final LinearLayout cards;
 private final EditText search;
 private boolean grid;
 private boolean dragTargetSeen;
 private final Set<String> selected=new HashSet<>();
 private LinearLayout selectionBar;
 private final Map<String,View> cardViews=new HashMap<>();
 private String section="备忘",labelFilter="",typeFilter="全部";
 private int colorFilter=-1;
 private static final int[] COLORS={0,0xfffd8263,0xffffc93e,0xff04bb9d,0xff07aefc,0xffa25bff,0xfff673a9};
 NotesView(Activity activity){
  super(activity);
  setOnClickListener(v->clearSelection());
  this.activity=activity;
  setOrientation(VERTICAL);
  prefs=activity.getSharedPreferences("notes",0);
  grid=prefs.getBoolean("grid",true);
  LinearLayout toolbar=new LinearLayout(activity);
  toolbar.setGravity(Gravity.CENTER_VERTICAL);
  toolbar.addView(UiKit.action(activity,"☰",false,this::sections));
  search=UiKit.input(activity,"搜索备忘、清单或标签","",1);
  toolbar.addView(search,new LinearLayout.LayoutParams(0,UiKit.dp(activity,48),1));
  Button layout=UiKit.action(activity,grid?"☷":"▦",false,()->{});
  layout.setContentDescription("切换单列和双列");
  layout.setOnClickListener(v->{grid=!grid;prefs.edit().putBoolean("grid",grid).apply();layout.setText(grid?"☷":"▦");refresh();});
  toolbar.addView(layout);
  addView(toolbar);
  selectionBar=new LinearLayout(activity);selectionBar.setGravity(Gravity.CENTER_VERTICAL);addView(selectionBar);
  addView(UiKit.action(activity,"筛选 · "+typeFilter,false,this::filters));
  cards=UiKit.col(activity);
  addView(cards,new LayoutParams(-1,-2));
  search.addTextChangedListener(new TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){refresh();}
   public void afterTextChanged(Editable e){}
  });
  setFocusableInTouchMode(true);
  requestFocus();
  refresh();
 }
 private boolean createOpen;
 private android.animation.ValueAnimator createMotion;
 private Runnable closeCreate;

 boolean dismissCreateMenu(){
  if(!createOpen)return false;
  closeCreate.run();
  return true;
 }

 void attachCreateMenu(FrameLayout overlay){
  int size=UiKit.dp(activity,64), margin=UiKit.dp(activity,16);
  LinearLayout menu=UiKit.card(activity);
  menu.setVisibility(View.GONE);
  FrameLayout.LayoutParams panel=new FrameLayout.LayoutParams(UiKit.dp(activity,180),-2,Gravity.BOTTOM|Gravity.END);
  panel.setMargins(margin,0,margin,margin+size+UiKit.dp(activity,8));
  overlay.addView(menu,panel);
  FrameLayout button=new FrameLayout(activity);
  TextView symbol=UiKit.label(activity,"+",32,android.graphics.Color.BLACK);
  symbol.setGravity(Gravity.CENTER);
  symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  button.addView(symbol,new FrameLayout.LayoutParams(-1,-1));
  button.setContentDescription("新建备忘");
  FrameLayout.LayoutParams anchor=new FrameLayout.LayoutParams(size,size,Gravity.BOTTOM|Gravity.END);
  anchor.setMargins(margin,0,margin,margin);
  overlay.addView(button,anchor);
  int light=UiKit.BRAND;
  android.graphics.drawable.GradientDrawable tile=UiKit.shape(activity,light,20);
  button.setBackground(tile);
  int dark=android.graphics.Color.rgb(android.graphics.Color.red(light)*3/7,
   android.graphics.Color.green(light)*3/7,android.graphics.Color.blue(light)*3/7);
  android.animation.ArgbEvaluator colors=new android.animation.ArgbEvaluator();
  final float[] position={0};
  java.util.function.Consumer<Boolean> toggle=open->{
   createOpen=open;
   if(createMotion!=null)createMotion.cancel();
   if(open)menu.setVisibility(View.VISIBLE);
   button.setContentDescription(open?"关闭新建菜单":"新建备忘");
   createMotion=android.animation.ValueAnimator.ofFloat(position[0],open?1:0);
   createMotion.setDuration(280);
   createMotion.setInterpolator(new android.view.animation.PathInterpolator(.22f,.72f,.25f,1f));
   createMotion.addUpdateListener(frame->{
    float t=(Float)frame.getAnimatedValue();position[0]=t;
    float outer=1-.19f*t;
    button.setScaleX(outer);button.setScaleY(outer);
    tile.setColor((Integer)colors.evaluate(t,light,dark));
    boolean cross=t>=.5f;
    String mark=cross?"×":"+";
    if(!mark.contentEquals(symbol.getText())){
     symbol.setText(mark);
     symbol.setTextColor(cross?android.graphics.Color.WHITE:android.graphics.Color.BLACK);
    }
    float glyphScale=Math.abs(2*t-1);
    symbol.setScaleX(glyphScale);symbol.setScaleY(glyphScale);
    menu.setAlpha(t);menu.setTranslationY(UiKit.dp(activity,12)*(1-t));
    if(t==0&&!createOpen)menu.setVisibility(View.GONE);
   });
   createMotion.start();
  };
  closeCreate=()->toggle.accept(false);
  button.setOnClickListener(v->toggle.accept(!createOpen));
  String[] types={"文字","清单","绘图","图片","拍照"};
  String[] labels={"新建备忘","新建清单","绘图","选择图片","拍照"};
  for(int i=0;i<types.length;i++){
   String type=types[i];
   menu.addView(UiKit.action(activity,labels[i],false,()->{
    closeCreate.run();
    activity.startActivity(new Intent(activity,NoteEditorActivity.class).putExtra("create",type));
   }));
  }
 }
 private void sections(){
  LinearLayout body=UiKit.col(activity);
  ScrollView list=new ScrollView(activity);list.addView(body);Dialog dialog=UiKit.sheet(activity,"备忘分类",list);
  for(String key:new String[]{"备忘","归档","回收站"})
   body.addView(UiKit.action(activity,key,key.equals(section),()->{section=key;labelFilter="";dialog.dismiss();refresh();}));
  Set<String> labels=prefs.getStringSet("labels",Collections.emptySet());
  for(String label:new TreeSet<>(labels))body.addView(UiKit.action(activity,label,label.equals(labelFilter),()->{
   section="备忘";labelFilter=label;dialog.dismiss();refresh();
  }));
  body.addView(UiKit.action(activity,"创建新标签",false,()->{
   EditText input=UiKit.input(activity,"标签名称","",1);
   UiKit.prompt(activity,"创建标签",input,"创建",()->{
    String value=input.getText().toString().trim();
    if(value.isEmpty())throw new IllegalArgumentException("请输入标签名称");
    Set<String> updated=new HashSet<>(prefs.getStringSet("labels",Collections.emptySet()));
    updated.add(value);prefs.edit().putStringSet("labels",updated).apply();dialog.dismiss();
   },null,null).show();
  }));
  dialog.show();
 }
 private void filters(){
  LinearLayout body=UiKit.col(activity);
  ScrollView list=new ScrollView(activity);list.addView(body);Dialog dialog=UiKit.sheet(activity,"筛选备忘",list);
  for(String type:new String[]{"全部","文字","清单","图片","绘图"})body.addView(UiKit.action(activity,type,type.equals(typeFilter),()->{
   typeFilter=type;dialog.dismiss();refresh();
  }));
  for(int i=-1;i<COLORS.length;i++){
   final int value=i;
   body.addView(UiKit.action(activity,i<0?"所有颜色":"颜色 "+(i+1),i==colorFilter,()->{
    colorFilter=value;dialog.dismiss();refresh();
   }));
  }
  body.addView(UiKit.action(activity,"清除筛选",false,()->{
   typeFilter="全部";colorFilter=-1;labelFilter="";dialog.dismiss();refresh();
  }));
  dialog.show();
 }
 private List<JSONObject> read(){
  List<JSONObject> notes=new ArrayList<>();
  for(Map.Entry<String,?> entry:prefs.getAll().entrySet()){
   if(!entry.getKey().startsWith("note.")||!(entry.getValue() instanceof String))continue;
   try{notes.add(new JSONObject((String)entry.getValue()));}catch(JSONException ignored){}
  }
  notes.sort(Comparator.comparingLong((JSONObject n)->n.optLong("order",Long.MIN_VALUE)).thenComparing(Comparator.comparingLong((JSONObject n)->n.optLong("updated")).reversed()));
  return notes;
 }
 private void refresh(){
  cards.removeAllViews();cardViews.clear();updateSelection();
  String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
  List<JSONObject> pinned=new ArrayList<>(),other=new ArrayList<>();
  for(JSONObject note:read()){
   if(!section.equals(note.optString("section","备忘")))continue;
   if(colorFilter>=0&&colorFilter!=note.optInt("color"))continue;
   if(!labelFilter.isEmpty()&&!Arrays.asList(note.optString("labels").split("、")).contains(labelFilter))continue;
   JSONArray media=note.optJSONArray("images");
   boolean images=media!=null&&media.length()>0;
   if(typeFilter.equals("清单")&&!note.optBoolean("checklist"))continue;
   if(typeFilter.equals("图片")&&!images)continue;
   if(typeFilter.equals("绘图")&&!note.optBoolean("drawing"))continue;
   if(typeFilter.equals("文字")&&(note.optBoolean("checklist")||images))continue;
   if(!(note.optString("title")+" "+preview(note)+" "+note.optString("labels")).toLowerCase(Locale.ROOT).contains(query))continue;
   (section.equals("备忘")&&note.optBoolean("pinned")?pinned:other).add(note);
  }
  if(!section.equals("备忘"))heading(section);
  if(!pinned.isEmpty()){heading("已置顶");group(pinned);}
  if(!other.isEmpty()){if(!pinned.isEmpty())heading("其他");group(other);}
  if(pinned.isEmpty()&&other.isEmpty()){
   TextView empty=UiKit.label(activity,query.isEmpty()?"暂无"+section:"没有找到匹配的备忘",16,UiKit.MUTED);
   empty.setGravity(Gravity.CENTER);empty.setPadding(0,UiKit.dp(activity,42),0,0);cards.addView(empty);
  }
 }
 private void heading(String title){
  TextView line=UiKit.label(activity,title,13,UiKit.MUTED);
  line.setPadding(0,UiKit.dp(activity,12),0,UiKit.dp(activity,6));cards.addView(line);
 }
 private String preview(JSONObject note){
  if(!note.optBoolean("checklist"))return note.optString("body");
  StringJoiner text=new StringJoiner("\n");
  JSONArray list=note.optJSONArray("items");
  if(list!=null)for(int i=0;i<list.length();i++){
   JSONObject item=list.optJSONObject(i);
   if(item!=null)text.add((item.optBoolean("done")?"☑ ":"☐ ")+item.optString("text"));
  }
  return text.toString();
 }
 private void group(List<JSONObject> notes){
  LinearLayout row=new LinearLayout(activity);
  LinearLayout left=UiKit.col(activity),right=UiKit.col(activity);
  row.addView(left,new LayoutParams(0,-2,1));
  if(grid)row.addView(right,new LayoutParams(0,-2,1));
  cards.addView(row);
  for(int i=0;i<notes.size();i++){
   JSONObject note=notes.get(i);
   LinearLayout card=UiKit.card(activity);
   card.setBackground(UiKit.shape(activity,color(note.optInt("color")),18));
   LayoutParams lp=new LayoutParams(-1,-2);
   int gap=UiKit.dp(activity,4);
   lp.setMargins(gap,gap,gap,gap);
   (grid&&i%2==1?right:left).addView(card,lp);
   String title=note.optString("title");
   if(!title.isEmpty()){
    TextView name=UiKit.label(activity,title,16,UiKit.INK);
    name.setTypeface(null,Typeface.BOLD);name.setMaxLines(8);name.setEllipsize(TextUtils.TruncateAt.END);card.addView(name);
   }
   TextView body=UiKit.label(activity,preview(note),14,UiKit.INK);
   body.setMaxLines(28);body.setEllipsize(TextUtils.TruncateAt.END);card.addView(body);
   if(!note.optString("labels").isEmpty())card.addView(UiKit.label(activity,note.optString("labels"),12,UiKit.MUTED));
   JSONArray images=note.optJSONArray("images");
   if(images!=null&&images.length()>0){
    ImageView image=new ImageView(activity);image.setAdjustViewBounds(true);image.setMaxHeight(UiKit.dp(activity,180));
    try{
     java.io.File file=new java.io.File(activity.getFilesDir(),"note-media/"+images.optString(0));
     android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();
     options.inSampleSize=4;
     image.setImageBitmap(android.graphics.BitmapFactory.decodeFile(file.getPath(),options));
    }catch(RuntimeException ignored){}
    card.addView(image,0,new LayoutParams(-1,-2));
   }
   String id=note.optString("id");
   cardViews.put(id,card);
   card.setOnLongClickListener(v->{
    if(section.equals("回收站")){trashActions(note);return true;}
    selected.clear();selected.add(id);updateSelection();markCards();
    card.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    dragTargetSeen=false;
    if(!card.startDragAndDrop(ClipData.newPlainText("note",id),new View.DragShadowBuilder(card),id,0))
     swapFeedback(card,"未能开始拖动");
    return true;
   });
   card.setOnDragListener((v,event)->{
    if(!(event.getLocalState() instanceof String))return false;
    String dragged=(String)event.getLocalState();
    if(event.getAction()==DragEvent.ACTION_DRAG_ENTERED&&!dragged.equals(id)){
     dragTargetSeen=true;
     card.animate().scaleX(1.025f).scaleY(1.025f).setDuration(100).start();
    }
    if(event.getAction()==DragEvent.ACTION_DRAG_EXITED||event.getAction()==DragEvent.ACTION_DRAG_ENDED){
     card.animate().scaleX(1).scaleY(1).setDuration(120).start();
    }
    if(event.getAction()==DragEvent.ACTION_DROP){
     if(dragged.equals(id))return false;
     if(!reorder(dragged,id))swapFeedback(card,"只能交换同一分组的备忘");
     return true;
    }
    if(event.getAction()==DragEvent.ACTION_DRAG_ENDED&&dragged.equals(id)&&dragTargetSeen&&!event.getResult())
     swapFeedback(card,"未交换位置");
    return true;
   });
   card.setOnClickListener(v->{
    if(!selected.isEmpty()){
     boolean same=selected.contains(id);
     selected.clear();if(!same)selected.add(id);
     updateSelection();markCards();
    }else if(section.equals("回收站"))trashActions(note);
    else edit(note,note.optBoolean("checklist"));
   });
   markCards();

  }
 }
 void clearSelection(){
  if(selected.isEmpty())return;
  selected.clear();updateSelection();markCards();
 }
 private void swapFeedback(View card,String message){
  card.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
  Toast.makeText(activity,message,Toast.LENGTH_SHORT).show();
 }
 private boolean reorder(String moved,String target){
  List<JSONObject> all=read();
  int from=-1,to=-1;
  for(int i=0;i<all.size();i++){
   String id=all.get(i).optString("id");
   if(moved.equals(id))from=i;
   if(target.equals(id))to=i;
  }
  if(from<0||to<0||all.get(from).optBoolean("pinned")!=all.get(to).optBoolean("pinned"))return false;
  Collections.swap(all,from,to);
  SharedPreferences.Editor editor=prefs.edit();
  for(int i=0;i<all.size();i++){
   JSONObject note=all.get(i);
   try{note.put("order",i);editor.putString("note."+note.optString("id"),note.toString());}
   catch(JSONException ignored){}
  }
  editor.apply();post(this::refresh);return true;
 }
 private void markCards(){
  for(Map.Entry<String,View> entry:cardViews.entrySet()){
   View card=entry.getValue();
   if(card.getBackground() instanceof android.graphics.drawable.GradientDrawable){
    android.graphics.drawable.GradientDrawable background=(android.graphics.drawable.GradientDrawable)card.getBackground();
    background.setStroke(UiKit.dp(activity,selected.contains(entry.getKey())?3:1),
     selected.contains(entry.getKey())?UiKit.accentText():UiKit.LINE);
   }
  }
 }
 private void batch(java.util.function.Consumer<JSONObject> edit){
  for(JSONObject note:read())if(selected.contains(note.optString("id"))){
   edit.accept(note);
   try{save(note);}catch(JSONException ignored){}
  }
  selected.clear();refresh();
 }
 private void updateSelection(){
  selectionBar.removeAllViews();
  selectionBar.setVisibility(selected.isEmpty()?View.GONE:View.VISIBLE);
  if(selected.isEmpty())return;
  selectionBar.setBackground(UiKit.shape(activity,UiKit.tint(UiKit.BRAND),18));
  selectionBar.addView(UiKit.action(activity,"× "+selected.size(),false,()->{selected.clear();refresh();}));
  selectionBar.addView(UiKit.action(activity,"固定",false,()->{
   boolean all=true;
   for(JSONObject note:read())if(selected.contains(note.optString("id"))&&!note.optBoolean("pinned"))all=false;
   final boolean pin=!all;
   batch(note->{try{note.put("pinned",pin);}catch(JSONException ignored){}});
  }));
  selectionBar.addView(UiKit.action(activity,"颜色",false,()->{
   LinearLayout list=UiKit.col(activity);Dialog dialog=UiKit.sheet(activity,"卡片颜色",list);
   String[] names={"默认","橙色","黄色","绿色","青色","紫色","粉色"};
   for(int i=0;i<COLORS.length;i++){final int at=i;View swatch=new View(activity);
    android.graphics.drawable.GradientDrawable fill=UiKit.shape(activity,i==0?UiKit.SURFACE:COLORS[i],20);
    fill.setStroke(UiKit.dp(activity,1),UiKit.LINE);swatch.setBackground(fill);
    LinearLayout chip=new LinearLayout(activity);chip.setGravity(Gravity.CENTER_VERTICAL);
    chip.addView(swatch,new LayoutParams(UiKit.dp(activity,28),UiKit.dp(activity,28)));
    TextView label=UiKit.label(activity,names[i],15,UiKit.INK);
    label.setPadding(UiKit.dp(activity,12),0,0,0);chip.addView(label);
    chip.setMinimumHeight(UiKit.dp(activity,46));
    chip.setOnClickListener(v->{
     dialog.dismiss();batch(note->{try{note.put("color",at);}catch(JSONException ignored){}});
    });
    list.addView(chip);}dialog.show();
  }));
  selectionBar.addView(UiKit.action(activity,"标签",false,()->{
   Set<String> names=new TreeSet<>(prefs.getStringSet("labels",Collections.emptySet()));
   Set<String> chosen=new TreeSet<>();
   LinearLayout list=UiKit.col(activity);
   for(String name:names){
    CheckBox box=new CheckBox(activity);box.setText(name);box.setTextColor(UiKit.INK);
    box.setOnCheckedChangeListener((v,on)->{if(on)chosen.add(name);else chosen.remove(name);});list.addView(box);
   }
   EditText input=UiKit.input(activity,"新标签","",1);list.addView(input);
   UiKit.prompt(activity,"添加标签",list,"完成",()->{
    String fresh=input.getText().toString().trim();
    if(!fresh.isEmpty()){chosen.add(fresh);names.add(fresh);prefs.edit().putStringSet("labels",names).apply();}
    batch(note->{
     Set<String> combined=new TreeSet<>(Arrays.asList(note.optString("labels").split("、")));
     combined.remove("");combined.addAll(chosen);
     try{note.put("labels",String.join("、",combined));}catch(JSONException ignored){}
    });
   },null,null).show();
  }));
  selectionBar.addView(UiKit.action(activity,"⋮",false,()->{
   LinearLayout list=UiKit.col(activity);Dialog dialog=UiKit.sheet(activity,"笔记操作",list);
   list.addView(UiKit.action(activity,section.equals("归档")?"取消归档":"归档",false,()->{
    dialog.dismiss();batch(note->{try{note.put("section",section.equals("归档")?"备忘":"归档");}catch(JSONException ignored){}});
   }));
   list.addView(UiKit.action(activity,"移到回收站",false,()->{
    dialog.dismiss();batch(note->{try{note.put("previousSection",note.optString("section","备忘")).put("section","回收站");}catch(JSONException ignored){}});
   }));
   dialog.show();
  }));
 }
 private int color(int index){
  return index>0&&index<COLORS.length?UiKit.tint(COLORS[index]):UiKit.SURFACE;
 }
 private void trashActions(JSONObject note){
  LinearLayout body=UiKit.col(activity);
  body.addView(UiKit.label(activity,preview(note),15,UiKit.INK));
  UiKit.prompt(activity,"回收站",body,"恢复",()->{
   updateSection(note,note.optString("previousSection","备忘"));
  },"永久删除",()->UiKit.prompt(activity,"永久删除这条备忘？",
   UiKit.label(activity,"删除后无法恢复。",15,UiKit.MUTED),"删除",()->{
    prefs.edit().remove("note."+note.optString("id")).apply();refresh();
   },null,null).show()).show();
 }
 private void updateSection(JSONObject note,String next){
  try{note.put("previousSection",note.optString("section","备忘")).put("section",next);save(note);refresh();}
  catch(JSONException e){throw new IllegalArgumentException("无法更新备忘");}
 }
 private void save(JSONObject note)throws JSONException{
  if(!note.has("id"))note.put("id",UUID.randomUUID().toString());
  note.put("updated",System.currentTimeMillis());
  prefs.edit().putString("note."+note.getString("id"),note.toString()).apply();
 }
 private void edit(JSONObject original,boolean checklist){
  activity.startActivity(new Intent(activity,NoteEditorActivity.class)
   .putExtra("id",original.optString("id")).putExtra("checklist",checklist));
 }
}
