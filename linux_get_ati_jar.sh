
#!/bin/bash

set -x;


mkdir "${HOME}/jars"
cd "${HOME}/jars"

wget https://www.ati-ia.com/Library/software/net_ft/ATINetFT.jar


#export PATH=/home/will/netbeans11/netbeans/java/maven/bin:${PATH}


mvn install:install-file "-Dfile=${HOME}/jars/ATINetFT.jar" -DgroupId=com.ati-ia -DartifactId=ATINetFT -Dversion=1.0 -Dpackaging=jar