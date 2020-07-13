// package com.google.sps.servlets;

// import static org.junit.Assert.assertTrue;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import com.google.appengine.api.datastore.DatastoreService;
// import com.google.appengine.api.datastore.DatastoreServiceFactory;
// import com.google.appengine.api.datastore.EmbeddedEntity;
// import com.google.appengine.api.datastore.Entity;
// import com.google.appengine.api.datastore.KeyFactory;
// import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
// import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.UserRecord;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.List;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.MockitoJUnitRunner;

// @RunWith(MockitoJUnitRunner.class)
// public class TATest {
//   private final LocalServiceTestHelper helper =
//       new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

//   private DatastoreService datastore;

//   @Mock FirebaseAuth authInstance;

//   @InjectMocks AddTA addTA;

//   @Mock HttpServletRequest httpRequest;

//   @Mock HttpServletResponse httpResponse;

//   @Before
//   public void setUp() {
//     helper.setUp();
//     datastore = DatastoreServiceFactory.getDatastoreService();
//   }

//   @After
//   public void tearDown() {
//     helper.tearDown();
//   }

//   @Test
//   // For a class with no current TAs, add one TA
//   public void addOneTAEmptyList() throws Exception {
//     Entity classEntity = new Entity("Class");

//     classEntity.setProperty("owner", "ownerID");
//     classEntity.setProperty("name", "testClass");
//     classEntity.setProperty("beingHelped", new EmbeddedEntity());
//     classEntity.setProperty("studentQueue", Collections.emptyList());
//     classEntity.setProperty("taList", Collections.emptyList());
//     classEntity.setProperty("visitKey", "");

//     datastore.put(classEntity);

//     // Create examples for the TA email and class code
//     when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
//     when(httpRequest.getParameter("classCode"))
//         .thenReturn(KeyFactory.keyToString(classEntity.getKey()));

//     // Firebase needs to return the correct user ID for the TA
//     UserRecord userRecord = mock(UserRecord.class);
//     when(authInstance.getUserByEmail("test@google.com")).thenReturn(userRecord);

//     addTA.doPost(httpRequest, httpResponse);

//     // Obtain the corresponding class entity and its TA list
//     Entity testEntity = datastore.get(classEntity.getKey());
//     List<String> listOfClassTAs = (List) testEntity.getProperty("taList");

//     // Verify the new TA was added to the class TA roster
//     assertTrue(listOfClassTAs.contains("test@google.com"));
//   }

//   @Test
//   // Add one TA to a non-empty TA list
//   public void addOneTA() throws Exception {
//     Entity classEntity = new Entity("Class");
//     List<String> taList = Arrays.asList("exTA1@google.com", "exTA2@google.com");

//     classEntity.setProperty("owner", "ownerID");
//     classEntity.setProperty("name", "testClass");
//     classEntity.setProperty("beingHelped", new EmbeddedEntity());
//     classEntity.setProperty("studentQueue", Collections.emptyList());
//     classEntity.setProperty("taList", taList);
//     classEntity.setProperty("visitKey", "");

//     datastore.put(classEntity);

//     // Create examples for the TA email and class code
//     when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
//     when(httpRequest.getParameter("classCode"))
//         .thenReturn(KeyFactory.keyToString(classEntity.getKey()));

//     addTA.doPost(httpRequest, httpResponse);

//     // Obtain the corresponding class entity and its TA list
//     Entity testEntity = datastore.get(classEntity.getKey());
//     List<String> listOfClassTAs = (List) testEntity.getProperty("taList");

//     // Verify the new TA was added to the class TA roster
//     assertTrue(listOfClassTAs.contains("test@google.com"));
//   }

//   @Test
//   // Throw an exception if class key isn't correct
//   public void keyUnavailable() throws Exception {

//     // Create examples for the TA email and class code
//     when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
//     when(httpRequest.getParameter("classCode")).thenReturn("testClassCode");

//     addTA.doPost(httpRequest, httpResponse);

//     verify(httpResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
//   }

//   @Test
//   // Throw an exception when the class doesn't exist
//   public void taClassDoesNotExist() throws Exception {

//     Entity classEntity = new Entity("Class");
//     datastore.put(classEntity);
//     String classCode = KeyFactory.keyToString(classEntity.getKey());
//     datastore.delete(classEntity.getKey());

//     // Create examples for the TA email and class code
//     when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
//     when(httpRequest.getParameter("classCode")).thenReturn(classCode);

//     addTA.doPost(httpRequest, httpResponse);

//     verify(httpResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
//   }

//   @Test
//   // Check correct size update for a class with multiple TAs
//   public void verifyListSize() throws Exception {
//     Entity classEntity = new Entity("Class");
//     List<String> taList =
//         Arrays.asList(
//             "exTA1@google.com",
//             "exTA2@google.com",
//             "exTA3@google.com",
//             "exTA4@gmail.com",
//             "exTA5@gmail.com");

//     classEntity.setProperty("owner", "ownerID");
//     classEntity.setProperty("name", "testClass");
//     classEntity.setProperty("beingHelped", new EmbeddedEntity());
//     classEntity.setProperty("studentQueue", Collections.emptyList());
//     classEntity.setProperty("taList", taList);
//     classEntity.setProperty("visitKey", "");

//     datastore.put(classEntity);

//     // Create examples for the TA email and class code
//     when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
//     when(httpRequest.getParameter("classCode"))
//         .thenReturn(KeyFactory.keyToString(classEntity.getKey()));

//     addTA.doPost(httpRequest, httpResponse);

//     // Obtain the corresponding class entity and its TA list
//     Entity testEntity = datastore.get(classEntity.getKey());
//     List<String> listOfClassTAs = (List) testEntity.getProperty("taList");

//     // Verify the new TA was added to the class TA roster
//     assertTrue(listOfClassTAs.contains("test@google.com"));
//     assertTrue(listOfClassTAs.size() == 6);
//   }
// }
