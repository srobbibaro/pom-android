package com.originalcontentsoftware.pomandroid;

import android.accounts.*;
import android.app.*;
import android.content.*;
import android.content.*;
import android.location.*;
import android.net.*;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.*;
import android.util.Log;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.MetaData;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.Geofence.Builder;
import com.originalcontentsoftware.pomandroid.R;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.json.*;

public class PomUtil {
  public final static boolean DEBUG_ENABLED = false;

  public static boolean hasNetworkConnection(Context context) {
    ConnectivityManager cm =
      (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    return netInfo != null && netInfo.isConnectedOrConnecting();
  }

  public static void debug(String message) {
    performDebug(message, false);
  }

  public static void debug(String message, boolean includeBugSnag) {
    performDebug(message, includeBugSnag);
  }

  public static Intent buildNotificationIntent(Context context, String title, String message, String notificationMessage, String action) {
    final Intent intent = new Intent(context, NotificationsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra("title", title);
    intent.putExtra("message", message);
    intent.putExtra("action", action);
    intent.putExtra("notifications_message", notificationMessage);
    return intent;
  }

  public static void buildNotification(Context context, Intent intent, int id, int icon) {
    final PendingIntent pi =
      PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    final String message = intent.getStringExtra("message");
    final String title = intent.getStringExtra("title");

    final NotificationCompat.Builder mBuilder =
      new NotificationCompat.Builder(context)
      .setSmallIcon(icon)
      .setContentTitle(title)
      .setContentIntent(pi)
      .setContentText(message);
    mBuilder.setAutoCancel(true);

    final NotificationManager mNotificationManager =
      (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(id, mBuilder.build());
  }

  public static void notifyLocationMatch(Context context, Intent intent) {
    buildNotification(context, intent, 12345, R.drawable.ic_launcher_pom_logo);
  }

  public static void notifyLocationSearchTimeout(Context context, Intent intent, PomLocation location) {
    intent.putExtra("locationName", location.getNearGeofenceName());
    buildNotification(context, intent, 12345, R.drawable.ic_pom_logo_error);
  }

  public static void notifyError(Context context, Intent intent, String exception) {
    final String message = intent.getStringExtra("message");

    buildNotification(context, intent, 67890, R.drawable.ic_pom_logo_error);
    debug("Error! " + message + " - " + exception);

    Bugsnag.notify(new RuntimeException(message + " - " + exception));
  }

  public static void notifyWarning(Context context, Intent intent) {
    buildNotification(context, intent, 45678, R.drawable.ic_pom_logo_error);
  }

  private static Intent launchIntent(Context context) {
    return new Intent(context, MainMenuActivity.class);
  }

  public static String buildLocationString(Location l) {
    return buildLocationString(
      l.getLatitude(), l.getLongitude(), l.getAccuracy(), l.getSpeed()
    );
  }

  public static String buildLocationString(double latitude, double longitude, float accuracy, float speed) {
    return
      String.format("%.8f", latitude)  + "," +
      String.format("%.8f", longitude) + "," +
      String.format("%.2f", accuracy)  + "m," +
      String.format("%.2f", speed)     + "m/s";
  }

  public static String getAuthToken(Context context) {
    PomUtil.debug("Fetching auth token...");

    String token = null;

    final AccountManager accountManager = AccountManager.get(context);
    Account[] accounts = accountManager.getAccountsByType(getAccountName(context));
    if (accounts.length > 0) {
      // TODO: Don't use peek here
      token = accountManager.peekAuthToken(accounts[0], getAccountName(context));
      PomUtil.debug("Found account and token.");
    }
    else {
      PomUtil.debug("Error - Could not find account and token.");
    }

    return token;
  }

  public static String getAccountName(Context context) {
    return context.getResources().getString(R.string.account_type);
  }

  public static void logToFile(String message) {
    final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), LOG_FILE_NAME);

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));

      Calendar c = Calendar.getInstance();
      SimpleDateFormat df  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String formattedDate = df.format(c.getTime());

      bw.write(formattedDate + " | " + message);
      bw.newLine();

      bw.flush();
      bw.close();
    }
    catch (Exception e) {
      Log.v(LOG_TAG, "Could not open file '" + LOG_FILE_NAME + "' for write!" + e.getMessage());
    }
  }

  public static void truncateLogFile() {
    final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), LOG_FILE_NAME);

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(file));
      bw.close();
    }
    catch (Exception e) {
      Log.v(LOG_TAG, "Could not truncate file '" + LOG_FILE_NAME + "'." + e.getMessage());
    }
  }

  public static Intent buildEmailLogFileIntent() {
    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "POM Debug Log");

    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "POM Debug log file attached.");

    final File file =
      new File(Environment.getExternalStorageDirectory().getAbsolutePath(), LOG_FILE_NAME);
    emailIntent.setType("application/octet-stream");
    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

    return emailIntent;
  }

  public static Intent buildOpenLogFileIntent() {
    final File file = new File(Environment.getExternalStorageDirectory()
        .getAbsolutePath(), LOG_FILE_NAME);

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(Uri.fromFile(file), "text/plain");

    return intent;
  }

  private static void performDebug(String message, boolean includeBugSnag) {
    if (DEBUG_ENABLED) {
      Log.d(LOG_TAG, message);
      logToFile(message);
    }

    if (includeBugSnag) {
      Bugsnag.notify(new RuntimeException(message));
    }
    else {
      Bugsnag.leaveBreadcrumb(message);
    }
  }

  private final static String LOG_TAG       = "POM";
  private final static String LOG_FILE_NAME = "pom_debug.log";
}
