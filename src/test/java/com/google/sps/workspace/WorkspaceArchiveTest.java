package com.google.sps.workspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceArchiveTest {
  @Mock Workspace workspace;
  @Mock Future<List<WorkspaceFile>> futureFiles;
  @Mock Future<String> futureString;
  @Mock TarArchiveOutputStream tarOut;
  @Mock WorkspaceFile file;

  @Test(expected = NullPointerException.class)
  public void workspaceArchiveNullWorkspace() {
    new WorkspaceArchive(null);
  }

  @Test
  public void archive() throws InterruptedException, ExecutionException, IOException {
    final int NUM_FILES = 3;
    final String FILENAME = "FILENAME";
    final String CONTENTS = "CONTENTS";

    List<WorkspaceFile> files = new ArrayList<>();
    for (int i = 0; i < NUM_FILES; i++) {
      files.add(file);
    }

    when(workspace.getFiles()).thenReturn(futureFiles);
    when(futureFiles.get()).thenReturn(files);
    when(file.getFilename()).thenReturn(FILENAME);
    when(file.getContents()).thenReturn(futureString);
    when(futureString.get()).thenReturn(CONTENTS);

    new WorkspaceArchive(workspace).archive(tarOut);

    verify(workspace, times(1)).getFiles();
    verify(file, times(NUM_FILES)).getFilename();
    verify(file, times(NUM_FILES)).getContents();

    verify(tarOut, times(NUM_FILES)).putArchiveEntry(any());
    verify(tarOut, times(NUM_FILES)).write(eq(CONTENTS.getBytes()));
    verify(tarOut, times(NUM_FILES)).closeArchiveEntry();

    verify(tarOut, times(1)).close();
  }

  @Test(expected = ExecutionException.class)
  public void archiveException() throws InterruptedException, ExecutionException, IOException {
    when(workspace.getFiles()).thenReturn(futureFiles);
    when(futureFiles.get()).thenThrow(new ExecutionException(new Exception()));

    new WorkspaceArchive(workspace).archive(tarOut);
  }
}
