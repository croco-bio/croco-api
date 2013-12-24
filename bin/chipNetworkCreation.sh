#!/bin/bash 

source setEnvironment.sh
COMPOSITE_NAME="ChIP"


fileList=""
for i in $files; do 
	fileList="$fileList $i"
done

java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	-experimentMappingFiles $HUMAN_EXPERIMENTAL_FILES\
	-taxId 10090   \
	-aggregateKey cell \
	-compositeName $COMPOSITE_NAME/Mouse  \
	-repositoryDir $REPOSITORY_DIR  \
  -chromosomNamePrefix chr \
  -chromosomNameMappings chrM=MT \
	-upstream 5000 -downstream 5000  -maxSize 2000\
	-gtf $HUMAN_GTF_FILE

exit
#Human
fileList=""
for i in $files; do 
	fileList="$fileList $i"
done

java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	 -experimentMappingFiles $MOUSE_EXPERIMENTAL_FILES  \
	 -aggregateKey cell \
	 -taxId 9606 \
	 -compositeName $COMPOSITE_NAME/Human \
	 -repositoryDir $REPOSITORY_DIR \
  -chromosomNameMappings chrM=MT \
    -chromosomNamePrefix chr \
  	-upstream 5000 -downstream 5000 -maxSize 2000\
	 -gtf $HUMAN_GTF_FILE
	

#Fly
java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	-experimentMappingFiles $FLY_EXPERIMENTAL_MAPPING_FILE \
	-taxId 7227   \
	-aggregateKey developmental-stage \
	-compositeName $COMPOSITE_NAME/Fly  \
	-repositoryDir $REPOSITORY_DIR  \
	-chromosomNameMappings M=dmel_mitochondrion_genome \
	-startIndex 3 -endIndex 4 \
	-upstream 500 -downstream 500  -maxSize 2000 \
	-gtf $FLY_GTF_FILE  -type "protein_coding"


#Worm
java -Xmx2G -Xms2G  -cp $CP de.lmu.ifi.bio.crco.processor.CHIP.ChIPExtWriter \
	-experimentMappingFiles $WORM_EXPERIMENTAL_MAPPING_FILE \
	-taxId 6239   \
	-aggregateKey developmental-stage \
	-compositeName $COMPOSITE_NAME/Worm  \
	-repositoryDir $REPOSITORY_DIR  \
	-startIndex 3 -endIndex 4 \
	-upstream 500 -downstream 500  -maxSize 2000 \
	-gtf $WORM_GTF_FILE










