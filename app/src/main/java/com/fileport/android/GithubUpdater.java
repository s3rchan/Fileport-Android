package com.fileport.android;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.WebView;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;

/**
 * User-initiated GitHub Releases updater. No background polling or silent installs.
 * Android verifies the APK package, version and installed signing certificate
 * before handing the file to the system installer for user approval.
 */
final class GithubUpdater {
 private static final String API="https://api.github.com/repos/s3rchan/Fileport-Android/releases/latest";
 private static final long MAX_APK=300L*1024*1024;
 private final MainActivity activity;
 private final WebView web;
 private volatile Release available;
 private volatile File downloaded;
 private volatile boolean busy;

 private static final class Release {
  final String version,url,digest;
  final long size;
  Release(String version,String url,String digest,long size){this.version=version;this.url=url;this.digest=digest;this.size=size;}
 }
 GithubUpdater(MainActivity activity,WebView web){this.activity=activity;this.web=web;}
 private void js(String method,String value){
  activity.runOnUiThread(()->web.evaluateJavascript("window."+method+"("+JSONObject.quote(value)+")",null));
 }
 private void status(String value){js("fileportUpdateStatus",value);}
 private static int[] versionParts(String version)throws Exception{
  if(!version.matches("[0-9]{1,4}\\.[0-9]{1,4}\\.[0-9]{1,4}"))throw new Exception("Invalid release version");
  String[] bits=version.split("\\.");
  return new int[]{Integer.parseInt(bits[0]),Integer.parseInt(bits[1]),Integer.parseInt(bits[2])};
 }
 private static boolean newer(String a,String b)throws Exception{
  int[] next=versionParts(a),old=versionParts(b);
  for(int i=0;i<3;i++){if(next[i]!=old[i])return next[i]>old[i];}
  return false;
 }
 private String installedVersion()throws Exception{
  return activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
 }
 private static HttpURLConnection connect(String raw)throws Exception{
  URL url=new URL(raw);
  for(int redirect=0;redirect<6;redirect++){
   if(!"https".equalsIgnoreCase(url.getProtocol()))throw new Exception("HTTPS is required");
   String host=url.getHost().toLowerCase(java.util.Locale.ROOT);
   if(!host.equals("api.github.com")&&!host.equals("github.com")&&!host.equals("release-assets.githubusercontent.com")&&!host.equals("objects.githubusercontent.com"))
    throw new Exception("Unexpected download host");
   HttpURLConnection conn=(HttpURLConnection)url.openConnection();
   conn.setInstanceFollowRedirects(false);
   conn.setConnectTimeout(15000);conn.setReadTimeout(45000);
   conn.setRequestProperty("User-Agent","Fileport-Android-Updater");
   conn.setRequestProperty("Accept",host.equals("api.github.com")?"application/vnd.github+json":"application/octet-stream");
   int code=conn.getResponseCode();
   if(code==301||code==302||code==303||code==307||code==308){
    String location=conn.getHeaderField("Location");conn.disconnect();
    if(location==null)throw new Exception("Missing redirect target");
    url=new URL(url,location);continue;
   }
   if(code!=200){conn.disconnect();throw new Exception("GitHub HTTP "+code);}
   return conn;
  }
  throw new Exception("Too many redirects");
 }
 private static String readText(InputStream stream,int max)throws Exception{
  java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
  byte[] buf=new byte[4096];int n;
  while((n=stream.read(buf))!=-1){if(out.size()+n>max)throw new Exception("Release metadata too large");out.write(buf,0,n);}
  return out.toString("UTF-8");
 }
 private Release getLatest()throws Exception{
  HttpURLConnection conn=connect(API);
  JSONObject release;
  try(InputStream in=conn.getInputStream()){release=new JSONObject(readText(in,1024*1024));}
  finally{conn.disconnect();}
  if(release.optBoolean("draft")||release.optBoolean("prerelease"))throw new Exception("No stable release found");
  String tag=release.optString("tag_name","");
  if(!tag.matches("android-v[0-9]{1,4}\\.[0-9]{1,4}\\.[0-9]{1,4}"))
   throw new Exception("Latest release is not an Android release");
  String version=tag.substring("android-v".length());
  String filename="Fileport-Android-v"+version+".apk";
  JSONArray assets=release.optJSONArray("assets");
  if(assets==null)throw new Exception("Release APK not found");
  for(int i=0;i<assets.length();i++){
   JSONObject asset=assets.getJSONObject(i);
   if(!filename.equals(asset.optString("name")))continue;
   String url=asset.optString("browser_download_url","");
   URL parsed=new URL(url);
   String path=parsed.getPath();
   if(!"https".equals(parsed.getProtocol())||!"github.com".equalsIgnoreCase(parsed.getHost())
      ||!path.startsWith("/s3rchan/Fileport-Android")
      ||!path.endsWith("/releases/download/"+tag+"/"+filename)
      ||parsed.getQuery()!=null||parsed.getRef()!=null)
    throw new Exception("Unexpected APK URL");
   long size=asset.optLong("size",-1);
   if(size<=0||size>MAX_APK)throw new Exception("APK size is invalid");
   String digest=asset.optString("digest","");
   if(!digest.isEmpty()&&!digest.matches("sha256:[a-fA-F0-9]{64}"))
    throw new Exception("Unexpected APK digest");
   return new Release(version,url,digest,size);
  }
  throw new Exception("Release APK not found");
 }
 private boolean start(){synchronized(this){if(busy)return false;busy=true;return true;}}
 private void finish(){synchronized(this){busy=false;}}
 void check(){
  if(!start()){status("An update operation is already running.");return;}
  status("Checking GitHub Releases...");
  new Thread(()->{
   try{
    Release release=getLatest();
    if(!newer(release.version,installedVersion())){
     available=null;js("fileportGithubUpdateUnavailable","You already have the latest version.");
    }else{
     available=release;downloaded=null;
     js("fileportGithubUpdateAvailable",release.version);
    }
   }catch(Exception e){available=null;js("fileportGithubUpdateUnavailable","Update check failed: "+e.getMessage());}
   finally{finish();}
  },"fileport-update-check").start();
 }
 void download(){
  if(!start()){status("An update operation is already running.");return;}
  Release release=available;
  if(release==null){finish();status("Check for an update first.");return;}
  status("Downloading Fileport v"+release.version+"...");
  new Thread(()->{
   File folder=new File(activity.getCacheDir(),"updates");
   File partial=new File(folder,"fileport-update.part");
   File apk=new File(folder,"fileport-update.apk");
   HttpURLConnection conn=null;
   try{
    if(!newer(release.version,installedVersion()))throw new Exception("Already up to date");
    if(!folder.exists()&&!folder.mkdirs())throw new Exception("Cannot create update directory");
    if(apk.exists())apk.delete();
    conn=connect(release.url);
    MessageDigest sha=MessageDigest.getInstance("SHA-256");
    long total=0;byte[] buffer=new byte[65536];int n;
    try(InputStream in=conn.getInputStream();FileOutputStream out=new FileOutputStream(partial)){
     while((n=in.read(buffer))!=-1){
      total+=n;if(total>MAX_APK||total>release.size)throw new Exception("APK exceeds expected size");
      out.write(buffer,0,n);sha.update(buffer,0,n);
     }
     out.getFD().sync();
    }
    if(total!=release.size)throw new Exception("Incomplete APK download");
    if(!release.digest.isEmpty()){
     StringBuilder hex=new StringBuilder();
     for(byte b:sha.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
     if(!release.digest.substring(7).equalsIgnoreCase(hex.toString()))throw new Exception("APK checksum mismatch");
    }
    verifyApk(partial);
    if(!partial.renameTo(apk))throw new Exception("Cannot finalize APK download");
    downloaded=apk;
    js("fileportGithubUpdateDownloaded",release.version);
    install();
   }catch(Exception e){downloaded=null;status("Update failed: "+e.getMessage());}
   finally{if(conn!=null)conn.disconnect();if(partial.exists())partial.delete();finish();}
  },"fileport-update-download").start();
 }
 private void verifyApk(File apk)throws Exception{
  PackageManager pm=activity.getPackageManager();
  int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
  PackageInfo next=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
  PackageInfo current=pm.getPackageInfo(activity.getPackageName(),flags);
  if(next==null||!activity.getPackageName().equals(next.packageName))throw new Exception("APK package does not match Fileport");
  long nextCode=Build.VERSION.SDK_INT>=28?next.getLongVersionCode():next.versionCode;
  long oldCode=Build.VERSION.SDK_INT>=28?current.getLongVersionCode():current.versionCode;
  if(nextCode<=oldCode)throw new Exception("APK version must be newer than the installed version");
  Signature[] incoming=Build.VERSION.SDK_INT>=28?next.signingInfo.getApkContentsSigners():next.signatures;
  Signature[] existing=Build.VERSION.SDK_INT>=28?current.signingInfo.getApkContentsSigners():current.signatures;
  if(incoming==null||existing==null||incoming.length==0||incoming.length!=existing.length)
   throw new Exception("APK signing key differs from the installed version");
  HashSet<String> signatures=new HashSet<>();
  for(Signature signature:existing)signatures.add(signature.toCharsString());
  for(Signature signature:incoming)if(!signatures.contains(signature.toCharsString()))
   throw new Exception("APK signing key differs from the installed version");
 }
 void install(){
  File apk=downloaded;
  if(apk==null||!apk.isFile()){status("Download an update first.");return;}
  try{verifyApk(apk);}catch(Exception e){status("Update rejected: "+e.getMessage());return;}
  activity.runOnUiThread(()->{
   try{
    if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
     status("Allow Fileport to install apps, then tap Install downloaded APK.");
     Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()));
     activity.startActivity(settings);return;
    }
    Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".fileprovider",apk);
    Intent intent=new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri,"application/vnd.android.package-archive");
    intent.setClipData(ClipData.newRawUri("Fileport update",uri));
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    activity.startActivity(intent);
    status("Android will ask you to confirm installation.");
   }catch(Exception e){status("Installer error: "+e.getMessage());}
  });
 }
}
