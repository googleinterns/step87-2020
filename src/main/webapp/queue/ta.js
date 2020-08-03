var taToken;

// These booleans represent whether an operation on the queue is in progress.
// This will allow us to update the ui before getting updates from the back-end.
let notifying = false;
let endingHelp = false;

function createLink(url){
  let link = document.createElement("a");
  link.id = "workspaceRedirect";
  link.rel= "noopener noreferrer";
  link.target= "_blank";
  link.href = url;
  link.style = "text-decoration: none;";
  link.innerText = "Go to Workspace";

  return link;
}

function endHelp(studentEmail){
  var params = window.location.search + "&studentEmail=" + studentEmail + "&taToken=" + taToken;
  const request = new Request("/end-help" + params, {method: "POST"});
    
  endingHelp = true;
  document.getElementById('beingHelped').innerText = "";
  document.querySelectorAll(".notify-button").forEach((ele) => ele.classList.remove("hidden"));

  fetch(request).then(() => {
    setTimeout(() => endingHelp = false, 100);
  });
}

function createHelpedElem(studentEmail, url){
  let studentElem = document.createElement("div");
  studentElem.className = "help-container";

  let student = document.createElement('p');
  student.innerText = "You are helping " + studentEmail;
  student.className = "help-info";

  let btn = document.createElement("input");
  btn.value = "end interaction";
  btn.type = "button";
  btn.id = studentEmail;
  btn.className = "end-button";
  btn.onclick = function(){endHelp(this.id);};

  studentElem.appendChild(student);
  studentElem.appendChild(btn);
  if (url) {
    studentElem.appendChild(createLink(url));
  }

  return studentElem;
}

function notifyStudent(studentEmail, notifyElem){
  var params = window.location.search + "&studentEmail=" + studentEmail + "&taToken=" + taToken;
  const request = new Request("/notify-student" + params, {method: "POST"});
  notifying = true;
  notifyElem.remove();

  document.getElementById('beingHelped').appendChild(createHelpedElem(studentEmail));
  document.querySelectorAll(".notify-button").forEach((ele) => ele.classList.add("hidden"));

  fetch(request).then(() => setTimeout(() => notifying = false, 100));
}

function createListElement(studentEmail) {
  let notifyElem = document.createElement("div");
  notifyElem.className = "student-container";

  let student = document.createElement('p');
  student.innerText = studentEmail;
  student.className = "student-info";

  let btn = document.createElement("input");
  btn.value = "notify student you're ready to help";
  btn.type = "button";
  btn.id = studentEmail;
  btn.className = "notify-button";
  btn.onclick = () => notifyStudent(studentEmail, notifyElem);

  notifyElem.appendChild(student);
  notifyElem.appendChild(btn);

  return notifyElem;
}

function getQueue() {
  var params = window.location.search;
  const request = new Request("/get-queue" + params + "&idToken=" + taToken, {method: "GET"});

  fetch(request).then(response => response.json()).then((queue) => {
    const queueListElement = document.getElementById('queue');
    if (!notifying) {
      queueListElement.innerText = "";
      queue.queue.forEach((studentEmail) => {
        queueListElement.appendChild(createListElement(studentEmail));
      });
    }

    const beinghelpedElem = document.getElementById('beingHelped');

    if (queue.helping) {
      if (!endingHelp){
        queueListElement.querySelectorAll(".notify-button").forEach((ele) => ele.classList.add("hidden"));
        beinghelpedElem.innerHTML = "";
        document.getElementById('beingHelped').appendChild(createHelpedElem(queue.helping.email, queue.helping.workspace));
      }
    } else if (!notifying) {
      beinghelpedElem.innerHTML = "";
    }
  });
}

var repeat = setInterval(getQueue, 1000);
function setToken(token){
  taToken = token;
  getQueue();
}

function getToken() {
  var user = firebase.auth().currentUser;
  firebase.auth().onAuthStateChanged(function(user) {
    if (user) {
      console.log("User is signed in");
      user.getIdToken().then((token) => setToken(token));
    } else {
      console.log("User is not logged in");
      window.location.href = "/"; // Redirect to home page if not logged in
    }
  });
}

function onload() {
  getToken();
  document.getElementById("dashboard-link").href = `/dashboard.html${window.location.search}`;
}