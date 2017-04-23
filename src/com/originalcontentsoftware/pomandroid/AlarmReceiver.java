package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.content.*;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

public class AlarmReceiver extends WakefulBroadcastReceiver {
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    final Intent i = new Intent(context, MainService.class);
    i.putExtras(intent);
    i.setAction(action);
    startWakefulService(context, i);
  }
}
