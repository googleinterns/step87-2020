package com.google.sps.servlets;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*; 
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreServiceConfig;


/** Servlet that creates a new class Datastore.*/
@WebServlet("/class")
public class ClassServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      return;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form.
    // navigate to /_ah/admin to view Datastore

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());

    String className = request.getParameter("className");
    String enter = request.getParameter("enter");

    if (enter.equals("create a new class!")){
      ArrayList<String> emptyQueue = new ArrayList<String>();

      Entity classEntity = new Entity("Class");
      classEntity.setProperty("owner", "");
      classEntity.setProperty("name", className);
      classEntity.setProperty("beingHelped", "");
      classEntity.setProperty("studentQueue", emptyQueue);
      classEntity.setProperty("beingHelped", "");

      datastore.put(classEntity);
    } 
    
    else {
      try {
        Key classCode = KeyFactory.stringToKey(request.getParameter("classCode"));
        Entity classEntity = datastore.get(classCode);
        ArrayList<String> updatedQueue = (ArrayList) classEntity.getProperty("studentQueue");
        updatedQueue.add("student");
        classEntity.setProperty("studentQueue", updatedQueue);

        datastore.put(classEntity);
        } 
        
        catch (Exception e) {
            System.out.println("try again");
        }
    }
  }
}