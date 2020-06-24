package com.google.sps.workspace;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class WorkspaceFile {
  private final Query q;
  private final String docBase;

  private final String fileName;

  public WorkspaceFile(DataSnapshot snap) {
    // TODO: decode fileName
    fileName = snap.getKey();
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
            String doc = padSurrogatePairs(docBase);
            for (DataSnapshot ops : snapshot.getChildren()) {
              long idx = 0;
              for (DataSnapshot op : (List<DataSnapshot>) ops.getValue()) {
                if (op.getValue() instanceof Long) {
                  long longOp = op.getValue(Long.class).longValue();
                  if (longOp > 0) {
                    // retain
                    idx += longOp;
                  } else {
                    // delete
                    doc = doc.substring(0, (int) idx) + doc.substring((int) (idx - longOp));
                  }
                } else {
                  // insert
                  String stringOp = padSurrogatePairs(op.getValue(String.class));
                  doc = doc.substring(0, (int) idx + 1) + stringOp + doc.substring((int) idx + 1);
                }
              }

              // Remove surrogate pair padding.
              doc.replaceAll("\0", "");
            }

            future.complete(doc);
          }

          @Override
          public void onCancelled(DatabaseError error) {
            future.completeExceptionally(error.toException());
          }
        });

    return future;
  }

  public static String padSurrogatePairs(String str) {
    String newStr = str;

    int offset = 0;
    for (int i = 0; i < str.length(); i++) {
      if (str.codePointAt(i) >= 0x10000 && str.codePointAt(i) <= 0x10FFFF) {
        newStr = newStr.substring(0, i + offset + 1) + '\0' + newStr.substring(i + offset + 1);
        offset++;
      }
    }

    return newStr;
  }
}
