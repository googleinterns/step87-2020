package com.google.sps.workspace;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.sps.utils.StringUtils;
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

  protected WorkspaceFile(DataSnapshot snap) {
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
                      doc = StringUtils.slice(doc, (int) idx, (int) -longOp);
                    }
                  } else {
                    // insert
                    String stringOp = padSurrogatePairs((String) op);
                    doc = StringUtils.insert(doc, stringOp, (int) idx);
                    idx += stringOp.length();
                  }
                }

                // Remove surrogate pair padding.
                doc.replaceAll("\0", "");

                if (idx != doc.length()) {
                  future.completeExceptionally(
                      new IllegalStateException("Operation did not operate on whole string."));
                  return;
                }
              }

              future.complete(doc);
            } catch (Exception e) {
              future.completeExceptionally(e);
            }
          }

          @Override
          public void onCancelled(DatabaseError error) {
            future.completeExceptionally(error.toException());
          }
        });

    return future;
  }

  private static String padSurrogatePairs(String str) {
    String newStr = str;

    int offset = 0;
    for (int i = 0; i < str.length(); i++) {
      if (str.codePointAt(i) >= 0x10000 && str.codePointAt(i) <= 0x10FFFF) {
        newStr = StringUtils.insert(newStr, "\0", i + offset);
        offset++;
      }
    }

    return newStr;
  }

  public String getFilename() {
    return fileName;
  }

  private String decodeFilename(String filename) {
    try {
      return URLDecoder.decode(filename.replaceAll("%2E", "."), StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return filename;
    }
  }
}
