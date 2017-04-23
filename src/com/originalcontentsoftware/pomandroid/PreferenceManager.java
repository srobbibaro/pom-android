package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.app.*;
import android.content.*;
import android.util.Log;
import android.os.Bundle;
import org.json.*;
import java.util.*;

class PreferenceManager {
  public PreferenceManager(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
  }

  public void clearSettings() {
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.clear();
    editor.commit();
  }

  public void storeLocationIds(JSONArray locationIds) {
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString("location_ids", locationIds.toString());
    editor.commit();
  }

  public JSONArray getLocationIds() {
    String jsonStr = sharedPreferences.getString("location_ids", "");
    try {
      return new JSONArray(jsonStr);
    }
    catch (Exception e) {
      return new JSONArray();
    }
  }

  public void storeServiceEnabled(boolean enabled) {
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean("service_enabled", enabled);
    editor.commit();
  }

  public boolean checkServiceEnabled() {
    return sharedPreferences.getBoolean("service_enabled", false);
  }

  public String getLocationByPosition(int pos) {
    JSONArray locationIds = getLocationIds();
    String name = null;

    if (locationIds != null) {
      int id = locationIds.optInt(pos, -1);
      name =  PomLocation.getNearGeofenceName(id);
    }

    return name;
  }

  public List <PomLocation> getLocations() {
    List <PomLocation> locations = new ArrayList<PomLocation>();
    JSONArray locationIds = getLocationIds();

    for (int i = 0; i < locationIds.length(); ++i) {
      PomLocation pomLocation = new PomLocation(sharedPreferences);
      pomLocation.load(locationIds.optInt(i));
      locations.add(pomLocation);
    }

    return locations;
  }

  private SharedPreferences sharedPreferences;
}
