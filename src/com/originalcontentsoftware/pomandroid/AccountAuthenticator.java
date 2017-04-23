package com.originalcontentsoftware.pomandroid;

import android.accounts.*;
import android.os.Bundle;
import android.content.*;
import android.util.Log;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.MetaData;

class AccountAuthenticator extends AbstractAccountAuthenticator {
  public AccountAuthenticator(Context context) {
    super(context);
    mContext = context;
    Bugsnag.init(mContext);
  }

  @Override
  public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
    PomUtil.debug("In getAuthToken method: account name = " + account.name + ", type = " + account.type);

    final Bundle bundle = new Bundle();

    if (!authTokenType.equals(PomUtil.getAccountName(mContext))) {
      PomUtil.debug("Error - invalid authTokenType: " + authTokenType);
      bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
      return bundle;
    }

    final AccountManager accountManager = AccountManager.get(mContext);
    final String authToken = accountManager.peekAuthToken(account, PomUtil.getAccountName(mContext));
    if (authToken != null) {
      PomUtil.debug("Found account auth token, returning results...");
      bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
      bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, PomUtil.getAccountName(mContext));
      bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
      return bundle;
    }

    // TODO: Does this even work?
    // If we couldn't obtain a valid token, launch the login activity to have
    // the user login and start the auth process over.
    return launchLogin(response, account.type, authTokenType);
  }

  @Override
  public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
    return launchLogin(response, accountType, authTokenType);
  }

  private Bundle launchLogin(AccountAuthenticatorResponse response, String accountType, String authTokenType) {
    Intent intent = new Intent(mContext, AuthActivity.class);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
    intent.putExtra("AuthTokenType", authTokenType);
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

    Bundle bundle = new Bundle();
    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
    return bundle;
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    return null;
  }

  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
    return null;
  }

  @Override
  public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
    return null;
  }

  @Override
  public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
    return null;
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    return null;
  }

  private Context mContext;
}
