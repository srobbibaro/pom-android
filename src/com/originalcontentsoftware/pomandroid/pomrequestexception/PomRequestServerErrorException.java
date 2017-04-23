package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import org.scribe.model.*;

public class PomRequestServerErrorException extends PomRequestException {
  PomRequestServerErrorException(Response response) {
    super(
      "Failure response returned from http request (" +
       String.valueOf(response.getCode()) + "): " +
       response.getMessage() != null ?
        response.getMessage() : "no response"
    );
  }
}
