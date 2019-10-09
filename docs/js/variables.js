var LAYERS  = ['buildings-MV-BEFORE','buildings-MV-AFTER','buildings-DELETED']

// Load a default timeline
PATH = "data/timelinedata.json";

LOCAL_TILESET_NAME = 'ghana'

var startDateInt = 0;
var endDateInt   = 2546475325; //2050



function prettyPrintString(properties){
  var copy = JSON.parse(JSON.stringify(properties))
  Object.keys(properties).forEach(function(k){
    if(k.indexOf('valid')>-1){
      copy[k] = new Date(properties[k]*1000)
    }
  })
  return JSON.stringify(copy)
}

function prettyPrint(properties){
  var output = "<table>"
  var copy = JSON.parse(JSON.stringify(properties))
  Object.keys(properties).forEach(function(k){
    output += `<tr><th>${k}</th>`
    if(k.indexOf('valid')>-1){
      copy[k] = new Date(copy[k]*1000).toISOString()
    }
    if(k.indexOf('uid')>-1){
      copy[k] = `<a class='link' href="https://openstreetmap.org/api/0.6/user/${copy[k]}" target="_blank">${copy[k]}</a>`
    }
    if(k=='@id'){
      copy[k] = `<a class='link' href="https://openstreetmap.org/way/${copy[k]}" target="_blank">${copy[k]}</a>`
    }
    output+= `<td style="padding-left:10px;">${copy[k]}</td></tr>`
  })
  return output + "</table>"
  return JSON.stringify(copy)
}
