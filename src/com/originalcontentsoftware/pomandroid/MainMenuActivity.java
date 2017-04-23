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
import android.location.LocationManager;
import android.support.v4.content.LocalBroadcastManager;

public class MainMenuActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Bugsnag.init(this);
    PomUtil.debug("Starting app activity...");

    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);
    preferenceManager = new PreferenceManager(getSharedPreferences());

    TextView editText = (TextView)findViewById(R.id.textViewNotice);
    editText.setText(editText.getText().toString() + " - v" + getAppVersion());

    setupCallbacks();
    getAuthToken();

    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(PomAction.SERVICE_ENABLED_UPDATE));
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(PomAction.NO_LOCATIONS_FETCHED_ERROR));

    updateServiceEnabled(isServiceEnabled());
    PomUtil.debug("Finished starting up app activity.");
  }

  @Override
  protected void onDestroy() {
  LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    super.onDestroy();
  }

  private void enableService() {
    if (isGpsEnabled()) {
      launchServiceWithAction(PomAction.START_UP);
      return;
    }

    final AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setMessage(R.string.main_menu_alert_location_not_enabled_message)
      .setPositiveButton(
        R.string.main_menu_alert_location_not_enabled_okay,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface d, int id) {
            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(gpsOptionsIntent, 50);
            d.dismiss();
          }
        }
      )
      .setNegativeButton(
        R.string.main_menu_alert_location_not_enabled_cancel,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface d, int id) {
            final Button enableDisableButton = (Button)findViewById(R.id.locationsEnableDisableButton);
            enableDisableButton.setEnabled(true);
            final Button refreshButton = (Button) findViewById(R.id.locationsRefreshButton);
            refreshButton.setEnabled(false);
            d.cancel();
          }
        }
      );
    builder.create().show();
  }

  private boolean isGpsEnabled() {
    final LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
  }

  private void disableService() {
    launchServiceWithAction(PomAction.HALT);
  }

  private void refreshLocations() {
    if (isServiceEnabled()) {
      launchServiceWithAction(PomAction.REFRESH_LOCATIONS);
    }
  }

  private void setupCallbacks() {
    final Button viewWebButton = (Button) findViewById(R.id.locationsViewWebButton);
    viewWebButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent i = new Intent(Intent.ACTION_VIEW);
        Intent myIntent = new Intent(getApplicationContext(), ManageLocationsActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(myIntent, 100);
      }
    });

    final Button enableDisableButton = (Button) findViewById(R.id.locationsEnableDisableButton);
    enableDisableButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final boolean enabled = isServiceEnabled();
        updateServiceEnabled(enabled);
        enableDisableButton.setEnabled(false);
        if (enabled) {
          disableService();
        }
        else {
          enableService();
        }
      }
    });

    final Button refreshButton = (Button) findViewById(R.id.locationsRefreshButton);
    refreshButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        refreshLocations();
      }
    });
  }

  private void shutdown() {
    finish();
  }

  @Override
  public void onBackPressed() {
    shutdown();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (PomUtil.DEBUG_ENABLED) {
      getMenuInflater().inflate(R.menu.activity_main, menu);

      menu.add(1,6,0, "View Locations");
      menu.add(1,8,0, "View Map");
      menu.add(1,10,0, "Location List");
      menu.add(1,12,0, "Truncate Log File");
      menu.add(1,15,0, "Email Log File");
      menu.add(1,18,0, "View Log File");
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case 6:
        {
          Intent myIntent = new Intent(getApplicationContext(), LocationsActivity.class);
          myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivityForResult(myIntent, 6);
          break;
        }
      case 8:
        {
          Intent myIntent = new Intent(getApplicationContext(), MapActivity.class);
          myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivityForResult(myIntent, 8);
          break;
        }
      case 12:
        {
          PomUtil.truncateLogFile();
          break;
        }
      case 15:
        {
          startActivity(PomUtil.buildEmailLogFileIntent());
          break;
        }
      case 18:
        {
          startActivity(PomUtil.buildOpenLogFileIntent());
          break;
        }
    }
    return super.onOptionsItemSelected(item);
  }

  private void getAuthToken() {
    PomUtil.debug("Fetching auth token...");
    final Context context = getApplicationContext();

    if (PomUtil.getAuthToken(context) == null) {
      PomUtil.debug("Could not find account and token.");
      try {
        final AccountManager accountManager = AccountManager.get(context);
        final String accountName = PomUtil.getAccountName(context);
        Bundle bundle = accountManager.addAccount(
            accountName, accountName, null, null, null, null, null).getResult();

        if (bundle.containsKey(AccountManager.KEY_INTENT)) {
          final Intent authIntent = bundle.getParcelable(AccountManager.KEY_INTENT);
          startActivityForResult(authIntent, 1);
        }
        else {
          PomUtil.debug("Error encountered while adding new account.");
          shutdown();
        }
      }
      catch (Exception e) {
        PomUtil.debug("Error encountered while adding new account: " + e);
        shutdown();
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 1) {
      if (resultCode != RESULT_OK) {
        PomUtil.debug("Error - Could not find account and token.");
        shutdown();
      }
      else {
        PomUtil.debug("New account added successfully");
      }
    }
    else if (requestCode == 50) {
      enableService();
    }
    else if (requestCode == 100) {
      refreshLocations();
    }
  }

  private SharedPreferences getSharedPreferences() {
    final Context context = getApplicationContext();
    return context.getSharedPreferences(
        PomUtil.getAccountName(context), Context.MODE_PRIVATE);
  }

  private boolean isServiceEnabled() {
    return preferenceManager.checkServiceEnabled();
  }

  private String getAppVersion() {
    String version = "0.0";
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pInfo.versionName;
    }
    catch (Exception e) {
      version = "0.0";
    }

    if (PomUtil.DEBUG_ENABLED) {
      version += "-debug";
    }

    return version;
  }

  private void launchServiceWithAction(String action) {
    final Intent i = new Intent(this, MainService.class);
    i.setAction(action);
    startService(i);
  }

  private void updateServiceEnabled(boolean enabled) {
    PomUtil.debug("Service enabled = " + enabled);

    final Button enableDisableButton = (Button)findViewById(R.id.locationsEnableDisableButton);
    enableDisableButton.setText(enabled ? "Disable" : "Enable");
    enableDisableButton.setEnabled(true);
    final Button refreshButton = (Button) findViewById(R.id.locationsRefreshButton);
    refreshButton.setEnabled(enabled);

    final TextView serviceEnabledText = (TextView)findViewById(R.id.serviceEnabledTextView);
    serviceEnabledText.setText("Location reporting is: " + (enabled ? "on" : "off"));
  }

  private void displayNoLocationsAlert() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(R.string.main_menu_alert_no_location_fetched_message)
      .setPositiveButton(
        R.string.main_menu_alert_no_location_fetched_okay,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface d, int id) {
            d.dismiss();
          }
        }
      );
    builder.create().show();
  }

  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      PomUtil.debug("Received broadcast with action: " + action + ". Processing...");

      if (action.equals(PomAction.SERVICE_ENABLED_UPDATE)) {
        updateServiceEnabled(intent.getExtras().getBoolean("service_enabled"));
      }
      else if (action.equals(PomAction.NO_LOCATIONS_FETCHED_ERROR)) {
        displayNoLocationsAlert();
      }
    }
  };

  private PreferenceManager preferenceManager;
}
