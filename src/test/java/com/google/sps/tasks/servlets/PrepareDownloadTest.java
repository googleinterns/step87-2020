package com.google.sps.tasks.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceArchive;
import com.google.sps.workspace.WorkspaceArchive.ArchiveType;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrepareDownloadTest {
  @Mock WorkspaceFactory workspaceFactory;
  @Mock GcsService instance;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock BufferedReader reader;
  @Mock GcsFileOptions options;
  @Mock GcsOutputChannel outputChannel;
  @Mock Workspace workspace;
  @Mock WorkspaceArchive archive;

  @Captor ArgumentCaptor<GcsFilename> filenameCaptor;

  @InjectMocks PrepareDownload servlet;

  @Test
  public void doPostTest() throws Exception {
    String workspaceID = "WORKSPACE_ID";
    String downloadID = "DOWNLOAD_ID";
    Stream<String> lines = Arrays.asList(workspaceID + ',' + downloadID).stream();

    when(req.getReader()).thenReturn(reader);
    when(reader.lines()).thenReturn(lines);
    when(workspaceFactory.fromWorkspaceID(eq(workspaceID))).thenReturn(workspace);
    when(instance.createOrReplace(any(GcsFilename.class), any(GcsFileOptions.class)))
        .thenReturn(outputChannel);
    when(workspace.getArchive(ArchiveType.ZIP)).thenReturn(archive);

    servlet.doPost(req, resp);

    verify(workspaceFactory, times(1)).fromWorkspaceID(workspaceID);
    verify(instance, times(1)).createOrReplace(filenameCaptor.capture(), any(GcsFileOptions.class));
    assertEquals(workspaceID + '/' + downloadID, filenameCaptor.getValue().getObjectName());
    verify(archive, times(1)).archive(any(OutputStream.class));
    verify(workspace, times(1))
        .updateDownloadName(downloadID, filenameCaptor.getValue().getObjectName());
  }
}
