package com.google.sps;

import java.util.Date;
import java.util.List;

// Helper class to hold class name and visits lists
public class VisitParentDates {
  private List<Date> classDates;
  private List<Long> visitsPerClass;

  public VisitParentDates(List<Date> dates, List<Long> visits) {
    classDates = dates;
    visitsPerClass = visits;
  }
}
