google.charts.load('current', {packages: ['corechart', 'line']});

// Render a chart visual on dashboard page for tracking class visits by date
function drawBasic() {

  // Set up the data table to have a class name and visits associated w/ that specific class
  var data = new google.visualization.DataTable();
  data.addColumn('date', 'Date');
  data.addColumn('number', 'Visits');
 
  // Organize visit data through visit-by-date servlet
  fetch(`/visit-date?classCode=` + getParam("classCode"))
    .then(response => response.json()).then(visits=> {
    
    var dates = visits.dates;

    // Convert JSON date format to Date type
    for (var k = 0; k < dates.length; k++) {
      var dateStr = dates[k];
      var realDate = new Date(dateStr);
      dates[k] = realDate;
    }

    var numVisits = visits.classVisits;

    var tempDataHolder = []; // To be pushed into datatable after updating

    // Loop through both lists and add info sets for each class 
    for (var i = 0; i < dates.length; i++) {
      tempDataHolder.push([dates[i], numVisits[i]]);
    }
    
    data.addRows(tempDataHolder); // Populate datatable with final data

    var options = {
      title: 'Number of Student Office Hour Visits',
      hAxis: {
        format: 'M/d/yy',
        title: 'Date',
        textStyle: {
          bold:true
        },
      },
      vAxis: {
        title: 'Number of Visits',
        textStyle: {
          bold:true
        },
      },
      backgroundColor: {
        fill: '#D6EBFF',
        stroke: '#031430',
        strokeWidth: 5
      },
    };

    var chart = new google.visualization.LineChart(
    document.getElementById("visit-chart"));

    chart.draw(data, options);

  });
}

// Render a chart visual on dashboard page for tracking average wait time by date
function drawTime() {

  // Set up the data table to have a class name and wait average associated w/ that specific class
  var data = new google.visualization.DataTable();
  data.addColumn('date', 'Date');
  data.addColumn('number', 'Wait');
 
  // Organize wait data through wait-time servlet
  fetch(`/wait-time?classCode=` + getParam("classCode"))
    .then(response => response.json()).then(waits=> {
    
    var dates = waits.dates;

    // Convert JSON date format to Date type
    for (var k = 0; k < dates.length; k++) {
      var dateStr = dates[k];
      var realDate = new Date(dateStr);
      dates[k] = realDate;
    }

    var waitAverages = waits.waitTimes;

    // Convert all times to minutes
    for (var j = 0; j < waitAverages.length; j++) {
      var timeInSeconds = waitAverages[j];
      var timeInMinutes = timeInSeconds / 60.0;
      waitAverages[j] = timeInMinutes.toFixed(2);  // Round to 2 decimal places
    }

    var tempDataHolder = []; // To be pushed into datatable after updating

    // Loop through both lists and add info sets for each class 
    for (var i = 0; i < dates.length; i++) {
      tempDataHolder.push([dates[i], Number(waitAverages[i])]);
    }
    
    data.addRows(tempDataHolder); // Populate datatable with final data

    var options = {
      title: 'Average Wait Time by Date',
      hAxis: {
        format: 'M/d/yy',
        title: 'Date',
        textStyle: {
          bold:true
        },
      },
      vAxis: {
        title: 'Wait Time (minutes)',
        format: '0.00',
        textStyle: {
          bold:true
        },
      },
      backgroundColor: {
        fill: '#D6EBFF',
        stroke: '#031430',
        strokeWidth: 5
      },
    };

    const waitChart = document.getElementById("wait-chart");
    waitChart.classList.remove("hidden");
    var chart = new google.visualization.LineChart(waitChart);

    chart.draw(data, options);
    waitChart.classList.add("hidden");
  });
}

google.charts.setOnLoadCallback(drawBasic);
google.charts.setOnLoadCallback(drawTime);

/* Creates a <li> element for every item in json */
function createListElement(text) {
  const liElement = document.createElement('li');
  liElement.innerText = text;
  return liElement;
}

// Provide a link to the TA queue and display class code
function setRedirect(){
  var params = window.location.search;
  document.getElementById("redirect").onclick = () => window.location.href = "/queue/ta.html" + params;
  document.getElementById("classCode").innerText =  params.slice(11);

  // Get TA participants
  fetch(`/participants?classCode=${getParam("classCode")}&type=teach-staff`).then(response => response.json()).then((list) => {
    const listElement = document.getElementById('classTAList');
    listElement.innerHTML = '';
    
    // Use HTML to display each message
    for (var i = 0; i < list.length; i++) {
      listElement.appendChild(
        createListElement(list[i]));
    }
  });

  // Get student participants
  fetch(`/participants?classCode=${getParam("classCode")}&type=student`).then(response => response.json()).then((list) => {
    const listElement = document.getElementById('classStudentList');
    listElement.innerHTML = '';
    
    // Use HTML to display each user
    for (var i = 0; i < list.length; i++) {
      listElement.appendChild(
        createListElement(list[i]));
    }
  });
}

// Obtain the class's specific code from URL parameter
function getClassCode() {
  document.getElementById("hiddenClassCode").value = getParam("classCode");
  document.getElementById("hiddenClassCode2").value = getParam("classCode");
  return true;
}  

