#!/bin/bash

source setEnvironment.sh
java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.TFBS.FIMOInputGenerator \
  -gtf $HUMAN_GTF_FILE  \
  -genome $HUMAN_GENOME_DIR  \
  -type $TYPE\
  -downstream 5000 \
  -upstream 5000 \
  -chromosoms $HUMAN_CHROMOSOMS \
  -output $HUMAN_FIMO_OUTPUT
