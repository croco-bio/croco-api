#!/bin/bash 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
CP="$DIR/target/classes:$DIR/target/dependency/*"

java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.util.NetworkRenderedHierachyProcessor \
  -repositoryDir /home/extproj/pesch/CroCo \
  -outputFormat png \
  -width 500 \
  -height 500
