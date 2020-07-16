package com.google.sps.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
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
  @Mock ApiFuture<Void> apiFuture;
  @Mock ApiFuture<Void> apiFutureStudent;
  @Mock ApiFuture<Void> apiFutureTA;
  @Mock DatabaseReference studentRef;
  @Mock DatabaseReference taRef;
  @Mock DatabaseReference filesRef;
  @Mock DataSnapshot snap;
  @Mock DatabaseReference historyRef;
  @Mock DatabaseReference downloadsRef;
  @Mock DatabaseReference newDownloadRef;
  @Mock DatabaseReference environmentRef;
  @Mock DatabaseReference executionsRef;
  @Mock DatabaseReference newExecutionRef;
  @Mock DatabaseReference outputRef;
  @Mock DatabaseReference outputElementRef;
  @Mock DatabaseReference timestampRef;

  @Captor ArgumentCaptor<ValueEventListener> listenerCaptor;

  @Mock DatabaseError error;
  @Mock DatabaseException exception;

  String STUDENT = "STUDENT";
  String TA = "TA";

  @Test
  public void setStudentID() throws Exception {
    when(reference.child(eq("student"))).thenReturn(studentRef);
    when(studentRef.setValueAsync(anyString())).thenReturn(apiFuture);

    new Workspace(reference).setStudentUID(STUDENT);

    verify(studentRef, times(1)).setValueAsync(eq(STUDENT));
    verify(apiFuture, times(1)).get();
  }

  @Test
  public void getStudentID() throws Exception {

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
  public void setTaID() throws Exception {
    when(reference.child(eq("ta"))).thenReturn(taRef);
    when(taRef.setValueAsync(anyString())).thenReturn(apiFuture);

    new Workspace(reference).setTaUID(TA);

    verify(taRef, times(1)).setValueAsync(eq(TA));
    verify(apiFuture, times(1)).get();
  }

  @Test
  public void getTaID() throws Exception {

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
    int NUM_FILES = 3;
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
    String WORKSPACE_ID = "ID";
    when(reference.getKey()).thenReturn(WORKSPACE_ID);

    assertEquals(WORKSPACE_ID, new Workspace(reference).getWorkspaceID());
  }

  @Test
  public void newDownloadTest() {
    String DOWNLOAD_ID = "DOWNLOAD_ID";

    when(reference.child(eq("downloads"))).thenReturn(downloadsRef);
    when(downloadsRef.push()).thenReturn(newDownloadRef);
    when(newDownloadRef.getKey()).thenReturn(DOWNLOAD_ID);

    assertEquals(DOWNLOAD_ID, new Workspace(reference).newDownloadID());
  }

  @Test
  public void updateDownloadName() throws Exception {
    String DOWNLOAD_ID = "DOWNLOAD_ID";
    String FILENAME = "FILENAME";

    when(reference.child(eq("downloads"))).thenReturn(downloadsRef);
    when(downloadsRef.child(eq(DOWNLOAD_ID))).thenReturn(newDownloadRef);
    when(newDownloadRef.setValueAsync(eq(FILENAME))).thenReturn(apiFuture);

    new Workspace(reference).updateDownloadName(DOWNLOAD_ID, FILENAME);

    verify(newDownloadRef, times(1)).setValueAsync(eq("FILENAME"));
    verify(apiFuture, times(1)).get();
  }

  @Test
  public void newExecutionID() throws Exception {
    String EXEC_KEY = "EXEC_KEY";

    when(reference.child(eq("executions"))).thenReturn(executionsRef);
    when(executionsRef.push()).thenReturn(newExecutionRef);
    when(newExecutionRef.child(eq("timestamp"))).thenReturn(timestampRef);
    when(timestampRef.setValueAsync(any())).thenReturn(apiFuture);
    when(newExecutionRef.getKey()).thenReturn(EXEC_KEY);

    assertEquals(EXEC_KEY, new Workspace(reference).newExecutionID());
    verify(timestampRef, times(1)).setValueAsync(ServerValue.TIMESTAMP);
  }

  @Test
  public void writeOutput() throws Exception {
    String EXEC_ID = "EXEC_ID";
    String OUTPUT = "OUTPUT";

    when(reference.child(eq("executions"))).thenReturn(executionsRef);
    when(executionsRef.child(eq(EXEC_ID))).thenReturn(newExecutionRef);
    when(newExecutionRef.child("output")).thenReturn(outputRef);
    when(outputRef.push()).thenReturn(outputElementRef);
    when(outputElementRef.setValueAsync(eq(OUTPUT))).thenReturn(apiFuture);

    new Workspace(reference).writeOutput(EXEC_ID, OUTPUT);

    verify(outputElementRef, times(1)).setValueAsync(eq(OUTPUT));
    verify(apiFuture, times(1)).get();
  }

  @Test
  public void setExitCode() throws Exception {
    String EXEC_ID = "EXEC_ID";
    int EXIT_CODE = 0;

    when(reference.child(eq("executions"))).thenReturn(executionsRef);
    when(executionsRef.child(eq(EXEC_ID))).thenReturn(newExecutionRef);
    when(newExecutionRef.child(eq("exitCode"))).thenReturn(outputElementRef);
    when(outputElementRef.setValueAsync(eq(EXIT_CODE))).thenReturn(apiFuture);

    new Workspace(reference).setExitCode(EXEC_ID, EXIT_CODE);

    verify(outputElementRef, times(1)).setValueAsync(eq(EXIT_CODE));
    verify(apiFuture, times(1)).get();
  }

  @Test
  public void delete() throws Exception {
    when(reference.removeValueAsync()).thenReturn(apiFuture);

    new Workspace(reference).delete();

    verify(reference, times(1)).removeValueAsync();
    verify(apiFuture, times(1)).get();
  }
}
