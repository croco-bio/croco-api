#!/bin/bash 

COMPOSITE_NAME="OpenChromTFBS"
source setEnvironment.sh

##Test
#for MOUSE_EXPERIMENT_MAPPING in $MOUSE_DNASE_EXPERIMENTAL_FILES; do
#  EXP=(${MOUSE_EXPERIMENT_MAPPING//=/ })
#  EXP_NAME=${EXP[0]}
#  EXP_FILE=${EXP[1]}
#  java -Xmx10G -Xms10G  -cp $CP de.lmu.ifi.bio.crco.processor.OpenChrom.DNaseTFBSExtWriter \
#	  -motifSetName JASPAR -openChromExpType $EXP_NAME\
#	  -experimentMappingFile $EXP_FILE \
#	  -taxId 10090 \
#    -test \
#	  -tfbsFiles $MOUSE_UNIPROBE_SCAN   \
#	  -tfbsRegion $MOUSE_REGIONS \
#	  -pValueCutOf $E6 \
#	  -motifMappingFiles $MOUSE_UNIPROBE_MAPPING \
#	  -compositeName $COMPOSITE_NAME/Mouse/$EXP_NAME/$E6/JASPAR \
#	  -repositoryDir $REPOSITORY_HOME \
#	  -gtf $MOUSE_GTF_FILE -chromosomNamePrefix chr -upstream 5000 -downstream 5000
#done

for MOUSE_EXPERIMENT_MAPPING in $MOUSE_DNASE_EXPERIMENTAL_FILES; do
  EXP=(${MOUSE_EXPERIMENT_MAPPING//=/ })
  EXP_NAME=${EXP[0]}
  EXP_FILE=${EXP[1]}
  for e in $E5 $E6; do 
	  for i in $MOUSE_JASPAR_SCAN,$MOUSE_JASPAR_MAPPING,"JASPAR" $MOUSE_TRANSFAC_SCAN,$MOUSE_TRANSFAC_MAPPING,"TRANSFAC" $MOUSE_WEI2010_SCAN,$MOUSE_WEI2010_MAPPING,"WEI" $MOUSE_CHEN_SCAN,$MOUSE_CHEN_MAPPING,"CHEN" $MOUSE_UNIPROBE_SCAN,$MOUSE_UNIPROBE_MAPPING,"UNIPROBE"; do 
		  continue
      IFS=",";
		  set $i;
		  SCAN=$1
		  MAPPING=$2;
		  NAME=$3
                  
		  java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.OpenChrom.DNaseTFBSExtWriter \
		   -motifSetName $NAME\
       -openChromExpType $EXP_NAME\
	   	 -experimentMappingFile $EXP_FILE \
		   -taxId 10090 \
		   -tfbsFiles $SCAN   \
	     -tfbsRegion $MOUSE_REGIONS \
		   -pValueCutOf $e \
		   -motifMappingFiles $MAPPING \
		   -compositeName $COMPOSITE_NAME/Mouse/$EXP_NAME/$e/$NAME \
		   -repositoryDir $REPOSITORY_HOME \
       -gtf $MOUSE_GTF_FILE -chromosomNamePrefix chr -upstream 5000 -downstream 5000
	  done
  done
  java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.OpenChrom.DNaseTFBSExtWriter \
  		 -motifSetName All-Motifs \
       -openChromExpType $EXP_NAME \
	   	 -experimentMappingFile $EXP_FILE \
		   -taxId 10090 \
		   -tfbsFiles $MOUSE_JASPAR_SCAN $MOUSE_TRANSFAC_SCAN $MOUSE_CHEN_SCAN $MOUSE_UNIPROBE_SCAN  $MOUSE_WEI2010_SCAN \
	     -tfbsRegion $MOUSE_REGIONS \
		   -pValueCutOf $e \
		   -motifMappingFiles $MOUSE_JASPAR_MAPPING $MOUSE_TRANSFAC_MAPPING $MOUSE_CHEN_MAPPING $MOUSE_UNIPROBE_MAPPING $MOUSE_WEI2010_MAPPING \
		   -compositeName $COMPOSITE_NAME/Mouse/$EXP_NAME/$e/$NAME \
		   -repositoryDir $REPOSITORY_HOME \
       -gtf $MOUSE_GTF_FILE -chromosomNamePrefix chr -upstream 5000 -downstream 5000
done

