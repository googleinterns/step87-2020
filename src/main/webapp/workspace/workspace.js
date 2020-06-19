let tabs = {};
let currentTab;

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
  tabs[currentTab].editor.layout();
};

function uploadFiles() {
  document.getElementById("upload-files").click();
}

function switchTab(tab) {
  if (currentTab !== tab) {
    if (tabs[currentTab]) {
      tabs[currentTab].editorContainer.classList.add("hidden");
      tabs[currentTab].tabButton.classList.remove("active-tab");
      tabs[currentTab].tabButton.classList.add("inactive-tab");
    }

    tabs[tab].editorContainer.classList.remove("hidden");
    tabs[tab].tabButton.classList.remove("inactive-tab");
    tabs[tab].tabButton.classList.add("active-tab");

    tabs[tab].editor.layout();

    currentTab = tab;
  }
}

// Firebase does not accept the folowing characters: 
// .$[]#/
// We must encode them to store them in realtime database.
function encodeFileName(filename) {
  return encodeURIComponent(filename).replace(/\./, '%2E');
}

function decodeFileName(filename) {
  return decodeURIComponent(filename.replace("%2E", "."));
}

function createNewTab(file) {
  if (!tabs[file.name]) {
    const tab = document.createElement("button");
    tab.classList.add("inactive-tab", "tab");
    tab.innerText = file.name;

    tab.onclick = (event) => {
      switchTab(file.name);
    };

    const firepadContainer = document.createElement("div");
    firepadContainer.classList.add("hidden", "firepad-container");

    const editor = monaco.editor.create(firepadContainer, {
      language: "javascript",
      theme: "vs-dark"
    });
    
    const firepad = Firepad.fromMonaco(getFirebaseRef().child(encodeFileName(file.name)), editor);

    firepad.on("ready", () => {
      file.text().then((contents) => {
        firepad.setText(contents);
      });
    });

    tabs[file.name] = {
      editor: editor,
      firepad: firepad,
      tabButton: tab,
      editorContainer: firepadContainer
    };

    document.getElementById("tabs-container").appendChild(tab);
    document.getElementById("firepads").appendChild(firepadContainer);
  } else {
    file.text().then(contents => {
      tabs[file.name].firepad.setText(contents);
    });
  }

}

function filesUploaded() {
  const files = document.getElementById("upload-files").files;

  for(var file of files) {
    createNewTab(file);
  }

  switchTab(files[0].name);
}