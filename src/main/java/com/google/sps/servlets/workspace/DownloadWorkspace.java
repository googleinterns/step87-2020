package com.google.sps.servlets.workspace;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/workspace/downloadWorkspace")
public class DownloadWorkspace extends HttpServlet {
  private BlobstoreService blobstoreService;

  @Override
  public void init() throws ServletException {
    // TODO Auto-generated method stub
    super.init();
    blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String downloadID = req.getParameter("filename");

    resp.setContentType("application/x-gzip");

    BlobKey blobKey =
        blobstoreService.createGsBlobKey(
            "/gs/fulfillment-deco-step-2020.appspot.com/" + downloadID);
    blobstoreService.serve(blobKey, resp);
  }
}
