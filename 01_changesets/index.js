const streamReduce = require('json-stream-reduce');
const path = require('path')


//TODO: Be smart about parsing the first line of the CSV as header row...
// for now, use this: 
const HEADERS = ["id","tags","created_at","min_lat","max_lat","min_lon","max_lon","num_changes","uid","user"];
//as generated from this

/* ```sql
    SELECT id,CAST(tags AS JSON) as tags,created_at,min_lat,max_lat,min_lon,max_lon,num_changes,uid,"user"
``` */

streamReduce({
  map: path.join(__dirname, 'map-changeset-parser.js'),
  file: path.join(__dirname, 'sample.csv'),
//   file: path.join(__dirname, 'ghana_changesets.csv'),
  mapOptions: {
      headers: HEADERS,
      maxAreaKM: 1000 // square kilometers (larger than these is a bit excessive?)
  },
  maxWorkers:5 // The number of cpus you'd like to use
})
.on('reduce', function(res) {
})
.on('end', function() {
});