google.charts.load('current', {'packages':['corechart']});

function drawChart() {
  // Instead of the following, get the visits and days array from localStorage
  var visits = [5, 10, 7,20,10];

  var data=[];

  var Header= ['Day', 'Visits', { role: 'style' }];

  data.push(Header);

  for (var i = 0; i < visits.length; i++) {
    var temp=[];
    temp.push(i.toString());
    temp.push(visits[i]);
    temp.push("blue"); // Line graph will change based on number of visits
    data.push(temp);
  }
  var chartdata = new google.visualization.arrayToDataTable(data);

  var options = {
    title: 'Site Visitors',
    legend: { position: 'right' },
    hAxis: {title: 'Day' },
    vAxis: {title: 'Number of Visits' },
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
    },
  };

  var chart = new google.visualization.LineChart(document.getElementById('line-chart'));

  chart.draw(chartdata, options);
}

google.charts.setOnLoadCallback(drawChart);