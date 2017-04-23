package com.originalcontentsoftware.pomandroid;

import com.originalcontentsoftware.pomandroid.R;
import android.content.*;
import android.util.Log;
import android.os.Bundle;

public class PomRequestNoTokenException extends PomRequestException {
  PomRequestNoTokenException() {
    super("No user auth token");
  }
}
