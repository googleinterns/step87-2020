package com.google.sps.workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class WorkspaceArchive {
  private final Workspace w;

  protected WorkspaceArchive(Workspace w) {
    this.w = Objects.requireNonNull(w);
  }

  public void archive(OutputStream out)
      throws InterruptedException, ExecutionException, IOException {
    archive(new TarArchiveOutputStream(new GZIPOutputStream(out)));
  }

  protected void archive(TarArchiveOutputStream tarOut)
      throws InterruptedException, ExecutionException, IOException {
    List<WorkspaceFile> files = w.getFiles().get();

    for (WorkspaceFile file : files) {
      byte contents[] = file.getContents().get().getBytes();

      TarArchiveEntry entry = new TarArchiveEntry(file.getFilename());
      entry.setSize(contents.length);

      tarOut.putArchiveEntry(entry);
      tarOut.write(contents);

      tarOut.closeArchiveEntry();
    }

    tarOut.close();
  }
}
