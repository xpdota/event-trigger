#!/bin/sh

if wget -O /tmp/java.tar.gz https://download.oracle.com/java/17/archive/jdk-17.0.4_linux-x64_bin.tar.gz
then
  tar -xvzf /tmp/java.tar.gz
  mv jdk-* jre
else
  echo "Download failed"
fi
