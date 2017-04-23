package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.content.*;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

public class LocationProviderChangedReceiver extends WakefulBroadcastReceiver {
  public void onReceive(Context context, Intent intent) {
    final Intent i = new Intent(context, MainService.class);
    i.setAction(PomAction.LOCATION_PROVIDER_CHANGED);
    startWakefulService(context, i);
  }
}
