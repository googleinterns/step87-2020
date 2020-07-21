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
        gradient: {
          // Start color for gradient
          color1: '#C2E1FF',
          // Finish color for gradient
          color2: '#2457AA',
          // Start and end point of gradient, start 
          // on upper left corner
          x1: '0%', y1: '0%',
          x2: '100%', y2: '100%',
          // If true, the boundary for x1,
          // y1, x2, and y2 is the box. If
          // false, it's the entire chart.
          useObjectBoundingBoxUnits: true
        },
        stroke: '#031430',
        strokeWidth: 5
      },
    };

    var chart = new google.visualization.LineChart(
    document.getElementById("line-chart"));

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
      tempDataHolder.push([dates[i], waitAverages[i]]);
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
        textStyle: {
          bold:true
        },
      },
      backgroundColor: {
        gradient: {
          // Start color for gradient
          color1: '#C2E1FF',
          // Finish color for gradient
          color2: '#2457AA',
          // Start and end point of gradient, start 
          // on upper left corner
          x1: '0%', y1: '0%',
          x2: '100%', y2: '100%',
          // If true, the boundary for x1,
          // y1, x2, and y2 is the box. If
          // false, it's the entire chart.
          useObjectBoundingBoxUnits: true
        },
        stroke: '#031430',
        strokeWidth: 5
      },
    };

    var chart = new google.visualization.LineChart(
    document.getElementById("wait-chart"));

    chart.draw(data, options);
  });
}

google.charts.setOnLoadCallback(drawBasic);
google.charts.setOnLoadCallback(drawTime);

// Provide a link to the TA queue and display class code
function setRedirect(){
  var params = window.location.search;
  document.getElementById("redirect").href = "/queue/ta.html" + params;
  document.getElementById("classCode").innerText =  params.slice(11);
}

// Obtain the class's specific code from URL parameter
function getClassCode() {
  document.getElementById("hiddenClassCode").value = getParam("classCode");
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
  fetch(`/environment?envID=${envID}`).then(resp => {
    if (resp.status === 404) {
      row.remove();
    } else {
      setTimeout(() => checkDeletionStatus(envID, row), 1000);
    }
  });
}

function checkEnvStatus(envID, row) {
  fetch(`/environment?envID=${envID}`).then(resp => resp.ok ? resp.json() : "failed").then(env => {
    row.querySelector(".envStatus").innerText = env.status;

    if (env.status === "pulling") {
      setTimeout(() => checkEnvStatus(envID, row), 1000);
    } else {
      const deleteButton = row.querySelector(".envDelete");
      deleteButton.disabled = false;
      deleteButton.onclick = () => {
        row.querySelector(".envStatus").innerText = "deleting";
        fetch(`/environment?envID=${envID}`, {method: 'DELETE'});
        checkDeletionStatus(envID, row);
      };
    }
  });
}

function pullImage() {

  const name = document.getElementById("envName").value;
  const image = document.getElementById("envImage").value;
  const tag = document.getElementById("envTag").value;
  const row = addEnvRow(name, "queueing");

  fetch(`/queueEnvPull?classID=${getParam("classCode")}&name=${name}&image=${image}&tag=${tag}`)
    .then(resp => resp.text()).then(envID => {
      checkEnvStatus(envID, row);
    });
}

function getEnvs() {
  fetch(`/getEnvironments?classID=${getParam("classCode")}`).then(resp => resp.json()).then(envs => {

    for (var env of envs) {
     const row = addEnvRow(env.name, env.status);

     row.querySelector(".envDelete").onclick = () => {
      row.querySelector(".envStatus").innerText = "deleting";
      fetch(`/environment?envID=${env.id}`, {method: 'DELETE'});
      checkDeletionStatus(env.id, row);
     }; 
    }
  });
}

// Display the queue redirect link and environments once page loads
function onload() {
  setRedirect();
  getEnvs();
}