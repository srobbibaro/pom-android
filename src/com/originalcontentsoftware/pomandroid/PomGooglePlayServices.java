package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.accounts.*;
import android.app.*;
import android.content.*;
import android.location.*;
import android.net.*;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.*;
import com.google.android.gms.location.Geofence.Builder;
import java.net.URL;
import java.util.*;
import org.json.*;
import com.bugsnag.android.Bugsnag;

public class PomGooglePlayServices {
  public static Location getLastKnownLocation(GoogleApiClient googleApiClient) {
    Location location = null;

    if (googleApiClient != null && googleApiClient.isConnected()) {
      PomUtil.debug("Connected to GooglePlayServices. Getting last known location...");
      location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }
    else {
      PomUtil.debug("Not Connected to GooglePlayServices. Could not get last known location.");
    }

    return location;
  }

  public static LocationRequest setupLocationRequest(int fetchLocationSecs) {
    LocationRequest locationRequest = new LocationRequest();
    locationRequest.setInterval(fetchLocationSecs * 1000);
    locationRequest.setFastestInterval(fetchLocationSecs * 1000);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    return locationRequest;
  }

  public static void startLocationUpdates(Context context, GoogleApiClient googleApiClient, int fetchLocationSecs, Class receiver) {
    LocationRequest locationRequest = setupLocationRequest(fetchLocationSecs);
    if (googleApiClient != null && googleApiClient.isConnected()) {
      PomUtil.debug("Starting location updates...");

      final PendingIntent pi = PendingIntent.getBroadcast(
          context,
          0,
          new Intent(context, receiver),
          PendingIntent.FLAG_UPDATE_CURRENT
      );
      LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, pi);
    }
    else {
      PomUtil.debug("Not Connected to GooglePlayServices. Could not start location updates.");
    }
  }

  public static void stopLocationUpdates(Context context, GoogleApiClient googleApiClient, Class receiver) {
    if (googleApiClient != null && googleApiClient.isConnected()) {
      PomUtil.debug("Stopping location updates...");

      final PendingIntent pi = PendingIntent.getBroadcast(
          context,
          0,
          new Intent(context, receiver),
          PendingIntent.FLAG_UPDATE_CURRENT
      );
      LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, pi);
    }
    else {
      PomUtil.debug("Not Connected to GooglePlayServices. Could not stop location updates.");
    }
  }
}
