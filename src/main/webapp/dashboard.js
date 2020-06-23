google.charts.load('current', {'packages':['corechart']});

function drawChart() {
  var data = google.visualization.arrayToDataTable([
    ['Day', 'Visitors'],
    ['1',  7],
    ['10',  21],
    ['20',  16],
    ['30',  10]
  ]);

  var options = {
    title: 'Site Visitors',
    legend: { position: 'bottom' }
  };

  var chart = new google.visualization.LineChart(document.getElementById('line-chart'));

  chart.draw(data, options);
}

google.charts.setOnLoadCallback(drawChart);