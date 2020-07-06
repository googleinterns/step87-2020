google.charts.load('current', {packages: ['corechart', 'bar']});

function drawBasic() {

  // Set up the data table to have a class name and visits associated w/ that specific class
  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Class');
  data.addColumn('number', 'Visits');
      
  fetch(`/visits`).then(response => response.json()).then(visits=> {

      // Temporary values for class and visit lists
    var classes = visits.listOfClassNames;
    var numVisits = visits.visitsPerClass;

    var tempDataHolder = []; // To be pushed into datatable after updating

    // Loop through both lists and add info sets for each class 
    for (var i = 0; i < classes.length; i++) {
      var temp = [];
      temp.push(classes[i]);
      temp.push(numVisits[i]);
      tempDataHolder.push(temp);
    }

    data.addRows(tempDataHolder); // Populate datatable with final data

    var options = {
      title: 'Number of Visits per Class',
      hAxis: {title: 'Class Name'},
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

    var chart = new google.visualization.ColumnChart(
    document.getElementById('bar-chart'));

    chart.draw(data, options);
  });
}

google.charts.setOnLoadCallback(drawBasic);

// Verify the user is logged in before accessing the dashboard
function auth() {
  const config = {
    apiKey: 'AIzaSyA1r_PfVDCXfTgoUNisci5Ag2MKEEwsZCE',
    databaseURL: "https://fulfillment-deco-step-2020.firebaseio.com",
    projectId: "fulfillment-deco-step-2020",
  };
  firebase.initializeApp(config);

  // Redirect to home page if not logged in
  firebase.auth().onAuthStateChanged(function(user) {
    if (!user) {
      window.location.href = "/";
    }
  });

  function getToken() {
    return firebase.auth().currentUser.getIdToken();
  }

  function getUid() {
    return firebase.auth().currentUser.uid;
  }
}