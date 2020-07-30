var taToken;

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

function notifyStudent(studentEmail){
    var params = window.location.search + "&studentEmail=" + studentEmail + "&taToken=" + taToken;
    const request = new Request("/notify-student" + params, {method: "POST"});
    
    fetch(request);
}

function endHelp(studentEmail){
    var params = window.location.search + "&studentEmail=" + studentEmail + "&taToken=" + taToken;
    const request = new Request("/end-help" + params, {method: "POST"});
    
    fetch(request).then(() => {
    document.getElementById('beingHelped').innerText = "";
    });
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
    btn.onclick = function(){notifyStudent(this.id);};

    notifyElem.appendChild(student);
    notifyElem.appendChild(btn);

    return notifyElem;
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
    studentElem.appendChild(createLink(url));

    return studentElem;
}

function getQueue() {
    var params = window.location.search;
    const request = new Request("/get-queue" + params + "&idToken=" + taToken, {method: "GET"});

    fetch(request).then(response => response.json()).then((queue) => {
    const queueListElement = document.getElementById('queue');
    queueListElement.innerText = "";
    queue.queue.forEach((studentEmail) => {
        queueListElement.appendChild(createListElement(studentEmail));
    });

    const beinghelpedElem = document.getElementById('beingHelped');

    if (queue.helping) {
        beinghelpedElem.innerHTML = "";
        document.getElementById('beingHelped').appendChild(createHelpedElem(queue.helping.email, queue.helping.workspace));
    } else {
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