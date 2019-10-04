Changesets
==========

Implements [stream-reduce](//github.com/jenningsanderson/stream-reduce), a fork of tile-reduce that reads output from Athena queries to the changesets table like: 

```sql
SELECT id, CAST(tags AS JSON) as tags, created_at, min_lat, max_lat,
         min_lon, max_lon, num_changes, uid, "user"
FROM changesets
WHERE min_lon > -3.2608
        AND max_lon < 1.2733
        AND min_lat > 4.5393
        AND max_lat < 11.1749
```
(This is an example for all changesets in Ghana. For more insightful values, this should be time filtered: `WHERE created_at > date '2014-01-01' ` for the entire planet. 

Once the CSV output is downloaded from S3 (shouldn't be _that_ big) and the options are set in `index.js` and `map-changeset-parser.js`, run it like so: 

	node index.js | tippecanoe -f -pf -pk -pg -Z12 -z12 -b0 -l changesets -o changesets.mbtiles

This will create a tileset at zoom 12 with the layer `changesets` that can be paired with osm-qa-tiles to provide changeset metadata.

Each changeset object has the following properties: 

|key|value|
|---|------|
|`@id`| Changeset ID
|`@uid`| User ID
|`@user`| User name
|`@comment`| Comment text from tags.comment
|`@area`   | Area (in `square km`) of the changeset.
|`@timestamp` | Timestamp (seconds) of `created_at`|
|`@changes` | Number of changes as reported by OSM (`num_changes`)|


Additionally, the following properties may exist:

|key|value|
|---|------|
|`@hot` | `1` if _hotosm_ was in the text |
|`#[hashtag]` |  `1` if this hashtag was present in comment. Such as `{"#hotosm-task-100":1}` |

All hashtags are made lowercase.
    
#### Future Goals
- [ ] Changeset comments 
- [ ] Continually adding new weekly/monthly changesets with `tippecanoe tile-join`
- [ ] More attributes; future versions should include: 
	
	|key|value|
	|---|------|
	|`@corporate` | _company name_ if UID was a data-team member |
	|`@youthmapper` |  _chapter name_ if UID is part of YouthMappers | 	|`@missingmaps` | `1` if this was a missing maps task |