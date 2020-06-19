let currentEditor;

function createFirepad(ref, parent, language) {
  const editor = monaco.editor.create(parent, {
    language: language,
    theme: "vs-dark"
  });

  Firepad.fromMonaco(ref, editor);
  currentEditor = editor;
}

function getFirebaseRef() {
  const workspaceID = getParam("workspaceID");
  if (workspaceID !== null) {
    return firebase.database().ref().child(workspaceID);
  } else {
    // If we were not given a workspace ID redirect to the home page.
    window.location.href = "/";
  }
}

function scrollTabs(event) {
  // Only translate vertical scrolling to horizontal scrolling.
  if (event.deltaY) {
    this.scrollLeft += (event.deltaY);
    event.preventDefault();
  }
}

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("tabs-container").onwheel = scrollTabs;

  const config = {
    apiKey: 'AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE',
    databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
    projectId: "fulfillment-deco-step-2020",
  };
  firebase.initializeApp(config);

  const ref = getFirebaseRef();

  require.config({ paths: {'vs': 'https://unpkg.com/monaco-editor@latest/min/vs'}});
  require(['vs/editor/editor.main'], function() {
    // Once the monaco library is loaded, we can start uploading files.
    const hiddenElements = document.getElementsByClassName("initially-hidden");

    for (let e of hiddenElements) {
      e.classList.remove("initially-hidden");
    }
  });
});

window.onresize = () => {
  currentEditor.layout();
};

function uploadFiles() {
  document.getElementById("upload-files").click();
}

function filesUploaded() {
  const files = document.getElementById("upload-files").files;

  for(var file of files) {
    console.log(file.name);
  }
}