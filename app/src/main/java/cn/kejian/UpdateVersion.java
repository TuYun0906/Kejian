package cn.kejian;

final class UpdateVersion {
 static boolean isNewer(String tag, String installed) {
  String pattern = "v?[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}";
  if (!tag.matches(pattern) || !installed.matches(pattern)) return false;
  String[] latest = tag.replaceFirst("^v", "").split("\\.");
  String[] current = installed.replaceFirst("^v", "").split("\\.");
  for (int i = 0; i < latest.length; i++) {
   int comparison = Integer.compare(Integer.parseInt(latest[i]), Integer.parseInt(current[i]));
   if (comparison != 0) return comparison > 0;
  }
  return false;
 }
}
