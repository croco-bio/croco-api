#!/bin/bash

if [ "$#" -ne 1 ]
then
  echo "Usage: <HUMAN|MOUSE|WORM>"
  exit 1
fi

source setEnvironment.sh

OPTION=$1
if [ $OPTION == "HUMAN" ]; then
  java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.TFBS.FIMOInputGenerator \
    -gtf $HUMAN_GTF_FILE  \
    -genome $HUMAN_GENOME_DIR  \
    -type "protein_coding"\
    -downstream 5000 \
    -upstream 5000 \
    -chromosoms $HUMAN_CHROMOSOMS \
    -output $HUMAN_FIMO_OUTPUT
elif [ $OPTION == "MOUSE" ]; then
  java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.TFBS.FIMOInputGenerator \
    -gtf $MOUSE_GTF_FILE  \
    -genome $MOUSE_GENOME_DIR  \
    -type "protein_coding"\
    -downstream 5000 \
    -upstream 5000 \
    -chromosoms $MOUSE_CHROMOSOMS \
    -output $MOUSE_FIMO_OUTPUT
elif [ $OPTION == "WORM" ]; then
  java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.TFBS.FIMOInputGenerator \
    -gtf $WORM_GTF_FILE  \
    -genome $WORM_GENOME_DIR  \
    -type "protein_coding"\
    -chromosoms $WORM_CHROMOSOMS \
    -downstream 500 \
    -upstream 500 \
    -chromosoms $WORM_CHROMOSOMS \
    -output $WORM_FIMO_OUTPUT
fi
