package com.google.sps.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
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
public class WorkspaceFileTest {
  @Mock DataSnapshot snap;
  @Mock DataSnapshot historySnap;
  @Mock DatabaseReference ref;
  @Mock DatabaseReference historyRef;
  @Mock Query q;

  @Mock DatabaseError error;
  @Mock DatabaseException exception;

  @Captor ArgumentCaptor<ValueEventListener> captor;

  @Test(expected = NullPointerException.class)
  public void workspaceFileNullSnap() {
    new WorkspaceFile(null);
  }

  @Test
  public void getContents() throws InterruptedException, ExecutionException {
    when(snap.hasChild(eq("checkpoint/id"))).thenReturn(false);
    when(snap.getKey()).thenReturn("filename");
    when(snap.getRef()).thenReturn(ref);
    when(ref.child(eq("history"))).thenReturn(historyRef);
    when(historyRef.orderByKey()).thenReturn(q);

    // Build up mock history
    List<DataSnapshot> ops = new ArrayList<>();

    DataSnapshot op1 = mock(DataSnapshot.class);
    DataSnapshot o1 = mock(DataSnapshot.class);
    when(op1.child(eq("o"))).thenReturn(o1);
    when(o1.getValue()).thenReturn(Arrays.asList("Heello"));
    ops.add(op1);

    DataSnapshot op2 = mock(DataSnapshot.class);
    DataSnapshot o2 = mock(DataSnapshot.class);
    when(op2.child(eq("o"))).thenReturn(o2);
    when(o2.getValue()).thenReturn(Arrays.asList(new Long(2), new Long(-1), new Long(3)));
    ops.add(op2);

    DataSnapshot op3 = mock(DataSnapshot.class);
    DataSnapshot o3 = mock(DataSnapshot.class);
    when(op3.child(eq("o"))).thenReturn(o3);
    when(o3.getValue()).thenReturn(Arrays.asList(new Long(1), new Long(-1), new Long(3)));
    ops.add(op3);

    DataSnapshot op4 = mock(DataSnapshot.class);
    DataSnapshot o4 = mock(DataSnapshot.class);
    when(op4.child(eq("o"))).thenReturn(o4);
    when(o4.getValue()).thenReturn(Arrays.asList(new Long(1), "e", new Long(3)));
    ops.add(op4);

    when(historySnap.getChildren()).thenReturn(ops);

    Future<String> future = new WorkspaceFile(snap).getContents();

    verify(q, times(1)).addListenerForSingleValueEvent(captor.capture());

    assertFalse(future.isDone());

    captor.getValue().onDataChange(historySnap);

    assertTrue(future.isDone());
    assertEquals("Hello", future.get());
  }

  @Test(expected = ExecutionException.class)
  public void getContentsException() throws InterruptedException, ExecutionException {
    when(snap.hasChild(eq("checkpoint/id"))).thenReturn(false);
    when(snap.getKey()).thenReturn("filename");
    when(snap.getRef()).thenReturn(ref);
    when(ref.child(eq("history"))).thenReturn(historyRef);
    when(historyRef.orderByKey()).thenReturn(q);

    when(error.toException()).thenReturn(exception);

    Future<String> future = new WorkspaceFile(snap).getContents();

    verify(q, times(1)).addListenerForSingleValueEvent(captor.capture());

    assertFalse(future.isDone());

    captor.getValue().onCancelled(error);

    assertTrue(future.isDone());
    future.get();
  }

  @Test
  public void getFilename() {
    final String FILENAME = "RequestHelper.java";
    final String ENCODED_FILENAME = "RequestHelper%2Ejava";

    when(snap.hasChild(eq("checkpoint/id"))).thenReturn(false);
    when(snap.getKey()).thenReturn(ENCODED_FILENAME);
    when(snap.getRef()).thenReturn(ref);
    when(ref.child(eq("history"))).thenReturn(historyRef);
    when(historyRef.orderByKey()).thenReturn(q);

    assertEquals(FILENAME, new WorkspaceFile(snap).getFilename());
  }
}
