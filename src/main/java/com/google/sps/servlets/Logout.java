package com.google.sps.servlets;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/sign-out")
public class Logout extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final int maxAge = 0;
    Cookie newCookie = new Cookie("session", " ");
    newCookie.setMaxAge(maxAge); // Set the expiration date to immediate deletion
    response.addCookie(newCookie);
    response.sendRedirect("/index.html"); // Go back to the main page
  }
}
