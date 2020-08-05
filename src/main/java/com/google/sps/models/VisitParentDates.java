package com.google.sps.models;

import java.util.Date;
import java.util.List;

// Helper class to hold dates and visits lists
public class VisitParentDates {
  private List<Date> dates;
  private List<Long> classVisits;

  public VisitParentDates(List<Date> classDates, List<Long> visits) {
    dates = classDates;
    classVisits = visits;
  }

  /** @return the dates */
  public List<Date> getDates() {
    return dates;
  }

  /** @return the classVisits */
  public List<Long> getClassVisits() {
    return classVisits;
  }
}
