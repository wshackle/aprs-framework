#!/bin/bash

set -x;


cd /home/shackle/aprs-framework; JAVA_HOME=/home/shackle/adoptopenjdk/jdk-11.0.2+7 /home/shackle/netbeans-11.0/java/maven/bin/mvn -X -Dorg.slf4j.simpleLogger.defaultLogLevel=all -Pcheckerframework_jdk11 clean install
