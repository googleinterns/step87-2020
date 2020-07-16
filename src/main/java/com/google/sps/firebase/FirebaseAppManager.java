package com.google.sps.firebase;

import com.google.appengine.api.utils.SystemProperty;
import com.google.auth.appengine.AppEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.ThreadManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// Only create app one time
public class FirebaseAppManager {
  private static FirebaseApp app = null;

  private static GoogleCredentials getCredentials() throws IOException {
    if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
      List<String> scopes =
          Arrays.asList(
              "https://www.googleapis.com/auth/cloud-platform",
              "https://www.googleapis.com/auth/userinfo.email");
      return AppEngineCredentials.newBuilder().setScopes(scopes).build();
    } else {
      // Local development server
      return GoogleCredentials.getApplicationDefault();
    }
  }

  public static synchronized FirebaseApp getApp() throws IOException {
    if (app == null) {
      FirebaseOptions options =
          new FirebaseOptions.Builder()
              .setCredentials(getCredentials())
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
                      return new ThreadPoolExecutor(
                          0,
                          1,
                          60L,
                          TimeUnit.SECONDS,
                          new SynchronousQueue<Runnable>(),
                          getThreadFactory());
                    }
                  })
              .build();
      app = FirebaseApp.initializeApp(options);
    }
    return app;
  }
}
