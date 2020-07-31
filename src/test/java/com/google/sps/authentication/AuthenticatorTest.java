package com.google.sps.authentication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.sps.workspace.Workspace;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticatorTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock FirebaseAuth auth;
  @Mock Workspace workspace;

  @InjectMocks Authenticator authenticator;

  private String ID_TOKEN = "ID_TOKEN";
  private String EMAIL = "EMAIL";
  private String UID = "UID";
  private String BAD_UID = "BAD_UID";

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void verifyInClassStudent() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));
    Key classKey2 = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Arrays.asList(classKey2, classKey));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertTrue(authenticator.verifyInClass(ID_TOKEN, classKey));
  }

  @Test
  public void verifyInClassTA() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));
    Key classKey2 = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Arrays.asList(classKey2, classKey));
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertTrue(authenticator.verifyInClass(ID_TOKEN, classKey));
  }

  @Test
  public void verifyInClassOwner() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));
    Key classKey2 = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Arrays.asList(classKey2, classKey));
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertTrue(authenticator.verifyInClass(ID_TOKEN, classKey));
  }

  @Test
  public void verifyInClassFail() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertFalse(authenticator.verifyInClass(ID_TOKEN, classKey));
  }

  @Test
  public void verifyInClassException() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    when(auth.verifyIdToken(ID_TOKEN))
        .thenThrow(new FirebaseAuthException("errorCode", "detailMessage"));

    assertFalse(authenticator.verifyInClass(ID_TOKEN, classKey));
  }

  @Test
  public void verifyTaOrStudentStudent() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));
    Key classKey2 = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Arrays.asList(classKey2, classKey));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertFalse(authenticator.verifyTaOrOwner(ID_TOKEN, classKey));
  }

  @Test
  public void verifyTaOrOwnerTA() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));
    Key classKey2 = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Arrays.asList(classKey2, classKey));
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertTrue(authenticator.verifyTaOrOwner(ID_TOKEN, classKey));
  }

  @Test
  public void verifyTaOrOwnerOwner() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));
    Key classKey2 = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Arrays.asList(classKey2, classKey));
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertTrue(authenticator.verifyTaOrOwner(ID_TOKEN, classKey));
  }

  @Test
  public void verifyTaOrOwnerFail() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertFalse(authenticator.verifyTaOrOwner(ID_TOKEN, classKey));
  }

  @Test
  public void verifyTaOrOwnerException() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    when(auth.verifyIdToken(ID_TOKEN))
        .thenThrow(new FirebaseAuthException("errorCode", "detailMessage"));

    assertFalse(authenticator.verifyTaOrOwner(ID_TOKEN, classKey));
  }

  @Test
  public void verifyWorkspaceStudent() throws Exception {
    FirebaseToken tok = mock(FirebaseToken.class);
    when(tok.getUid()).thenReturn(UID);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);

    CompletableFuture<String> studentFuture = new CompletableFuture<>();
    studentFuture.complete(UID);

    when(workspace.getStudentUID()).thenReturn(studentFuture);

    assertTrue(authenticator.verifyWorkspace(ID_TOKEN, workspace));
  }

  @Test
  public void verifyWorkspaceTa() throws Exception {
    FirebaseToken tok = mock(FirebaseToken.class);
    when(tok.getUid()).thenReturn(UID);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);

    CompletableFuture<String> studentFuture = new CompletableFuture<>();
    studentFuture.complete(BAD_UID);
    CompletableFuture<String> taFuture = new CompletableFuture<>();
    taFuture.complete(UID);

    when(workspace.getStudentUID()).thenReturn(studentFuture);
    when(workspace.getTaUID()).thenReturn(taFuture);

    assertTrue(authenticator.verifyWorkspace(ID_TOKEN, workspace));
  }

  @Test
  public void verifyWorkspaceFail() throws Exception {
    FirebaseToken tok = mock(FirebaseToken.class);
    when(tok.getUid()).thenReturn(UID);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);

    CompletableFuture<String> studentFuture = new CompletableFuture<>();
    studentFuture.complete(BAD_UID);
    CompletableFuture<String> taFuture = new CompletableFuture<>();
    taFuture.complete(BAD_UID);

    when(workspace.getStudentUID()).thenReturn(studentFuture);
    when(workspace.getTaUID()).thenReturn(taFuture);

    assertFalse(authenticator.verifyWorkspace(ID_TOKEN, workspace));
  }

  @Test
  public void verifyWorkspaceException() throws Exception {
    when(auth.verifyIdToken(ID_TOKEN))
        .thenThrow(new FirebaseAuthException("errorCode", "detailMessage"));

    assertFalse(authenticator.verifyWorkspace(ID_TOKEN, workspace));
  }

  @Test
  public void verifyOwner() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));
    Key classKey2 = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Arrays.asList(classKey2, classKey));
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertTrue(authenticator.verifyOwner(ID_TOKEN, classKey));
  }

  @Test
  public void verifyOwnerFail() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(EMAIL);

    assertFalse(authenticator.verifyOwner(ID_TOKEN, classKey));
  }

  @Test
  public void verifyOwnerException() throws Exception {
    Key classKey = datastore.put(new Entity("Class"));

    Entity user = new Entity("User");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());
    user.setProperty("userEmail", EMAIL);
    datastore.put(user);

    when(auth.verifyIdToken(ID_TOKEN))
        .thenThrow(new FirebaseAuthException("errorCode", "detailMessage"));

    assertFalse(authenticator.verifyOwner(ID_TOKEN, classKey));
  }
}
