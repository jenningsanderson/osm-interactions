var fs   = require('fs')
var _    = require('lodash');
var turf = require("@turf/turf");
var tilebelt = require("@mapbox/tilebelt");

try{
  var CONFIG = JSON.parse(fs.readFileSync('../extraction.config'))
}catch(e){
  console.warn(e)
  console.warn("COULD NOT PARSE extraction.config FILE")
  process.exit(1)
}

var contributors  = require(CONFIG.editors)

// Unfortunately it might be easier to calculate some of these challenges at the tile level
var uniqueTeams = _.uniq(contributors.map(_=>{return _.c}));
var teamUIDTracker = {}

// COUNTERS
function countKM(features){
  var totalKM = 0;
  features.forEach(function(f){
    try{
      if (f.properties.highway && f.geometry.type==="LineString"){
        totalKM +=  turf.lineDistance(f)
      }
    }catch(e){
        console.warn("countKMFail: ", e.message)
    }
  })
  return totalKM;
}

function countBuildings(features){
  var totalBuildings = 0;
  features.forEach(function(f){
    try{
      if (f.properties.building && f.properties.building != "no"){
        totalBuildings++
      }
    }catch(e){
        console.warn("countBuildingFail: ", e.message)
    }
  })
  return totalBuildings;
}

function countPOIs(features){
  //How's this for a POI definition?
  var totalPOIs = 0;
  features.forEach(function(f){
    try{
      if (!f.properties.highway) {
        if (f.properties.amenity || f.properties.name){
          totalPOIs++
        }
      }
    }catch(e){
        console.warn("countPOIFail: ", e.message)
    }
  });
  return totalPOIs
}

