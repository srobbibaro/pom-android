package com.originalcontentsoftware.pomandroid;

import android.accounts.*;
import android.app.*;
import android.content.*;
import android.location.*;
import android.net.*;
import android.os.*;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.bugsnag.android.Bugsnag;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.*;
import com.google.android.gms.location.Geofence.Builder;
import com.originalcontentsoftware.pomandroid.R;
import java.net.URL;
import java.util.*;
import org.json.*;
import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.extractors.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
import org.scribe.utils.*;

public class MainService extends IntentService {
  public MainService() {
    super("MainService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, POM_WAKE_LOCK);
    wakeLock.acquire();

    Bugsnag.init(this);

    if (preferenceManager == null) {
      preferenceManager = new PreferenceManager(getSharedPreferences());
    }

    if (mGoogleApiClient == null) {
      mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .build();
    }

    ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();

    if (!connectionResult.isSuccess()) {
      PomUtil.debug("Error! Could not connect to google play services.", true);
      return;
    }

    PomUtil.debug("Connected to google play services.");
    handleIntent(intent);
    mGoogleApiClient.disconnect();
    PomUtil.debug("Disconnected from google play services.");

    wakeLock.release();
  }

  public void handleIntent(Intent intent) {
    final String action = intent.getAction();
    final Context context = getApplicationContext();

    PomUtil.debug("Received action: " + action + ". Processing...");

    if (action.equals(PomAction.START_UP)) {
      enableService();
      AutoStartReceiver.completeWakefulIntent(intent);
    }
    else if (action.equals(PomAction.HALT)) {
      disableService();
    }
    else if (action.equals(PomAction.REFRESH_LOCATIONS)) {
      refreshLocations();
    }
    else if (action.equals(PomAction.LOCATION_TARGET_ARRIVE)) {
      handleLocationAction(action, intent);
    }
    else if (action.equals(PomAction.LOCATION_NEAR_ARRIVE)) {
      handleLocationAction(action, intent);
    }
    else if (action.equals(PomAction.CLEAR_LOCATION)) {
      PomLocation pomLocation = new PomLocation(getSharedPreferences()).load(intent);
      pomLocation.state = PomLocation.State.FAR;
      pomLocation.store();
      PomUtil.debug("Arrivals re-enabled for location: " + pomLocation.name );
      AlarmReceiver.completeWakefulIntent(intent);
    }
    else if (action.equals(PomAction.LOCATION_SEARCH_TIMEOUT)) {
      PomLocation pomLocation = new PomLocation(getSharedPreferences()).load(intent);
      PomUtil.notifyLocationSearchTimeout(
        context,
        PomUtil.buildNotificationIntent(
          context,
          getText(R.string.notify_search_timeout_title).toString(),
          getText(R.string.notify_search_timeout_message).toString(),
          String.format(getText(R.string.notify_search_timeout_notification_message).toString(), pomLocation.name, pomLocation.name),
          PomAction.LOCATION_SEARCH_TIMEOUT
        ),
        pomLocation
      );
      PomUtil.debug("Presented location arrival search timeout notification. Location: " + pomLocation.name);
      AlarmReceiver.completeWakefulIntent(intent);
    }
    else if (action.equals(PomAction.LOCATION_PULSE)) {
      PomGooglePlayServices.startLocationUpdates(context, mGoogleApiClient, LOCATION_UPDATE_SECS, LocationPulseUpdateReceiver.class);
      AlarmReceiver.completeWakefulIntent(intent);
    }
    else if (action.equals(PomAction.LOCATION_PULSE_UPDATE)) {
      handleLocationUpdate(intent);
    }
    else if (action.equals(PomAction.TEST_LOCATION)) {
      PomLocation pomLocation = new PomLocation(getSharedPreferences()).load(intent);
      sendLocation(pomLocation, pomLocation.latitude, pomLocation.longitude);
    }
    else if (action.equals(PomAction.LAST_LOCATION)) {
      final Location lastLocation = PomGooglePlayServices.getLastKnownLocation(mGoogleApiClient);
      PomUtil.debug("Last location = " + PomUtil.buildLocationString(lastLocation));
      final Intent lastLocationIntent = new Intent(PomAction.LAST_LOCATION);
      lastLocationIntent.putExtra("location", lastLocation);
      LocalBroadcastManager.getInstance(this).sendBroadcast(lastLocationIntent);
    }
    else if (action.equals(PomAction.LOCATION_PROVIDER_CHANGED)) {
      if (isServiceEnabled() && !isGpsEnabled()) {
        disableService();
        PomUtil.notifyWarning(
          context,
          PomUtil.buildNotificationIntent(
            context,
            "Warning",
            getText(R.string.notify_error_gps_disabled_message).toString(),
            getText(R.string.notify_error_gps_disabled_message).toString(),
            PomAction.GPS_DISABLED_ERROR
          )
        );
      }
      LocationProviderChangedReceiver.completeWakefulIntent(intent);
    }
    else {
      PomUtil.debug("Unknown action: " + action + ". Nothing to do.", true);
    }
  }

  private void handleLocationUpdate(Intent intent) {
    final Location location = (Location)intent.getExtras().get(LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED);

    if (location != null && location.getAccuracy() < 150.0f) {
      PomUtil.debug("Received location = " + PomUtil.buildLocationString(location));
      List <PomLocation> locations = preferenceManager.getLocations();

      PomUtil.debug(locations.size() + " Locations:");
      for (PomLocation pomLocation : locations) {
        final float distance = pomLocation.distanceToLocation(location);

        final boolean isInNearRange = pomLocation.isInNearRange(distance);
        final boolean isInRange     = pomLocation.isInRange(distance);

        switch (pomLocation.state) {
          case PomLocation.State.FAR:
            if (isInRange) {
              handleLocationTargetAction(Geofence.GEOFENCE_TRANSITION_ENTER, location, pomLocation);
            }
            else if (isInNearRange) {
              handleLocationNearAction(Geofence.GEOFENCE_TRANSITION_ENTER, pomLocation);
            }
            break;
          case PomLocation.State.NEAR:
            if (isInRange) {
              handleLocationTargetAction(Geofence.GEOFENCE_TRANSITION_ENTER, location, pomLocation);
            }
            else if (!isInNearRange) {
              handleLocationNearAction(Geofence.GEOFENCE_TRANSITION_EXIT, pomLocation);
            }
            break;
          case PomLocation.State.ARRIVED:
            if (!isInNearRange) {
              handleLocationNearAction(Geofence.GEOFENCE_TRANSITION_EXIT, pomLocation);
            }
            break;
          case PomLocation.State.DEPARTING:
            if (isInNearRange) {
              handleLocationNearAction(Geofence.GEOFENCE_TRANSITION_ENTER, pomLocation);
            }
            break;
        }

        PomUtil.debug(pomLocation + ", distance: " + distance + "m, in range? " + pomLocation.isInRange(distance));
      }

      PomGooglePlayServices.stopLocationUpdates(getApplicationContext(), mGoogleApiClient, LocationPulseUpdateReceiver.class);
      setLocationPulseAlarm();
      if (PomUtil.DEBUG_ENABLED) {
        sendLocationToServer(location.getLatitude(), location.getLongitude());
      }
    }
    else {
      PomUtil.debug("Error: received invalid location.");
    }

    LocationPulseUpdateReceiver.completeWakefulIntent(intent);
  }

  private void handleLocationAction(String action, Intent intent) {
    if (GeofencingEvent.fromIntent(intent).hasError()) {
      PomUtil.debug("Geofence event threw error. Cannot continue. Is GPS enabled?");
      return;
    }

    int transition = GeofencingEvent.fromIntent(intent).getGeofenceTransition();
    Location l     = GeofencingEvent.fromIntent(intent).getTriggeringLocation();

    PomUtil.debug("Event triggering location: " + (l != null ? PomUtil.buildLocationString(l) : "none"));

    for (Geofence g : GeofencingEvent.fromIntent(intent).getTriggeringGeofences()) {
      if (action.equals(PomAction.LOCATION_NEAR_ARRIVE)) {
        PomLocation pomLocation = new PomLocation(getSharedPreferences()).load(g.getRequestId());
        PomUtil.debug("Processing event for location: " + pomLocation.name);

        handleLocationNearAction(transition, pomLocation);
      }
      else if (action.equals(PomAction.LOCATION_TARGET_ARRIVE)) {
        PomLocation pomLocation = new PomLocation(getSharedPreferences()).load(intent);
        PomUtil.debug("Processing event for location: " + pomLocation.name);

        handleLocationTargetAction(transition, l, pomLocation);
      }
    }
  }

  private void handleLocationNearAction(int transition, PomLocation location) {
    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
      PomUtil.debug("Outter (near) geofence enter transition. Processing...");

      if (location.state == PomLocation.State.FAR) {
        PomUtil.debug("Arrival allowed for location; creating inner (target) geofence for location...");
        addLocationGeofence(location);
        setAlarm(location, SEARCH_TIMEOUT_MINS, PomAction.LOCATION_SEARCH_TIMEOUT);
        location.state = PomLocation.State.NEAR;
        location.store();
      }
      else if (location.state == PomLocation.State.DEPARTING) {
        PomUtil.debug("Arrival not allowed for location; inner (target) geofence not created for location.");
        location.state = PomLocation.State.ARRIVED;
        location.store();
      }
      else {
        PomUtil.debug("Unknown starting state (" + location.state + ") for transition.");
      }

      cancelAlarm(location, CLEAR_LOCATION_MINS, PomAction.CLEAR_LOCATION);
    }
    else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
      PomUtil.debug("Outter (near) geofence exit transition. Processing...");

      if (location.state == PomLocation.State.ARRIVED) {
        PomUtil.debug("Arrival not allowed for location. Starting location re-enable timer...");
        setAlarm(location, CLEAR_LOCATION_MINS, PomAction.CLEAR_LOCATION);
        location.state = PomLocation.State.DEPARTING;
        location.store();
      }
      else if (location.state == PomLocation.State.NEAR) {
        PomUtil.debug("Arrival allowed for location. Nothing to do.");
        location.state = PomLocation.State.FAR;
        location.store();
      }
      else {
        PomUtil.debug("Unknown starting state (" + location.state + ") for transition.");
      }

      cancelAlarm(location, SEARCH_TIMEOUT_MINS, PomAction.LOCATION_SEARCH_TIMEOUT);
      removeLocationGeofence(location);
    }
    else {
      PomUtil.debug("No action taken; unexpected outer (near) geofence transition: " + transition);
    }
  }

  private void handleLocationTargetAction(int transition, Location l, PomLocation location) {
    if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
        transition != Geofence.GEOFENCE_TRANSITION_EXIT) {
      // Consider both enter and exit events valid, since if you exit the geofence,
      // you must have been there to begin with.
      PomUtil.debug("No action taken; unexpected inner (target) geofence transition: " + transition);
      return;
    }

    if (l == null) {
      PomUtil.debug("Geofence event had no location. Cannot send location arrival!", true);
      return;
    }

    if (location.state == PomLocation.State.FAR || location.state == PomLocation.State.NEAR) {
      PomUtil.debug("Inner (target) geofence enter transition. Processing...");
      sendLocation(location, l.getLatitude(), l.getLongitude());
    }
    else {
      PomUtil.debug("Unknown starting state (" + location.state + ") for transition.");
    }
  }

  private void removeGeofences(List<String> geofenceNames) {
    if (geofenceNames.size() > 0) {
      for (String name : geofenceNames) {
        PomUtil.debug("Removing geofence: " + name);
      }
      LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofenceNames);
    }
    PomUtil.debug("Finished removing geofences.");
  }

  private int getActiveLocations() {
    PomUtil.debug("Fetching all active locations...");

    int numLocations = 0;

    try {
      Response response = callPomApi("arrival_schedule/active_locations", null);
      JSONArray array   = new JSONArray(response.getBody());
      numLocations      = array.length();

      if (numLocations > 0) {
        JSONArray locationIds = addGeofences(array);
        preferenceManager.storeLocationIds(locationIds);
      }

      PomUtil.debug("Successfully finished fetching " + array.length() + " active locations.");
    }
    catch (JSONException e) {
      notifyError("Could not fetch your active locations from the server.", e.toString(), PomAction.LOCATION_UPDATE_ERROR);
    }
    catch (PomRequestException e) {
      notifyError("Could not fetch your active locations from the server.", e.toString(), PomAction.LOCATION_UPDATE_ERROR);
    }

    return numLocations;
  }

  private void notifyError(String message, String error, String type) {
    final Context context = getApplicationContext();

    PomUtil.notifyError(
      context,
      PomUtil.buildNotificationIntent(
        context,
        "Error",
        message,
        message,
        type
      ),
      error
    );
  }

  private Response callPomApi(String route, Map<String, String> params) throws PomRequestException {
    final Context context = getApplicationContext();
    String token = PomUtil.getAuthToken(context);

    if (!PomUtil.hasNetworkConnection(context)) {
      throw new PomRequestNoNetworkException();
    }
    else if (token == null) {
      throw new PomRequestNoTokenException();
    }

    OAuthRequest request = new OAuthRequest(
      Verb.POST,
      this.getResources().getString(R.string.pom_server_url) +
        "/api/v1/" + route
    );
    request.addHeader("Authorization", "Bearer " + token);
    if (params != null) {
      for (Map.Entry<String, String> param : params.entrySet()) {
        request.addBodyParameter(param.getKey(), param.getValue());
      }
    }

    PomUtil.debug("Http request setup; attempting send...");
    Response response = request.send();
    PomUtil.debug("Finished calling https request send.");

    if (!response.isSuccessful()) {
      if (response.getCode() >= 400 && response.getCode() < 500) {
        throw new PomRequestNoTokenException();
      }
      else {
        throw new PomRequestServerErrorException(response);
      }
    }

    return response;
  }

  private JSONArray addGeofences(JSONArray array) throws JSONException {
    JSONArray locationIds = new JSONArray();
    PomUtil.debug("Adding " + array.length() + " outer (near) geofences...");
    final GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

    for (int i = 0; i < array.length(); ++i) {
      PomLocation location = new PomLocation(getSharedPreferences()).fromJson(array.getJSONObject(i)).store();
      PomUtil.debug("Location: " + location.name);
      locationIds.put(location.id);
      builder.addGeofence(location.buildNearGeofence());
    }

    final Intent intent = new Intent(this, MainService.class);
    intent.setAction(PomAction.LOCATION_NEAR_ARRIVE);
    final PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, builder.build(), pi);

    PomUtil.debug("Finished adding outer (near) geofences.");

    return locationIds;
  }

  private PendingIntent getAlarmIntent(PomLocation location, int minsTimeout, String action) {
    int alarmId = (location.id * 10000) + minsTimeout;

    Intent i = new Intent(this, AlarmReceiver.class);
    i.setAction(action);
    i.putExtra("locationName", location.getNearGeofenceName());
    return PendingIntent.getBroadcast(this, alarmId, i, 0);
  }

  private void setAlarm(PomLocation location, int minsTimeout, String action) {
    final PendingIntent pi = getAlarmIntent(location, minsTimeout, action);
    final AlarmManager am =
      (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * minsTimeout, pi);
    PomUtil.debug(
      "Set alarm: location = " + location.name + " timeout = " + minsTimeout +
      " minutes action = " + action + " id = " + location.id
    );
  }

  private void setLocationPulseAlarm() {
    Intent i = new Intent(this, AlarmReceiver.class);
    i.setAction(PomAction.LOCATION_PULSE);
    final PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

    final AlarmManager am =
      (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * LOCATION_PULSE_SECS, pi);
    PomUtil.debug(
      "Set location pulse alarm: timeout = " + LOCATION_PULSE_SECS +
      " seconds action = " + PomAction.LOCATION_PULSE + " id = 0"
    );
  }

  private void cancelAlarm(PomLocation location, int minsTimeout, String action) {
    final PendingIntent pi = getAlarmIntent(location, minsTimeout, action);
    final AlarmManager alarmManager =
      (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(pi);
    PomUtil.debug(
      "Cancel alarm: location = " + location.name + " timeout = " + minsTimeout +
      " minutes action = " + action + " id = " + location.id
    );
  }

  private void cancelLocationPulseAlarm() {
    Intent i = new Intent(this, AlarmReceiver.class);
    i.setAction(PomAction.LOCATION_PULSE);
    final PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
    final AlarmManager alarmManager =
      (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(pi);
    PomUtil.debug(
      "Cancel location pulse alarm: timeout = " + LOCATION_PULSE_SECS +
      " seconds action = " + PomAction.LOCATION_PULSE + " id = 0"
    );
  }

  private boolean checkLocationWithServer(double latitude, double longitude) {
    PomUtil.debug("Checking location for arrival with server...");
    PomUtil.debug("Location: " + PomUtil.buildLocationString(latitude, longitude, 0.0f, 0.0f));

    boolean status = false;

    try {
      Map <String, String> params = new HashMap<String, String>();

      params.put("latitude", String.valueOf(latitude));
      params.put("longitude", String.valueOf(longitude));

      Response response = callPomApi("arrival_schedule/check_location", params);
      JSONArray array   = new JSONArray(response.getBody());

      PomUtil.debug("Location arrival check with server completed successfully. Arrival match count: " + array.length());

      for (int i = 0; i < array.length(); ++i) {
        JSONObject obj          = array.getJSONObject(i);
        JSONObject notification = obj.getJSONObject("notification");
        String name             = obj.getString("name");
        String shortMsg         = notification.getString("short");
        String longMsg          = notification.getString("long");
        String fullMsg          = notification.getString("full");

        final Context context = getApplicationContext();

        PomUtil.notifyLocationMatch(
          context,
          PomUtil.buildNotificationIntent(
            context,
            shortMsg,
            longMsg,
            fullMsg,
            PomAction.LOCATION_TARGET_ARRIVE
          )
        );
        PomUtil.debug("Presented arrival notification. Location: " + name);
        status = true;
      }
    }
    catch (JSONException e) {
      notifyError("Could not check your location with the server.", e.toString(), PomAction.LOCATION_VERIFY_ERROR);
    }
    catch (PomRequestException e) {
      notifyError("Could not check your location with the server.", e.toString(), PomAction.LOCATION_VERIFY_ERROR);
    }

    return status;
  }

  private boolean sendLocationToServer(double latitude, double longitude) {
    PomUtil.debug("Sending location to server...");

    boolean status = false;

    try {
      Map <String, String> params = new HashMap<String, String>();

      params.put("latitude", String.valueOf(latitude));
      params.put("longitude", String.valueOf(longitude));

      Response response = callPomApi("arrival_schedule/process_locations", params);
      PomUtil.debug("Location sent to server successfully.");
      status = true;
    }
    catch (PomRequestException e) {
      PomUtil.debug("Could not send location to server: " + e.toString(), true);
    }

    return status;
  }

  private SharedPreferences getSharedPreferences() {
    return this.getSharedPreferences(
        PomUtil.getAccountName(getApplicationContext()), Context.MODE_PRIVATE);
  }

  private void enableService() {
    disableService();
    if (getActiveLocations() > 0) {
      updateServiceEnabled(true);
      PomGooglePlayServices.startLocationUpdates(getApplicationContext(), mGoogleApiClient, LOCATION_UPDATE_SECS, LocationPulseUpdateReceiver.class);
    }
    else {
      PomUtil.debug("Could not fetch locations from server - disabling service...");
      disableService();
      broadcastNoLocationsFetchedError();
    }
  }

  private void broadcastNoLocationsFetchedError() {
    final Intent noLocationsFetchedIntent = new Intent(PomAction.NO_LOCATIONS_FETCHED_ERROR);
    LocalBroadcastManager.getInstance(this).sendBroadcast(noLocationsFetchedIntent);
  }

  private void updateServiceEnabled(boolean enabled) {
    preferenceManager.storeServiceEnabled(enabled);
    final Intent serviceEnabledIntent = new Intent(PomAction.SERVICE_ENABLED_UPDATE);
    serviceEnabledIntent.putExtra("service_enabled", enabled);
    LocalBroadcastManager.getInstance(this).sendBroadcast(serviceEnabledIntent);
  }

  private void removeAllGeofences(List <PomLocation> locations) {
    List <String> geofenceNames = new ArrayList<String>();
    for (PomLocation location : locations) {
      geofenceNames.add(location.getTargetGeofenceName());
      geofenceNames.add(location.getNearGeofenceName());
    }
    removeGeofences(geofenceNames);
  }

  private void cancelAllAlarms(List <PomLocation> locations) {
    for (PomLocation location : locations) {
      cancelAlarm(location, CLEAR_LOCATION_MINS, PomAction.CLEAR_LOCATION);
      cancelAlarm(location, SEARCH_TIMEOUT_MINS, PomAction.LOCATION_SEARCH_TIMEOUT);
    }

    cancelLocationPulseAlarm();
  }

  public void disableService() {
    List <PomLocation> locations = preferenceManager.getLocations();

    removeAllGeofences(locations);
    cancelAllAlarms(locations);

    preferenceManager.clearSettings();
    PomGooglePlayServices.stopLocationUpdates(getApplicationContext(), mGoogleApiClient, LocationPulseUpdateReceiver.class);
    updateServiceEnabled(false);
    PomUtil.debug("Service disabled.");
  }

  public void refreshLocations() {
    List <PomLocation> oldLocations = preferenceManager.getLocations();
    removeAllGeofences(oldLocations);

    if (getActiveLocations() < 1) {
      PomUtil.debug("Could not fetch locations from server - disabling service...");
      disableService();
      broadcastNoLocationsFetchedError();
      return;
    }

    for (PomLocation oldLocation : oldLocations) {
      PomLocation pomLocation = new PomLocation(getSharedPreferences()).load(oldLocation.id);
      if (pomLocation.id < 0) {
        cancelAlarm(oldLocation, CLEAR_LOCATION_MINS, PomAction.CLEAR_LOCATION);
        cancelAlarm(oldLocation, SEARCH_TIMEOUT_MINS, PomAction.LOCATION_SEARCH_TIMEOUT);
      }
      else {
        pomLocation.state = oldLocation.state;
        pomLocation.store();

        if (pomLocation.state == PomLocation.State.NEAR) {
          addLocationGeofence(pomLocation);
        }
      }
    }
  }

  private void addLocationGeofence(PomLocation location) {
    final GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

    builder.addGeofence(location.buildTargetGeofence());

    final Intent intent = new Intent(this, MainService.class);
    intent.setAction(PomAction.LOCATION_TARGET_ARRIVE);
    intent.putExtra("locationName", location.getNearGeofenceName());
    final PendingIntent i = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, builder.build(), i);
    PomUtil.debug("Added geofence: " + location.getTargetGeofenceName());
  }

  private void removeLocationGeofence(PomLocation location) {
    PomUtil.debug("Removing inner (target) geofence...");
    List <String> geofenceNames = new ArrayList<String>();
    geofenceNames.add(PomLocation.getTargetGeofenceName(location.id));
    removeGeofences(geofenceNames);
  }

  private void sendLocation(PomLocation location, double latitude, double longitude) {
    if (!checkLocationWithServer(latitude, longitude)) {
      PomUtil.debug("Location arrival check with server failed!");
      return;
    }

    PomUtil.debug("Location arrival check with server confirmed; Arrivals for this location no longer allowed.");
    cancelAlarm(location, SEARCH_TIMEOUT_MINS, PomAction.LOCATION_SEARCH_TIMEOUT);
    removeLocationGeofence(location);
    location.state = PomLocation.State.ARRIVED;
    location.store();

    final Location lastLocation = PomGooglePlayServices.getLastKnownLocation(mGoogleApiClient);
    final float distance        = lastLocation.distanceTo(location.toLocation());

    PomUtil.debug("Last location = " + PomUtil.buildLocationString(lastLocation) + ", distance = " + distance);

    if (location.getNearGeofenceRange() < distance) {
      PomUtil.debug("Location is not in range of near geofence. Starting location re-enable timer...");
      setAlarm(location, CLEAR_LOCATION_MINS, PomAction.CLEAR_LOCATION);
    }
    else {
      PomUtil.debug("Location is in range of near geofence.");
    }
  }

  private boolean isGpsEnabled() {
    final LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
  }

  private boolean isServiceEnabled() {
    return preferenceManager.checkServiceEnabled();
  }

  private PreferenceManager preferenceManager;
  private GoogleApiClient mGoogleApiClient;

  private final int SEARCH_TIMEOUT_MINS  = 5;
  private final int CLEAR_LOCATION_MINS  = 20;
  private final int LOCATION_UPDATE_SECS = 30;
  private final int LOCATION_PULSE_SECS  = 240;

  private final String POM_WAKE_LOCK = "PomServiceWakelock";
}
