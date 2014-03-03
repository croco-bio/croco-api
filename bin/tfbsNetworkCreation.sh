#!/bin/bash 
source setEnvironment.sh

if [ "$#" -ne 1 ]
then
  echo "Usage: <HUMAN|MOUSE>"
  exit 1
fi


COMPOSITE_NAME="TFBS"
source setEnvironment.sh
COUNTER=0

OPTION=$1
if [ $OPTION == "HUMAN" ]; then
    for e in $E5 $E6; do 
	    for i in $HUMAN_JASPAR_SCAN,$HUMAN_JASPAR_MAPPING,"JASPAR" $HUMAN_TRANSFAC_SCAN,$HUMAN_TRANSFAC_MAPPING,"TRANSFAC" $HUMAN_WEI_SCAN,$HUMAN_WEI_MAPPING,"WEI" $HUMAN_WANG_SCAN,$HUMAN_WANG_MAPPING,"WANG" $HUMAN_UNIPROBE_SCAN,$HUMAN_UNIPROBE_MAPPING,"UNIPROBE"; do 
        IFS=",";
		    set $i;
		    SCAN=$1
		    MAPPING=$2;
		    NAME=$3
        
        java -Xmx10G -Xms10G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	         -taxId 9606 \
	         -compositeName $COMPOSITE_NAME/Human \
	         -repositoryDir $REPOSITORY_HOME \
           -upstream 5000 -downstream 5000 \
	         -gtf $HUMAN_GTF_FILE \
           -pValueCutOf $e \
           -tfbsRegion $HUMAN_REGIONS \
           -tfbsFiles $SCAN \
           -motifMappingFiles $MAPPING -motifSetName $NAME            
     
      done
      NAME="All-Motifs"
        java -Xmx10G -Xms10G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	         -taxId 9606 \
	         -compositeName $COMPOSITE_NAME/Human \
	         -repositoryDir $REPOSITORY_HOME \
           -upstream 5000 -downstream 5000 \
	         -gtf $HUMAN_GTF_FILE \
           -pValueCutOf $e \
           -tfbsRegion $HUMAN_REGIONS \
           -tfbsFiles $HUMAN_JASPAR_SCAN $HUMAN_TRANSFAC_SCAN $HUMAN_WEI_SCAN  $HUMAN_UNIPROBE_SCAN  $HUMAN_WANG_SCAN \
           -motifMappingFiles $HUMAN_JASPAR_MAPPING $HUMAN_TRANSFAC_MAPPING $HUMAN_WEI_MAPPING  $HUMAN_UNIPROBE_MAPPING $HUMAN_WANG_MAPPING -motifSetName $NAME   
    done
elif [ $OPTION == "MOUSE" ]; then
    for e in $E5 $E6; do 
	    for i in $MOUSE_JASPAR_SCAN,$MOUSE_JASPAR_MAPPING,"JASPAR" $MOUSE_TRANSFAC_SCAN,$MOUSE_TRANSFAC_MAPPING,"TRANSFAC" $MOUSE_WEI2010_SCAN,$MOUSE_WEI2010_MAPPING,"WEI" $MOUSE_CHEN_SCAN,$MOUSE_CHEN_MAPPING,"CHEN" $MOUSE_UNIPROBE_SCAN,$MOUSE_UNIPROBE_MAPPING,"UNIPROBE"; do 
        IFS=",";
		    set $i;
		    SCAN=$1
		    MAPPING=$2;
		    NAME=$3
                    
        java -Xmx10G -Xms10G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	         -taxId 10090 \
	         -compositeName $COMPOSITE_NAME/Mouse \
	         -repositoryDir $REPOSITORY_HOME \
           -upstream 5000 -downstream 5000 \
	         -gtf $MOUSE_GTF_FILE \
           -pValueCutOf $e \
           -tfbsRegion $MOUSE_REGIONS \
           -tfbsFiles $SCAN \
           -motifMappingFiles $MAPPING -motifSetName $NAME    
      done
      NAME="All-Motifs"
        java -Xmx10G -Xms10G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	         -taxId 10090 \
	         -compositeName $COMPOSITE_NAME/Mouse \
	         -repositoryDir $REPOSITORY_HOME \
           -upstream 5000 -downstream 5000 \
	         -gtf $MOUSE_GTF_FILE \
           -pValueCutOf $e \
           -tfbsRegion $MOUSE_REGIONS \
           -tfbsFiles $MOUSE_JASPAR_SCAN $MOUSE_TRANSFAC_SCAN $MOUSE_CHEN_SCAN $MOUSE_UNIPROBE_SCAN $MOUSE_WEI2010_SCAN \
           -motifMappingFiles $MOUSE_JASPAR_MAPPING $MOUSE_TRANSFAC_MAPPING $MOUSE_CHEN_MAPPING $MOUSE_UNIPROBE_MAPPING $MOUSE_WEI2010_MAPPING -motifSetName $NAME    
    done
elif [ $OPTION == "WORM" ]; then
    for e in $E5 $E6; do 
	    for i in $WORM_JASPAR_SCAN,$WORM_JASPAR_MAPPING,"JASPAR" $WORM_TRANSFAC_SCAN,$WORM_TRANSFAC_MAPPING,"TRANSFAC" $WORM_UNIPROBE_SCAN,$WORM_UNIPROBE_MAPPING,"UNIPROBE"; do 
        IFS=",";
		    set $i;
		    SCAN=$1
		    MAPPING=$2;
		    NAME=$3
                    
        java -Xmx10G -Xms10G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	         -taxId 6239 \
	         -compositeName $COMPOSITE_NAME/Worm \
	         -repositoryDir $REPOSITORY_HOME \
           -upstream 500 -downstream 500 \
	         -gtf $WORM_GTF_FILE \
           -pValueCutOf $e \
           -tfbsRegion $WORM_REGIONS \
           -tfbsFiles $SCAN \
           -motifMappingFiles $MAPPING -motifSetName $NAME    
      done
      NAME="All-Motifs"
        java -Xmx10G -Xms10G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	         -taxId 6239 \
	         -compositeName $COMPOSITE_NAME/Worm \
	         -repositoryDir $REPOSITORY_HOME \
           -upstream 500 -downstream 500 \
	         -gtf $WORM_GTF_FILE \
           -pValueCutOf $e \
           -tfbsRegion $WORM_REGIONS \
           -tfbsFiles $WORM_JASPAR_SCAN $WORM_TRANSFAC_SCAN $WORM_UNIPROBE_SCAN  \
           -motifMappingFiles $WORM_JASPAR_MAPPING $WORM_TRANSFAC_MAPPING $WORM_UNIPROBE_MAPPING  -motifSetName $NAME    
    done
else
 echo "Unkown parameter $OPTION"
fi