module.exports = function(data, tile, writeData, done) {

  var teams           = {};
  var teamTileTotals  = {};

  for (var i in uniqueTeams){
    teams[uniqueTeams[i]] = [];
    teamTileTotals[uniqueTeams[i]] = {
      'features'         : 0,
      'km'               : 0,
      'buildings'        : 0,
      'pois'             : 0,
    }
  }

  // Extract the osm layer from the osm-qa-tile
  var layer = data.osm.osm;

  // We love quadkeys for simplicity
  var thisTile =  tilebelt.tileToQuadkey(tile);

  // Group all objects by day to get daily summaries
  var gbDayFull = _.groupBy(layer.features,function(feat){
    return Math.floor(feat.properties['@timestamp']/86400)
  });

  // Group features by UID for counting
  var gbUID = _.groupBy(layer.features, function(f){return f.properties['@uid']})

  var gbHandle = _.groupBy(layer.features, function(f){return f.properties['@user']})

  // Everything should be by team
  uniqueTeams.forEach(function(teamName){

    // Main object
    var teamUIDs = {}
    var teamEdits = [];

    // Loop through the users on this team
    contributors.filter(_=>{return _.c==teamName && _.u}).forEach(function(contributor){
      // prioritize our UID match
      if (gbUID.hasOwnProperty( contributor.u.toString() ) ){
        var feats = JSON.parse(JSON.stringify(gbUID[contributor.u.toString()])) //a cheep deepcopy

        if (CONFIG.USE_F_FILTERING && contributor.hasOwnProperty('f') ){
          feats = feats.filter(feat=>{return feat.properties['@timestamp'] > contributor.f})
        }

        if (CONFIG.USE_T_FILTERING && contributor.hasOwnProperty('t') ){
          feats = feats.filter(feat=>{return feat.properties['@timestamp'] <= contributor.t})
        }

        teamUIDs[contributor.u.toString()] = feats || [];
      }
    });

    //Backup: find those from handles only...
    contributors.filter(_=>{return _.c==teamName && !_.u}).forEach(function(contributor){
      if (gbHandle.hasOwnProperty( contributor.h ) ){
        var uid = gbHandle[contributor.h][0].properties['@uid']

        //If this UID is already in the above, it means multiple usernames for 1 ID exists in master
        if ( teamUIDs.hasOwnProperty(uid.toString() ) ){
//           console.warn('Already have UID, skipping: ' + JSON.stringify(contributor))
        }else{
          //Add this user (by UID) to our records
          var feats = JSON.parse(JSON.stringify(gbHandle[contributor.h])) //a cheep deepcopy

          if (CONFIG.USE_F_FILTERING && contributor.hasOwnProperty('f') ){
            feats = feats.filter(feat=>{return feat.properties['@timestamp'] > contributor.f})
          }

          if (CONFIG.USE_T_FILTERING && contributor.hasOwnProperty('t') ){
            feats = feats.filter(feat=>{return feat.properties['@timestamp'] <= contributor.t})
          }
          teamUIDs[uid.toString()] = feats || [];
        }
      }
    });

    //Now we have all of a team's edits stored by UID in teamUIDs
    Object.keys(teamUIDs).forEach(function(uid){

      //If not everything was filtered out...
      if (teamUIDs[uid].length){
        
        //Iterate through all of this user's edits
        teamUIDs[uid].forEach(function(feature){

          //Write out the actual geometry first
          var length = undefined;
            
          try{
            length = turf.length(feature, {'units':'kilometers'})
          }catch(e){
            console.warn("FAILED TURF.length on feat: ", JSON.stringify(feature))
          }
            
          writeData(JSON.stringify({
                'type':'Feature',
                'geometry':feature.geometry,
                'properties':{
                  //user & team meta
                  'u': Number(uid),
                  'h': feature.properties['@user'],
                  'c': teamName,

                  //standard qa-metadata
                  't': feature.properties['@timestamp'],
                  '@c': feature.properties['@changeset'],
                  'i': feature.properties['@id'],
                  'v': feature.properties['@version'],

                  //a few enhanced attributes that might matter (from qa-tiles-plus)
                  'r': (feature.properties.hasOwnProperty('@tr')? true : undefined),
                  'h': (feature.properties.hasOwnProperty('highway')? feature.properties['highway'] : undefined),
                  'b': (feature.properties.hasOwnProperty('building')? feature.properties['building'] : undefined),
                  'n': (feature.properties.hasOwnProperty('name')? feature.properties['name'] : undefined),
                  'l': length,
                  'q': "q"+ thisTile
                },
                'tippecanoe':{
                  'layer':'objects',
                  'maxzoom': 16,
                  'minzoom': 12
                }
          })+"\n")
        });

        //Also write out a per-day, per-tile summary for this particular user:
        var gbUserDay = _.groupBy(teamUIDs[uid], function(feat){
          return Math.floor(feat.properties['@timestamp']/86400)
        });
        var handle = teamUIDs[uid][0].properties['@user'];
            
        Object.keys(gbUserDay).forEach(function(day){
          try{
            var dailyCenter = turf.centerOfMass({
              'type':'GeometryCollection',
              'geometries': gbUserDay[day].map(function(f){return f.geometry})
            })
          }catch(e){
            console.warn("FAILED TURF.centerOfMass | with feat count: ",teamUIDs[uid].length)
            var dailyCenter = {'properties':{}}
          }

          dailyCenter.properties['h'] = handle
          dailyCenter.properties['u'] = Number(uid)
          dailyCenter.properties['e'] = gbUserDay[day].length
          dailyCenter.properties['c'] = teamName
          dailyCenter.properties['t'] = Number(day)*86400;
          dailyCenter.properties['q'] = "q"+ thisTile

          dailyCenter['tippecanoe'] = {
            'minzoom': 1,
            'maxzoom': 12,
            'layer'  : 'userDailyPointSummaries'
          }
          writeData(JSON.stringify(dailyCenter)+"\n");
        });

        //Store for full summary?
        teamEdits = teamEdits.concat(teamUIDs[uid]);

      }else{
        //All of this users edits were filtered out? TODO
      }
    })

    //Now calculate team/tile summaries / day
    var gbDay = _.groupBy(teamEdits, function(feat){
      return Math.floor(feat.properties['@timestamp']/86400)
    });

    //Iterate over the days...
    Object.keys(gbDay).forEach(function(day){

      var dailyKM        = countKM(gbDay[day]);
      var dailyBuildings = countBuildings(gbDay[day]);
      var dailyPOIs      = countPOIs(gbDay[day]);
      var dailyEdits     = gbDay[day].length

      teamTileTotals[teamName]['features']  += dailyEdits
      teamTileTotals[teamName]['km']        += dailyKM
      teamTileTotals[teamName]['buildings'] += dailyBuildings
      teamTileTotals[teamName]['pois']      += dailyPOIs

      try{
        var center = turf.centerOfMass( turf.featureCollection(gbDay[day]) )
      }catch(e){
        console.warn(e)
        console.warn("FAILED turf.centerOfMass(turf.featureCollection(<feats>))) with feats.length: ",gbDay[day].length)
        center = {'properties':{}} //So create a geometryless point
      }
      center.tippecanoe = {
        'layer'  : 'dailyPointSummaries',
        'minzoom': 8,
        'maxzoom': 11
      }
        
      center.properties['c']     = teamName;
      center.properties['e']     = dailyEdits;
      center.properties['b']     = dailyBuildings;
      center.properties['km']    = dailyKM;
      center.properties['p']     = dailyPOIs;
      center.properties['t']     = Number(day)*86400;    
      center.properties['q']     = 'q' + thisTile;

      writeData(JSON.stringify(center)+"\n")
    })
  })//and onto the next team

  //AND ONE MORE ROUND for TILE POINT Summaries
  var totals = turf.center(tilebelt.tileToGeoJSON(tile))

  totals.properties = {
    'qKey'             : 'q' + thisTile,
    'totalFeatures'    : layer.features.length,
    'totalKM'          : countKM(layer.features),
    'totalBuildings'   : countBuildings(layer.features),
    'totalPOIs'        : countPOIs(layer.features),
    'teamSumFeatures'  : 0,
    'teamSumKM'        : 0,
    'teamSumBuildings' : 0,
    'teamSumPOIs'      : 0
  }

  Object.keys(teamTileTotals).forEach(function(team){
    if (teamTileTotals[team].km > 0){
      totals.properties[team+"-km"] = teamTileTotals[team].km
      totals.properties['teamSumKM']       += teamTileTotals[team].km
    }  

    if (teamTileTotals[team].buildings > 0){
      totals.properties[team+"-b"]  = teamTileTotals[team].buildings
      totals.properties['teamSumBuildings']+= teamTileTotals[team].buildings
    }

    if (teamTileTotals[team].pois > 0){
      totals.properties[team+"-p"]  = teamTileTotals[team].pois
      totals.properties['teamSumPOIs']     += teamTileTotals[team].pois
    }

    if (teamTileTotals[team].features > 0){
      totals.properties[team+"-e"]  = teamTileTotals[team].features 
      totals.properties['teamSumFeatures'] += teamTileTotals[team].features
    }
  })

  totals.tippecanoe = {
        'layer'  : 'tilePointSummaries',
        'minzoom': 7,
        'maxzoom': 11
      }
  writeData(JSON.stringify(totals)+"\n")
  done(null, null)
};
