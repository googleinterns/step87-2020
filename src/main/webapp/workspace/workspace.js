let tabs = {};
let currentTab;
let jitsiVisible = true;

/**
 * Gets the base firebase reference for this workspace.
 */
function getFirebaseRef() {
  const workspaceID = getParam("workspaceID");
  if (workspaceID !== null) {
    return firebase.database().ref().child(workspaceID);
  } else {
    // If we were not given a workspace ID redirect to the home page.
    window.location.href = "/";
  }
}

/**
 * Scroll event handler that allows users to scroll tabs using
 * a scroll wheel. 
 */
function scrollTabs(event) {
  // Only translate vertical scrolling to horizontal scrolling.
  if (event.deltaY) {
    this.scrollLeft += (event.deltaY);
    event.preventDefault();
  }
}

/**
 * Switches the current tab.
 * @param {String} tab name of the tab to switch to . 
 */
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

/**
 * Firebase does not accept the folowing characters: 
 * .$[]#/
 * We must encode them to store them in realtime database.
 * @param {String} filename  filename to encode.
 */
function encodeFileName(filename) {
  return encodeURIComponent(filename).replace(/\./g, '%2E');
}

/**
 * Decodes a file name from the realtime database
 * @param {String} filename  filename to decode.
 */
function decodeFileName(filename) {
  return decodeURIComponent(filename.replace(/%2E/g, "."));
}

/**
 * Creates a new tab with the given filename and the given contents.
 * @param {String} filename The filename for the tab.
 * @param {String} contents  The contents of the file. If non null the 
 * tab will be initialized with this string.
 */
function createNewTab(filename, contents) {
  if (!tabs[filename]) {
    // Add filename to tabs immediately so that the tab is not added
    // twice in the firebase child_added callback.
    tabs[filename] = {};

    const tab = document.createElement("button");
    tab.classList.add("inactive-tab", "tab");
    tab.innerText = filename;

    tab.onclick = (event) => {
      switchTab(filename);
    };

    const firepadContainer = document.createElement("div");
    firepadContainer.classList.add("hidden", "firepad-container");

    const fileExtension = "." + filename.split('.').pop();

    const languages = monaco.languages.getLanguages().filter((lang) => lang.extensions.includes(fileExtension));

    const language = languages.length >= 1 ? languages[0].id : "plaintext";

    const editor = monaco.editor.create(firepadContainer, {
      language: language,
      theme: "vs-dark"
    });

    //Use LF
    editor.getModel().setEOL("\n");
    
    const firepad = Firepad.fromMonaco(getFirebaseRef().child("files").child(encodeFileName(filename)), editor);

    if (contents !== null) {
      firepad.on("ready", () => {
        firepad.setText(contents);
      });
    }

    tabs[filename] = {
      editor: editor,
      firepad: firepad,
      tabButton: tab,
      editorContainer: firepadContainer
    };

    document.getElementById("tabs-container").appendChild(tab);
    document.getElementById("firepads").appendChild(firepadContainer);
  } else if (contents !== null) {
    tabs[filename].firepad.setText(contents);
  }

}

function addJitsiWindow() {
  const parent = document.getElementById("jitsi-window");

  const api = new JitsiMeetExternalAPI("meet.jit.si", {
    roomName: getParam("workspaceID"),
    height: 300,
    width: 500,
    parentNode: parent
  });
}

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("tabs-container").onwheel = scrollTabs;

  const config = {
    apiKey: 'AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE',
    databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
    projectId: "fulfillment-deco-step-2020",
  };
  firebase.initializeApp(config);

  require.config({ paths: {'vs': 'https://unpkg.com/monaco-editor@latest/min/vs'}});
  require(['vs/editor/editor.main'], function() {
    // Once the monaco library is loaded, we can start uploading files.
    const hiddenElements = document.getElementsByClassName("initially-hidden");

    for (let e of hiddenElements) {
      e.classList.remove("initially-hidden");
    }

    getFirebaseRef().child("files").on("child_added", (snapshot) => {
      const filename = decodeFileName(snapshot.key);
      createNewTab(filename, null);
      if (!currentTab) {
        switchTab(filename);
      }
    });
  });

  document.getElementById("downloadLink").href = `downloadWorkspace?workspaceID=${getParam("workspaceID")}`;
  addJitsiWindow();
});

window.onresize = () => {
  if (currentTab) {
    tabs[currentTab].editor.layout();
  }
};

/**
 * Called when the upload files button is clicked.
 */
function uploadFiles() {
  document.getElementById("upload-files").click();
}

/**
 * Called when files have been uploaded.
 */
async function filesUploaded() { // jshint ignore:line
  const files = document.getElementById("upload-files").files;

  for(var file of files) {
    // Convert to LF if in CRLF or CR
    let contents =  (await file.text()).replace(/\r\n?/g, "\n"); // jshint ignore:line
    createNewTab(file.name, contents);
  }

  switchTab(files[0].name);
}

function downloadFiles() {
  document.getElementById("downloadLink").click();
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