// google.charts.load('current', {'packages':['corechart']});

// function drawChart() {
//   // Instead of the following, get the visits and days array from localStorage
//   let classNames = ["First", "Second"];
//   let visits = [5, 10];

//   let data=[];

//   const Header= ['Name', 'Visits', { role: 'style' }];

//   data.push(Header);

//   for (let i = 0; i < classNames.length; i++) {
//     let temp=[];
//     temp.push(classNames[i]);
//     temp.push(visits[i]);
//     temp.push("blue"); // Line graph will change based on number of visits
//     data.push(temp);
//   }
//   let chartdata = new google.visualization.arrayToDataTable(data);

//   let options = {
//     title: 'Class Visitors',
//     legend: { position: 'right' },
//     hAxis: {title: 'Class Name' },
//     vAxis: {title: 'Number of Visits' },
//     backgroundColor: {
//       gradient: {
//         // Start color for gradient
//         color1: '#fcf7b6',
//         // Finish color for gradient
//         color2: '#4ccd88',
//         // Start and end point of gradient, start 
//         // on upper left corner
//         x1: '0%', y1: '0%',
//         x2: '100%', y2: '100%',
//         // If true, the boundary for x1,
//         // y1, x2, and y2 is the box. If
//         // false, it's the entire chart.
//         useObjectBoundingBoxUnits: true
//       },
//       stroke: '#082f44',
//       strokeWidth: 5
//     },
//   };

//   let chart = new google.visualization.LineChart(document.getElementById('line-chart'));

//   chart.draw(chartdata, options);
// }

// google.charts.setOnLoadCallback(drawChart);




google.charts.load('current', {packages: ['corechart', 'bar']});

function drawBasic() {
     var data = new google.visualization.DataTable();
      data.addColumn('string', 'Class');
      data.addColumn('number', 'Visits');
      
      var classes = ["First", "Second", "Third", "Fourth"];
      var visits = [4, 8, 15, 2];
      
      var tempDataHolder = [];
      
      for (var i = 0; i < classes.length; i++) {
        var temp = [];
        temp.push(classes[i]);
        temp.push(visits[i]);
        tempDataHolder.push(temp);
      }
      
      data.addRows(tempDataHolder);

      var options = {
        title: 'Number of Visits per Class',
        hAxis: {
          title: 'Class Name',
        },
        vAxis: {
          title: 'Number of Visits'
        }
      };

      var chart = new google.visualization.ColumnChart(
        document.getElementById('chart_div'));

      chart.draw(data, options);
    }

    google.charts.setOnLoadCallback(drawBasic);