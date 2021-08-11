#!/bin/bash

set -x;

rm -rf ~/.aprs* ~/.lastAprs* ~/.crcl* ~/.m2/repository/* /tmp/aprs* /tmp/*.xml
mvn clean && mvn -Pjar-with-depends package && mvn exec:java

