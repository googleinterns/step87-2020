package com.google.sps.tasks;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

public class TaskScheduler {
  private final String projectID;
  private final String queueName;
  private final String location;
  private final String URI;
  private final HttpMethod httpMethod;


  protected TaskScheduler(String projectID, String queueName, String location, String URI, HttpMethod httpMethod) {
    this.projectID = projectID;
    this.queueName = Objects.requireNonNull(queueName);
    this.location = location;
    this.URI = Objects.requireNonNull(URI);
    this.httpMethod = httpMethod;
  }

  /**
   * @return the projectID
   */
  public String getProjectID() {
    return projectID != null ? projectID : System.getenv("GOOGLE_CLOUD_PROJECT");
  }

  /**
   * @return the queueName
   */
  public String getQueueName() {
    return queueName;
  }

  /**
   * @return the location
   */
  public String getLocation() {
    return location != null ? location : System.getenv("LOCATION_ID");
  }

  /**
   * @return the uRI
   */
  public String getURI() {
    return URI;
  }

  /**
   * @return the httpMethod
   */
  public HttpMethod getHttpMethod() {
    return httpMethod != null ? httpMethod : HttpMethod.POST;
  }

  private CloudTasksClient getClient() throws IOException {
    return CloudTasksClient.create();
  }

  public void schedule(String payload) throws IOException {
    schedule(payload, 0);
  }

  public void schedule(String payload, long seconds) throws IOException {
    try (CloudTasksClient client = getClient()) {
      String queuePath = QueueName.of(getProjectID(), getLocation(), getQueueName()).toString();

      Task.Builder taskBuilder = 
        Task.newBuilder()
            .setAppEngineHttpRequest(
              AppEngineHttpRequest.newBuilder()
              .setBody(
                ByteString.copyFrom(payload, Charset.defaultCharset())
              ).setRelativeUri(getURI())
              .setHttpMethod(getHttpMethod())
              .build()
            );

      if (seconds > 0) {
        taskBuilder.setScheduleTime(Timestamp.newBuilder().setSeconds(Instant.now(Clock.systemUTC()).plusSeconds(seconds).getEpochSecond()));
      }

      client.createTask(queuePath, taskBuilder.build());
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String projectID;
    private String queueName;
    private String location;
    private String URI;
    private HttpMethod httpMethod;

    private Builder() {}

    public Builder setProjectID(String projectID) {
      this.projectID = projectID;
      return this;
    }

    public Builder setQueueName(String queueName) {
      this.queueName = queueName;
      return this;
    }
    public Builder setLocation(String location) {
      this.location = location;
      return this;
    }
    public Builder setURI(String URI) {
      this.URI = URI;
      return this;
    }
    public Builder setHttpMethod(HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public TaskScheduler build() {
      return new TaskScheduler(projectID, queueName, location, URI, httpMethod);
    }
  }
}