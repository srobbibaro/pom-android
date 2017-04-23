package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.content.*;
import android.util.Log;
import android.os.Bundle;
import org.json.*;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.Geofence.Builder;
import android.location.Location;

public class PomLocation {
  public double latitude;
  public double longitude;
  public float range;
  public String name;
  public int id;
  public int state;

  public class State {
    public final static int FAR       = 0;
    public final static int NEAR      = 1;
    public final static int ARRIVED   = 2;
    public final static int DEPARTING = 3;
  }

  private SharedPreferences sharedPreferences;
  private static float NEAR_RADIUS_INC = 100.0f;
  private static float MIN_RADIUS      = 50.0f;
  private static String NAME_PREFIX    = "PomLocation";

  public PomLocation(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
  }

  public Geofence buildTargetGeofence() {
    return buildGeofence(range, getTargetGeofenceName());
  }

  public Geofence buildNearGeofence() {
    return buildGeofence(getNearGeofenceRange(), getNearGeofenceName());
  }

  public String getTargetGeofenceName() {
    return PomLocation.getTargetGeofenceName(id);
  }

  public String getNearGeofenceName() {
    return PomLocation.getNearGeofenceName(id);
  }

  public float getNearGeofenceRange() {
    return getNearRange() < minNearRange() ? minNearRange() : getNearRange();
  }

  public static String getTargetGeofenceName(int id) {
    return NAME_PREFIX + "Target" + String.valueOf(id);
  }

  public static String getNearGeofenceName(int id) {
    return NAME_PREFIX + "Near" + String.valueOf(id);
  }

  public PomLocation fromJson(JSONObject json) {
    latitude = json.optDouble("latitude", 0.0);
    longitude = json.optDouble("longitude", 0.0);
    range = (float)json.optDouble("range", MIN_RADIUS);
    name = json.optString("name", "Undefined");
    id = json.optInt("id", -1);
    state = json.optInt("state", State.FAR);

    return this;
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();

    result.append("{name: " + name + ", id: " + id + ", state: " + state + ", ");
    result.append("lat: " + latitude + ", lon: " + longitude + ", range: " + range + "}");

    return result.toString();
  }

  public Location toLocation() {
    Location result = new Location("PomLocationProvider");
    result.setLatitude(latitude);
    result.setLongitude(longitude);
    return result;
  }

  public PomLocation store() {
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    try {
      editor.putString(getNearGeofenceName(), toJson().toString());
      editor.commit();
    }
    catch (JSONException e) {
      // TODO - how to handle?
    }
    return this;
  }

  public PomLocation load(String locationName) {
    try {
      fromJson(new JSONObject(sharedPreferences.getString(locationName, "")));
    }
    catch (JSONException e) {
      loadDefault();
    }

    return this;
  }

  public PomLocation load(int id) {
    return load(PomLocation.getNearGeofenceName(id));
  }

  public PomLocation load(Intent intent) {
    return load(intent.getStringExtra("locationName"));
  }

  public float distanceToLocation(Location location) {
      return location == null ?
        9999 : location.distanceTo(toLocation());
  }

  public boolean isInRange(float distance) {
    return range > distance;
  }

  public boolean isInNearRange(float distance) {
    return getNearRange() > distance;
  }

  private void loadDefault() {
    latitude = 0.0;
    longitude = 0.0;
    range = MIN_RADIUS;
    name = "Undefined";
    id = -1;
    state = State.FAR;
  }

  private JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put("latitude", latitude);
    json.put("longitude", longitude);
    json.put("range", range);
    json.put("name", name);
    json.put("id", id);
    json.put("state", state);
    return json;
  }

  private Geofence buildGeofence(float finalRange, String fenceName) {
    return new Geofence.Builder()
      .setCircularRegion(latitude, longitude, finalRange)
      .setExpirationDuration(Geofence.NEVER_EXPIRE)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT)
      .setRequestId(fenceName)
      .build();
  }

  private float minNearRange() {
    return NEAR_RADIUS_INC + MIN_RADIUS;
  }

  private float getNearRange() {
    return range + NEAR_RADIUS_INC;
  }
}
