package com.google.sps.firebase;

import java.io.IOException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

// Only create app one time
public class FirebaseAppManager {
  private static FirebaseApp app = null;

  public static FirebaseApp getApp() throws IOException{
    if (app == null) {
      FirebaseOptions options = new FirebaseOptions.Builder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .setDatabaseUrl("https://fulfillment-deco-step-2020.firebaseio.com")
        .setProjectId("fulfillment-deco-step-2020")
        .build();
      app = FirebaseApp.initializeApp(options);
    }
    return app;
  }
}