package com.originalcontentsoftware.pomandroid;

import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.os.IBinder;

public class AuthenticatorService extends Service {
  @Override
  public IBinder onBind(Intent intent) {
    return intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT) ?
      getAuthenticator().getIBinder() : null;
  }

  private AccountAuthenticator getAuthenticator() {
    if (pomAccountAuthenticator == null)
      pomAccountAuthenticator = new AccountAuthenticator(this);
    return pomAccountAuthenticator;
  }

  private static AccountAuthenticator pomAccountAuthenticator = null;
}
