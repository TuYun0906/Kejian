package cn.kejian;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

public final class NoteMediaProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
  String name=uri.getLastPathSegment();
  if(name==null||!name.matches("[a-f0-9-]+\\.jpg"))throw new FileNotFoundException();
  File folder=new File(getContext().getFilesDir(),"note-media");folder.mkdirs();
  return ParcelFileDescriptor.open(new File(folder,name),mode.contains("w")?
    ParcelFileDescriptor.MODE_READ_WRITE|ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_TRUNCATE:
    ParcelFileDescriptor.MODE_READ_ONLY);
 }
 @Override public String getType(Uri uri){return "image/jpeg";}
 @Override public Cursor query(Uri u,String[] p,String s,String[] a,String sort){return null;}
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
 @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
