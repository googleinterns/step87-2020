package com.google.sps;

import java.util.List;

// Helper class to hold class name and visits lists
public class VisitParent {
  private List<String> listOfClassNames;
  private List<Integer> visitsPerClass;

  public VisitParent(List<String> names, List<Integer> visits) {
    listOfClassNames = names;
    visitsPerClass = visits;
  }
}
