package com.google.sps.workspace;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import java.util.Objects;

/**
 * Provides an interface to access and modify workspaces in the datastore. The contents of this
 * object will only reflect the current state of the entities. It will not update with the
 * datastore.
 */
public class Workspace {
  private static final String KIND = "Workspace";

  private final transient DatastoreService datastore;
  private String studentUID;
  private String TaUID;
  private String workspaceID;

  private Workspace() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  public Workspace(String studentUID, String TaUID) {
    this();
    this.studentUID = Objects.requireNonNull(studentUID);
    this.TaUID = Objects.requireNonNull(TaUID);

    Entity entity = new Entity(KIND);
    entity.setProperty("studentUID", studentUID);
    entity.setProperty("taUID", TaUID);

    workspaceID = KeyFactory.keyToString(datastore.put(entity));
  }

  public Workspace(String workspaceID) throws EntityNotFoundException {
    this();
    this.workspaceID = Objects.requireNonNull(workspaceID);

    Entity entity = datastore.get(KeyFactory.stringToKey(workspaceID));
    studentUID = (String) entity.getProperty("studentUID");
    TaUID = (String) entity.getProperty("taUID");
  }

  /** @return the studentUID */
  public String getStudentUID() {
    return studentUID;
  }

  /** @return the taUID */
  public String getTaUID() {
    return TaUID;
  }

  /** @return the workspaceID */
  public String getWorkspaceID() {
    return workspaceID;
  }
}
