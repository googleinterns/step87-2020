package com.google.sps.servlets;

import com.google.firebase.auth.FirebaseAuth;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Once class owner submits a TA email, retrieve that user and add them as a TA to the class
@WebServlet("/submit-roster")
public class SubmitRoster extends HttpServlet {

  private FirebaseAuth authInstance;

  // Get the current session
  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  // Add a user to the datastore
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String rosterNames = request.getParameter("roster").trim();

    // Split the emails and collapse whitespaces
    List<String> emailsContainer = Arrays.asList(rosterNames.split("\\s*,\\s*"));

    for (String email : emailsContainer) {
      response.setContentType("text/html;");
      response.getWriter().println(email);
    }
  }
}
