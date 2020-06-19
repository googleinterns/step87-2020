package com.google.sps.servlets;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.SessionCookieOptions;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/init")
public class SessionInit extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String idToken = RequestUtils.getParameter(request, "idToken", "");
    // Set session expiration to 5 days.
    long expiresIn = TimeUnit.DAYS.toMillis(5);
    SessionCookieOptions options = SessionCookieOptions.builder().setExpiresIn(expiresIn).build();
    try {
      // Create the session cookie. This will also verify the ID token in the process.
      // The session cookie will have the same claims as the ID token.
      String sessionCookie =
          FirebaseAuth.getInstance(FirebaseAppManager.getApp())
              .createSessionCookie(idToken, options);
      // Set cookie policy parameters as required.
      Cookie cookie = new Cookie("session", sessionCookie /* ... other parameters */);
      cookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(expiresIn));
      response.addCookie(cookie);
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN); // server understands request, but user cannot proceed w/o logging in
    }
  }
}
