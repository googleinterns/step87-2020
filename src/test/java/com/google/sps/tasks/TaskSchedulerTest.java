package com.google.sps.tasks;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.repackaged.org.apache.commons.httpclient.URI;
import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import java.time.Clock;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TaskSchedulerTest {
  @Mock CloudTasksClient client;

  @Captor ArgumentCaptor<Task> taskCaptor;

  private String PROJECT = "PROJECT";
  private String QUEUE_NAME = "QUEUE_NAME";
  private String LOCATION = "LOCATION";
  private String URI = "URI";
  private String PAYLOAD = "PAYLOAD";
  private HttpMethod HTTP_METHOD = HttpMethod.POST;
  private long SECONDS = 10;

  @Test
  public void schedule() throws Exception {
    TaskScheduler scheduler =
        spy(new TaskScheduler(PROJECT, QUEUE_NAME, LOCATION, URI, HTTP_METHOD));

    when(scheduler.getClient()).thenReturn(client);

    scheduler.schedule(PAYLOAD);

    verify(client, times(1))
        .createTask(
            eq(QueueName.of(PROJECT, LOCATION, QUEUE_NAME).toString()), taskCaptor.capture());

    AppEngineHttpRequest appengineReq = taskCaptor.getValue().getAppEngineHttpRequest();

    assertEquals(PAYLOAD, appengineReq.getBody().toStringUtf8());
    assertEquals(URI, appengineReq.getRelativeUri());
    assertEquals(HTTP_METHOD, appengineReq.getHttpMethod());
    assertEquals(0, taskCaptor.getValue().getScheduleTime().getSeconds());
  }

  @Test
  public void scheduleWithSeconds() throws Exception {
    TaskScheduler scheduler =
        spy(new TaskScheduler(PROJECT, QUEUE_NAME, LOCATION, URI, HTTP_METHOD));

    Instant now = Instant.now(Clock.systemUTC());
    when(scheduler.getClient()).thenReturn(client);
    when(scheduler.getInstant()).thenReturn(now);

    scheduler.schedule(PAYLOAD, SECONDS);

    verify(client, times(1))
        .createTask(
            eq(QueueName.of(PROJECT, LOCATION, QUEUE_NAME).toString()), taskCaptor.capture());

    AppEngineHttpRequest appengineReq = taskCaptor.getValue().getAppEngineHttpRequest();

    assertEquals(PAYLOAD, appengineReq.getBody().toStringUtf8());
    assertEquals(URI, appengineReq.getRelativeUri());
    assertEquals(HTTP_METHOD, appengineReq.getHttpMethod());
    assertEquals(
        now.plusSeconds(SECONDS).getEpochSecond(),
        taskCaptor.getValue().getScheduleTime().getSeconds());
  }
}
