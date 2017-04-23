package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.accounts.*;
import android.content.*;
import android.net.*;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import java.net.URL;
import org.json.*;
import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.extractors.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
import org.scribe.utils.*;
import android.content.res.Resources;

public class AuthTokenManager {
  public AuthTokenManager(String serverUrl, String apiKey, String apiSecret) {
    this.serverUrl = serverUrl;
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
  }

  public OAuthService buildService() {
    PomApi20.setServerUrl(serverUrl);
    return new ServiceBuilder()
       .provider(PomApi20.class)
       .apiKey(apiKey)
       .apiSecret(apiSecret)
       .callback(serverUrl + "/pages/auth_success")
       .signatureType(SignatureType.QueryString)
       .build();
  }

  public Token getAuthTokenFromServiceAndUri(OAuthService service, Uri uri) {
    Verifier verifier = new Verifier(uri.getQueryParameter("code"));
    return service.getAccessToken(null, verifier);
  }

  public String getRefreshTokenFromAuthToken(Token authToken) {
    String refreshToken = null;
    try {
      JSONObject obj = new JSONObject(authToken.getRawResponse());
      refreshToken = obj.getString("refresh_token");
    }
    catch (Exception e) {
      PomUtil.debug("Error fetching refresh token: " + e.toString());
    }
    return refreshToken;
  }

  private String serverUrl;
  private String apiKey;
  private String apiSecret;
}
