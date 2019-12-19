'use strict'

var fs = require('fs')
var path = require('path');
var tileReduce = require('@mapbox/tile-reduce');

//Read in configuration file here
try{
  var CONFIG = JSON.parse(fs.readFileSync('../extraction.config'))
}catch(e){
  console.warn("COULD NOT FIND extraction.config FILE")
  process.exit(1)
}

console.warn("Starting qa-tile-contribution-extractor with configuration details:")
console.warn(JSON.stringify(CONFIG, null, 2))

var dailySumsEditorList = {}
var dailySumsAllEditors = {}

tileReduce({
    map: path.join(__dirname, "map-contribution-extractor-reduce-sums.js"),
    zoom: 12,
//     sources: [{name: 'osm', mbtiles: path.join("../../osm-qa-with-tr.mbtiles"), raw: false}],
    sources: [{name: 'osm', mbtiles: path.join(CONFIG.qaTilesPath), raw: false}],
    output: fs.createWriteStream(CONFIG.output),
//     bbox: [93.5,-11.14,129.74,10.19], //SE Asia
//     bbox: [99.6,-6.1,108.62,6.58], //smaller SE Asia
    bbox: CONFIG.bbox || undefined,
    numworkers: 32
})
.on('reduce', function(res){

    Object.keys(res[0]).forEach(function(day){
      if (dailySumsAllEditors.hasOwnProperty(day) ){
        dailySumsAllEditors[day]+= res[0][day]
      }else{
        dailySumsAllEditors[day] = res[0][day]
      }
    })
    
    Object.keys(res[1]).forEach(function(day){
      if (dailySumsEditorList.hasOwnProperty(day) ){
        dailySumsEditorList[day]+= res[1][day]
      }else{
        dailySumsEditorList[day] = res[1][day]
      }
    })
    
})
.on('end', function(){
  console.warn("Done...probably still writing buffer to disk?");
    
  fs.writeFileSync('dailySumsEditorList.json', JSON.stringify(dailySumsEditorList));
  fs.writeFileSync('dailySumsAllEditors.json', JSON.stringify(dailySumsAllEditors));
    
})
