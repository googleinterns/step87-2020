let studentToken;
let iidToken;
let notifyBool = true; 
const SERVER_KEY = "AAAAAasd75g:APA91bGfddnjqVWJxOtnGQLMnJi4D8tnJUwP3ce04qB0GfUI3piA2lf3EGHPL86FfVyrWKayIXFCLqrrzthb_2i8ZjC9_xa3Hq-j0EXkp8HUpnMN7am3luEjkf56jM8K7zAQ7Edg6n4j";
const VAPID_KEY = "BATH4W6KjVLUSE-tGXhgBbFr6BINlPDO0gf4L9NB8F8qCLStYVzIvYpH4m32Yzlit26G0f4ofC2Zr044t1-hxHk";

//Retrieve Firebase Messaging object 
const messaging = firebase.messaging();
messaging.usePublicVapidKey(VAPID_KEY);

//Ask user for permission to send notifications
messaging
    .requestPermission()
    .then(function () {
    console.log("Notification permission granted.");
        
    // Get the token in the form of promise
    return messaging.getToken();
    })
    .then(function(token) {
    iidToken = token;
    })
    .catch(function (err) {
    console.log("Unable to get permission to notify.", err);
    });

// If user on page, no notification sent
messaging.onMessage(function(payload) {
  console.log("User on page- message received. ", payload);
});

function removeSelf(){
  var params = window.location.search + "&idToken=" + studentToken;
  const request = new Request("/remove-from-queue" + params, {method: "POST"});
    
  fetch(request).then(() => {
    window.location.href = "/userDash.html";
  });
}

let gotWorkspaceID = false;

function sendNotification(){
  var key = SERVER_KEY; 
  var to = iidToken; 
  var notification = {
    'title': 'you were just taken off the queue!',
    'body': 'navigate back to get the workspace link',
  };

  fetch('https://fcm.googleapis.com/fcm/send', {
    'method': 'POST',
    'headers': {
      'Authorization': 'key=' + key,
      'Content-Type': 'application/json'
    },
    'body': JSON.stringify({
      'notification': notification,
      'to': to
    })
  }).then(function(response) {
    console.log(response);
  }).catch(function(error) {
    console.error(error);
  });
}

function makeRequest(){
  var params = window.location.search + "&studentToken=" + studentToken;
  const request = new Request("/check-student" + params, {method: "GET"});
  fetch(request).then(response => response.json()).then((studentPosition) => {
    if (studentPosition.position === 0) {
        document.getElementById("queue_status").innerText = "";
        
        if (!studentPosition.ta){
            document.getElementById("beingHelped").innerText = "You are done being helped.";
            document.getElementById("workspaceRedirect").href = "";
            document.getElementById("workspaceRedirect").innerText = "";
            gotWorkspaceID = false;
            clearInterval(repeat);
    
        } else {
            document.getElementById("beingHelped").innerText = "You are being helped by " + studentPosition.ta;
        }

      if (notifyBool) {
        sendNotification();
        notifyBool = false;
      }    
    } else {
      document.getElementById("studentPosition").innerText = "You are #" + studentPosition.position + " on the queue.";
        
      document.getElementById("workspaceRedirect").href = studentPosition.workspace;
      document.getElementById("workspaceRedirect").innerText = "go to workspace";
    }
  });
}
let repeat = setInterval(makeRequest, 1000);

function setToken(token){
  studentToken = token;
  makeRequest();
}

function getToken() {
  // Initialize user's Firebase token
  var user = firebase.auth().currentUser;
  firebase.auth().onAuthStateChanged(function(user) {
    if (user) {
      console.log("User is signed in");
      user.getIdToken().then((token) => setToken(token));
    } 
    // Redirect to home page if not logged in
    else {
      console.log("User is not logged in");
      window.location.href = "/";
    }
  }); 
}