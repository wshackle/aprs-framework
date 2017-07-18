#!/bin/bash

# This script updates the readme.html file from the README.md file.
# It requires the markdown and sed utilities to be installed.
# On Ubuntu based Linux distributions use
#     sudo apt-get install markdown sed
# to install these. (sed is very likely to already be installed.)

markdown README.md  | sed 's#?raw=true##g' | sed 's#img src="/#img src="#g' > readme.html

mkdir -p src/main/javadoc
touch src/main/javadoc/overview.html
echo "<html><body>" > src/main/javadoc/overview.html
cat readme.html >>  src/main/javadoc/overview.html
echo "</body></html>" >> src/main/javadoc/overview.html

# As a convenience we try to launch firefox to view the new html.
firefox src/main/javadoc/overview.html &
netbeans --open-file src/main/javadoc/overview.html



