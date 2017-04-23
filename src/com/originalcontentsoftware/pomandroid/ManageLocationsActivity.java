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
import android.webkit.WebSettings.RenderPriority;
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

public class ManageLocationsActivity extends AccountAuthenticatorActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bugsnag.init(this);
    PomUtil.debug("Starting manage locations activity...");

    setContentView(R.layout.manage_locations);

    // Setup webview to display server's location management page
    final WebView myWebView = (WebView) findViewById(R.id.webview);
    myWebView.getSettings().setRenderPriority(RenderPriority.HIGH);
    myWebView.setVerticalScrollBarEnabled(true);

    final WebSettings webSettings = myWebView.getSettings();
    webSettings.setSaveFormData(true);
    webSettings.setSavePassword(true);
    webSettings.setJavaScriptEnabled(true);
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
      }
    });

    myWebView.loadUrl(getApplicationContext().getResources().getString(R.string.pom_server_url));
    myWebView.requestFocus(View.FOCUS_DOWN);
  }

  @Override
  public void onBackPressed() {
    setResult(RESULT_CANCELED, new Intent());
    shutdown();
  }

  private void shutdown() {
    PomUtil.debug("Finished manage locations activity.");
    finish();
  }
}
