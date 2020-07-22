package com.google.sps;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import com.google.appengine.api.datastore.DatastoreServiceConfig;

public class OnStart implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());
  }
}