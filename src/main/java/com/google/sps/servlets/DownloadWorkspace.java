package com.google.sps.servlets;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/workspace/downloadWorkspace")
public class DownloadWorkspace extends HttpServlet {
  WorkspaceFactory workspaceFactory;

  @Override
  public void init() throws ServletException {
    // TODO Auto-generated method stub
    super.init();
    workspaceFactory = WorkspaceFactory.getInstance();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String downloadID = req.getParameter("downloadID");

    resp.setContentType("application/x-gzip");

    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    BlobKey blobKey =
        blobstoreService.createGsBlobKey(
            "/gs/fulfillment-deco-step-2020.appspot.com/" + downloadID);
    blobstoreService.serve(blobKey, resp);
  }
}
