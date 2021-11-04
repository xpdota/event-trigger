# Fully-Event-Driver XIV Trigger Prototype

### Why?

The current solutions each have advantages and disadvantages.

Triggernometry makes it very easy to whip up quick triggers. However, where it falls flat is making triggers that are
more complicated. You *can* bodge things like loops into it, but it's overall not going to be the best tool for the job
as soon as you hit the "I wish I was just writing code rather that trying to do this in a GUI" point. There are some
other pain points - it is harder to debug than code with a proper debugger+IDE, triggers lack automated testing, and
while you get expression-level validation, you don't get validation for things like variable names.

It is also poor in terms of code re-use. Making a function or framework that other people can use is doable,
but more complicated and hacky than it should be.

Cactbot, with its triggers being written in JS, makes it easier to write complex triggers, and even offers some degree
of state management (most things should not carry over from pull to pull). However, it still doesn't take the next
logical step of abstracting away the actual log lines into objects where everything is already parsed.

Both suffer from the issue that parsing log lines is treated as being *every* trigger's job, when in
reality, it should just be done once and parsed into a convenient object. Cactbot provides some nice functionality
for programmatically creating the regices, but this still doesn't answer the question of why an individual trigger
should be remotely concerned with a regex in the first place.

Then, there's the bespoke ACT plugins, like the Jail plugin. These are, in my opinion, severely lacking in
functionality, and suffer from re-use issues as well. For example, what if I want automarks, *and* a personal callout?
Setting up two separate triggers for that would make it prone to getting the logic or priority inconsistent between the
two, leading to wrong callouts.

### So How Does This Work?

This is intended as a proof of concept for how things *could* work. This is just to show that a better way exists.

The core idea here is to take events, starting with events at as low a level as possible, and emit new events based off
those (hence the name of the 'reevent' module).

For example, in everyone's favorite mechanic, Titan Jails, here is how it might look:

1. ACT log reader sees a log line
   of `21|2021-09-30T19:43:43.1650000-07:00|40016AA2|Titan|2B6C|Rock Throw|10669D22|Some Dude|...`
2. It emits
   a `ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA2|Titan|2B6C|Rock Throw|10669D22|Some Dude|...")`
3. Another event handler will read the ACTLogLineEvent and parse it into a rich object, like:

```
AbilityUsedEvent(
    time = 2021-09-30T19:43:43.1650000-07:00,
    caster = Entity(name=Titan, id=40016AA2),
    ability = Ability(name=Rock Throw, id=2B6C),
    target = Entity(name=Some Dude, id=10669D22)
)
```

4. Then, another event handler, subscribed to `AbilityUsedEvent`, would turn it into a more specific event:

```
TitanJailEvent(player = Entity(name=Some Dude, id=10669D22))
```

5. Finally, yet another event handler would be subscribed to `TitanJailEvent`, and would put this player into a list.
   Nothing would happen yet.
6. Upon receiving two more `TitanJailEvent`s, this handler would then emit one final event:

```
UnsortedTitanJailsSolvedEvent(players = [Entity(name=some dude, ...), Entity(...), Entity(...)])
```
7. However, we still need to sort the list by whatever priority system we want. We would make something to do that, and then
it would emit another event:
```
FinalTitanJailsSolvedEvent(players = [Entity(name=some dude, ...), Entity(...), Entity(...)])
```

9. Both an automarker plugin, and a personal callout plugin could subscribe to the `FinalTitanJailsSolvedEvent`. Perhaps
   others too, such as visual auras.

### Why?

Unlike other solutions, every small piece of this could be unit tested, and the whole solution could be end-to-end
tested. In addition, once you have a working `TitanJailsSolvedEvent`, any additional triggers need not concern
themselves with any of that logic - they merely subscribe to that event and can do whatever they wish with it. In
addition, even the thing that collects the three players who have been jailed need not concern itself with regices at
all - an individual trigger component might look something like this:

```java

@Scope(Scopes.PULL) // One instance of this class per pull - this functionality doesn't exist yet
public class JailCollector implements EventHandler<AbilityUsedEvent> {

   private final List<XivEntity> jailedPlayers = new ArrayList<>();

   @Override
   public void handle(EventContext<Event> context, AbilityUsedEvent event) {
      // Check ability ID - we only care about these two
      int id = event.getAbility().getId();
      if (id != 0x2B6B && id != 0x2B6C) {
         return;
      }
      jailedPlayers.add(event.getTarget());
      // Fire off new event if we have exactly 3 events
      if (jailedPlayers.size() == 3) {
         context.accept(new UnsortedTitanJailsSolvedEvent(new ArrayList<>(jailedPlayers)));
      }
   }
}
```

Take a look at [JailExampleTest](/xivsupport/src/test/java/gg/xp/events/JailExampleTest.java) to see how it all fits together.

The code is very readable and understandable. No regex parsing - that's already handled by the time we get
here. We just use `event.getAbility().getId()` to check if it's one of the ability IDs we care about, and then we
extract the player out of it. The sorting/prioritization, as well as the actual callout/marking, are completely
de-coupled from the collection logic.

You also avoid a lot of nonsense. hex vs decimal conversion only needs to happen once, and then anything past that can
specify IDs in hex or decimal natively. This also sidesteps weird issues of a few hex IDs in log lines being in
lowercase while most are upper,
as well as the ability to abstract away certain details that are useless 99% of the time (e.g. 21 NetworkAbility 
vs 22 NetworkAOEAbility).

Another advantage of abstracting away the log lines is that if log line format or fields change in the future, only a
single update is needed, rather than potentially every trigger needing an update. Or, if SE changes how a particular
ability shows up in the log lines (e.g. headmarker obfuscation), then once again the logic only needs to be updated
in a single place.

### Future Functionality

Due to how the architecture works, debugging would be extremely easy. You'd even have the ability to produce a
visual "tree" of events, showing exactly what event triggered what.

Obviously, there also needs to be a system for actually installing triggers. Java does support hot-swapping of classes
and such, as well as having a good deal of control over class loaders, so it shouldn't be difficult from a language
standpoint. 

Context will also be an important feature. This goes for both filtering based on context (i.e. zone-locked or
job-locked) triggers, and state management (i.e. discarding pull-specific state on a wipe).