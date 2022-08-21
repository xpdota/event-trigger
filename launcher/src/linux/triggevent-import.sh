#!/bin/sh

if [ -x jre/bin/java ]
then
  java=jre/bin/java
  echo "Using directory-supplied Java"
else
  java=$(command -v java)
  if [ $? -ne 0 ]
  then
    echo "Found neither a 'jre' dir nor a built in 'java' executable. Please do one of the following to fix it:"
    echo "1. Install Java 17 from your distro's package manager, and make sure the 'java' program on your PATH points to the correct java installation."
    echo "2. Download Java 17 from https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html and place it in a directory called 'jre' (rename it from jdk-x.y.z)"
    echo "3. Run setup-java.sh (requires wget)"
    exit 1;
  fi
fi

$java -cp './userdata:./preload/*:./deps/*:./user/*:launcher-1.0-SNAPSHOT.jar' @args.txt gg.xp.xivsupport.gui.GuiImportLaunch