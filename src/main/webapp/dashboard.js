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
      textStyle: {
        color: 'white'
      },
      backgroundColor: {
        color: '#2457AA',
        stroke: '#031430',
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