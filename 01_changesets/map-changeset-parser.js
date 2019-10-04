'use strict';

const csv = require('fast-csv');

const turf = require('@turf/turf');

const HEADERS = mapOptions.headers;
const MAX_AREA = mapOptions.maxAreaKM;

const ALL_HASHTAGS = true;
const HOT = true;
const CORP = false;

module.exports = function(line, writeData, done) {
    
    csv.parseString(line, { headers: HEADERS })
        .on('error', error => console.error(error))
        .on('data', row => {
        
            try{

                var changesetObject = turf.bboxPolygon([
                    Number(row.min_lon), 
                    Number(row.min_lat), 
                    Number(row.max_lon), 
                    Number(row.max_lat)]);

                var areaKM = turf.area(changesetObject) / 1000000

                //Convert to point if it was really a point object.
                if (areaKM == 0){
                    changesetObject.geometry.type = 'Point'
                    changesetObject.geometry.coordinates = [Number(row.min_lon),Number(row.min_lat)]
                }

                if (areaKM < MAX_AREA){

                    var tags = JSON.parse(row.tags)

                    if ( tags.hasOwnProperty('comment') ){

                        if(ALL_HASHTAGS){
                            if(tags.comment.indexOf('#') > -1 ) {
                                tags.comment.split(" ").forEach(function(w){
                                    if (w.startsWith("#") ){
                                        changesetObject.properties[w.toLowerCase()] = 1;
                                    }
                                })
                            }
                        }

                        if(HOT){
                            if ( tags.comment.indexOf('hotosm') > -1){
                                changesetObject.properties['@hot'] = 1
                            }
                        }
                    }

                    changsetObject.properties['@area'] = areaKM

                    changesetObject.properties['@id'] = row.id
                    changesetObject.properties['@uid'] = row.uid
                    changesetObject.properties['@user'] = row.user
                    changesetObject.properties['@comment'] = tags.comment
                    
                    console.log(JSON.stringify(changesetObject))
                }
            }catch(e){
                console.error(e)
//                 console.error(row)
            }
        })
    //TODO: Pass something more exciting back
    done(null, null); 
}