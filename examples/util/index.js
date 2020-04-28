'use strict';

var fs = require('fs')
var path = require('path');
var tileReduce = require('@mapbox/tile-reduce');

var cre_above_v1=0

tileReduce({
    map: path.join(__dirname, "map-building-extraction.js"),
    zoom: 14,
//     sources: [{name: 'osm', mbtiles: path.join("../../osm-qa-with-tr.mbtiles"), raw: false}],
    sources: [{name: 'osm', mbtiles: path.join("/home/cc/buildings.mbtiles"), raw: false}],

    output: fs.createWriteStream('../buildings-sample.geojsonseq'),
//     bbox: [93.5,-11.14,129.74,10.19], //SE Asia
//     bbox: [99.6,-6.1,108.62,6.58], //smaller SE Asia
//     bbox: [-122.557337,37.412555,-121.841301,37.929844], //sf
//    bbox: [-124.48,32.53,-114.13,42.01],//CA
//     bbox: [-126.4,24.5,-60.3,50.8], // NA
//     bbox: [-104.917086,38.715452,-104.599914,39.035125], //coloradosprings
    bbox: [-72.36515,18.506916,-72.263818,18.59513], //port au prince, haiti
    numWorkers: 32
})
.on('reduce', function(res){
  cre_above_v1+=res
})
.on('end', function(){
  console.warn("cre_above_v1: " + cre_above_v1);
  console.warn("Done...probably still writing buffer to disk?");
})


/*
Port Au Prince: 
https://www.townsendjennings.com/geo/?geojson={%20%22type%22:%20%22Feature%22,%20%22geometry%22:%20{%20%22type%22:%20%22Polygon%22,%20%22coordinates%22:%20[%20[%20[%20-72.3651501341,%2018.5069163693%20],%20[%20-72.2638181381,%2018.5069163693%20],%20[%20-72.2638181381,%2018.595130045%20],%20[%20-72.3651501341,%2018.595130045%20],%20[%20-72.3651501341,%2018.5069163693%20]%20]%20]%20},%20%22properties%22:%20{}%20}#6.63/16.628/-69.730
*/