package com.google.sps.servlets;

import com.google.sps.tasks.TaskScheduler;
import javax.servlet.http.HttpServlet;

public abstract class QueueServlet extends HttpServlet {
  protected TaskScheduler.Builder getTaskBuilder() {
    return TaskScheduler.builder();
  }
}
