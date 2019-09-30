# osm-interactions

More an idea at this point than an implementable application / project, I want to document the following observations and put a plan in place to move forward.

### Objective
OSM is a _community of communities_. As such we can no longer look at the map as a single object, but to understand how the map has and will continue to evolve, we need to be able to tell the complete story behind the map. This is certainly not a new problem or realization among those in the community, but the feasibility of analyzing the map in this way has only more recently become possible through technical innovations. All the while, the community that supports the map has grown immensely and we're overdue for new tools that can reflect this.

OSM is not a map or a spatial database, but a record of interactions between contributors and the map (database), each of which is a discrete _editing_ action (though is not always an edit: consider TM validation).

These discrete editing actions are then `osm-interactions`. These _interactions_ have the following properties: 

##### 1. Happen at the object level (Though are not always observable)
  - Consider the objects that a validator or secondary editor _did not edit_ when performing Q/A updates to nearby objects. This is implicit validation. While their user is not present in the history of these objects, they have implicitly validated the quality (accuracy, temporality, etc.)
  - Otherwise, an interaction is synonymous with a new version (or _minor version_) of an object.

##### 2. May or may not involve another user
  - Creating new objects on the map where other data does not currently exist is an interaction between a user and the map.
  - Editing an existing object is aa form of interaction with the other contributors who also edited that object.
  - Stretching it: The presence of another user's map interaction was the impetus of a new interaction? New objects were added during validation or Q/A that the original mapper missed. 
  
##### 3. Always has additional context
  - HOT task? Organized editing effort? Import? Local editor with passion for mapping object type `x`. Another obvious attribute of all edits in OSM, the context surrounding an edit is often hidden or excluded in analysis. Changeset comments are of critical importance to most forms of analysis, but are difficult to involve at scale.
  
... This list will contintue to grow?



### Requirements (A brainstorm)
 - [ ] A changeset (lookup) database that is annotated per changeset ID with additional context. "HOT:True/False", "CORP:True/False", "IMPORT:True/False", "BOT:True/False", "NEW_USER:True/False"








This idea came out of conversations at SOTM 2019, specifically discussions with folks at DevSeed, Mapbox, and Facebook.

#### Longterm Vision

![OSM Brain](https://i.imgflip.com/3bz5ok.jpg)
