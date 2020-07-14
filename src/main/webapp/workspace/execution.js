const clear = '\x1bc';

let outputVisible = false;
let currOutputRef = null;
let currExitCode = null;

const term = new Terminal();
const fit = new FitAddon.FitAddon();
term.loadAddon(fit);

document.addEventListener("DOMContentLoaded", () => {
  const container = document.getElementById("output-container");
  container.classList.remove("hidden");
  term.open(container);
  container.classList.add("hidden");
});

getFirebaseRef().child("classID").once("value", snap => {
  fetch(`/getEnvironments?classID=${snap.val}`).then(resp => resp.json()).then(envs => {
    const select = document.getElementById("envSelect");
    for (var env of envs) {
      const option = document.createElement("option");
      option.value = env.id;
      option.innerText = env.name;
    }

    if (envs.length > 0) {
      select.classList.remove("hidden");
      document.getElementById("executeButton").classList.remove("hidden");
    }
  });
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
    const select = document.getElementById("envSelect");
    fetch(`/workspace/queueExecution?workspaceID=${getParam("workspaceID")}&envID=${select.value}&idToken=${tok}`);
  });
}

getFirebaseRef().child("executions").orderByChild("timestamp").startAt(Date.now()).on("child_added", snap => {
  if(currOutputRef !== null) {
    currOutputRef.off("child_added");
  }

  if(currExitCode !== null) {
    currExitCode.off("value");
  }
  
  const executeButton = document.getElementById("executeButton");
  executeButton.classList.add("download-in-progress");
  executeButton.disabled = true;

  seenFirstOutput = false;
  term.write(clear);  // Clear terminal

  currOutputRef = snap.ref.child("output");
  currOutputRef.on("child_added", snap => {
    if (!outputVisible && !seenFirstOutput) {
      toggleOutput();
      seenFirstOutput = true;
    }    

    fit.fit();
    term.write(snap.val());
  });

  currExitCode = snap.ref.child("exitCode");
  currExitCode.on("value", snap => {
    if (snap.val() !== null) {
      executeButton.disabled = false;	
      executeButton.classList.remove("download-in-progress");


      currOutputRef.off("child_added");
      currExitCode.off("value");

      currOutputRef = null;
      currExitCode = null;
    }
  });
});