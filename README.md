# Triggevent

Triggevent is a comprehensive FFXIV addon that provides triggers, overlays, log analysis, and more. Triggers can even
have custom configuration GUIs and overlays.

Want to have a multi target DoT tracker without going through all the manual setup pain of Hojoring/Special Spell
Timer (SpeSpe)?

Maybe you just want a Titan Gaol plugin that actually works?

Want to make triggers without dealing with regex, which was never the right tool for the job in the first place?

You've come to the right place!

Even if you don't plan to actually use it, its log analysis and replay functions can be invaluable for making other
addons and triggers.

Most documentation is in the app, or on the [Wiki](https://github.com/xpdota/event-trigger/wiki/).

For the technical explanation of why I made this,
see [Why](https://github.com/xpdota/event-trigger/wiki/Why-I-Made-This).

## How to Download/Install

READ THIS: See [Installation](https://github.com/xpdota/event-trigger/wiki/Installation-and-Setup)

Short version: Install Overlayplugin in your ACT, then start OverlayPlugin WSServer from within ACT.

## Features

### Overlays

There are several overlays built in. Perhaps the most interesting is a multi-target DoT tracker, since nothing else
seems to have good multi-target support. It even has tick display built-in if you want to optimize:

![image](https://user-images.githubusercontent.com/14287379/158668694-38697d1f-3e3a-4afb-8b68-bb8c9ff531fb.png)

### Cooldown Tracker

![CD Tracker](https://i.imgur.com/FgSHZY8.png)

Shows remaining cooldown and active time. You can have two separate overlays - one for party, one for personal.

### Jail Plugin

Triggevent has by far the most powerful Titan Jail plugin. You choose a priority by dragging and dropping jobs - no
typing names. It supports both automark and personal callouts, so you don't have to configure two plugins separately.

![Jail Plugin](https://user-images.githubusercontent.com/14287379/142813080-c44d1ff7-873b-4119-9c15-1212c9e31133.png)

### Triggers

Triggers can be written as code (like Cactbot) or with a simple point-and-click interface. Both options let you write
clear conditions, rather than stuffing everything into a hard-to-understand regex.

For example, consider this small bit of code here:

![image](https://user-images.githubusercontent.com/14287379/158666921-7bcc2ee2-f80c-44c3-9750-7e09c0f2b8f2.png)

The conditions are all easier to read than a regex - it triggers on a buff application, with an ID of 0xACA, where the
target is the player, and it is not a buff refresh. Then, we want to switch our callout based on the number of stacks of
the buff.

On top of that, it puts configurable callouts on the UI: will result in you having four easily configurable callouts,
which support both on-screen text and TTS:

![image](https://user-images.githubusercontent.com/14287379/158667132-b2d816a9-34c7-414a-9079-93dab4703760.png)

You can also make triggers using simple conditions - no regex required! No more worrying about future patches breaking
regexes, since everything is parsed for you:

![image](https://user-images.githubusercontent.com/14287379/158667347-286d6afa-3392-4526-a8ff-0fe7a7879144.png)

In addition, any trigger attached to an event with a duration (buffs, castbars) can easily display a countdown until the
event takes place, if you have the on-screen callout overlay:

![image](https://user-images.githubusercontent.com/14287379/158667758-97a47fec-5ff6-476d-a511-b868b43086f3.png)

Callouts can be further customized, as they support Groovy expressions:

![image](https://user-images.githubusercontent.com/14287379/158668357-6ad34ac9-42ec-4cd0-8177-80275ef9ebc0.png)

### Analysis

Easy access to combatants and events data

![Overview](https://user-images.githubusercontent.com/14287379/142812965-7666c15e-12b3-4b6c-91a6-ed38820a7aa8.png)

Makes it easier to create and test reliable, extensible overlays and triggers. Even if you're developing triggers for a
different plugin like Cactbot or Triggernometry.

### Replay Support

You can import a previous session, or an ACT log. You can even force-on overlays to see what your overlays *would*
have looked like at that point. FFLogs is also supported to some degree. This is a great way to rapidly test new
triggers - if fflogs exposes enough data, you don't even *need to have the log file*!

To open a replay, simple run triggevent-import.exe instead of triggevent.exe, and then choose a file or FFLogs URL. Note
that FFLogs support requires you to supply your own API key - I don't have a way of securely distributing a key.

![image](https://user-images.githubusercontent.com/14287379/158670751-3252a0d1-854b-47a7-8a47-2a74a2cf30bd.png)

## Future Features

See [Feature Ideas](https://github.com/xpdota/event-trigger/wiki/Feature-Ideas) for what I plan to implement down the
line.
