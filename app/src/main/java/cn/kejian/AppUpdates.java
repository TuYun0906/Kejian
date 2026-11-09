package cn.kejian;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

final class AppUpdates {
 record Release(String tag, String notes, String downloadUrl) {}

 static String repository(Context context) {
  try (InputStream stream = context.getAssets().open("update-repository.txt")) {
   String value = readText(stream, 512).trim();
   return value.matches("[A-Za-z0-9-]+/[A-Za-z0-9_.-]+") ? value : "";
  } catch (IOException ignored) {
   return "";
  }
 }

 static Release latest(String repository, String installed) throws Exception {
  HttpURLConnection connection = (HttpURLConnection) URI.create(
    "https://api.github.com/repos/" + repository + "/releases/latest").toURL().openConnection();
  connection.setConnectTimeout(8000);
  connection.setReadTimeout(8000);
  connection.setInstanceFollowRedirects(false);
  connection.setRequestProperty("Accept", "application/vnd.github+json");
  connection.setRequestProperty("User-Agent", "Kejian-Android");
  try {
   if (connection.getResponseCode() != 200) throw new IOException("Release unavailable");
   try (InputStream stream = connection.getInputStream()) {
    return parse(new JSONObject(readText(stream, 512 * 1024)), repository, installed);
   }
  } finally {
   connection.disconnect();
  }
 }

 private static String readText(InputStream stream, int limit) throws IOException {
  ByteArrayOutputStream output = new ByteArrayOutputStream();
  byte[] buffer = new byte[4096];
  for (int count; (count = stream.read(buffer)) != -1;) {
   if (output.size() + count > limit) throw new IOException("Update data too large");
   output.write(buffer, 0, count);
  }
  return new String(output.toByteArray(), StandardCharsets.UTF_8);
 }

 static Release parse(JSONObject result, String repository, String installed) throws IOException {
  String tag = result.optString("tag_name");
  if (result.optBoolean("draft") || result.optBoolean("prerelease")) return null;
  if (!tag.matches("v?[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}")) {
   throw new IOException("Invalid release tag");
  }
  if (!UpdateVersion.isNewer(tag, installed)) return null;
  JSONArray assets = result.optJSONArray("assets");
  String name = "kejian-" + tag.replaceFirst("^v", "") + ".apk";
  String expected = "https://github.com/" + repository + "/releases/download/" + tag + "/" + name;
  for (int i = 0; assets != null && i < assets.length(); i++) {
   JSONObject asset = assets.optJSONObject(i);
   if (asset != null && name.equals(asset.optString("name"))
     && "uploaded".equals(asset.optString("state"))
     && expected.equals(asset.optString("browser_download_url"))) {
    String notes = result.isNull("body") ? "" : result.optString("body");
    return new Release(tag, notes.substring(0, Math.min(notes.length(), 2000)), expected);
   }
  }
  throw new IOException("Release APK unavailable");
 }
}
