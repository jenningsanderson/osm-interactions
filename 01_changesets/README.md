Changesets
==========

A new implementation of an older project to create tilesets of changesets, this is a [stream-reduce](//github.com/jenningsanderson/stream-reduce) script that reads output from Athena queries of the following format.

```sql
SELECT id,
         CAST(tags AS JSON) as tags,
         created_at,
         min_lat,
         max_lat,
         min_lon,
         max_lon,
         num_changes,
         uid,
         "user"
FROM changesets
WHERE min_lon > -3.2608
        AND max_lon < 1.2733
        AND min_lat > 4.5393
        AND max_lat < 11.1749
```

It then creates changeset objects that are points or square polygons with the standard properties:
`@id`
`@uid`
`@user`
`@comment`

And additional: 
`@area` = area in square km
`@hot`  = `1` if `hotosm` was in the text, otherwise property isn't there. 
`#[hashtag]>` = `1` if this hashtag existed in the comment, otherwise not present. Hashtags are all lowercase.

Future additions include: `@corporate`, `@youthmapper`, `@missingmaps`, etc. These will need to be looked up by UID.

These tilesets should be generated at z12 or z14 and then used in tandem with osm-interactions tilesets to provide valuable changeset metadata.

Not present: changeset comments.

Filtering by date should mean that tile-join could simply add features to these tilesets on a weekly basis or something?