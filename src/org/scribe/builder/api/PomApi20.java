package org.scribe.builder.api;
import org.scribe.extractors.*;
import org.scribe.model.*;
import org.scribe.utils.*;

public class PomApi20 extends DefaultApi20 {
  public static void setServerUrl(String serverUrl) {
    SERVER = serverUrl;
    AUTHORIZE_URL = SERVER + "/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code";
    SCOPED_AUTHORIZE_URL = AUTHORIZE_URL + "&scope=%s";
  }

  @Override
  public String getAccessTokenEndpoint() {
    return SERVER + "/oauth/token?grant_type=authorization_code";
  }

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.POST;
  }

  @Override
  public String getAuthorizationUrl(OAuthConfig config) {
    Preconditions.checkValidUrl(config.getCallback(), "Must provide a valid url as callback. POM does not support OOB");
    // TODO: We are not presently using scopes (and should be)
    // Append scope if present
    if (config.hasScope()) {
      return String.format(SCOPED_AUTHORIZE_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()), OAuthEncoder.encode(config.getScope()));
    }
    else {
      return String.format(AUTHORIZE_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()));
    }
  }

  @Override
  public AccessTokenExtractor getAccessTokenExtractor() {
    return new JsonTokenExtractor();
  }

  private static String SERVER;
  private static String AUTHORIZE_URL;
  private static String SCOPED_AUTHORIZE_URL;
}
