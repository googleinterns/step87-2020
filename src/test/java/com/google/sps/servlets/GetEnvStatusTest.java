package com.google.sps.servlets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GetEnvStatusTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PrintWriter printWriter;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void doGetTest() throws Exception {
    final String STATUS = "STATUS";

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    when(req.getParameter(eq("envID"))).thenReturn(envID);
    when(resp.getWriter()).thenReturn(printWriter);

    new GetEnvStatus().doGet(req, resp);

    verify(printWriter, times(1)).print(eq(STATUS));
  }

  @Test
  public void doGetTestNoEntity() throws Exception {
    when(req.getParameter(eq("envID"))).thenReturn("invalid key");

    new GetEnvStatus().doGet(req, resp);

    verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
  }
}
