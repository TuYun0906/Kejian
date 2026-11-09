package cn.kejian;

import android.content.Context;
import org.json.JSONObject;

enum School {
 WFU("潍坊学院","潍坊学院"),
 AUST("安徽理工大学","安徽理工大学"),
 GENERAL("通用教务","通用教务");

 final String name,label;
 School(String name,String label){this.name=name;this.label=label;}
 static School current(Context context){
  return valueOf(context.getSharedPreferences("school",0).getString("selected","WFU"));
 }
 static School from(JSONObject data){
  return valueOf(data.optString("school","WFU"));
 }
 void select(Context context){
  context.getSharedPreferences("school",0).edit().putString("selected",name()).apply();
 }
 static android.view.View loginSelector(android.app.Activity activity,School shown,
                                         java.util.function.BooleanSupplier busy){
  return UiKit.settingRow(activity,"学校 · "+shown.label,
   UiKit.label(activity,"切换 ›",14,UiKit.accentText()),()->{
    if(busy.getAsBoolean())return;
    android.widget.LinearLayout body=UiKit.col(activity);
    android.app.Dialog panel=UiKit.sheet(activity,"选择学校",body);
    for(School school:values())body.addView(UiKit.action(activity,school.label,school==shown,()->{
     panel.dismiss();
     if(school==shown||busy.getAsBoolean())return;
     school.select(activity);
     Class<?> target=school.importActivity();
     activity.startActivity(new android.content.Intent(activity,target).addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
     if(android.os.Build.VERSION.SDK_INT>=34)
      activity.overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_CLOSE,0,0);
     activity.finish();
     activity.overridePendingTransition(0,0);
    }));
    panel.show();
   });
 }

 Class<?> importActivity() {
  return switch(this) {
   case WFU -> ImportActivity.class;
   case AUST -> AustImportActivity.class;
   case GENERAL -> GeneralImportActivity.class;
  };
 }
}
