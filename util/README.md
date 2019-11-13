Contribution Extraction Scripts
===============================

These scripts run based on the contents of `extraction.config`

qa-tile-contribution-extractor.js
---------------------------------

For current osm-qa-tiles and qa-tiles-plus:

Generates single geojsonseq otuput file with four levels of editing summaries:


|Level | layer | Description |
|------|-----|-------------|
| 1. | `objects` | Individual objects with the latest edit metadata if from an editor of interest. | 
| 2. | `userDailyPointSummaries` | Per day, per tile summary of a user's edits (centerOfMass)
| 3. | `dailyPointSummaries` | Per day, per tile summary of a team's edits (centerOfMass)
| 4. | `tilePointSummaries` | Per tile summary of each team's edits (tile center)
