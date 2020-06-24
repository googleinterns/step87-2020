google.charts.load('current', {'packages':['corechart']});

function drawChart() {
  var visits = [5, 10, 15,20,10];
  var days = ['1', '2', '3','4','5'];

  var data=[];

  var Header= ['Day', 'Visits', { role: 'style' }];

  data.push(Header);

  for (var i = 0; i < days.length; i++) {
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

// This function would calculate number of visits to the site
function incrementVisits() {
  if (typeof(Storage) !== "undefined") { // Check if browser supports local storage
    // if (localStorage.clickcount) {
    //   localStorage.clickcount = Number(localStorage.clickcount)+1;
    // } else {
    //   localStorage.clickcount = 1;
    // }
   // console.log('There are' + localStorage.clickcount + 'visits to the site');
   console.log('Browser suppports web storage!');
  } else {
    console.log('Browser does not support web storage'); 
  }
}