function leftJitsi() {
  document.getElementById("jitsi-window").innerHTML = "";
  document.getElementById("jitsi-container").classList.add("hidden");

  const button = document.getElementById("jitsi-join");
  button.innerText = "Join Meeting";
  button.onclick = addJitsiWindow;

  jitsiVisible = false;
}

function toggleJitsi() {
  const button = document.getElementById("toggleJitsiButton");
  const jitsiWindow = document.getElementById("jitsi-window");

  if (jitsiVisible) {
    jitsiWindow.classList.add("hidden");
    button.innerText = "Show Jitsi";
  } else {
    jitsiWindow.classList.remove("hidden");
    button.innerText = "Hide Jitsi";
  }

  jitsiVisible = !jitsiVisible;
}

function addJitsiWindow() { // jshint ignore:line
  jitsiVisible = true;

  const jitsiJoin = document.getElementById("jitsi-join");
  const jitsiShow = document.getElementById("toggleJitsiButton");
  document.getElementById("jitsi-container").classList.remove("hidden");

  jitsiShow.innerText = "Hide Jitsi";
  jitsiJoin.innerText = "Leave Meeting";

  const parent = document.getElementById("jitsi-window");

  const api = new JitsiMeetExternalAPI("meet.jit.si", {
    roomName: getParam("workspaceID"),
    height: 300,
    width: 500,
    parentNode: parent
  });

  jitsiJoin.onclick = () => api.executeCommand('hangup'); 

  api.addEventListener("videoConferenceLeft", leftJitsi);
  api.executeCommand("displayName", firebase.auth().currentUser.displayName);
}