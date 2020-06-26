package com.google.sps.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import java.util.ArrayList;
import java.util.List;
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

  @Mock DatabaseReference filesRef;

  @Mock DataSnapshot snap;

  @Mock DatabaseReference historyRef;

  @Captor ArgumentCaptor<ValueEventListener> listenerCaptor;

  @Mock DatabaseError error;
  @Mock DatabaseException exception;

  @Test
  public void getStudentID() throws Exception {
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
  public void getStudentIDException() throws Exception {
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
  public void getTaID() throws Exception {
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
  public void getTaIDException() throws Exception {
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

  @Test
  public void getFiles() throws Exception {
    final int NUM_FILES = 3;
    ArrayList<DataSnapshot> files = new ArrayList<>();
    for (int i = 0; i < NUM_FILES; i++) {
      files.add(snap);
    }

    when(reference.child(eq("files"))).thenReturn(filesRef);
    when(snap.getKey()).thenReturn("KEY");
    when(snap.hasChild(eq("checkpoint/id"))).thenReturn(false);
    when(snap.getRef()).thenReturn(filesRef);
    when(filesRef.child(eq("history"))).thenReturn(historyRef);
    when(snap.getChildren()).thenReturn(files);

    Future<List<WorkspaceFile>> filesFuture = new Workspace(reference).getFiles();

    verify(filesRef, times(1)).addListenerForSingleValueEvent(listenerCaptor.capture());

    assertFalse(filesFuture.isDone());

    listenerCaptor.getValue().onDataChange(snap);

    assertTrue(filesFuture.isDone());

    assertEquals(filesFuture.get().size(), NUM_FILES);
  }

  @Test(expected = ExecutionException.class)
  public void getFilesException() throws Exception {
    when(reference.child(eq("files"))).thenReturn(filesRef);
    when(error.toException()).thenReturn(exception);

    Future<List<WorkspaceFile>> filesFuture = new Workspace(reference).getFiles();

    verify(filesRef, times(1)).addListenerForSingleValueEvent(listenerCaptor.capture());

    assertFalse(filesFuture.isDone());

    listenerCaptor.getValue().onCancelled(error);

    assertTrue(filesFuture.isDone());
    filesFuture.get();
  }

  @Test
  public void getWorkspaceID() {
    final String WORKSPACE_ID = "ID";
    when(reference.getKey()).thenReturn(WORKSPACE_ID);

    assertEquals(WORKSPACE_ID, new Workspace(reference).getWorkspaceID());
  }
}
