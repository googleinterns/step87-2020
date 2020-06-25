package com.google.sps.workspace;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class WorkspaceFile {
  private final Query q;
  private final String docBase;

  private final String fileName;

  public WorkspaceFile(DataSnapshot snap) {
    // TODO: decode fileName
    fileName = decodeFilename(snap.getKey());
    if (snap.hasChild("checkpoint/id")) {
      docBase = (String) snap.child("checkpoint").child("o").getValue();

      q =
          snap.getRef()
              .child("history")
              .orderByKey()
              .startAt((String) snap.child("checkpoint").child("id").getValue());
    } else {
      docBase = "";

      q = snap.getRef().child("history").orderByKey();
    }
  }

  /**
   * Reconstructs a file from its edit history.
   *
   * <p>Modified from firepad ruby example:
   * https://github.com/FirebaseExtended/firepad/blob/master/examples/firepad.rb
   *
   * @return A Future<String> which will contain the file contents.
   */
  public Future<String> getContents() {
    CompletableFuture<String> future = new CompletableFuture<>();

    q.addListenerForSingleValueEvent(
        new ValueEventListener() {

          @Override
          public void onDataChange(DataSnapshot snapshot) {
            try {
              String doc = padSurrogatePairs(docBase);
              for (DataSnapshot ops : snapshot.getChildren()) {
                long idx = 0;
                for (Object op : (List<Object>) ops.child("o").getValue()) {
                  if (op instanceof Long) {
                    long longOp = ((Long) op).longValue();
                    if (longOp > 0) {
                      // retain
                      idx += longOp;
                    } else {
                      // delete
                      doc = slice(doc, (int) idx, (int) -longOp);
                      // doc.substring(0, (int) idx + 1) + doc.substring((int) (idx - longOp + 1));
                    }
                  } else {
                    // insert
                    String stringOp = padSurrogatePairs((String) op);
                    doc = insert(doc, stringOp, (int) idx);
                  }
                }

                // Remove surrogate pair padding.
                doc.replaceAll("\0", "");
              }

              future.complete(doc);
            } catch (Exception e) {
              StringWriter sw = new StringWriter();
              PrintWriter pw = new PrintWriter(sw);
              e.printStackTrace(pw);
              future.complete(sw.toString());
            }
          }

          @Override
          public void onCancelled(DatabaseError error) {
            future.completeExceptionally(error.toException());
          }
        });

    return future;
  }

  public static String slice(String str, int start, int length) {
    if (str.length() > start + length) {
      return str.substring(0, (int) start) + str.substring((int) (start + length));
    } else {
      return str.substring(0, (int) start);
    }
  }

  public static String insert(String orig, String insert, int idx) {
    if (orig.length() > idx) {
      return orig.substring(0, (int) idx) + insert + orig.substring((int) idx);
    } else {
      return orig + insert;
    }
  }

  public static String padSurrogatePairs(String str) {
    String newStr = str;

    int offset = 0;
    for (int i = 0; i < str.length(); i++) {
      if (str.codePointAt(i) >= 0x10000 && str.codePointAt(i) <= 0x10FFFF) {
        newStr = insert(newStr, "\0", i + offset);
        offset++;
      }
    }

    return newStr;
  }

  public String getFilename() {
    return fileName;
  }

  public String decodeFilename(String filename) {
    try {
      return URLDecoder.decode(filename.replaceAll("%2E", "."), StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return filename;
    }
  }
}
