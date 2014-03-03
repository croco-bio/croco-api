#!/bin/bash 

if [ "$#" -ne 1 ]
then
  echo "Usage: <HUMAN|MOUSE|WORM|FLY>"
  exit 1
fi

OPTION=$1

source setEnvironment.sh
COMPOSITE_NAME="ChIP"

if [ $OPTION == "MOUSE" ]; then
  java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	  -experimentMappingFiles $MOUSE_CHIP_EXPERIMENTAL_FILES\
	  -taxId 10090   \
	  -aggregateKey cell \
	  -compositeName $COMPOSITE_NAME/Mouse  \
	  -repositoryDir $REPOSITORY_HOME  \
    -chromosomNamePrefix chr \
    -chromosomNameMappings chrM=MT \
	  -upstream 5000 -downstream 5000  -maxSize 2000\
	  -gtf $MOUSE_GTF_FILE
elif [ $OPTION == "HUMAN" ]; then
  java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	   -experimentMappingFiles $HUMAN_CHIP_EXPERIMENTAL_FILES  \
	   -aggregateKey cell \
	   -taxId 9606 \
	   -compositeName $COMPOSITE_NAME/Human \
	   -repositoryDir $REPOSITORY_HOME \
    -chromosomNameMappings chrM=MT \
      -chromosomNamePrefix chr \
    	-upstream 5000 -downstream 5000 -maxSize 2000\
	   -gtf $HUMAN_GTF_FILE
elif [ $OPTION == "FLY" ]; then
  java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	  -taxId 7227   \
	  -experimentMappingFiles $FLY_CHIP_EXPERIMENTAL_FILE  \
	  -aggregateKey developmental-stage \
	  -compositeName $COMPOSITE_NAME/Fly  \
	  -repositoryDir $REPOSITORY_HOME  \
	  -chromosomNameMappings M=dmel_mitochondrion_genome \
	  -startIndex 3 -endIndex 4 \
	  -upstream 500 -downstream 500  -maxSize 2000 \
	  -gtf $FLY_GTF_FILE 
elif [ $OPTION == "WORM" ]; then
  java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	  -experimentMappingFiles $WORM_CHIP_EXPERIMENTAL_FILE \
	  -taxId 6239   \
	  -aggregateKey developmental-stage \
	  -compositeName $COMPOSITE_NAME/Worm  \
	  -repositoryDir $REPOSITORY_HOME  \
	  -startIndex 3 -endIndex 4 \
	  -upstream 500 -downstream 500  -maxSize 2000 \
	  -gtf $WORM_GTF_FILE
fi








