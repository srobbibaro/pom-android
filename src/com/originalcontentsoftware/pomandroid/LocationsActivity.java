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
import android.location.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import java.util.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.*;
import android.content.*;
import android.support.v4.content.LocalBroadcastManager;

public class LocationsActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Bugsnag.init(this);
    PomUtil.debug("Starting locations activity...");

    super.onCreate(savedInstanceState);

    setContentView(R.layout.locations);
    preferenceManager = new PreferenceManager(getSharedPreferences());

    final Button refreshButton = (Button) findViewById(R.id.locationsRefreshButton);
    refreshButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        launchServiceWithAction(PomAction.LAST_LOCATION);
      }
    });

    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(PomAction.LAST_LOCATION));
    launchServiceWithAction(PomAction.LAST_LOCATION);
    PomUtil.debug("Finished starting up locations activity.");
  }

  @Override
  protected void onDestroy() {
  LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    super.onDestroy();
  }

  @Override
  public void onBackPressed() {
    finish();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  }

  private void buildLocations(Location lastLocation) {
    LinearLayout linearLayout = (LinearLayout)findViewById(R.id.locationList);
    List <PomLocation> locations = preferenceManager.getLocations();

    if (linearLayout.getChildCount() > 0) {
      linearLayout.removeAllViews();
    }

    String lastLocationStr = lastLocation == null ? "Could not read location" : "Last location: " + PomUtil.buildLocationString(lastLocation);
    lastLocationStr += System.getProperty("line.separator");
    appendText(0, linearLayout, lastLocationStr);

    int id = 1;
    for (PomLocation pomLocation : locations) {
      final float distance = pomLocation.distanceToLocation(lastLocation);
      String locationStr = buildPomLocationStr(pomLocation, distance);
      appendText(id, linearLayout, locationStr);
      appendButton(id++, linearLayout, pomLocation);
    }
  }

  private void appendText(int id, LinearLayout linearLayout, String text) {
    TextView editText = new TextView(this);

    editText.setText(text);
    editText.setTextColor(getResources().getColor(R.color.text));
    editText.setId(id);
    editText.setLayoutParams(
      new LayoutParams(
        LayoutParams.FILL_PARENT,
        LayoutParams.WRAP_CONTENT
      )
    );

    linearLayout.addView(editText);
  }

  private void appendButton(int id, LinearLayout linearLayout, final PomLocation location) {
    final Button button = (Button) new Button(this);
    button.setText("Notify");
    button.setId(id);
    button.setLayoutParams(
      new LayoutParams(
        LayoutParams.FILL_PARENT,
        LayoutParams.WRAP_CONTENT
      )
    );
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent i = new Intent(getApplicationContext(), MainService.class);
        i.setAction(PomAction.TEST_LOCATION);
        i.putExtra("locationName", location.getNearGeofenceName());
        startService(i);
      }
    });

    linearLayout.addView(button);
  }

  private String buildPomLocationStr(PomLocation pomLocation, float distance) {
    StringBuilder result = new StringBuilder();
    String NEW_LINE = System.getProperty("line.separator");

    result.append("Name: " + pomLocation.name + " (ID: " + pomLocation.id + ")" + NEW_LINE);
    result.append("Lat: " + pomLocation.latitude + NEW_LINE);
    result.append("Lon: " + pomLocation.longitude + NEW_LINE);
    result.append("Range: " + pomLocation.range + "m" + NEW_LINE);
    result.append("State: " + pomLocation.state + NEW_LINE);
    result.append("Distance Away: " + distance + "m" + NEW_LINE);
    result.append("In Target Range: " + pomLocation.isInRange(distance) + NEW_LINE);
    result.append("In Near Range: " + pomLocation.isInNearRange(distance) + NEW_LINE);

    return result.toString();
  }

  private SharedPreferences getSharedPreferences() {
    final Context context = getApplicationContext();
    return context.getSharedPreferences(
        PomUtil.getAccountName(context), Context.MODE_PRIVATE);
  }

  private void launchServiceWithAction(String action) {
    final Intent i = new Intent(this, MainService.class);
    i.setAction(action);
    startService(i);
  }

  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      PomUtil.debug("Received broadcast with last known location");
      final Location location = (Location)intent.getExtras().get("location");
      buildLocations(location);
    }
  };

  private PreferenceManager preferenceManager;
}
