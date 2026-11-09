package android.util;
import java.io.*;import java.util.concurrent.*;
// Deterministic fault injection at the write/read boundary; desktop test only.
public class AtomicFile {
 private final File base;public static volatile boolean writing=false,block=false;public static CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
 public AtomicFile(File file){base=file;}public File getBaseFile(){return base;}
 public InputStream openRead()throws IOException{if(writing)throw new IOException("concurrent read would discard in-flight write");return new FileInputStream(base);}
 public FileOutputStream startWrite()throws IOException{writing=true;FileOutputStream out=new FileOutputStream(base);if(block){entered.countDown();try{if(!release.await(3,TimeUnit.SECONDS))throw new IOException("test timed out");}catch(InterruptedException e){throw new IOException(e);}}return out;}
 public void finishWrite(FileOutputStream out)throws IOException{out.close();writing=false;}public void failWrite(FileOutputStream out){try{out.close();}catch(Exception ignored){}writing=false;}
 public void delete(){if(writing)throw new IllegalStateException("concurrent delete");base.delete();}
}
