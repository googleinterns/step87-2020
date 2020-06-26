package com.google.sps.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.ThreadManager;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

// Only create app one time
public class FirebaseAppManager {
  private static FirebaseApp app = null;

  public static FirebaseApp getApp() throws IOException {
    if (app == null) {
      FirebaseOptions options =
          new FirebaseOptions.Builder()
              .setCredentials(GoogleCredentials.getApplicationDefault())
              .setDatabaseUrl("https://fulfillment-deco-step-2020.firebaseio.com")
              .setProjectId("fulfillment-deco-step-2020")
              .setThreadManager(
                  new ThreadManager() {

                    @Override
                    protected void releaseExecutor(FirebaseApp app, ExecutorService executor) {
                      executor.shutdownNow();
                    }

                    @Override
                    protected ThreadFactory getThreadFactory() {
                      // TODO Auto-generated method stub
                      return com.google.appengine.api.ThreadManager.backgroundThreadFactory();
                    }

                    @Override
                    protected ExecutorService getExecutor(FirebaseApp app) {
                      // TODO Auto-generated method stub
                      return Executors.newCachedThreadPool(
                          com.google.appengine.api.ThreadManager.backgroundThreadFactory());
                    }
                  })
              .build();
      app = FirebaseApp.initializeApp(options);
    }
    return app;
  }
}
