package com.google.sps.models;

import java.util.Date;
import java.util.List;

// Helper class to hold dates and times lists
public class WaitData {
  private List<Date> dates;
  private List<Long> waitTimes;

  public WaitData(List<Date> classDates, List<Long> times) {
    dates = classDates;
    waitTimes = times;
  }
}
