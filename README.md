# osm-interactions

More an idea at this point than an implementable application / project, I want to document the following observations and put a plan/research agenda in place.

### Objective
To understand how the map has and will continue to evolve, we need to be able to tell the complete story behind the map to this point [1]. This is not a new realization among the OSM analysis community, but the feasibility of analyzing the map in this way has only more recently become possible through technical innovations. All the while, the community that supports the map has grown immensely and we're overdue for new tools that can handle this.

**tl;dr**
OSM is not a map or a spatial database, but a record of editing actions, each of which is a discrete _interaction_ between contributors and the map (database); though these are not always visible edits---consider [Tasking-Manager](//tasks.hotosm.org) validation. Analyzing the map as a series of interactions rather than the number of objects enables us to tell a more detailed story of the map's evolution.

These discrete editing actions are then called `osm-interactions`. These _interactions_ have the following properties: 

##### 1. Happen at the object level (Though are not always observable)
  - Consider the objects that a validator or secondary editor _did not edit_ when performing Q/A updates to nearby objects. This is implicit validation. While their user is not present in the history of these objects, they have implicitly validated the quality (accuracy, temporality, etc.)
  - Otherwise, an interaction is synonymous with a new version (or [_minor version](https://www.openstreetmap.org/user/Jennings%20Anderson/diary/47133#background)_) of an object.

##### 2. May or may not involve another user
  - Creating new objects on the map where other data does not currently exist is an interaction between a user and the map.
  - Editing an existing object is aa form of interaction with the other contributors who also edited that object.
  - Stretching it: The presence of another user's map interaction was the impetus of a new interaction? New objects were added during validation or Q/A that the original mapper missed. 
  
##### 3. Always has additional context
  - HOT task? Organized editing effort? Import? Local editor with passion for mapping object type `x`. Another obvious attribute of all edits in OSM, the context surrounding an edit is often hidden or excluded in analysis. Changeset comments are of critical importance to most forms of analysis, but are difficult to involve at scale.
  
... This list will contintue to grow?


#### Why is this so important?
My current use-case for such an approach is an extension / implementation of my _contributor-centric_ analytic approaches to OSM data analysis. My current research assessing the impact of organized editing on the map requires such approaches: it's about the actions and their results, not the status of the data: that's a known entity, it's increasing in both volume and quality.



### Requirements (A brainstorm)
 - [ ] A changeset (lookup) database that is annotated per changeset ID with additional context. `"HOT:True/False", "CORP:True/False", "IMPORT:True/False", "BOT:True/False", "NEW_USER:True/False"`
 - [ ] A taxonomy of interaction types such as `"validation" : ["delete", "square", "tag_correction"], "import" : ["building", address", roads", "corporate":["map seeding", "navigation-detail"]...` that can be easily enumerated and applied to the entire map to get a better idea of whats changing and where. (Very similar to [OSMCha](https://osmcha.mapbox.com/)).
 - [ ] Tasking Manager(s) integration. Where has the map been (in)validated? Where have tasks been performed? (Or at least a Service/API that makes visible where this has happened.
 - [ ] A service/API(?) that connects each of these pieces with tools like [ohsome](https://heigit.org/big-spatial-data-analytics-en/ohsome/) or [OSMesa](//github.com/azavea/osmesa) for historical analysis of the map in depth.
 - [ ] A service/API(?) that incorporates more real-time edits into such analysis (OSMCha basically does this).


Much of this is the product of conversations at SOTM 2019, specifically discussions with @joto, @geohacker, @batpad, @kamicut and other folks from Devseed, Mapbox, and Facebook.

Related topics: [LoChas (Logical Changesets)](https://engineering.fb.com/ml-applications/mars/), & atomic changesets. 

[1] A concept I've been pushing in my OSM (historical) analysis work for years, I was re-inspired by David Garcia & Martin Dittus' _Caretography_ talk which included a similar saying. 

![OSM Brain](https://i.imgflip.com/3bz5ok.jpg)
