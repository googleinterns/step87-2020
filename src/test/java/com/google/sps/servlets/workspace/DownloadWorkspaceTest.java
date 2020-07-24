package com.google.sps.servlets.workspace;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DownloadWorkspaceTest {
  @Mock BlobstoreService blobstoreService;
  @Mock HttpServletResponse resp;
  @Mock HttpServletRequest req;

  @InjectMocks DownloadWorkspace servlet;

  @Test
  public void doGet() throws Exception {
    BlobKey blobKey = mock(BlobKey.class);

    String filename = "FILENAME";
    when(req.getParameter(eq("filename"))).thenReturn(filename);
    when(blobstoreService.createGsBlobKey(anyString())).thenReturn(blobKey);

    servlet.doGet(req, resp);

    verify(req, times(1)).getParameter(eq("filename"));
    verify(resp, times(1)).setContentType(eq("application/x-gzip"));
    verify(blobstoreService, times(1))
        .createGsBlobKey(eq("/gs/fulfillment-deco-step-2020.appspot.com/" + filename));
    verify(blobstoreService, times(1)).serve(blobKey, resp);
  }
}
