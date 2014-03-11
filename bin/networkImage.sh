#!/bin/bash 

source setEnvironment.sh

java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.processor.hierachy.NetworkRenderedHierachyProcessor \
  -repositoryDir /home/extproj/pesch/CroCo/Database/ \
  -outputFormat png \
  -width 500 \
  -height 500
