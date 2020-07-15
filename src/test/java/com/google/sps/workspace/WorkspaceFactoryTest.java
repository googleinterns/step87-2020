package com.google.sps.workspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceFactoryTest {
  @Mock DatabaseReference reference;
  @Mock DatabaseReference classRef;
  @Mock ApiFuture<Void> apiFutureClass;

  @Test(expected = NullPointerException.class)
  public void workspaceNullRef() throws Exception {
    WorkspaceFactory.getInstance().create("", null);
  }

  @Test(expected = NullPointerException.class)
  public void workspaceNullClass() throws Exception {
    WorkspaceFactory.getInstance().create(null, reference);
  }

  @Test
  public void workspace() throws Exception {
    String CLASS = "CLASS";

    when(reference.child(eq("classID"))).thenReturn(classRef);
    when(classRef.setValueAsync(any())).thenReturn(apiFutureClass);

    WorkspaceFactory.getInstance().create(CLASS, reference);

    verify(reference, times(1)).child(eq("classID"));
    verify(classRef, times(1)).setValueAsync(eq(CLASS));
    verify(apiFutureClass, times(1)).get();
  }
}
