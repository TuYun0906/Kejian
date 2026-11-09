package cn.kejian;
import java.util.*;
public class PaletteTest {
 public static void main(String[] args){List<String> names=new ArrayList<>();for(int i=0;i<40;i++)names.add("课程"+i);names.add("Aa");names.add("BB");CoursePalette p=new CoursePalette(names);Set<Integer> colors=new HashSet<>();for(String n:names)if(!colors.add(p.color(n)))throw new AssertionError("different courses share color");Collections.reverse(names);CoursePalette q=new CoursePalette(names);for(String n:names)if(p.color(n)!=q.color(n))throw new AssertionError("unstable color");System.out.println("PASS: distinct course colors, hash collision and order stability");}
}
