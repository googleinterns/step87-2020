package com.google.sps.servlets;

import java.util.Date;
import java.util.List;

// Helper class to hold dates and times lists
public class WaitParent {
  private List<Date> dates;
  private List<Long> waitTimes;

  public WaitParent(List<Date> classDates, List<Long> times) {
    dates = classDates;
    waitTimes = times;
  }
}
