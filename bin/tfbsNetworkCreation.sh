#!/bin/bash 
source setEnvironment.sh

java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	 -taxId 9606 \
	 -compositeName TFBS/Human \
	 -repositoryDir $REPOSITORY_DIR \
   -upstream 5000 -downstream 5000 \
	 -gtf $HUMAN_GTF_FILE \
   -pValueCutOf $E6 \
   -tfbsRegion $HUMAN_REGIONS \
   -tfbsFiles $HUMAN_JASPAR_SCAN \
   -motifMappingFiles $HUMAN_JASPAR_MAPPING -motifSetName Jaspar
