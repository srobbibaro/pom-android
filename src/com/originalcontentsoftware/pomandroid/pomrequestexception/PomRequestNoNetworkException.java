package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.content.*;
import android.util.Log;
import android.os.Bundle;

public class PomRequestNoNetworkException extends PomRequestException {
  PomRequestNoNetworkException() {
    super("No network connection present");
  }
}
