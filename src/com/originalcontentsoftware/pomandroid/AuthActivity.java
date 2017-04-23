package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.accounts.*;
import android.app.Activity;
import android.content.*;
import android.location.*;
import android.net.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.*;
import java.net.URL;
import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.extractors.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
import org.scribe.utils.*;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.MetaData;

public class AuthActivity extends AccountAuthenticatorActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bugsnag.init(this);
    PomUtil.debug("Starting auth activity...");

    setContentView(R.layout.auth);

    final AuthTokenManager authTokenManager = new AuthTokenManager(
      getApplicationContext().getResources().getString(R.string.pom_server_url),
      getApplicationContext().getResources().getString(R.string.pom_api_key),
      getApplicationContext().getResources().getString(R.string.pom_api_secret)
    );

    final OAuthService service = authTokenManager.buildService();

    // Setup webview to display server's login page
    clearCookies();

    final WebView myWebView = (WebView) findViewById(R.id.webview);
    myWebView.setVerticalScrollBarEnabled(false);

    final WebSettings webSettings = myWebView.getSettings();
    webSettings.setSaveFormData(false);
    webSettings.setSavePassword(false);
    myWebView.setWebViewClient(new WebViewClient() {
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (Uri.parse(url).getHost().equals(getApplicationContext().getResources().getString(R.string.pom_server_domain))) {
          return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        return true;
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        // TODO: Use full path here instead to better ensure we're where we think we are?
        if (url.contains("?code=")) {
          // Obtain access token and refresh token from the server
          Token accessToken = authTokenManager.getAuthTokenFromServiceAndUri(service, Uri.parse(url));
          String refreshToken = "none"; //authTokenManager.getRefreshTokenFromAuthToken(accessToken);

          // Create account, set access token and set refresh token as password
          final String accountName = PomUtil.getAccountName(getApplicationContext());
          final Account account = new Account(accountName, accountName);
          AccountManager am = AccountManager.get(getApplicationContext());

          // Build response to caller containing new user's information
          final Bundle extras = getIntent().getExtras();
          if (extras != null) {
            if (am.addAccountExplicitly(account, refreshToken, null)) {
              am.setAuthToken(account, accountName, accessToken.getToken());
              final Bundle bundle = buildResult(accessToken, refreshToken);
              setAccountAuthenticatorResult(bundle);
              final Intent i = new Intent();
              i.putExtras(bundle);
              setResult(RESULT_OK, i);
              PomUtil.debug("Account added - returning from auth activity...");
            }
            else {
              PomUtil.debug("Error: Could not new add account!");
            }
            shutdown();
          }
        }
      }
    });

    myWebView.loadUrl(service.getAuthorizationUrl(null));
    myWebView.requestFocus(View.FOCUS_DOWN);
  }

  @Override
  public void onBackPressed() {
    setResult(RESULT_CANCELED, new Intent());
    shutdown();
  }

  private void clearCookies() {
    CookieSyncManager.createInstance(this);
    CookieManager.getInstance().removeAllCookie();
  }

  private void shutdown() {
    PomUtil.debug("Finished auth activity.");
    clearCookies();
    finish();
  }

  private Bundle buildResult(Token accessToken, String refreshToken) {
    final Bundle result = new Bundle();

    final String accountName = PomUtil.getAccountName(getApplicationContext());
    result.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    result.putString(AccountManager.KEY_ACCOUNT_TYPE, accountName);
    result.putString(AccountManager.KEY_PASSWORD, refreshToken);

    if (accessToken != null) {
      result.putString(AccountManager.KEY_AUTHTOKEN, accessToken.getToken());
      PomUtil.debug("Setting token on account...");
    }
    else {
      PomUtil.debug("Not setting token for account (none found).");
    }

    return result;
  }
}
