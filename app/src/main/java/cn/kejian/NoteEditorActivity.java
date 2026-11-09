package cn.kejian;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

public final class NoteEditorActivity extends Activity {
 private JSONObject note;
 private EditText title,body;
 private LinearLayout root,items,media;
 private String id,cameraFile;
 private boolean ready;
 private static final int[] COLORS={0,0xfffd8263,0xffffc93e,0xff04bb9d,0xff07aefc,0xffa25bff,0xfff673a9};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);UiKit.apply(this);
  id=state==null?getIntent().getStringExtra("id"):state.getString("id");
  if(id==null||id.isEmpty())id=UUID.randomUUID().toString();
  try{note=new JSONObject(getSharedPreferences("notes",0).getString("note."+id,"{}"));note.put("id",id);}
  catch(JSONException e){finish();return;}
  if(state!=null)cameraFile=state.getString("camera");
  String create=getIntent().getStringExtra("create");
  if(!note.has("checklist"))put("checklist",getIntent().getBooleanExtra("checklist",false)||"清单".equals(create));
  root=UiKit.col(this);root.setBackgroundColor(UiKit.BG);
  root.setOnApplyWindowInsetsListener((v,in)->{
   int pad=UiKit.dp(this,16);v.setPadding(pad,in.getSystemWindowInsetTop()+pad,pad,in.getSystemWindowInsetBottom()+pad);return in;
  });
  LinearLayout actions=new LinearLayout(this);
  actions.addView(UiKit.action(this,"‹ 返回",false,this::finish));
  actions.addView(UiKit.action(this,note.optBoolean("pinned")?"取消固定":"固定",false,()->{
   put("pinned",!note.optBoolean("pinned"));save();recreate();
  }));
  actions.addView(UiKit.action(this,"归档",false,()->{
   put("section","归档".equals(note.optString("section"))?"备忘":"归档");save();finish();
  }));
  actions.addView(UiKit.action(this,"⋯",false,this::more));root.addView(actions);
  ScrollView scroll=new ScrollView(this);LinearLayout fields=UiKit.col(this);scroll.addView(fields);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  title=UiKit.input(this,"标题",note.optString("title"),131073);title.setSingleLine(false);title.setMinHeight(UiKit.dp(this,64));title.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));fields.addView(title);
  media=UiKit.col(this);fields.addView(media);
  items=UiKit.col(this);fields.addView(items);
  body=UiKit.input(this,"记笔记…",note.optString("body"),131073);body.setSingleLine(false);body.setMinLines(7);body.setMinHeight(UiKit.dp(this,168));
  body.setGravity(Gravity.TOP);body.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));fields.addView(body);
  if(note.optBoolean("checklist")){
   body.setVisibility(View.GONE);
   JSONArray saved=note.optJSONArray("items");
   if(saved!=null)for(int i=0;i<saved.length();i++){JSONObject item=saved.optJSONObject(i);if(item!=null)addItem(item.optString("text"),item.optBoolean("done"));}
   if(items.getChildCount()==0)addItem("",false);
   fields.addView(UiKit.action(this,"＋ 新增项目",false,()->addItem("",false).requestFocus()));
  }
  LinearLayout tools=new LinearLayout(this);
  tools.addView(UiKit.action(this,"＋",false,this::attachments));
  tools.addView(UiKit.action(this,"调色板",false,this::palette));
  tools.addView(UiKit.action(this,"标签",false,this::labels));
  root.addView(tools);setContentView(root);ready=true;showMedia();shade();
  if(state==null&&!note.has("updated"))root.post(()->{
   if("图片".equals(create))pick();else if("拍照".equals(create))camera();else if("绘图".equals(create))draw();
  });
 }
 private void put(String key,Object value){try{note.put(key,value);}catch(JSONException ignored){}}
 private void save(){
  if(!ready)return;
  put("title",title.getText().toString());put("body",body.getText().toString());
  JSONArray entries=new JSONArray();
  for(int i=0;i<items.getChildCount();i++){
   LinearLayout row=(LinearLayout)items.getChildAt(i);
   String text=((EditText)row.getChildAt(1)).getText().toString();
   if(!text.trim().isEmpty())try{entries.put(new JSONObject().put("text",text).put("done",((CheckBox)row.getChildAt(0)).isChecked()));}catch(JSONException ignored){}
  }
  put("items",entries);
  boolean empty=note.optString("title").trim().isEmpty()&&note.optString("body").trim().isEmpty()&&entries.length()==0&&note.optJSONArray("images")==null;
  if(empty&&!note.has("updated"))return;
  put("updated",System.currentTimeMillis());
  getSharedPreferences("notes",0).edit().putString("note."+id,note.toString()).apply();
 }
 @Override public void onPause(){save();super.onPause();}
 @Override public void onSaveInstanceState(Bundle out){save();out.putString("id",id);out.putString("camera",cameraFile);super.onSaveInstanceState(out);}
 private EditText addItem(String value,boolean checked){
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
  CheckBox check=new CheckBox(this);check.setChecked(checked);row.addView(check);
  EditText text=UiKit.input(this,"待办事项",value,1);row.addView(text,new LinearLayout.LayoutParams(0,UiKit.dp(this,50),1));
  Button handle=UiKit.action(this,"≡",false,()->{});handle.setContentDescription("长按拖动排序");
  handle.setOnLongClickListener(v->row.startDragAndDrop(ClipData.newPlainText("item",""),new View.DragShadowBuilder(row),row,0));row.addView(handle);
  row.setOnDragListener((v,event)->{
   if(!(event.getLocalState() instanceof LinearLayout))return false;
   if(event.getAction()==DragEvent.ACTION_DROP){
    LinearLayout dragged=(LinearLayout)event.getLocalState();
    if(dragged!=row&&dragged.getParent()==items){
     int index=items.indexOfChild(row);items.removeView(dragged);items.addView(dragged,index);
    }
   }
   return true;
  });
  row.addView(UiKit.action(this,"×",false,()->items.removeView(row)));items.addView(row);return text;
 }
 private Dialog options(String title,String[] names,java.util.function.IntConsumer selected){
  LinearLayout list=UiKit.col(this);ScrollView scroll=new ScrollView(this);scroll.addView(list);
  Dialog dialog=UiKit.sheet(this,title,scroll);
  for(int i=0;i<names.length;i++){final int at=i;list.addView(UiKit.action(this,names[i],false,()->{dialog.dismiss();selected.accept(at);}));}
  dialog.show();return dialog;
 }
 private void more(){options("笔记选项",new String[]{"标签","移到回收站"},index->{
  if(index==0)labels();else{put("previousSection",note.optString("section","备忘"));put("section","回收站");save();finish();}
 });}
 private void palette(){options("笔记颜色",new String[]{"默认","橙色","黄色","绿色","青色","紫色","粉色"},index->{put("color",index);shade();});}
 private void shade(){int at=note.optInt("color");root.setBackgroundColor(at>0&&at<COLORS.length?UiKit.tint(COLORS[at]):UiKit.BG);}
 private void labels(){
  Set<String> names=new TreeSet<>(getSharedPreferences("notes",0).getStringSet("labels",Collections.emptySet()));
  Set<String> checked=new HashSet<>(Arrays.asList(note.optString("labels").split("、")));
  LinearLayout list=UiKit.col(this);
  for(String name:names){CheckBox box=new CheckBox(this);box.setText(name);box.setTextColor(UiKit.INK);box.setChecked(checked.contains(name));box.setOnCheckedChangeListener((b,on)->{if(on)checked.add(name);else checked.remove(name);});list.addView(box);}
  EditText add=UiKit.input(this,"新标签","",1);list.addView(add);
  UiKit.prompt(this,"标签",list,"完成",()->{
   String value=add.getText().toString().trim();
   if(!value.isEmpty()){names.add(value);checked.add(value);}
   checked.remove("");
   put("labels",String.join("、",new TreeSet<>(checked)));
   getSharedPreferences("notes",0).edit().putStringSet("labels",names).apply();
   save();
  },null,null).show();
 }
 private void attachments(){options("添加",new String[]{"图片","拍照","绘图"},i->{save();if(i==0)pick();else if(i==1)camera();else draw();});}
 private File directory(){File folder=new File(getFilesDir(),"note-media");folder.mkdirs();return folder;}
 private void pick(){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),80);}
 private void camera(){
  cameraFile=UUID.randomUUID()+".jpg";
  Uri uri=Uri.parse("content://cn.kejian.note-media/"+cameraFile);
  Intent intent=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT,uri);
  intent.setClipData(ClipData.newRawUri("photo",uri));
  intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(intent,81);}catch(ActivityNotFoundException e){Toast.makeText(this,"找不到相机应用",0).show();}
 }
 @Override public void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);if(result!=RESULT_OK)return;
  try{
   Uri uri=request==81?Uri.fromFile(new File(directory(),cameraFile)):data==null?null:data.getData();
   if(uri==null)return;
   android.graphics.ImageDecoder.Source source=android.graphics.ImageDecoder.createSource(getContentResolver(),uri);
   Bitmap bitmap=android.graphics.ImageDecoder.decodeBitmap(source,(decoder,info,s)->{
    int w=info.getSize().getWidth(),h=info.getSize().getHeight();float scale=Math.min(1,1600f/Math.max(w,h));
    decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
   });
   attach(bitmap);
   if(request==81&&cameraFile!=null){new File(directory(),cameraFile).delete();cameraFile=null;}
  }catch(Exception e){Toast.makeText(this,"无法读取图片",0).show();}
 }
 private void attach(Bitmap bitmap)throws IOException{
  saveMedia(bitmap,-1,null);
 }
 private void saveMedia(Bitmap bitmap,int index,JSONObject history)throws IOException{
  boolean drawing=history!=null;
  String name=(drawing?"drawing-":"")+UUID.randomUUID()+".png";
  try(OutputStream out=new FileOutputStream(new File(directory(),name))){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException();}
  bitmap.recycle();
  if(drawing)java.nio.file.Files.write(new File(directory(),name+".json").toPath(),history.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  JSONArray images=note.optJSONArray("images");if(images==null)images=new JSONArray();
  try{
   if(index<0)images.put(name);else images.put(index,name);
  }catch(JSONException e){throw new IOException(e);}
  put("images",images);
  if(drawing){
   if(!note.optBoolean("drawing"))put("drawingMediaIdentified",true);
   put("drawing",true);
  }
  save();showMedia();

 }
 private void showMedia(){
  media.removeAllViews();JSONArray images=note.optJSONArray("images");if(images==null)return;
  boolean legacy=note.optBoolean("drawing")&&!note.optBoolean("drawingMediaIdentified");
  for(int i=0;i<images.length();i++){
   int index=i;
   String name=images.optString(i);
   File file=new File(directory(),name);
   ImageView image=new ImageView(this);image.setAdjustViewBounds(true);
   image.setContentDescription("点击放大查看图片");
   image.setImageURI(Uri.fromFile(file));media.addView(image,new LinearLayout.LayoutParams(-1,-2));
   image.setOnClickListener(v->previewImage(file));
   if(name.startsWith("drawing-")||legacy)
    media.addView(UiKit.action(this,name.startsWith("drawing-")?"继续绘图":"在画布中编辑",false,()->{
     draw(index,name);
    }));
  }
 }
 private void previewImage(File file){
  Bitmap bitmap=BitmapFactory.decodeFile(file.getPath());
  if(bitmap==null){Toast.makeText(this,"无法读取图片",Toast.LENGTH_SHORT).show();return;}
  Dialog viewer=new Dialog(this,android.R.style.Theme_Material_NoActionBar_Fullscreen);
  FrameLayout frame=new FrameLayout(this);
  frame.setBackgroundColor(android.graphics.Color.BLACK);
  frame.addView(new ZoomImage(this,bitmap),new FrameLayout.LayoutParams(-1,-1));
  TextView close=UiKit.label(this,"×",30,android.graphics.Color.WHITE);
  close.setGravity(Gravity.CENTER);close.setContentDescription("关闭图片");
  FrameLayout.LayoutParams button=new FrameLayout.LayoutParams(UiKit.dp(this,56),UiKit.dp(this,56),Gravity.TOP|Gravity.END);
  frame.addView(close,button);
  close.setOnClickListener(v->viewer.dismiss());
  viewer.setContentView(frame);
  viewer.show();
 }
 private void draw(){draw(-1,"");}
 private void draw(int index,String previous){
  DrawingCanvas canvas;
  try{
   File sidecar=new File(directory(),previous+".json");
   JSONObject history=previous.isEmpty()||!sidecar.isFile()?null:
    new JSONObject(new String(java.nio.file.Files.readAllBytes(sidecar.toPath()),java.nio.charset.StandardCharsets.UTF_8));
   String base=history==null?previous:history.optString("base");
   Bitmap bitmap=base.isEmpty()?null:BitmapFactory.decodeFile(new File(directory(),base).getPath());
   if(!base.isEmpty()&&bitmap==null)throw new IOException();
   canvas=new DrawingCanvas(this,bitmap,base,history==null?new JSONArray():history.getJSONArray("strokes"));
  }catch(IOException|JSONException e){
   Toast.makeText(this,"无法读取绘图",Toast.LENGTH_SHORT).show();return;
  }
  LinearLayout content=UiKit.col(this);
  LinearLayout tools=new LinearLayout(this);
  tools.addView(UiKit.action(this,"撤销",false,canvas::undo));
  tools.addView(UiKit.action(this,"清空",false,canvas::clear));
  Button eraserButton=UiKit.action(this,"橡皮擦",false,()->{});
  tools.addView(eraserButton);
  Button colors=UiKit.action(this,"颜色",false,()->{});
  tools.addView(colors);
  Runnable sync=()->{
   eraserButton.setText(canvas.erase?"橡皮擦 ✓":"橡皮擦");
   colors.setTextColor(canvas.erase?UiKit.MUTED:UiKit.INK);
   colors.setBackground(UiKit.shape(this,UiKit.tint(canvas.color),12));
  };
  eraserButton.setOnClickListener(v->{canvas.erase=!canvas.erase;sync.run();});
  java.util.function.IntConsumer choose=color->{canvas.color=color;canvas.erase=false;sync.run();};
  colors.setOnClickListener(v->{
   LinearLayout palette=UiKit.col(this);
   Dialog panel=UiKit.sheet(this,"画笔颜色",palette);
   LinearLayout row=null;
   for(int i=0;i<UiKit.ACCENTS.length+1;i++){
    int color=i==0?0xff111111:UiKit.ACCENTS[i-1];
    if(i%4==0){row=new LinearLayout(this);palette.addView(row);}
    Button swatch=UiKit.action(this,"●",false,()->{choose.accept(color);panel.dismiss();});
    swatch.setTextColor(color);swatch.setBackground(UiKit.shape(this,UiKit.tint(color),14));
    swatch.setContentDescription(i==0?"黑色":UiKit.ACCENT_NAMES[i-1]);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,UiKit.dp(this,44),1);
    lp.setMargins(3,3,3,3);row.addView(swatch,lp);
   }
   palette.addView(UiKit.action(this,"自定义 RGB",false,()->{panel.dismiss();RgbPicker.show(this,canvas.color,choose);}));
   panel.show();
  });
  sync.run();
  content.addView(tools);content.addView(canvas,new LinearLayout.LayoutParams(-1,UiKit.dp(this,300)));
  UiKit.prompt(this,"绘图",content,index<0?"添加":"保存",()->{
   try{saveMedia(canvas.bitmap(),index,canvas.history());}
   catch(IOException|JSONException e){throw new IllegalArgumentException("绘图保存失败");}
  },null,null).show();
 }
}
