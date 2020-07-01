package com.google.sps.workspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.sps.workspace.WorkspaceArchive.ArchiveType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceArchiveTest {
  @Mock Workspace workspace;
  @Mock Future<List<WorkspaceFile>> futureFiles;
  @Mock Future<String> futureString;
  @Mock ArchiveOutputStream archiveOut;
  @Mock OutputStream out;
  @Mock WorkspaceFile file;

  @Test(expected = NullPointerException.class)
  public void workspaceArchiveNullWorkspace() {
    new WorkspaceArchive(null, ArchiveType.ZIP);
  }

  @Test(expected = NullPointerException.class)
  public void workspaceArchiveNullType() {
    new WorkspaceArchive(workspace, null);
  }

  @Test
  public void archiveTar() throws InterruptedException, ExecutionException, IOException {
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

    new WorkspaceArchive(workspace, ArchiveType.TAR).archive(archiveOut);

    verify(workspace, times(1)).getFiles();
    verify(file, times(NUM_FILES)).getFilename();
    verify(file, times(NUM_FILES)).getContents();

    verify(archiveOut, times(NUM_FILES)).putArchiveEntry(any(TarArchiveEntry.class));
    verify(archiveOut, times(NUM_FILES)).write(eq(CONTENTS.getBytes()));
    verify(archiveOut, times(NUM_FILES)).closeArchiveEntry();

    verify(archiveOut, times(1)).close();
  }

  @Test
  public void archiveZip() throws InterruptedException, ExecutionException, IOException {
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

    new WorkspaceArchive(workspace, ArchiveType.ZIP).archive(archiveOut);

    verify(workspace, times(1)).getFiles();
    verify(file, times(NUM_FILES)).getFilename();
    verify(file, times(NUM_FILES)).getContents();

    verify(archiveOut, times(NUM_FILES)).putArchiveEntry(any(ZipArchiveEntry.class));
    verify(archiveOut, times(NUM_FILES)).write(eq(CONTENTS.getBytes()));
    verify(archiveOut, times(NUM_FILES)).closeArchiveEntry();

    verify(archiveOut, times(1)).close();
  }

  @Test(expected = ExecutionException.class)
  public void archiveException() throws Exception {
    when(workspace.getFiles()).thenReturn(futureFiles);
    when(futureFiles.get()).thenThrow(new ExecutionException(new Exception()));

    new WorkspaceArchive(workspace, ArchiveType.TAR).archive(archiveOut);
  }

  @Test
  public void archiveHelperTar() throws Exception {
    WorkspaceArchive archive = spy(new WorkspaceArchive(workspace, ArchiveType.TAR));
    doNothing().when(archive).archive(any(ArchiveOutputStream.class));

    archive.archive(out);

    verify(archive, times(1)).archive(any(TarArchiveOutputStream.class));
  }

  @Test
  public void archiveHelperZip() throws Exception {
    WorkspaceArchive archive = spy(new WorkspaceArchive(workspace, ArchiveType.ZIP));
    doNothing().when(archive).archive(any(ArchiveOutputStream.class));

    archive.archive(out);

    verify(archive, times(1)).archive(any(ZipArchiveOutputStream.class));
  }
}
