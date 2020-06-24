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
    legend: { position: 'bottom' }
  };

  var chart = new google.visualization.LineChart(document.getElementById('line-chart'));

  chart.draw(chartdata, options);
}

google.charts.setOnLoadCallback(drawChart);