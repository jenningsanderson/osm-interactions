let urlParams = new URLSearchParams(window.location.search);

let style = urlParams.get('style');
var styleString = "mapbox://styles/mapbox/" + ( (style==null)? "dark-v10" : style )

mapboxgl.accessToken = 'pk.eyJ1IjoiamVubmluZ3NhbmRlcnNvbiIsImEiOiIzMHZndnpvIn0.PS-j7fRK3HGU7IE8rbLT9A';

var map = new mapboxgl.Map({
    container: 'map',
    zoom: 6.5,
    maxZoom:30,
    center: [-0.895, 8.261],
    style: styleString,
    hash: true
});

map.addControl(new mapboxgl.NavigationControl());


//This kicks off everything
map.once('load',function(){

  //This is for debugging locally

  map.addSource("interactions",{
    type: 'vector',
    tiles: [
      // 'http://localhost:5000/tristandacunha-interations.mbtiles/{z}/{x}/{y}.pbf'
      // 'http://localhost:4000/south-america-interactions.mbtiles/{z}/{x}/{y}.pbf'
      'http://localhost:4000/south-america-interations.mbtiles/{z}/{x}/{y}.pbf'
    ],
    maxzoom: 14
  })

  map.addLayer({
    "id": "roads",
    "type": "line",
    'source': 'interactions',
    'source-layer':'interactions',
    'filter':['==','$type','LineString'],
    "paint": {
      'line-color':'green',
      'line-opacity':0.5,
      'line-width':2
    }
  })
  
  // map.addLayer({
  //   "id": "buildings-MV-AFTER",
  //   "type": "fill",
  //   'source': 'interactions',
  //   'source-layer':'interactions',
  //   'filter':['==','@e','MV_AFTER'],
  //   "paint": {
  //     'fill-color':'orange',
  //     'fill-opacity':0.5
  //   }
  // })

  
  // map.addLayer({
  //   "id": "buildings-MV-BEFORE",
  //   "type": "fill",
  //   'source': 'interactions',
  //   'source-layer':'interactions',
  //   'filter':['==','@e','MG'],
  //   "paint": {
  //     'fill-color':'purple',
  //     'fill-opacity':0.5
  //   }
  // })
  
  // map.addLayer({
  //   "id": "buildings-MV-AFTER",
  //   "type": "fill",
  //   'source': 'interactions',
  //   'source-layer':'interactions',
  //   'filter':['==','@e','MV_AFTER'],
  //   "paint": {
  //     'fill-color':'orange',
  //     'fill-opacity':0.5
  //   }
  // })
  
  // map.addLayer({
  //   "id": "buildings-DELETED",
  //   "type": "fill",
  //   'source': 'interactions',
  //   'source-layer':'interactions',
  //   'filter':['==','@e','DEL'],
  //   "paint": {
  //     'fill-color':'red',
  //     'fill-opacity':0.5
  //   }
  // })
  
  // map.addLayer({
  //   "id": "heatmap",
  //   "type": "heatmap",
  //   "source": "interactions",
  //   "source-layer": "interactions",
  //   "maxzoom": 9,
  //   "minzoom": 2,
  //   "paint": {
  //     "heatmap-intensity": [
  //       "interpolate",
  //       ["linear"],
  //       ["zoom"],
  //       0, 1,
  //       9, 3
  //       ],
  //     "heatmap-color": [
  //       "interpolate",
  //       ["linear"],
  //       ["heatmap-density"],
  //       0, "rgba(33,102,172,0)",
  //       0.2, "rgb(103,169,207)",
  //       0.4, "rgb(209,229,240)",
  //       0.6, "rgb(253,219,199)",
  //       0.8, "rgb(239,138,98)",
  //       1, "rgb(178,24,43)"
  //     ],
  //     // Adjust the heatmap radius by zoom level
  //     "heatmap-radius": [
  //       "interpolate",
  //       ["linear"],
  //       ["zoom"],
  //       0, 2,
  //       9, 20
  //     ],
  //     // Transition from heatmap to circle layer by zoom level
  //     "heatmap-opacity": [
  //       "interpolate",
  //       ["linear"],
  //       ["zoom"],
  //       7, 1,
  //       9, 0
  //     ],
  //   }
  // }, 'waterway-label');

  document.getElementById('loading').style.display = 'none';

  //Map interaction
  LAYERS.forEach(function(layerID){
    map.on('mouseenter', layerID, function () {
      map.getCanvas().style.cursor = 'pointer';
    });

    map.on('mouseleave', layerID, function () {
      map.getCanvas().style.cursor = '';
    });

    map.on('click', layerID, function (e) {
      var p = e.features[0].properties;

      // console.log(prettyPrintString(p))

      new mapboxgl.Popup()
        .setLngLat(e.lngLat)
        .setHTML(prettyPrint(p))
        .addTo(map);
    });



  })
});

