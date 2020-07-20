package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.sps.ApplicationDefaults;
import com.google.sps.firebase.FirebaseAppManager;
import com.google.sps.utils.TransportDelegate;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/requestAccess")
public class RequestAccess extends HttpServlet {
  private FirebaseAuth auth;
  private TransportDelegate transportDelegate;
  protected String FROM_ADDRESS;

  @Override
  public void init() throws ServletException {
    FROM_ADDRESS = System.getenv("FROM_ADDRESS");
    transportDelegate = new TransportDelegate();

    try {
      auth = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String idToken = req.getParameter("idToken");
    String classCode = req.getParameter("classCode");
    Key classKey = KeyFactory.stringToKey(classCode);

    try {
      FirebaseToken tok = auth.verifyIdToken(idToken);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

      PreparedQuery query =
          datastore.prepare(
              new Query("User")
                  .setFilter(
                      new FilterPredicate("userEmail", FilterOperator.EQUAL, tok.getEmail())));

      Entity userEntity = query.asSingleEntity();
      List<Key> registeredClasses =
          userEntity != null ? (List<Key>) userEntity.getProperty("registeredClasses") : null;

      if (registeredClasses == null || !registeredClasses.contains(classKey)) {
        Entity classEntity = datastore.get(classKey);
        UserRecord owner = auth.getUser((String) classEntity.getProperty("owner"));

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_ADDRESS));
        msg.addRecipient(
            Message.RecipientType.TO,
            new InternetAddress(owner.getEmail(), owner.getDisplayName()));

        msg.setSubject("Access request");

        msg.setText(
            new StringBuilder()
                .append("Hello ")
                .append(owner.getDisplayName())
                .append(", \n\n")
                .append("The user ")
                .append(tok.getName())
                .append(" (")
                .append(tok.getEmail())
                .append(") has requested access to your class: ")
                .append(classEntity.getProperty("name"))
                .append(".\n\n")
                .append("To grant them access to this class please add them via the dashboard: ")
                .append(
                    new URL(
                        req.getScheme(),
                        req.getServerName(),
                        ApplicationDefaults.DASHBOARD.concat(classCode)))
                .toString());

        transportDelegate.send(msg);
      }
    } catch (FirebaseAuthException e) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    } catch (EntityNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (MessagingException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
