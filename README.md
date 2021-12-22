# Triggevent

Fully event driven trigger + overlay system for FFXIV.

Makes triggers easier to develop and test.

Allows triggers to have custom configuration GUIs and overlays.

For the technical explanation of why I made this, see [Why](https://github.com/xpdota/event-trigger/wiki/Why%3F).

## How to Download/Install

READ THIS: See [Installation](https://github.com/xpdota/event-trigger/wiki/Installation-and-Setup)

Short version: Install Overlayplugin in your ACT, then start OverlayPlugin WSServer from within ACT.

## Features

#### General

Easy access to combtants and events data

![Overview](https://user-images.githubusercontent.com/14287379/142812965-7666c15e-12b3-4b6c-91a6-ed38820a7aa8.png)

Makes it easier to create and test reliable, extensible overlays and triggers

![Jail Plugin](https://user-images.githubusercontent.com/14287379/142813080-c44d1ff7-873b-4119-9c15-1212c9e31133.png)

#### Multi Target DoT Tracker

![Dot Tracker](https://i.imgur.com/72Zof3c.png)

It even combines multiple targets with the same DoT + similar duration into one row.

#### CD Tracker

![CD Tracker](https://i.imgur.com/FgSHZY8.png)

Shows remaining cooldown and active time. 

#### Replay Support

You can import a previous session, or an ACT log. You can even force-on overlays to see what your overlays *would*
have looked like at that point.

To open a replay, simple run triggevent-import.exe instead of triggevent.exe, and then choose a file.

![Replay](https://user-images.githubusercontent.com/14287379/146716550-189ba0ef-5f04-480a-9477-903f82882584.png)

## Future Features

See [Feature Ideas](https://github.com/xpdota/event-trigger/wiki/Feature-Ideas) for what I plan to implement down the
line.