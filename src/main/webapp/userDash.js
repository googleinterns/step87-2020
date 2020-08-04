let userToken;
    
let registeredClasses;
let ownedClasses;
let taClasses;

function enterQueue(params, urlRedirect, type){
  if (type === "registeredClasses"){
    const request = new Request("/enterqueue" + params, {method: "POST"});
    fetch(request).then(response => {
      window.location.assign(urlRedirect);
    }).catch(function(err) {
        console.info(err);
    });
  } else {
    window.location.assign(urlRedirect);
  } 
}

function createClassElement(classCode, nameClass, type, inQueue) {
  var params;
  var urlRedirect;
  if (type !== "registeredClasses"){
    params = "?classCode="+classCode+"&idToken="+userToken+ "&enterTA=" + "isTA";
    urlRedirect = "/queue/ta.html?classCode=" + classCode;
  } else {
    params = "?classCode="+classCode+"&idToken="+userToken;
    urlRedirect = "/queue/student.html?classCode=" + classCode;
  }

  let enterElem = document.createElement("div");
  enterElem.className = "class-container";
  enterElem.onclick = function(){enterQueue(params, urlRedirect, type);};

  let center = document.createElement("div");
  center.className = "class-info-container";
  let classElem;

  if (inQueue) {
    classElem = document.createElement('div');
    classElem.className = "class-info";

    let classP = document.createElement('p');
    classP.innerHTML = nameClass;

    let inQueueIndicator = document.createElement("p");
    inQueueIndicator.innerText = "In Queue!";
    inQueueIndicator.className = "in-queue-indicator";

    classElem.appendChild(classP);
    classElem.appendChild(inQueueIndicator);
  } else {
    classElem = document.createElement('p');
    classElem.innerText = nameClass;
    classElem.className = "class-info";
  }

  center.appendChild(classElem);
  enterElem.appendChild(center);

  return enterElem;
}

function requestAccess() {
  const classCodeInput = document.getElementById("classCode");
  fetch(`/requestAccess?idToken=${userToken}&classCode=${classCodeInput.value}`).then(resp => {
    classCodeInput.value = "";
    window.location.href = "#";
  });
}

function getClasses(){
  var params ="?idToken=" + userToken;
  const request = new Request("/get-user" + params, {method: "GET"});

  fetch(request).then(response => response.json()).then(userData => {
    userData.forEach(element => {
      if (element.type === "taClasses") {
        document.getElementById("taHeader").classList.remove("hidden");
      }
      var sectionElement = document.getElementById(element.type);
      sectionElement.prepend(createClassElement(element.code, element.name, element.type, element.inQueue));
    });
  });
}

function init() {
  var user = firebase.auth().currentUser;
  firebase.auth().onAuthStateChanged(function(user) {
    if (user) {
      user.getIdToken().then((token) => {
      userToken = token;
      document.getElementById("idToken").value = token;

      getClasses();
      });
    } else {
      console.log("User is not logged in");
      window.location.href = "/";
    }
  });
}

