package com.google.sps.workspace;

import static org.junit.Assert.assertEquals;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WorkspaceTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  static final String STUDENT = "STUDENT";
  static final String TA = "TA";

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void WorkspaceNewEntity() throws Exception {
    Workspace w = new Workspace(STUDENT, TA);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity e = ds.get(KeyFactory.stringToKey(w.getWorkspaceID()));

    assertEquals(STUDENT, e.getProperty("studentUID"));
    assertEquals(TA, e.getProperty("taUID"));
  }

  @Test(expected = NullPointerException.class)
  public void WorkspaceNullStudent() throws Exception {
    new Workspace(null, TA);
  }

  @Test(expected = NullPointerException.class)
  public void WorkspaceNullTA() throws Exception {
    new Workspace(STUDENT, null);
  }

  @Test(expected = NullPointerException.class)
  public void WorkspaceNullID() throws Exception {
    new Workspace(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void WorkspaceNonExistantID() throws Exception {
    new Workspace("NotValid");
  }

  @Test
  public void WorkspaceID() throws Exception {
    String id = new Workspace(STUDENT, TA).getWorkspaceID();

    Workspace w = new Workspace(id);
    assertEquals(STUDENT, w.getStudentUID());
    assertEquals(TA, w.getTaUID());
  }

  @Test
  public void getStudentID() {
    Workspace w = new Workspace(STUDENT, TA);
    assertEquals(STUDENT, w.getStudentUID());
  }

  @Test
  public void getTaID() {
    Workspace w = new Workspace(STUDENT, TA);
    assertEquals(TA, w.getTaUID());
  }

  @Test
  public void getWorkspaceID() throws Exception {
    Workspace w1 = new Workspace(STUDENT, TA);
    Workspace w2 = new Workspace(w1.getWorkspaceID());
    assertEquals(w1.getWorkspaceID(), w2.getWorkspaceID());
  }
}
