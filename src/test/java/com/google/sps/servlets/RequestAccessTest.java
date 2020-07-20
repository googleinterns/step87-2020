package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.sps.ApplicationDefaults;
import com.google.sps.utils.TransportDelegate;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestAccessTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          // Use High Rep job policy to allow cross group transactions in tests.
          new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

  private DatastoreService datastore;

  @Mock FirebaseAuth auth;
  @Mock TransportDelegate transportDelegate;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;

  @Captor ArgumentCaptor<Message> msgCaptor;

  @InjectMocks RequestAccess servlet;

  private String ID_TOKEN = "ID_TOKEN";
  private String FROM_EMAIL = "from@example.com";
  private String USER_EMAIL = "user@example.com";
  private String USER_NAME = "USER_NAME";
  private String OWNER_EMAIL = "owner@example.com";
  private String OWNER_ID = "OWNER_ID";
  private String OWNER_NAME = "OWNER_NAME";
  private String SCHEME = "https";
  private String SERVER_NAME = "SERVER_NAME";

  @Before
  public void setUp() {
    helper.setUp();
    servlet.FROM_ADDRESS = FROM_EMAIL;
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void doGetUserDoesntExist() throws Exception {
    Entity classEntity = new Entity("Class");
    classEntity.setProperty("owner", OWNER_ID);
    datastore.put(classEntity);

    when(req.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(req.getParameter("classCode")).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getScheme()).thenReturn(SCHEME);
    when(req.getServerName()).thenReturn(SERVER_NAME);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(USER_EMAIL);
    when(tok.getName()).thenReturn(USER_NAME);

    UserRecord owner = mock(UserRecord.class);
    when(auth.getUser(OWNER_ID)).thenReturn(owner);
    when(owner.getEmail()).thenReturn(OWNER_EMAIL);
    when(owner.getDisplayName()).thenReturn(OWNER_NAME);

    servlet.doGet(req, resp);

    verify(transportDelegate, times(1)).send(msgCaptor.capture());

    Message msg = msgCaptor.getValue();

    assertEquals(new InternetAddress(FROM_EMAIL), msg.getFrom()[0]);
    assertEquals(
        new InternetAddress(OWNER_EMAIL, OWNER_NAME),
        msg.getRecipients(Message.RecipientType.TO)[0]);

    assertTrue(((String) msg.getContent()).contains(USER_EMAIL));
    assertTrue(
        ((String) msg.getContent())
            .contains(
                new URL(
                        req.getScheme(),
                        req.getServerName(),
                        ApplicationDefaults.DASHBOARD.concat(
                            KeyFactory.keyToString(classEntity.getKey())))
                    .toString()));
  }

  @Test
  public void doGetUserExists() throws Exception {
    Entity classEntity = new Entity("Class");
    classEntity.setProperty("owner", OWNER_ID);
    datastore.put(classEntity);

    Entity user = new Entity("User");
    user.setProperty("userEmail", USER_EMAIL);
    user.setProperty("registeredClasses", Collections.emptyList());
    datastore.put(user);

    when(req.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(req.getParameter("classCode")).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getScheme()).thenReturn(SCHEME);
    when(req.getServerName()).thenReturn(SERVER_NAME);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(USER_EMAIL);
    when(tok.getName()).thenReturn(USER_NAME);

    UserRecord owner = mock(UserRecord.class);
    when(auth.getUser(OWNER_ID)).thenReturn(owner);
    when(owner.getEmail()).thenReturn(OWNER_EMAIL);
    when(owner.getDisplayName()).thenReturn(OWNER_NAME);

    servlet.doGet(req, resp);

    verify(transportDelegate, times(1)).send(msgCaptor.capture());

    Message msg = msgCaptor.getValue();

    assertEquals(new InternetAddress(FROM_EMAIL), msg.getFrom()[0]);
    assertEquals(
        new InternetAddress(OWNER_EMAIL, OWNER_NAME),
        msg.getRecipients(Message.RecipientType.TO)[0]);

    assertTrue(((String) msg.getContent()).contains(USER_EMAIL));
    assertTrue(
        ((String) msg.getContent())
            .contains(
                new URL(
                        req.getScheme(),
                        req.getServerName(),
                        ApplicationDefaults.DASHBOARD.concat(
                            KeyFactory.keyToString(classEntity.getKey())))
                    .toString()));
  }

  @Test
  public void doGetAlreadyInClass() throws Exception {
    Entity classEntity = new Entity("Class");
    classEntity.setProperty("owner", OWNER_ID);
    datastore.put(classEntity);

    Entity user = new Entity("User");
    user.setProperty("userEmail", USER_EMAIL);
    user.setProperty("registeredClasses", Arrays.asList(classEntity.getKey()));
    datastore.put(user);

    when(req.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(req.getParameter("classCode")).thenReturn(KeyFactory.keyToString(classEntity.getKey()));

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(USER_EMAIL);

    servlet.doGet(req, resp);

    verify(transportDelegate, times(0)).send(any());
  }

  @Test
  public void doGetWithTAs() throws Exception {
    Entity classEntity = new Entity("Class");
    classEntity.setProperty("owner", OWNER_ID);
    datastore.put(classEntity);

    Entity classEntity2 = new Entity("Class");
    classEntity.setProperty("owner", OWNER_ID);
    datastore.put(classEntity2);

    Entity user = new Entity("User");
    user.setProperty("userEmail", USER_EMAIL);
    user.setProperty("registeredClasses", Collections.emptyList());
    datastore.put(user);

    String TA_EMAIL_1 = "ta1@example.com";
    String TA_EMAIL_2 = "ta2@example.com";

    Entity ta1 = new Entity("User");
    ta1.setProperty("userEmail", TA_EMAIL_1);
    ta1.setProperty("taClasses", Arrays.asList(classEntity.getKey()));
    datastore.put(ta1);

    Entity ta2 = new Entity("User");
    ta2.setProperty("userEmail", TA_EMAIL_2);
    ta2.setProperty("taClasses", Arrays.asList(classEntity2.getKey(), classEntity.getKey()));
    datastore.put(ta2);

    when(req.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(req.getParameter("classCode")).thenReturn(KeyFactory.keyToString(classEntity.getKey()));
    when(req.getScheme()).thenReturn(SCHEME);
    when(req.getServerName()).thenReturn(SERVER_NAME);

    FirebaseToken tok = mock(FirebaseToken.class);
    when(auth.verifyIdToken(ID_TOKEN)).thenReturn(tok);
    when(tok.getEmail()).thenReturn(USER_EMAIL);
    when(tok.getName()).thenReturn(USER_NAME);

    UserRecord owner = mock(UserRecord.class);
    when(auth.getUser(OWNER_ID)).thenReturn(owner);
    when(owner.getEmail()).thenReturn(OWNER_EMAIL);
    when(owner.getDisplayName()).thenReturn(OWNER_NAME);

    servlet.doGet(req, resp);

    verify(transportDelegate, times(1)).send(msgCaptor.capture());

    Message msg = msgCaptor.getValue();

    assertEquals(new InternetAddress(FROM_EMAIL), msg.getFrom()[0]);
    assertEquals(
        new InternetAddress(OWNER_EMAIL, OWNER_NAME),
        msg.getRecipients(Message.RecipientType.TO)[0]);

    assertTrue(
        Arrays.asList(msg.getRecipients(Message.RecipientType.CC))
            .contains(new InternetAddress(TA_EMAIL_1)));
    assertTrue(
        Arrays.asList(msg.getRecipients(Message.RecipientType.CC))
            .contains(new InternetAddress(TA_EMAIL_2)));

    assertTrue(((String) msg.getContent()).contains(USER_EMAIL));
    assertTrue(
        ((String) msg.getContent())
            .contains(
                new URL(
                        req.getScheme(),
                        req.getServerName(),
                        ApplicationDefaults.DASHBOARD.concat(
                            KeyFactory.keyToString(classEntity.getKey())))
                    .toString()));
  }
}