// Obtain the class's specific code from URL parameter
function getRosterClassCode() {
  document.getElementById("hiddenRosterClassCode").value = getParam("classCode");
  return true;
} 

function addEnvRow(name, status) {

  const template = document.getElementById("envRowTemplate");
  const copy = template.content.cloneNode(true).querySelector("tr");

  copy.querySelector(".envName").innerText = name;
  copy.querySelector(".envStatus").innerText = status;

  const deleteButton = copy.querySelector(".envDelete");
  deleteButton.disabled = status !== "ready" && status !== "failed";

  document.getElementById("envTable").appendChild(copy);

  return copy;
}

function checkDeletionStatus(envID, row) {
  getToken().then(tok => fetch(`/environment?envID=${envID}&idToken=${tok}`).then(resp => {
    if (resp.status === 404) {
      row.remove();
    } else {
      setTimeout(() => checkDeletionStatus(envID, row), 1000);
    }
  }));
}

function checkEnvStatus(envID, row) {
  getToken().then(tok => {
    fetch(`/environment?envID=${envID}&idToken=${tok}`).then(resp => resp.ok ? resp.json() : "failed").then(env => {
      row.querySelector(".envStatus").innerText = env.status;
  
      if (env.status === "pulling") {
        setTimeout(() => checkEnvStatus(envID, row), 1000);
      } else {
        const deleteButton = row.querySelector(".envDelete");
        deleteButton.disabled = false;
        deleteButton.onclick = () => {
          row.querySelector(".envStatus").innerText = "deleting";
          getToken().then(tok => fetch(`/environment?envID=${envID}&idToken=${tok}`, {method: 'DELETE'}));
          checkDeletionStatus(envID, row);
        };
      }
    });
  });
}

function pullImage() {
  const name = document.getElementById("envName").value;
  const image = document.getElementById("envImage").value;
  const tag = document.getElementById("envTag").value;
  const row = addEnvRow(name, "queueing");

  getToken().then(tok => {
    fetch(`/queueEnvPull?classID=${getParam("classCode")}&name=${name}&image=${image}&tag=${tag}&idToken=${tok}`)
      .then(resp => resp.text()).then(envID => {
        checkEnvStatus(envID, row);
    });
  });
}

function getEnvs() {
  getToken().then(tok => {
    fetch(`/getEnvironments?classID=${getParam("classCode")}&idToken=${tok}`).then(resp => resp.json()).then(envs => {

      for (var env of envs) {
       const row = addEnvRow(env.name, env.status);
  
       row.querySelector(".envDelete").onclick = () => {
        row.querySelector(".envStatus").innerText = "deleting";
        fetch(`/environment?envID=${env.id}&idToken=${tok}`, {method: 'DELETE'});
        checkDeletionStatus(env.id, row);
       }; 
      }
    });
  });
}

function displayClass(){
  getToken().then((token) => {
    var params = window.location.search + "&idToken=" + token;

    const nameRequest = new Request("/get-class" + params, {method: "GET"});
    fetch(nameRequest).then(response => response.json()).then((name) => {
      document.getElementById("className").innerText = name;
    });
  });
}

// Only show delete button to owners
function displayDelete(){
  getToken().then((token) => {
    var params = window.location.search + "&idToken=" + token;

    const displayRequest = new Request("/get-role" + params, {method: "GET"});
    fetch(displayRequest).then(response => response.json()).then((role) => {
      var elem = document.getElementById("delete");
      if (role === "owner"){
        elem.classList.remove("hidden");
      }
    });
  });
}

// Only show add owner form to owners
function displayAddOwner(){
  getToken().then((token) => {
    var params = window.location.search + "&idToken=" + token;

    const displayRequest = new Request("/get-role" + params, {method: "GET"});
    fetch(displayRequest).then(response => response.json()).then((role) => {
      var elem = document.getElementById("ownerForm");
      if (role === "owner"){
        elem.style.display = "inline-block";
      }
    });
  });
}

function deleteClass(){
  if (confirm("Are you certain you want to permanently delete this class?") === true) {
	getToken().then((token) => {
      var params = window.location.search + "&idToken=" + token;
      const request = new Request("/delete-class" + params, {method: "POST"});
      fetch(request).then(response => {
        window.location.assign("/userDash.html");
      });
    });
  }
}

function onload() {
  setRedirect();

  firebase.auth().onAuthStateChanged(function(user) {
    displayClass();
    displayDelete();
    displayAddOwner();
    getEnvs();
  });
}

function switchTab(tabName) {
  const tabs = document.getElementsByClassName("tab");

  for (let tab of tabs) {
    tab.classList.remove("active-tab");
    tab.classList.add("inactive-tab");
  }

  const charts = document.getElementsByClassName("chart");

  for (let chart of charts) {
    chart.classList.add("hidden");
  }

  const tab = document.getElementById(tabName + "-tab");
  tab.classList.remove("inactive-tab");
  tab.classList.add("active-tab");

  document.getElementById(tabName + "-chart").classList.remove("hidden");
}
