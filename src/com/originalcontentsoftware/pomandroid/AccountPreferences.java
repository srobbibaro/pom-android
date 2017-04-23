package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.preference.PreferenceActivity;
import android.os.Bundle;

public class AccountPreferences extends PreferenceActivity {
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.preferences_resources);
  }
}
