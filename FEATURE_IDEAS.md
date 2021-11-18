### Quick Trigger Maker

Simple UI for making a trigger based on simple event types and properties. 

Triggers based on log line regices (i.e. similar to vanilla ACT triggers) should be trivial to implement, though
it would only support network lines.


### Map/Replay Support

This would be memory-intensive (or disk-intensive), but I could probably get something like the FFLogs replay
function working. Since we capture combatant information once per second by default, we should be able to get a
reasonably accurate replay. The problem with the FFLogs replay is that it only works during action, so mechanics
like Wormhole tend to just not work very well.


### Import/Export

Similar to above, there should be a way to export and replay data. I'm thinking the way it could work is that when
importing, a separate instance of the application could open, with the imported data being the only event source.
Then, the "replay" function would allow you to step through and see the state at that point.

However, I'm not sure how the details would work. A simple front-to-back replay would be easy, but making it reversible
and seekable is the challenge. I'd probably have to compute intermediate states (either after every event, or timed), 
which would likely take an absurd amount of memory.


### Pulls View

I'd like to make a view that shows all pulls and such, similar to what you'd see on FFLogs.


### UI Icons

All of the tables and such could have icons, bars, etc. So for example, the combatants view could have job icons and
proper HP/MP bars. The events view could show icons for abilities/buffs and job icons. When I get around to making a
"pulls" view, we could have icons for bosses and stuff. Care needs to be taken for performance, but proper table
renderers should be more than fast enough without too much CPU load, especially since the really spammy events don't
tend to have icons anyway.

"Jump To" icons might be harder (and would have a more noticeable performance impact). Maybe a right click menu? 
Speaking of which, I need a normal right click menu so you can copy things from tables.

### In-Game Overlay

Not sure what the best implementation would be. Could either go for web-based (i.e. someone else would be doing the
actual work) or some kind of transparent window.