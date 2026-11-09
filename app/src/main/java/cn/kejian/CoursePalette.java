package cn.kejian;
import java.util.*;
public final class CoursePalette {
 private final Map<String,Integer> colors=new HashMap<>();
 public CoursePalette(Collection<String> names){int i=0;for(String name:new TreeSet<>(names)){double hue=(218+i++*137.507764)%360;colors.put(name,hsv(hue,.55,.70));}}
 public int color(String name){return colors.getOrDefault(name,0xff537ca8);}
 public static int tint(int color){int r=(color>>16)&255,g=(color>>8)&255,b=color&255;return 0xff000000|((r+255*6)/7<<16)|((g+255*6)/7<<8)|(b+255*6)/7;}
 private static int hsv(double hue,double saturation,double value){double h=hue/60,c=value*saturation,x=c*(1-Math.abs(h%2-1)),m=value-c;double r=0,g=0,b=0;switch((int)h){case 0:r=c;g=x;break;case 1:r=x;g=c;break;case 2:g=c;b=x;break;case 3:g=x;b=c;break;case 4:r=x;b=c;break;default:r=c;b=x;}return 0xff000000|((int)Math.round((r+m)*255)<<16)|((int)Math.round((g+m)*255)<<8)|(int)Math.round((b+m)*255);}
}
