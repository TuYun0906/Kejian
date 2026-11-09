package cn.kejian;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

final class AccountAvatar {
 static Bitmap decode(String encoded) {
  if(encoded.isEmpty()||encoded.length()>1_400_000)return null;
  try{
   byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
   BitmapFactory.Options options=new BitmapFactory.Options();
   options.inJustDecodeBounds=true;
   BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
   if(options.outWidth<=0||options.outHeight<=0)return null;
   options.inSampleSize=1;
   while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>512)
    options.inSampleSize*=2;
   options.inJustDecodeBounds=false;
   options.inPreferredConfig=Bitmap.Config.ARGB_8888;
   Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
   if(bitmap==null)return null;
   int side=Math.min(bitmap.getWidth(),bitmap.getHeight());
   int left=(bitmap.getWidth()-side)/2, top=(bitmap.getHeight()-side)/2;
   int[] row=new int[side];
   for(int y=top;y<top+side;y++){
    bitmap.getPixels(row,0,side,left,y,side,1);
    for(int x=0;x<side;x++){
     double dx=(x+.5)/side-.5,dy=(y-top+.5)/side-.5;
     if(dx*dx+dy*dy<=.25&&(row[x]>>>24)>0)return bitmap;
    }
   }
   bitmap.recycle();
  }catch(RuntimeException ignored){}
  return null;
 }
}
