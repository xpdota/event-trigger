#!/bin/sh

#        <preCp>./userdata;./preload/*.jar;./deps/*.jar;./user/*.jar;${launcherJar}</preCp>
java -cp './userdata:./preload/*:./deps/*:./user/*:launcher-1.0-SNAPSHOT.jar' @args.txt gg.xp.xivsupport.gui.GuiLaunch