'use strict';

const parse = require('csv-parse/lib/sync');
const turf = require('@turf/turf');

const HEADERS  = mapOptions.headers;
const MAX_AREA = mapOptions.maxAreaKM;

const ALL_HASHTAGS = true;
const HOT = true;
const CORP = false;

module.exports = function(line, writeData, done) {
     
    const row = parse(line.trim(), {columns: HEADERS})[0]
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

            changesetObject.properties['@area'] = areaKM

            changesetObject.properties['@id']             = row.id
            changesetObject.properties['@uid']            = row.uid
            changesetObject.properties['@user']           = row.user
            changesetObject.properties['@comment']        = tags.comment
            changesetObject.properties['@timestamp']      = (new Date(row.created_at)).valueOf()/1000
            changesetObject.properties['@changes']        = row.num_changes
            
            if (HEADERS.indexOf('days') > -1){
                changesetObject.properties['@u_changesets']   = row.num_changesets
                changesetObject.properties['@u_sum_changes']  = row.sum_changes
                changesetObject.properties['@u_first_edit']   = row.first
                changesetObject.properties['@u_latest_edit']  = row.latest
                changesetObject.properties['@u_days_editing'] = row.days
            }

            writeData(JSON.stringify(changesetObject)+"\n");
        }
    }catch(e){
        console.error(e)
        console.error(row)
    }

    done(null, null); 
}