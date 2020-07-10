let outputVisible = false;
const term = new Terminal();
const fit = new FitAddon.FitAddon();
term.loadAddon(fit);

document.addEventListener("DOMContentLoaded", () => {
  const container = document.getElementById("output-container");
  container.classList.remove("hidden");
  term.open(container);
  container.classList.add("hidden");
});

getFirebaseRef().child("environment").on("value", snap => {
  if (snap.val() !== null) {
    document.getElementById("executeButton").classList.remove("hidden");
  }
});


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

function executeCode() {
  const executeButton = document.getElementById("executeButton");
  executeButton.classList.add("download-in-progress");
  executeButton.disabled = true;
  getToken().then(tok => {
    fetch(`/workspace/queueExecution?workspaceID=${getParam("workspaceID")}&idToken=${tok}`)
      .then(resp => resp.text()).then(execID => {
        seenFirstOutput = false;
        getFirebaseRef().child("executions").child(execID).on("child_added", snap => {
          if (snap.val() !== null) {
            if (typeof snap.val() === 'string' || snap.val() instanceof String) {
              if (!outputVisible && ! seenFirstOutput) {
                toggleOutput();
                seenFirstOutput = true;
              }    

              fit.fit();
              term.write(snap.val());
            } else {
              executeButton.disabled = false;	
              executeButton.classList.remove("download-in-progress");	
              getFirebaseRef().child("executions").child(execID).off("child_added");
            }
          }
        });
      });
  });
}