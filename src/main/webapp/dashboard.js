google.charts.load('current', {packages: ['corechart', 'line']});

// Render a chart visual on dashboard page for tracking class visits by date
function drawBasic2() {

  // Set up the data table to have a class name and visits associated w/ that specific class
  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Date'); // Is this supposed to be string or date?
  data.addColumn('number', 'Visits');

  fetch(`/visit-date?classCode=` + getParam("classCode")).then(response => response.json()).then(visits=> {
    
    var dates = visits.dates;
    var numVisits = visits.classVisits;

    var tempDataHolder = []; // To be pushed into datatable after updating

    // Loop through both lists and add info sets for each class 
    for (var i = 0; i < dates.length; i++) {
      var temp = [];
      temp.push(dates[i]);
      temp.push(numVisits[i]);
      tempDataHolder.push(temp);
    }
    
    data.addRows(tempDataHolder); // Populate datatable with final data

    var options = {
      title: 'Number of Visits per Day',
      hAxis: {title: 'Date'},
      vAxis: {title: 'Number of Visits'},
      backgroundColor: {
        gradient: {
          // Start color for gradient
          color1: '#fcf7b6',
          // Finish color for gradient
          color2: '#4ccd88',
          // Start and end point of gradient, start 
          // on upper left corner
          x1: '0%', y1: '0%',
          x2: '100%', y2: '100%',
          // If true, the boundary for x1,
          // y1, x2, and y2 is the box. If
          // false, it's the entire chart.
          useObjectBoundingBoxUnits: true
        },
        stroke: '#082f44',
        strokeWidth: 5
      },
    };

    var chart = new google.visualization.LineChart(
    document.getElementById("line-chart"));

    chart.draw(data, options);

  });
}

google.charts.setOnLoadCallback(drawBasic2);

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