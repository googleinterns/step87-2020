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
  @Mock DatabaseReference studentRef;
  @Mock DatabaseReference classRef;
  @Mock DatabaseReference taRef;
  @Mock ApiFuture<Void> apiFutureStudent;
  @Mock ApiFuture<Void> apiFutureTA;
  @Mock ApiFuture<Void> apiFutureClass;

  @Test(expected = NullPointerException.class)
  public void workspaceNullRef() throws Exception {
    WorkspaceFactory.getInstance().fromStudentAndTA("", "", "", null);
  }

  @Test(expected = NullPointerException.class)
  public void workspaceNullTA() throws Exception {
    WorkspaceFactory.getInstance().fromStudentAndTA("", "", null, reference);
  }

  @Test(expected = NullPointerException.class)
  public void workspaceNullStudent() throws Exception {
    WorkspaceFactory.getInstance().fromStudentAndTA("", null, "", reference);
  }

  @Test(expected = NullPointerException.class)
  public void workspaceNullClass() throws Exception {
    WorkspaceFactory.getInstance().fromStudentAndTA(null, "", "", reference);
  }

  @Test
  public void workspace() throws Exception {
    String STUDENT = "STUDENT";
    String TA = "TA";
    String CLASS = "CLASS";

    when(reference.child(eq("student"))).thenReturn(studentRef);
    when(reference.child(eq("ta"))).thenReturn(taRef);
    when(reference.child(eq("classID"))).thenReturn(classRef);
    when(studentRef.setValueAsync(any())).thenReturn(apiFutureStudent);
    when(taRef.setValueAsync(any())).thenReturn(apiFutureTA);
    when(classRef.setValueAsync(any())).thenReturn(apiFutureClass);

    WorkspaceFactory.getInstance().fromStudentAndTA(CLASS, STUDENT, TA, reference);

    verify(reference, times(1)).child(eq("student"));
    verify(reference, times(1)).child(eq("ta"));
    verify(reference, times(1)).child(eq("classID"));

    verify(studentRef, times(1)).setValueAsync(eq(STUDENT));
    verify(taRef, times(1)).setValueAsync(eq(TA));
    verify(classRef, times(1)).setValueAsync(eq(CLASS));

    verify(apiFutureStudent, times(1)).get();
    verify(apiFutureTA, times(1)).get();
    verify(apiFutureClass, times(1)).get();
  }
}
