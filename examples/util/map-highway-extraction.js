// var _    = require('lodash');
// var turf = require("@turf/turf");
// var tilebelt = require("@mapbox/tilebelt");

module.exports = function(data, tile, writeData, done) {

  const highways = data.osm.highways
  
  var cre_above_v1 = 0;
    
  highways.features.forEach(function(f){
     
      writeData(JSON.stringify(f)+ "\n")
     
//     //First, find all of the NEW parking aisles
//     if (f.properties['@e'] == 'CRE'){
        
//         const aA = JSON.parse(f.properties['@aA'])
        
//         if ( aA.hasOwnProperty('service') && aA.service== 'parking_aisle' ){

//           //Validation
//           if (f.properties['@v']==1){     
//               f.properties = {
//                 id  : f.properties['@id'],
//                 uid : f.properties['@uid'],
//                 timestamp: f.properties['@vS'],
//                 vs: f.properties['@vS'],
//                 geom: f.geometry
//               }
//               writeData(JSON.stringify(f)+ "\n")
//           }else{
//             cre_above_v1++
//           }
//         }
//     }
        
        
  })
  
  done(null, cre_above_v1)
};
