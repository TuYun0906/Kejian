package cn.kejian;

public final class UpdateVersionTest {
 public static void main(String[] args) {
  check(UpdateVersion.isNewer("v0.8.12", "0.8.9"));
  check(!UpdateVersion.isNewer("v0.8.9", "0.8.12"));
  check(!UpdateVersion.isNewer("v0.8.12", "0.8.12"));
  check(UpdateVersion.isNewer("1.0.0", "0.99.99"));
  check(!UpdateVersion.isNewer("v1.0.0-beta", "0.8.12"));
  check(!UpdateVersion.isNewer("v999999999999.0.0", "0.8.12"));
 }

 private static void check(boolean condition) {
  if (!condition) throw new AssertionError("Unexpected update version comparison");
 }
}