var timeline = new D3Timeline(function(brushEvent, isSet){
      startDateInt = Math.floor( brushEvent[0].getTime() / 1000 )
      endDateInt   = Math.floor( brushEvent[1].getTime() / 1000 )

      if (isSet){
        map.setFilter('roads',['all',
          ['==','$type','LineString'],
          

          [">",'@vS',startDateInt],
          ["<=",'@vS',endDateInt],


          // [">=",'@validUntil',startDateInt],

          
          // ["<",'@vU',endDateInt]
          // ["<",'@validUntil',endDateInt],
          // ["<",'@validSince',startDateInt]

          // ["<","@validUntil",]

          // ['>=','@validSince',startDateInt], // The time the edit happened has to be greater than the start date we're looking at.
          // ['<','@validUntil',endDateInt] //The edit must be valid up unto the end date
        ])

        // map.setFilter('roads',['all',
          // ['has','@vU'],
          // ["<",'@vU',endDateInt],
          // ["<",'@vS',startDateInt]
          // ])

        // map.setFilter('buildings-MV-AFTER',['all',
        //   ['==','@edit','MV_AFTER'],
        //   ['>=','@validSince',startDateInt], // The time the edit happened has to be greater than the start date we're looking at.
        //   ['<','@validSince',endDateInt] // The time the edit happened has to be greater than the start date we're looking at.
        // ])


        // map.setFilter('buildings-DELETED',['all',
        //   ['==','@edit','DELETION'],
        //   ['>','@validUntil',startDateInt], // The time the edit happened has to be greater than the start date we're looking at.
        //   ['<','@validUntil', endDateInt], //The edit must be valid up unto the end date
        //   // ['>','@validSince', startDateInt],
        // ])
      }

      else{
        map.setFilter('roads',['all',
          ['==','$type','LineString'],
        ])
        // map.setFilter('buildings-MV-AFTER',['all',
        //   ['==','@edit','MV_AFTER'],
        // ])
        // map.setFilter('buildings-DELETED',['all',
        //   ['==','@edit','DELETION'],
        // ])
      }
})

function reloadTimeline(){

  document.getElementById('loading').style.display = 'block';

  var timelineCounts = [];
  var params = {};

  d3.json(PATH, function(error, data) {
    if (error) {
      throw error;
    }

    data.data.forEach(function(record){
      d = new Date(record.date)

      if (params.maxDate && params.minDate){
        if ( (d <= params.maxDate) && (d>= params.minDate) ) {
          timelineCounts.push({date: d, count: record.e})
        }
      }else{
        timelineCounts.push({date: d, count: record.e})
      }
    })

    if (timelineCounts.length){
      params.maxDate = params.maxDate || d3.max(timelineCounts, function(d) { return d.date; });
      params.minDate = params.minDate || d3.min(timelineCounts, function(d) { return d.date; });

      timeline.createD3Timeline({
        docID: "timeline-svg",
        data:  timelineCounts
      })

      if(map.loaded()){
        timeline.fireBrushEvent( [params.minDate, params.maxDate] )
      }

      document.getElementById('loading').style.display = 'none';
    }else{
      alert("No edits for this year, please choose another year")
      document.getElementById('loading').style.display = 'none';
    }

  })
}

//Run the page!
reloadTimeline();
