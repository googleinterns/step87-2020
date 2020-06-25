package com.google.sps.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceTest {
  @Mock DatabaseReference reference;

  @Mock ApiFuture<Void> apiFutureStudent;

  @Mock ApiFuture<Void> apiFutureTA;

  @Mock DatabaseReference studentRef;

  @Mock DatabaseReference taRef;

  @Mock DataSnapshot snap;

  @Captor ArgumentCaptor<ValueEventListener> listenerCaptor;

  @Mock DatabaseError error;
  @Mock DatabaseException exception;

  @Test(expected = NullPointerException.class)
  public void workspaceNullRef() throws IOException, InterruptedException, ExecutionException {
    new Workspace("", "", null);
  }

  @Test(expected = NullPointerException.class)
  public void workspaceNullTA() throws IOException, InterruptedException, ExecutionException {
    new Workspace("", null, reference);
  }

  @Test(expected = NullPointerException.class)
  public void workspaceNullStudent() throws IOException, InterruptedException, ExecutionException {
    new Workspace(null, "", reference);
  }

  @Test
  public void workspace() throws InterruptedException, ExecutionException {
    final String STUDENT = "STUDENT";
    final String TA = "TA";

    when(reference.child(eq("student"))).thenReturn(studentRef);
    when(reference.child(eq("ta"))).thenReturn(taRef);
    when(studentRef.setValueAsync(any())).thenReturn(apiFutureStudent);
    when(taRef.setValueAsync(any())).thenReturn(apiFutureTA);

    new Workspace(STUDENT, TA, reference);

    verify(reference, times(1)).child(eq("student"));
    verify(reference, times(1)).child(eq("ta"));

    verify(studentRef, times(1)).setValueAsync(eq(STUDENT));
    verify(taRef, times(1)).setValueAsync(eq(TA));

    verify(apiFutureStudent, times(1)).get();
    verify(apiFutureTA, times(1)).get();
  }

  @Test
  public void getStudentID() throws InterruptedException, ExecutionException {
    final String STUDENT = "STUDENT";

    when(reference.child(eq("student"))).thenReturn(studentRef);
    when(snap.getValue()).thenReturn(STUDENT);

    Future<String> future = new Workspace(reference).getStudentUID();

    verify(reference, times(1)).child(eq("student"));
    verify(studentRef, times(1)).addListenerForSingleValueEvent(listenerCaptor.capture());

    assertFalse(future.isDone());

    listenerCaptor.getValue().onDataChange(snap);

    assertTrue(future.isDone());
    assertEquals(STUDENT, future.get());
  }

  @Test(expected = ExecutionException.class)
  public void getStudentIDException() throws InterruptedException, ExecutionException {
    final String STUDENT = "STUDENT";

    when(reference.child(eq("student"))).thenReturn(studentRef);
    when(error.toException()).thenReturn(exception);

    Future<String> future = new Workspace(reference).getStudentUID();

    verify(reference, times(1)).child(eq("student"));
    verify(studentRef, times(1)).addListenerForSingleValueEvent(listenerCaptor.capture());

    assertFalse(future.isDone());

    listenerCaptor.getValue().onCancelled(error);

    assertTrue(future.isDone());
    future.get();
  }

  @Test
  public void getTaID() throws InterruptedException, ExecutionException {
    final String TA = "TA";

    when(reference.child(eq("ta"))).thenReturn(taRef);
    when(snap.getValue()).thenReturn(TA);

    Future<String> future = new Workspace(reference).getTaUID();

    verify(reference, times(1)).child(eq("ta"));
    verify(taRef, times(1)).addListenerForSingleValueEvent(listenerCaptor.capture());

    assertFalse(future.isDone());

    listenerCaptor.getValue().onDataChange(snap);

    assertTrue(future.isDone());
    assertEquals(TA, future.get());
  }

  @Test(expected = ExecutionException.class)
  public void getTaIDException() throws InterruptedException, ExecutionException {
    final String TA = "TA";

    when(reference.child(eq("ta"))).thenReturn(taRef);
    when(error.toException()).thenReturn(exception);

    Future<String> future = new Workspace(reference).getTaUID();

    verify(reference, times(1)).child(eq("ta"));
    verify(taRef, times(1)).addListenerForSingleValueEvent(listenerCaptor.capture());

    assertFalse(future.isDone());

    listenerCaptor.getValue().onCancelled(error);

    assertTrue(future.isDone());
    future.get();
  }
}
