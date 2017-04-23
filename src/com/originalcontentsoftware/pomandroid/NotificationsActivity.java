package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.app.*;
import android.accounts.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.net.*;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.app.NotificationCompat.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.MetaData;

public class NotificationsActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Bugsnag.init(this);
    PomUtil.debug("Starting app activity...");

    super.onCreate(savedInstanceState);

    setContentView(R.layout.notifications);

    setupCallbacks();

    final Intent intent = getIntent();

    if (intent != null) {
      final String message = intent.getStringExtra("notifications_message");
      final String action = intent.getStringExtra("action");

      PomUtil.debug("message=" + message);
      PomUtil.debug("action=" + action);

      if (message != null) {
        TextView editText = (TextView)findViewById(R.id.notificationsMessageText);
        editText.setText(message);
      }

      final Button actionButton = (Button) findViewById(R.id.notificationsActionButton);

      if (action.equals(PomAction.LOCATION_SEARCH_TIMEOUT)) {
        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText("Send Notification Now");
        actionButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final Intent i = new Intent(getApplicationContext(), MainService.class);
            i.setAction(PomAction.TEST_LOCATION);
            i.putExtra("locationName", intent.getStringExtra("locationName"));
            startService(i);
            handleDone();
          }
        });
      }
      else {
        actionButton.setVisibility(View.GONE);
      }
    }

    PomUtil.debug("Finished starting up app activity.");
  }

  private void setupCallbacks() {
    final Button backButton = (Button) findViewById(R.id.notificationsBackButton);
    backButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        handleDone();
      }
    });
  }

  private void handleDone() {
    Intent myIntent = new Intent(getApplicationContext(), MainMenuActivity.class);
    myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    setResult(RESULT_OK, myIntent);
    finish();
  }

  @Override
  public void onBackPressed() {
    handleDone();
  }

  private SharedPreferences getSharedPreferences() {
    final Context context = getApplicationContext();
    return context.getSharedPreferences(
        PomUtil.getAccountName(context), Context.MODE_PRIVATE);
  }
}
