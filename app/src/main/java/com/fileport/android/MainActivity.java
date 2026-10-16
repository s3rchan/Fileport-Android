package com.fileport.android;
import android.content.Context;
import android.content.BroadcastReceiver;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.provider.Settings;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;
import android.widget.FrameLayout;
import android.graphics.Color;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.MediaStore;


import org.json.JSONObject;


import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.security.MessageDigest;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import android.os.ParcelFileDescriptor;

import java.nio.charset.StandardCharsets;
import android.webkit.*;
import android.widget.Toast;
import android.util.Base64;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {
 
 private WebView web;
 private volatile boolean backgrounded=false;
 private volatile String pendingLanRequest=null;
 private volatile long pendingLanAt=0;
 private static final long BACKGROUND_GRACE_MS=180000L;
 private volatile boolean transferActive=false;
 private final Handler idleHandler=new Handler(Looper.getMainLooper());
 private final Runnable idleClose=new Runnable(){@Override public void run(){
   if(!backgrounded)return;
   if(transferActive){idleHandler.postDelayed(this,30000);return;}
   if(web!=null)web.evaluateJavascript("window.FileportDisconnectIfIdle && window.FileportDisconnectIfIdle()",null);
   closeTransport();stopLanPresence();hideConnectedNotification();
 }};
 private static volatile MainActivity activeActivity;
 private static final String CONNECTED_CHANNEL="fileport_connection";
 private static final String DISCONNECT_ACTION="com.yakin.app.FILEPORT_DISCONNECT";
 public static class DisconnectReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context,Intent intent){
   if(intent==null||!DISCONNECT_ACTION.equals(intent.getAction()))return;
   MainActivity activity=activeActivity;
   if(activity!=null){activity.runOnUiThread(()->{
    activity.closeTransport();activity.hideConnectedNotification();
    if(activity.web!=null)activity.web.evaluateJavascript(
     "window.FileportConnection && FileportConnection.disconnect()",null);
   });}
   NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
   if(manager!=null)manager.cancel(208);
  }
 }
 private void showConnectedNotification(String computer,boolean turkish){
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(manager==null)return;
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CONNECTED_CHANNEL,"Fileport connection",NotificationManager.IMPORTANCE_LOW));
  Intent open=new Intent(this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent openIntent=PendingIntent.getActivity(this,208,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Intent stop=new Intent(this,DisconnectReceiver.class).setAction(DISCONNECT_ACTION);
  PendingIntent stopIntent=PendingIntent.getBroadcast(this,209,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CONNECTED_CHANNEL):new Notification.Builder(this);
  String safe=computer==null?"Computer":computer.replaceAll("[\\p{Cntrl}]","");
  b.setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Fileport")
   .setContentText(turkish?safe+" cihazıyla bağlantı kuruldu":"Connected to "+safe).setOngoing(true).setContentIntent(openIntent)
   .addAction(android.R.drawable.ic_menu_close_clear_cancel,turkish?"Kapat":"Disconnect",stopIntent);
  manager.notify(208,b.build());
 }
 private void hideConnectedNotification(){
  NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(manager!=null)manager.cancel(208);
 }
 private void notifyIncoming(String filename){
   if(!backgrounded)return;
   if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
   NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
   if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("yakin_files","Received files",NotificationManager.IMPORTANCE_DEFAULT));
   Intent intent=new Intent(this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   PendingIntent pi=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
   Notification.Builder b=Build.VERSION.SDK_INT>=26 ? new Notification.Builder(this,"yakin_files") : new Notification.Builder(this);
   b.setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Fileport").setContentText("File received: "+filename).setContentIntent(pi).setAutoCancel(true);
   nm.notify(207,b.build());
 }
 @Override protected void onStop(){super.onStop();backgrounded=true;idleHandler.removeCallbacks(idleClose);idleHandler.postDelayed(idleClose,BACKGROUND_GRACE_MS);}
 @Override protected void onStart(){
  super.onStart();backgrounded=false;idleHandler.removeCallbacks(idleClose);startLanPresence();
  // PC-led reconnection: replay the latest waiting request after foregrounding.
  // It still must pass the JavaScript ECDSA signature and freshness checks.
  String queued=pendingLanRequest;
  if(queued!=null && System.currentTimeMillis()-pendingLanAt<25000){
   pendingLanRequest=null;
   idleHandler.postDelayed(()->{if(web!=null)web.evaluateJavascript(queued,null);},400);
  }
 }

 private FrameLayout rootContainer;
 private static final int LIGHT_BAR = Color.rgb(245,247,251);
 private static final int DARK_BAR = Color.rgb(16,23,34);
 private void applySystemBars(boolean dark) {
   int color = dark ? DARK_BAR : LIGHT_BAR;
   getWindow().setStatusBarColor(color);
   getWindow().setNavigationBarColor(color);
   if (Build.VERSION.SDK_INT >= 29) {
     getWindow().setStatusBarContrastEnforced(false);
     getWindow().setNavigationBarContrastEnforced(false);
   }
   View decor = getWindow().getDecorView();
   int flags = decor.getSystemUiVisibility();
   flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
   if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
   decor.setSystemUiVisibility(flags);
   decor.setBackgroundColor(color);
   if (rootContainer != null) rootContainer.setBackgroundColor(color);
 }
 private boolean nativeDarkTheme() {
   String mode = getPreferences(MODE_PRIVATE).getString("yakin_theme", "system");
   if ("dark".equals(mode)) return true;
   if ("light".equals(mode)) return false;
   return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
 }
 @Override public void onConfigurationChanged(Configuration newConfig) {
   super.onConfigurationChanged(newConfig);
   if ("system".equals(getPreferences(MODE_PRIVATE).getString("yakin_theme", "system")))
     applySystemBars(nativeDarkTheme());
 }

 private ValueCallback<Uri[]> pending;
 private final Map<String, FileOutputStream> downloads = new HashMap<>();
 private final Map<String, ParcelFileDescriptor> openDescriptors=new HashMap<>();
 private final Map<String,Long> expectedDownloads = new HashMap<>();
 private final Map<String,Long> receivedDownloads = new HashMap<>();
 private static final long MAX_RECEIVE_BYTES=1024L*1024*1024;
 private static final int MAX_CHUNK_BYTES=65536;
 private final Bridge bridge=new Bridge();
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  web = new WebView(this);
  web.setOverScrollMode(View.OVER_SCROLL_NEVER);
  web.setVerticalScrollBarEnabled(false);
  web.setHorizontalScrollBarEnabled(false);
  FrameLayout container = new FrameLayout(this);
  rootContainer = container;
  container.addView(web, new FrameLayout.LayoutParams(-1, -1));
  container.setFitsSystemWindows(false);
  container.setOnApplyWindowInsetsListener((view, insets) -> {
    int top, bottom;
    if (Build.VERSION.SDK_INT >= 30) {
      android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
      top = bars.top; bottom = bars.bottom;
    } else {
      top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
    }
    view.setPadding(0, top, 0, bottom);
    return insets;
  });
  applySystemBars(nativeDarkTheme());
  setContentView(container);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(true);
  web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.getSettings().setAllowFileAccessFromFileURLs(false);
  web.getSettings().setAllowUniversalAccessFromFileURLs(false);
  web.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
  web.getSettings().setSupportMultipleWindows(false);
  if(Build.VERSION.SDK_INT>=26)web.getSettings().setSafeBrowsingEnabled(true);
  WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
      .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClientCompat() {
   @Override public WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
    Uri url=request.getUrl();
    if ("https".equals(url.getScheme()) && "appassets.androidplatform.net".equals(url.getHost())
         && url.getPath()!=null && url.getPath().startsWith("/assets/"))
      return loader.shouldInterceptRequest(url);
    return new WebResourceResponse("text/plain", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
    Uri url=request.getUrl();
    return !("https".equals(url.getScheme()) && "appassets.androidplatform.net".equals(url.getHost())
         && url.getPath()!=null && url.getPath().startsWith("/assets/"));
   }
  });
  web.setWebChromeClient(new WebChromeClient() {
   @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> callback, FileChooserParams params) {
    if(pending!=null) pending.onReceiveValue(null);
    pending=callback;
    try { startActivityForResult(params.createIntent(),100); return true; }
    catch(Exception e){pending.onReceiveValue(null);pending=null;return false;}
   }
  });
  web.addJavascriptInterface(bridge,"FileportNative");
  web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
  activeActivity=this;startLanPresence();requestDiscoveryPermissions();
 }
 @Override protected void onActivityResult(int request,int result,Intent data) {
  super.onActivityResult(request,result,data);
  if(request==441 && result==RESULT_OK && data!=null && data.getData()!=null) {
   verifyAndInstallUpdate(data.getData());return;
  }
  if(request==201) {
   // The hosted file upload feature has been removed.
   return;
  }
  if(request==100 && pending!=null) {
   pending.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));pending=null;
  }
 }

 private void requestDiscoveryPermissions(){
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
   requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},321);
 }
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){
  super.onRequestPermissionsResult(code,permissions,grants);
 }
 @Override protected void onDestroy(){
   stopLanPresence();closeTransport();hideConnectedNotification();if(activeActivity==this)activeActivity=null;
   synchronized(bridge){for(String id:new ArrayList<>(downloads.keySet()))bridge.pauseDownload(id);}
   idleHandler.removeCallbacks(idleClose);
   super.onDestroy();
 }
 // UDP messages only wake the application. WebCrypto verifies the Windows
 // ECDSA signature and pinned identity before FPTP TCP is initiated.
 private volatile boolean lanActive=false;
 private volatile String lanId="";
 private volatile String lanName="Android phone";
 private volatile DatagramSocket lanRequestSocket;
 private void setLanIdentity(String id,String name){
  if(!id.matches("[a-f0-9]{64}"))return;
  lanId=id;
  lanName=name.replaceAll("[\\p{Cntrl}\\|]", " ").trim();
  if(lanName.isEmpty())lanName="Android phone";
  if(lanName.length()>32)lanName=lanName.substring(0,32);
 }
 private synchronized void stopLanPresence(){
  lanActive=false;
  if(lanRequestSocket!=null){lanRequestSocket.close();lanRequestSocket=null;}
 }
 private synchronized void startLanPresence(){
  if(lanActive)return;
  lanActive=true;
  new Thread(()->{
   try(DatagramSocket out=new DatagramSocket()){
    out.setBroadcast(true);
    while(lanActive){
     String id=lanId;
     if(id.matches("[a-f0-9]{64}")){
      String payload="FILEPORT_PHONE_V1|"+id+"|"+lanName;
      byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);
      DatagramPacket p=new DatagramPacket(bytes,bytes.length,InetAddress.getByName("255.255.255.255"),38478);
      try{out.send(p);}catch(Exception error){android.util.Log.w("FileportLan","Announcement failed",error);}
     }
     try{Thread.sleep(2000);}catch(InterruptedException e){break;}
    }
   }catch(Exception error){android.util.Log.w("FileportLan","Announcement unavailable",error);}
  },"fileport-lan-advertiser").start();
  new Thread(()->{
   try(DatagramSocket server=new DatagramSocket(null)){
    server.setReuseAddress(true);
    server.bind(new java.net.InetSocketAddress("0.0.0.0",38479));
    server.setSoTimeout(1000);lanRequestSocket=server;
    byte[] buf=new byte[2048];
    while(lanActive){
     DatagramPacket p=new DatagramPacket(buf,buf.length);
     try{server.receive(p);}catch(SocketTimeoutException error){continue;}
     if(!p.getAddress().isSiteLocalAddress()&&!p.getAddress().isLinkLocalAddress())continue;
     if(!(p.getAddress() instanceof java.net.Inet4Address))continue;
     String payload=new String(p.getData(),0,p.getLength(),StandardCharsets.UTF_8);
     try{JSONObject obj=new JSONObject(payload);
      if(obj.optInt("v")!=1||!"connect-request".equals(obj.optString("kind")))continue;
      String js="window.FileportLan && FileportLan.onRequest("+obj.toString()+","+JSONObject.quote(p.getAddress().getHostAddress())+")";
      if(backgrounded){pendingLanRequest=js;pendingLanAt=System.currentTimeMillis();}
      // Attempt verification now (works on some backgrounded WebViews), and
      // replay the freshest request on returning to foreground if JS was paused.
      runOnUiThread(()->{if(web!=null)web.evaluateJavascript(js,null);});
     }catch(Exception ignored){}
    }
   }catch(Exception error){if(lanActive)android.util.Log.w("FileportLan","Request listener unavailable",error);}
   finally{lanRequestSocket=null;}
  },"fileport-lan-request-listener").start();
 }
 // FPTP/1 uses length-prefixed frames; the Java socket has no access to
 // decrypted messages. Authentication and AES-GCM run in the WebCrypto codec.
 private final Object transportLock=new Object();
 private Socket transportSocket;
 private DataOutputStream transportWriter;
 private volatile int transportGeneration=0;
 private void transportEvent(String action,String value){
   String js="window.FileportTransport && FileportTransport."+action+"("+JSONObject.quote(value)+")";
   runOnUiThread(()->{if(web!=null)web.evaluateJavascript(js,null);});
 }
 private void closeTransport(){
  synchronized(transportLock){transportGeneration++;
   if(transportSocket!=null)try{transportSocket.close();}catch(Exception ignored){}
   transportSocket=null;transportWriter=null;
  }
 }
 private void connectTransport(String address,int port){
  if(port!=38477||address==null||!address.matches("[0-9.]{7,15}")){transportEvent("onError","Invalid LAN address");return;}
  closeTransport();final int generation=transportGeneration;
  new Thread(()->{
   try{
    InetAddress target=InetAddress.getByName(address);
    if(!(target.isSiteLocalAddress()||target.isLinkLocalAddress())){transportEvent("onError","Not a local network address");return;}
    Socket socket=new Socket();socket.connect(new java.net.InetSocketAddress(target,port),4000);
    socket.setSoTimeout(16000);socket.setTcpNoDelay(true);
    synchronized(transportLock){if(generation!=transportGeneration){socket.close();return;}
     transportSocket=socket;transportWriter=new DataOutputStream(socket.getOutputStream());}
    transportEvent("onOpen","");
    new Thread(()->{
     final byte[] keepalive="{\"v\":1,\"t\":\"keepalive\"}".getBytes(StandardCharsets.UTF_8);
     while(generation==transportGeneration){
      try{Thread.sleep(4000);}catch(InterruptedException ignored){break;}
      synchronized(transportLock){
       if(generation!=transportGeneration||transportWriter==null)break;
       try{transportWriter.writeInt(keepalive.length);transportWriter.write(keepalive);transportWriter.flush();}
       catch(Exception error){try{if(transportSocket!=null)transportSocket.close();}catch(Exception ignored){}break;}
      }
     }
    },"fileport-native-keepalive").start();
    DataInputStream input=new DataInputStream(socket.getInputStream());
    while(generation==transportGeneration&&!socket.isClosed()){
     int size=input.readInt();if(size<1||size>196608)throw new java.io.IOException("Invalid frame length");
     byte[] data=new byte[size];input.readFully(data);
     transportEvent("onFrame",new String(data,StandardCharsets.UTF_8));
    }
   }catch(Exception ex){if(generation==transportGeneration)transportEvent("onError","FPTP transport: "+ex.getClass().getSimpleName());}
   finally{synchronized(transportLock){if(generation==transportGeneration){
    if(transportSocket!=null)try{transportSocket.close();}catch(Exception ignored){}
    transportSocket=null;transportWriter=null;transportEvent("onClosed","");
   }}}
  },"fileport-fptp-transport").start();
 }
 private void updateStatus(String message){
  runOnUiThread(()->web.evaluateJavascript("window.fileportUpdateStatus("+JSONObject.quote(message)+")",null));
 }
 private void selectLocalUpdate(){runOnUiThread(()->{
  try{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
   pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("application/vnd.android.package-archive");
   pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(pick,441);
  }catch(Exception e){updateStatus("Cannot open APK picker: "+e.getMessage());}
 });}
 private void verifyAndInstallUpdate(Uri uri){
  new Thread(()->{
   java.io.File temp=null;
   try{
    temp=new java.io.File(getCacheDir(),"fileport-selected-update.apk");
    try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(temp)){
     if(in==null)throw new Exception("Cannot read APK");byte[] buffer=new byte[65536];int n;long total=0;
     while((n=in.read(buffer))!=-1){total+=n;if(total>300L*1024*1024)throw new Exception("APK exceeds 300 MB");out.write(buffer,0,n);}
    }
    int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
    PackageInfo apk=getPackageManager().getPackageArchiveInfo(temp.getAbsolutePath(),flags);
    PackageInfo installed=getPackageManager().getPackageInfo(getPackageName(),flags);
    if(apk==null||!getPackageName().equals(apk.packageName))throw new Exception("Not a Fileport APK");
    long next=Build.VERSION.SDK_INT>=28?apk.getLongVersionCode():apk.versionCode;
    long old=Build.VERSION.SDK_INT>=28?installed.getLongVersionCode():installed.versionCode;
    if(next<=old)throw new Exception("Select a newer Fileport version");
    Signature[] incoming=Build.VERSION.SDK_INT>=28?apk.signingInfo.getApkContentsSigners():apk.signatures;
    Signature[] current=Build.VERSION.SDK_INT>=28?installed.signingInfo.getApkContentsSigners():installed.signatures;
    if(incoming==null||current==null||incoming.length!=current.length||incoming.length==0)throw new Exception("APK signature mismatch");
    java.util.HashSet<String> known=new java.util.HashSet<>();for(Signature sig:current)known.add(sig.toCharsString());
    for(Signature sig:incoming)if(!known.contains(sig.toCharsString()))throw new Exception("APK signature mismatch");
    updateStatus("APK verified. Android will request installation approval.");
    // Use original document URI so the system package installer can read it.
    runOnUiThread(()->{
     try{
      if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
       updateStatus("Allow Fileport to install apps, then select the APK again.");
       Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));startActivity(settings);return;
      }
      Intent install=new Intent(Intent.ACTION_VIEW);
      install.setDataAndType(uri,"application/vnd.android.package-archive");
      install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(install);
     }catch(Exception e){updateStatus("Installer error: "+e.getMessage());}
    });
   }catch(Exception e){updateStatus("Update rejected: "+e.getMessage());}
   finally{if(temp!=null)temp.delete();}
  }).start();
 }
 class Bridge {
  @JavascriptInterface public void selectUpdateApk(){selectLocalUpdate();}
  @JavascriptInterface public String getDeviceName(){
   String name=null;
   try{name=Settings.Global.getString(getContentResolver(),"device_name");}catch(Exception ignored){}
   if(name==null||name.trim().isEmpty())name=Build.MODEL==null?"Android phone":Build.MODEL.trim();
   name=name.replaceAll("[\\p{Cntrl}\\|]"," ").trim();
   return name.length()>32?name.substring(0,32):name;
  }
  @JavascriptInterface public void setLanIdentity(String id,String name){
   if(id==null||name==null)return;MainActivity.this.setLanIdentity(id,name);
  }
  @JavascriptInterface public void showConnectedNotification(String computer,boolean turkish){
   runOnUiThread(()->MainActivity.this.showConnectedNotification(computer,turkish));
  }
  @JavascriptInterface public void hideConnectedNotification(){
   runOnUiThread(()->MainActivity.this.hideConnectedNotification());
  }

  @JavascriptInterface public void connectTransport(String address,int port){MainActivity.this.connectTransport(address,port);}
  @JavascriptInterface public void transportSend(String frame){
   if(frame==null||frame.length()>196608)return;
   try{
    JSONObject object=new JSONObject(frame);String type=object.optString("t","");
    if(!"hello".equals(type)&&!"sealed".equals(type)&&!"keepalive".equals(type))return;
    byte[] bytes=frame.getBytes(StandardCharsets.UTF_8);
    synchronized(transportLock){if(transportWriter==null)return;
     transportWriter.writeInt(bytes.length);transportWriter.write(bytes);transportWriter.flush();}
   }catch(Exception err){android.util.Log.w("Fileport","FPTP send error",err);closeTransport();}
  }
  @JavascriptInterface public void disconnectTransport(){closeTransport();}

  @JavascriptInterface public void setTransferActive(boolean active){transferActive=active;}

  @JavascriptInterface public void setTheme(String mode) {
    if (!"dark".equals(mode) && !"light".equals(mode)) mode = "system";
    final String selected = mode;
    getPreferences(MODE_PRIVATE).edit().putString("yakin_theme", selected).apply();
    runOnUiThread(() -> applySystemBars(nativeDarkTheme()));
  }

  // Fileport partial transfers persist across network loss and process restart.
  // Only a trusted JavaScript session may invoke this WebView bridge.
  private String metaKey(String transferId){return "fptp_"+transferId;}
  @JavascriptInterface public synchronized String beginDownload(String filename,String mime,long expectedSize,String transferId){
   try{
    if(transferId==null||!transferId.matches("[a-f0-9]{32}"))throw new Exception("Invalid transfer ID");
    if(expectedSize<0||expectedSize>MAX_RECEIVE_BYTES)throw new Exception("Invalid file size");
    if(downloads.size()>=2&&!downloads.containsKey(transferId))throw new Exception("Too many simultaneous files");
    String safe=filename==null?"":filename.replaceAll("[\\/<>:\"|?*\\p{Cntrl}]","_").trim();
    if(safe.length()>150)safe=safe.substring(0,150);
    if(safe.isEmpty()||safe.equals(".")||safe.equals(".."))throw new Exception("Invalid filename");
    String metadata=getPreferences(MODE_PRIVATE).getString(metaKey(transferId),null);
    if(downloads.containsKey(transferId)){
      if(expectedDownloads.get(transferId)!=expectedSize)throw new Exception("Transfer ID collision");
      return String.valueOf(receivedDownloads.get(transferId));
    }
    Uri uri=null;boolean reused=false;
    if(metadata!=null){
     JSONObject rec=new JSONObject(metadata);
     if(rec.optLong("size",-1)!=expectedSize||!safe.equals(rec.optString("name")))throw new Exception("Transfer metadata mismatch");
     uri=Uri.parse(rec.getString("uri"));reused=true;
    }else{
     android.os.StatFs stats=new android.os.StatFs(getFilesDir().getAbsolutePath());
     if(stats.getAvailableBytes()<Math.min(expectedSize,64L*1024*1024)+16L*1024*1024)throw new Exception("Insufficient storage");
     ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,safe);
     values.put(MediaStore.Downloads.MIME_TYPE,(mime!=null&&mime.matches("[A-Za-z0-9.+_-]+/[A-Za-z0-9.+_-]+"))?mime:"application/octet-stream");
     values.put(MediaStore.Downloads.IS_PENDING,1);
     uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
     if(uri==null)throw new Exception("Cannot create file");
    }
    ParcelFileDescriptor fd=null;FileOutputStream stream=null;
    try{
     fd=getContentResolver().openFileDescriptor(uri,"rw");
     if(fd==null)throw new Exception("Cannot open output file");
     stream=new FileOutputStream(fd.getFileDescriptor());
     long offset=stream.getChannel().size();
     if(offset>expectedSize)throw new Exception("Partial file larger than expected");
     stream.getChannel().position(offset);
     downloads.put(transferId,stream);openDescriptors.put(transferId,fd);
     uris.put(transferId,uri);expectedDownloads.put(transferId,expectedSize);receivedDownloads.put(transferId,offset);
     if(!reused){JSONObject record=new JSONObject();record.put("uri",uri.toString());record.put("size",expectedSize);record.put("name",safe);
       getPreferences(MODE_PRIVATE).edit().putString(metaKey(transferId),record.toString()).apply();}
     return String.valueOf(offset);
    }catch(Exception ex){if(stream!=null)try{stream.close();}catch(Exception ignored){}
     if(fd!=null)try{fd.close();}catch(Exception ignored){}
     if(!reused&&uri!=null)getContentResolver().delete(uri,null,null);throw ex;}
   }catch(Exception e){throw new RuntimeException(e);}
  }
  @JavascriptInterface public synchronized long appendDownload(String id,long offset,String base64){
   try{
    FileOutputStream out=downloads.get(id);
    if(out==null||base64==null||base64.length()>90000)throw new Exception("Invalid chunk");
    byte[] data=Base64.decode(base64,Base64.DEFAULT);
    if(data.length>MAX_CHUNK_BYTES)throw new Exception("Chunk too large");
    long received=receivedDownloads.getOrDefault(id,-1L);
    long expected=expectedDownloads.getOrDefault(id,-1L);
    if(received!=offset||expected<0||received+data.length>expected)throw new Exception("Invalid chunk offset");
    out.write(data);receivedDownloads.put(id,received+data.length);return received+data.length;
   }catch(Exception e){throw new RuntimeException(e);}
  }
  @JavascriptInterface public synchronized long syncDownload(String id){
   try{FileOutputStream out=downloads.get(id);if(out==null)throw new Exception("No transfer");
    out.getFD().sync();return receivedDownloads.getOrDefault(id,-1L);
   }catch(Exception e){throw new RuntimeException(e);}
  }
  @JavascriptInterface public synchronized boolean verifyDownload(String id,String expected){
   try{
    if(expected==null||!expected.matches("[a-fA-F0-9]{64}"))return false;
    FileOutputStream out=downloads.get(id);Uri uri=uris.get(id);
    if(out==null||uri==null||!receivedDownloads.getOrDefault(id,-1L).equals(expectedDownloads.getOrDefault(id,-2L)))return false;
    out.getFD().sync();MessageDigest sha=MessageDigest.getInstance("SHA-256");
    try(InputStream input=getContentResolver().openInputStream(uri)){
     if(input==null)return false;
     byte[] buffer=new byte[65536];int count;
     while((count=input.read(buffer))!=-1)sha.update(buffer,0,count);
    }
    StringBuilder value=new StringBuilder();for(byte b:sha.digest())value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
    return value.toString().equalsIgnoreCase(expected);
   }catch(Exception error){android.util.Log.w("Fileport","SHA-256 verification failed",error);return false;}
  }
  @JavascriptInterface public synchronized void pauseDownload(String id){
   FileOutputStream out=downloads.remove(id);ParcelFileDescriptor fd=openDescriptors.remove(id);
   if(out!=null)try{out.getFD().sync();out.close();}catch(Exception ignored){}
   if(fd!=null)try{fd.close();}catch(Exception ignored){}
   expectedDownloads.remove(id);receivedDownloads.remove(id);uris.remove(id);
  }
  @JavascriptInterface public synchronized void abortDownload(String id){
   Uri uri=uris.get(id);if(uri==null){String meta=getPreferences(MODE_PRIVATE).getString(metaKey(id),null);
    try{if(meta!=null)uri=Uri.parse(new JSONObject(meta).getString("uri"));}catch(Exception ignored){}}
   pauseDownload(id);
   if(uri!=null)try{getContentResolver().delete(uri,null,null);}catch(Exception ignored){}
   getPreferences(MODE_PRIVATE).edit().remove(metaKey(id)).apply();
  }
  @JavascriptInterface public synchronized void finishDownload(String id){
   Uri uri=uris.get(id);
   try{
    if(uri==null||!receivedDownloads.getOrDefault(id,-1L).equals(expectedDownloads.getOrDefault(id,-2L)))throw new Exception("Transfer incomplete");
    syncDownload(id);pauseDownload(id);
    ContentValues values=new ContentValues();values.put(MediaStore.Downloads.IS_PENDING,0);
    if(getContentResolver().update(uri,values,null,null)<=0)throw new Exception("Cannot publish received file");
    getPreferences(MODE_PRIVATE).edit().remove(metaKey(id)).apply();
    runOnUiThread(()->notifyIncoming("File saved to Downloads"));
   }catch(Exception error){abortDownload(id);throw new RuntimeException(error);}
  }

 }
 private final Map<String,Uri> uris=new HashMap<>();
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
