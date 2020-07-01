package com.google.sps.visits;

import java.util.List;

// Helper class to hold class name and visits lists
public class VisitParent {
  private List<String> listOfClassNames;
  private List<Long> visitsPerClass;

  public VisitParent(List<String> names, List<Long> visits) {
    listOfClassNames = names;
    visitsPerClass = visits;
  }
}
