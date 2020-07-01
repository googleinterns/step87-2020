package com.google.sps.workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class WorkspaceArchive {
  public enum ArchiveType {
    ZIP,
    TAR
  }

  private final ArchiveType type;

  private final Workspace w;

  protected WorkspaceArchive(Workspace w, ArchiveType type) {
    this.w = Objects.requireNonNull(w);
    this.type = Objects.requireNonNull(type);
  }

  public void archive(OutputStream out)
      throws InterruptedException, ExecutionException, IOException {
    switch (type) {
      case ZIP:
        archive(new ZipArchiveOutputStream(out));
        break;
      case TAR:
        archive(new TarArchiveOutputStream(new GZIPOutputStream(out)));
        break;
    }
  }

  private ArchiveEntry getEntry(String name, long size) {
    switch (type) {
      case ZIP:
        ZipArchiveEntry zipEntry = new ZipArchiveEntry(name);
        zipEntry.setSize(size);
        return zipEntry;
      case TAR:
        TarArchiveEntry tarEntry = new TarArchiveEntry(name);
        tarEntry.setSize(size);
        return tarEntry;
      default:
        return null;
    }
  }

  protected void archive(ArchiveOutputStream archiveOut)
      throws InterruptedException, ExecutionException, IOException {
    List<WorkspaceFile> files = w.getFiles().get();

    for (WorkspaceFile file : files) {
      byte contents[] = file.getContents().get().getBytes();

      ArchiveEntry entry = getEntry(file.getFilename(), contents.length);

      archiveOut.putArchiveEntry(entry);
      archiveOut.write(contents);

      archiveOut.closeArchiveEntry();
    }

    archiveOut.close();
  }
}
