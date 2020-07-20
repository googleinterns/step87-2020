package com.google.sps.authentication;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.sps.firebase.FirebaseAppManager;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class Authenticator {
  private FirebaseAuth auth;
  private DatastoreService datastore;

  public Authenticator() throws IOException {
    this(FirebaseAuth.getInstance(FirebaseAppManager.getApp()));
  }

  protected Authenticator(FirebaseAuth auth) {
    this.auth = Objects.requireNonNull(auth);
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  public boolean verifyTaOrOwner(String idToken, Key classKey) {
    try {
      FirebaseToken tok = auth.verifyIdToken(idToken);
      PreparedQuery q = datastore.prepare(new Query("User").setFilter(
        new FilterPredicate("userEmail", FilterOperator.EQUAL, tok.getEmail())
      ));

      if(q.countEntities(FetchOptions.Builder.withLimit(1)) > 0) {
        return ((List<Key>) q.asSingleEntity().getProperty("taClasses")).contains(classKey) ||
               ((List<Key>) q.asSingleEntity().getProperty("ownedClasses")).contains(classKey);
      } else {
        return false;
      }
    } catch (FirebaseAuthException | IllegalArgumentException e) {
      return false;
    }
    
  } 
}