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
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Bugsnag.init(this);
    PomUtil.debug("Starting map activity...");

    super.onCreate(savedInstanceState);

    setContentView(R.layout.map);
    preferenceManager = new PreferenceManager(getSharedPreferences());

    MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(PomAction.LAST_LOCATION));
    PomUtil.debug("Finished starting up map activity.");
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

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;

    List <PomLocation> locations = preferenceManager.getLocations();

    for (PomLocation pomLocation : locations) {
      this.map.addMarker(
        new MarkerOptions().position(new LatLng(pomLocation.latitude, pomLocation.longitude)
      ).title(pomLocation.name).snippet(buildPomLocationStr(pomLocation)));
    }
    launchServiceWithAction(PomAction.LAST_LOCATION);
  }

  private String buildPomLocationStr(PomLocation pomLocation) {
    StringBuilder result = new StringBuilder();
    String NEW_LINE = ", ";

    result.append("ID: " + pomLocation.id + NEW_LINE);
    result.append("Range: " + pomLocation.range + "m" + NEW_LINE);
    result.append("State: " + pomLocation.state + NEW_LINE);

    return result.toString();
  }

  private void buildLocations(Location lastLocation) {
    List <PomLocation> locations = preferenceManager.getLocations();

    for (PomLocation pomLocation : locations) {
      final float distance = pomLocation.distanceToLocation(lastLocation);
    }
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
      map.animateCamera(CameraUpdateFactory.newLatLngZoom(
        new LatLng(location.getLatitude(), location.getLongitude()
      ), 18.0f));
    }
  };

  private PreferenceManager preferenceManager;
  private GoogleMap map;
}
