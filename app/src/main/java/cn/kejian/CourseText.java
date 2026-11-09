package cn.kejian;
public final class CourseText {
 public static String display(String name){return name.replaceAll("\\s*([（(][^()（）]{1,12}[）)])", "\n$1").trim();}
}
