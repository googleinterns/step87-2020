package com.google.sps.tasks;

import com.google.cloud.tasks.v2.HttpMethod;

public class TaskSchedulerFactory {
  private static final TaskSchedulerFactory FACTORY = new TaskSchedulerFactory();

  private TaskSchedulerFactory() {}

  public static TaskSchedulerFactory getInstance() {
    return FACTORY;
  }

  public TaskScheduler create(String queueName, String URI) {
    return create(
      System.getenv("GOOGLE_CLOUD_PROJECT"), queueName, System.getenv("LOCATION_ID"), URI, HttpMethod.POST);
  }

  public TaskScheduler create(String projectID, String queueName, String location, String URI, HttpMethod httpMethod) {
    return new TaskScheduler(projectID, queueName, location, URI, httpMethod);
  }
  
}