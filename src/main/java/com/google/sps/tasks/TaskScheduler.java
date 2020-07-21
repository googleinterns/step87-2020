package com.google.sps.tasks;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** This is a utility class to schedule Cloud Tasks on a given queue at some point in the future */
public class TaskScheduler {
  private final String projectID;
  private final String queueName;
  private final String location;
  private final String URI;
  private final HttpMethod httpMethod;

  /**
   * Creates a TaskScheduler
   *
   * @param projectID projectID of the task queue.
   * @param queueName name of the task queue.
   * @param location location of the task queue.
   * @param URI URI of the task servlet.
   * @param httpMethod HTTP Method to use to call the servlet.
   */
  protected TaskScheduler(
      String projectID, String queueName, String location, String URI, HttpMethod httpMethod) {
    this.projectID = Objects.requireNonNull(projectID);
    this.queueName = Objects.requireNonNull(queueName);
    this.location = Objects.requireNonNull(location);
    this.URI = Objects.requireNonNull(URI);
    this.httpMethod = Objects.requireNonNull(httpMethod);
  }

  @VisibleForTesting
  protected CloudTasksClient getClient() throws IOException {
    return CloudTasksClient.create();
  }

  @VisibleForTesting
  protected Instant getInstant() {
    return Instant.now(Clock.systemUTC());
  }

  /**
   * Schedules the task to run as soon as possible
   *
   * @param payload Payload for the task
   * @throws IOException
   */
  public void schedule(String payload) throws IOException {
    schedule(payload, 0);
  }

  /**
   * Schedules the task to run in the future.
   *
   * @param payload Payload for the task
   * @param seconds seconds in the future when the task should run.
   * @throws IOException
   */
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
            Timestamp.newBuilder().setSeconds(getInstant().plusSeconds(seconds).getEpochSecond()));
      }

      client.createTask(queuePath, taskBuilder.build());
    }
  }
}
