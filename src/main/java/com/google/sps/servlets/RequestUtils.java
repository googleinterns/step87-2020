package com.google.sps.servlets;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

public class RequestUtils extends HttpServlet {

  // Return empty string if no comment, otherwise return text
  public static String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }
}
