getFirebaseRef().child("environment").on("value", snap => {
  if (snap.val() !== null) {
    document.getElementById("executeButton").classList.remove("hidden");
  }
});

function executeCode() {
  const executeButton = document.getElementById("executeButton");
  executeButton.classList.add("download-in-progress");
  executeButton.disabled = true;
  getToken().then(tok => {
    fetch(`/workspace/queueExecution?workspaceID=${getParam("workspaceID")}&idToken=${tok}`)
      .then(resp => resp.text()).then(execID => {
        getFirebaseRef().child("executions").child(execID).on("value", snap => {
          if (snap.val() !== null) {
            console.log(snap.val());

            getFirebaseRef().child("executions").child(execID).off("value");
          }
        });
      });
  });
}