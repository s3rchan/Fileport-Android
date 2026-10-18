package com.fileport.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

/**
 * Time-bounded foreground service protecting local device connectivity while
 * Fileport is backgrounded. Encryption/consent remain in the Activity's WebView.
 * No background connection is attempted after the idle grace period expires.
 */
public final class BackgroundConnectionService extends Service {
  public static final String ACTION_START = "com.yakin.app.BACKGROUND_START";
  public static final String ACTION_TRANSFER = "com.yakin.app.BACKGROUND_TRANSFER";
  public static final String ACTION_STOP = "com.yakin.app.BACKGROUND_STOP";
  public static final String EXTRA_ACTIVE = "transfer_active";
  private static final String CHANNEL = "fileport_background_connection";
  private static final int NOTIFICATION_ID = 309;
  private static final long IDLE_MS = 180_000L;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean transferActive;
  private long idleDeadline;
  private boolean inForeground;
  private PowerManager.WakeLock transferWakeLock;
  private final Runnable refreshTransferWakeLock = new Runnable() {
    @Override public void run() {
      if (!transferActive || transferWakeLock == null) return;
      transferWakeLock.acquire(15 * 60_000L);
      handler.postDelayed(this, 10 * 60_000L);
    }
  };
  private void manageTransferWakeLock() {
    handler.removeCallbacks(refreshTransferWakeLock);
    if (transferWakeLock == null) return;
    if (transferActive) {
      transferWakeLock.acquire(15 * 60_000L);
      handler.postDelayed(refreshTransferWakeLock,10 * 60_000L);
    } else if (transferWakeLock.isHeld()) {
      transferWakeLock.release();
    }
  }
  private final Runnable expire = new Runnable() {
    @Override public void run() {
      if (transferActive) return;
      long remaining = idleDeadline - SystemClock.elapsedRealtime();
      if (remaining > 0) {
        handler.postDelayed(this, remaining);
        return;
      }
      MainActivity.expireBackgroundSession();
      stopForegroundCompat();
      stopSelf();
    }
  };

  @Override public void onCreate() {
    super.onCreate();
    PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
    if(pm!=null){transferWakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Fileport:ActiveFileTransfer");transferWakeLock.setReferenceCounted(false);}
    NotificationManager nm = getSystemService(NotificationManager.class);
    if (nm != null && Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel = new NotificationChannel(CHANNEL,
          "Fileport background connection", NotificationManager.IMPORTANCE_LOW);
      channel.setDescription("Local connectivity and ongoing file transfers.");
      nm.createNotificationChannel(channel);
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    String action = intent != null ? intent.getAction() : ACTION_STOP;
    if (ACTION_STOP.equals(action) || intent == null) {
      handler.removeCallbacks(expire);
      stopForegroundCompat();
      stopSelf();
      return START_NOT_STICKY;
    }
    transferActive = intent.getBooleanExtra(EXTRA_ACTIVE, transferActive);
    manageTransferWakeLock();
    if (!inForeground) {
      Intent open = new Intent(this, MainActivity.class);
      open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      PendingIntent pending = PendingIntent.getActivity(this, 310, open,
          PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      Notification notification = new Notification.Builder(this, CHANNEL)
          .setSmallIcon(android.R.drawable.stat_sys_upload)
          .setContentTitle("Fileport")
          .setContentText(transferActive ? "File transfer in progress" : "Connected in background for up to 3 minutes")
          .setContentIntent(pending)
          .setOngoing(true)
          .build();
      if (Build.VERSION.SDK_INT >= 29) {
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
      } else {
        startForeground(NOTIFICATION_ID, notification);
      }
      inForeground = true;
    }
    if(inForeground && ACTION_TRANSFER.equals(action)){
      NotificationManager nm=getSystemService(NotificationManager.class);
      if(nm!=null){Notification notify=new Notification.Builder(this,CHANNEL)
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setContentTitle("Fileport")
        .setContentText(transferActive?"File transfer in progress":"Background connection: up to 3 minutes remaining")
        .setOngoing(true).build();
        nm.notify(NOTIFICATION_ID,notify);
      }
    }
    handler.removeCallbacks(expire);
    if (transferActive) {
      idleDeadline = 0;
    } else {
      // The clock starts when the app backgrounds or when a transfer finishes.
      idleDeadline = SystemClock.elapsedRealtime() + IDLE_MS;
      handler.postDelayed(expire, IDLE_MS);
    }
    return START_NOT_STICKY;
  }

  private void stopForegroundCompat() {
    if (!inForeground) return;
    if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
    else stopForeground(true);
    inForeground = false;
  }
  @Override public void onDestroy() {
    handler.removeCallbacks(expire);
    handler.removeCallbacks(refreshTransferWakeLock);
    if(transferWakeLock!=null&&transferWakeLock.isHeld())transferWakeLock.release();
    super.onDestroy();
  }
  @Override public IBinder onBind(Intent intent) {return null;}
}
