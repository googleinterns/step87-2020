outputVisible = false;

getFirebaseRef().child("environment").on("value", snap => {
  if (snap.val() !== null) {
    document.getElementById("executeButton").classList.remove("hidden");
    if (!outputVisible) {
      toggleOutput();
    }
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
            const pre = document.createElement("pre");
            pre.innerText = snap.val();

            const outputContainer = document.getElementById("output-container");
            outputContainer.innerHTML = "";
            outputContainer.appendChild(pre);

            executeButton.disabled = false;
            executeButton.classList.remove("download-in-progress");
            getFirebaseRef().child("executions").child(execID).off("value");
          }
        });
      });
  });
}

function toggleOutput() {
  document.getElementById("output-container").classList.toggle("hidden");
  
  if(outputVisible) {
    document.getElementById("output-minimize-button").innerText = String.fromCodePoint(0x25B2);
  } else {
    document.getElementById("output-minimize-button").innerText = String.fromCodePoint(0x25BC);
  }

  outputVisible = !outputVisible;

  tabs[currentTab].editor.layout();
}