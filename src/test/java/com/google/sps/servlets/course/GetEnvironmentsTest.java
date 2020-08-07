package com.google.sps.servlets.course;

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
import com.google.gson.Gson;
import com.google.sps.authentication.Authenticator;
import com.google.sps.models.Environment;
import java.io.PrintWriter;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GetEnvironmentsTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PrintWriter writer;
  @Mock Authenticator auth;

  @InjectMocks GetEnvironments servlet;

  private DatastoreService datastore;

  private final String ID_TOKEN = "ID_TOKEN";
  private final String STATUS1 = "STATUS1";
  private final String STATUS2 = "STATUS2";
  private final String STATUS3 = "STATUS3";
  private final String NAME1 = "NAME1";
  private final String NAME2 = "NAME2";
  private final String NAME3 = "NAME3";
  private final String IMAGE1 = "IMAGE1";
  private final String IMAGE2 = "IMAGE2";
  private final String IMAGE3 = "IMAGE3";
  private final String ERROR = "ERROR";

  private Entity classEntity;
  private Entity classEntity2;
  private Entity env1;
  private Entity env2;
  private Entity env3;
  private Entity env4;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    classEntity = new Entity("Class");
    classEntity2 = new Entity("Class");

    env1 = new Entity("Environment");
    env1.setProperty("name", NAME1);
    env1.setProperty("status", STATUS1);
    env1.setProperty("image", IMAGE1);
    env1.setProperty("class", classEntity.getKey());

    env2 = new Entity("Environment");
    env2.setProperty("name", NAME2);
    env2.setProperty("status", STATUS2);
    env2.setProperty("image", IMAGE2);
    env2.setProperty("class", classEntity.getKey());

    env3 = new Entity("Environment");
    env3.setProperty("name", NAME3);
    env3.setProperty("status", STATUS3);
    env3.setProperty("image", IMAGE3);
    env3.setProperty("class", classEntity2.getKey());

    env4 = new Entity("Environment");
    env4.setProperty("name", NAME2);
    env4.setProperty("status", STATUS2);
    env4.setProperty("image", IMAGE2);
    env4.setProperty("error", ERROR);
    env4.setProperty("class", classEntity.getKey());
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void doGet() throws Exception {
    datastore.put(classEntity);
    datastore.put(classEntity2);
    datastore.put(env1);
    datastore.put(env2);
    datastore.put(env3);

    when(req.getParameter(eq("classID"))).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyInClass(eq(ID_TOKEN), eq(KeyFactory.keyToString(classEntity.getKey()))))
        .thenReturn(true);
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(writer, times(1))
        .print(
            new Gson()
                .toJson(
                    Arrays.asList(
                        new Environment(
                            NAME1, STATUS1, null, KeyFactory.keyToString(env1.getKey())),
                        new Environment(
                            NAME2, STATUS2, null, KeyFactory.keyToString(env2.getKey())))));
  }

  @Test
  public void doGetEmpty() throws Exception {
    datastore.put(classEntity);

    when(req.getParameter(eq("classID"))).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyInClass(eq(ID_TOKEN), eq(KeyFactory.keyToString(classEntity.getKey()))))
        .thenReturn(true);
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(writer, times(1)).print(new Gson().toJson(Arrays.asList()));
  }

  @Test
  public void doGetWithStatus() throws Exception {
    datastore.put(classEntity);
    datastore.put(classEntity2);
    datastore.put(env1);
    datastore.put(env2);
    datastore.put(env3);

    when(req.getParameter(eq("classID"))).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getParameter(eq("status"))).thenReturn(STATUS1);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyInClass(eq(ID_TOKEN), eq(KeyFactory.keyToString(classEntity.getKey()))))
        .thenReturn(true);
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(writer, times(1))
        .print(
            new Gson()
                .toJson(
                    Arrays.asList(
                        new Environment(
                            NAME1, STATUS1, null, KeyFactory.keyToString(env1.getKey())))));
  }

  @Test
  public void doGetEmptyAuthFail() throws Exception {
    datastore.put(classEntity);

    when(req.getParameter(eq("classID"))).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyInClass(eq(ID_TOKEN), eq(KeyFactory.keyToString(classEntity.getKey()))))
        .thenReturn(false);

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void doGetWithError() throws Exception {
    datastore.put(classEntity);
    datastore.put(classEntity2);
    datastore.put(env1);
    datastore.put(env4);
    datastore.put(env3);

    when(req.getParameter(eq("classID"))).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyInClass(eq(ID_TOKEN), eq(KeyFactory.keyToString(classEntity.getKey()))))
        .thenReturn(true);
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(writer, times(1))
        .print(
            new Gson()
                .toJson(
                    Arrays.asList(
                        new Environment(
                            NAME1, STATUS1, null, KeyFactory.keyToString(env1.getKey())),
                        new Environment(
                            NAME2, STATUS2, ERROR, KeyFactory.keyToString(env4.getKey())))));
  }
}
