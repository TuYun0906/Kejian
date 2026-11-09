package cn.kejian;
public class CourseTextTest {public static void main(String[] args){if(!CourseText.display("形势与政策（1）").equals("形势与政策\n（1）"))throw new AssertionError();if(!CourseText.display("高数 (一)").equals("高数\n(一)"))throw new AssertionError();if(!CourseText.display("C语言").equals("C语言"))throw new AssertionError();System.out.println("PASS course suffix line breaks");}}
