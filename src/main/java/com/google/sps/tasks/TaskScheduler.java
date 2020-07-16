package com.google.sps.tasks;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class TaskScheduler {
  private final String projectID;
  private final String queueName;
  private final String location;
  private final String URI;
  private final HttpMethod httpMethod;

  protected TaskScheduler(
      String projectID, String queueName, String location, String URI, HttpMethod httpMethod) {
    this.projectID = projectID;
    this.queueName = Objects.requireNonNull(queueName);
    this.location = location;
    this.URI = Objects.requireNonNull(URI);
    this.httpMethod = httpMethod;
  }

  private CloudTasksClient getClient() throws IOException {
    return CloudTasksClient.create();
  }

  public void schedule(String payload) throws IOException {
    schedule(payload, 0);
  }

  public void schedule(String payload, long seconds) throws IOException {
    try (CloudTasksClient client = getClient()) {
      String queuePath = QueueName.of(projectID, location, queueName).toString();

      Task.Builder taskBuilder =
          Task.newBuilder()
              .setAppEngineHttpRequest(
                  AppEngineHttpRequest.newBuilder()
                      .setBody(ByteString.copyFrom(payload, Charset.defaultCharset()))
                      .setRelativeUri(URI)
                      .setHttpMethod(httpMethod)
                      .build());

      if (seconds > 0) {
        taskBuilder.setScheduleTime(
            Timestamp.newBuilder()
                .setSeconds(Instant.now(Clock.systemUTC()).plusSeconds(seconds).getEpochSecond()));
      }

      client.createTask(queuePath, taskBuilder.build());
    }
  }
}
