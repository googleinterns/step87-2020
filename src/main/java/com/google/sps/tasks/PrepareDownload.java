package com.google.sps.tasks;

import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceArchive.ArchiveType;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class uploads an archive of the workspace to cloud storage and then updates the realtime
 * database with the name of the GCS object.
 */
@WebServlet("/tasks/prepareDownload")
public class PrepareDownload extends HttpServlet {
  private WorkspaceFactory workspaceFactory;
  private GcsService instance;

  @Override
  public void init() throws ServletException {
    super.init();
    workspaceFactory = WorkspaceFactory.getInstance();
    instance = GcsServiceFactory.createGcsService();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

    String args[] = body.split(",");
    String workspaceID = args[0];
    String downloadID = args[1];

    Workspace w = workspaceFactory.fromWorkspaceID(workspaceID);

    GcsFileOptions options = GcsFileOptions.getDefaultInstance();
    GcsFilename filename =
        new GcsFilename("fulfillment-deco-step-2020.appspot.com", workspaceID + '/' + downloadID);
    GcsOutputChannel outputChannel = instance.createOrReplace(filename, options);

    try {
      w.getArchive(ArchiveType.ZIP).archive(Channels.newOutputStream(outputChannel));

      w.updateDownloadName(downloadID, filename.getObjectName());
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
