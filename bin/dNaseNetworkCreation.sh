#!/bin/bash 

COMPOSITE_NAME="OpenChromTFBS"
source setEnvironment.sh

##Test

java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.OpenChrom.DNaseTFBSExtWriter \
	-motifSetName JASPAR \
	-experimentMappingFile $HUMAN_EXPERIMENT_MAPPING \
	-taxId 9606 \
	-tfbsFiles $HUMAN_JASPAR_SCAN   \
	-tfbsRegion $HUMAN_REGIONS \
	-pValueCutOf $E6\
	-motifMappingFiles $HUMAN_JASPAR_MAPPING \
	-compositeName $COMPOSITE_NAME/Human/$E6/JASPAR \
	-repositoryDir $REPOSITORY_HOME \
	-gtf $HUMAN_GTF_FILE \
	-chromosomNamePrefix chr
##Remove
exit
for e in $E5 $E6; do 
	for i in $MOUSE_JASPAR_SCAN,$MOUSE_JASPAR_MAPPING,"JASPAR" $MOUSE_TRANSFAC_SCAN,$MOUSE_TRANSFAC_MAPPING,"TRANSFAC" $MOUSE_WEI2010_SCAN,$MOUSE_WEI2010_MAPPING,"WEI" $MOUSE_CHEN_SCAN,$MOUSE_CHEN_MAPPING,"CHEN" $MOUSE_UNIPROBE_SCAN,$MOUSE_UNIPROBE_MAPPING,"UNIPROBE"; do 
		IFS=",";
		set $i;
		SCAN=$1
		MAPPING=$2;
		NAME=$3
                
		java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.OpenChrom.DNaseTFBSExtWriter \
		 -motifSetName $NAME \
	 	 -experimentMappingFile $MOUSE_EXPERIMENT_MAPPING \
		 -taxId 10090 \
		 -tfbsFiles $SCAN   \
	         -tfbsRegion $MOUSE_REGIONS \
		 -pValueCutOf $e \
		 -motifMappingFiles $MAPPING \
		 -compositeName $COMPOSITE_NAME/Mouse/$e/$NAME \
		 -repositoryDir $REPOSITORY_HOME
	done
done


for e in $E5 $E6; do 
	for i in $HUMAN_JASPAR_SCAN,$HUMAN_JASPAR_MAPPING,"JASPAR" $HUMAN_TRANSFAC_SCAN,$HUMAN_TRANSFAC_MAPPING,"TRANSFAC" $HUMAN_WEI_SCAN,$HUMAN_WEI_MAPPING,"WEI" $HUMAN_WANG_SCAN,$HUMAN_WANG_MAPPING,"WANG" $HUMAN_UNIPROBE_SCAN,$HUMAN_UNIPROBE_MAPPING,"UNIPROBE"; do 
		IFS=",";
		set $i;
		SCAN=$1
		MAPPING=$2;
		NAME=$3
                
		java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.OpenChrom.DNaseTFBSExtWriter \
		 -motifSetName $NAME \
	 	 -experimentMappingFile $HUMAN_EXPERIMENT_MAPPING \
		 -taxId 9606 \
		 -tfbsFiles $SCAN   \
	         -tfbsRegion $HUMAN_REGIONS \
		 -pValueCutOf $e \
		 -motifMappingFiles $MAPPING \
		 -compositeName $COMPOSITE_NAME/Human/$e/$NAME/ \
		 -repositoryDir $REPOSITORY_HOME
	done
done

##Combined import 
#E6
java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -motifSetName All-Motifs  -experimentMappingFile $HUMAN_EXPERIMENT_MAPPING -taxId 9606   -tfbsFiles $HUMAN_JASPAR_SCAN $HUMAN_TRANSFAC_SCAN $HUMAN_WEI_SCAN  $HUMAN_UNIPROBE_SCAN  $HUMAN_WANG_SCAN    -tfbsRegion $HUMAN_REGIONS -pValueCutOf $E6 -motifMappingFiles $HUMAN_JASPAR_MAPPING_ONLY $HUMAN_TRANSFAC_MAPPING $HUMAN_WEI_MAPPING  $HUMAN_UNIPROBE_MAPPING $HUMAN_WANG_MAPPING    -compositeName $COMPOSITE_NAME/Human/$E6/All -repositoryDir $REPOSITORY_DIR
java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -motifSetName All-Motifs  -experimentMappingFile $MOUSE_EXPERIMENT_MAPPING -taxId 10090  -tfbsFiles $MOUSE_JASPAR_SCAN $MOUSE_TRANSFAC_SCAN $MOUSE_CHEN_SCAN $MOUSE_UNIPROBE_SCAN  $MOUSE_WEI2010_SCAN -tfbsRegion $MOUSE_REGIONS -pValueCutOf $E6 -motifMappingFiles $MOUSE_JASPAR_MAPPING_ONLY $MOUSE_TRANSFAC_MAPPING $MOUSE_CHEN_MAPPING $MOUSE_UNIPROBE_MAPPING $MOUSE_WEI2010_MAPPING -compositeName $COMPOSITE_NAME/Mouse/$E6/All -repositoryDir $REPOSITORY_DIR
#E5
java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -motifSetName All-Motifs  -experimentMappingFile $HUMAN_EXPERIMENT_MAPPING -taxId 9606   -tfbsFiles $HUMAN_JASPAR_SCAN $HUMAN_TRANSFAC_SCAN $HUMAN_WEI_SCAN  $HUMAN_UNIPROBE_SCAN  $HUMAN_WANG_SCAN    -tfbsRegion $HUMAN_REGIONS -pValueCutOf $E5 -motifMappingFiles $HUMAN_JASPAR_MAPPING_ONLY $HUMAN_TRANSFAC_MAPPING $HUMAN_WEI_MAPPING  $HUMAN_UNIPROBE_MAPPING $HUMAN_WANG_MAPPING    -compositeName $COMPOSITE_NAME/Human/$E5/All -repositoryDir $REPOSITORY_DIR
java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -motifSetName All-Motifs  -experimentMappingFile $MOUSE_EXPERIMENT_MAPPING -taxId 10090  -tfbsFiles $MOUSE_JASPAR_SCAN $MOUSE_TRANSFAC_SCAN $MOUSE_CHEN_SCAN $MOUSE_UNIPROBE_SCAN  $MOUSE_WEI2010_SCAN -tfbsRegion $MOUSE_REGIONS -pValueCutOf $E5 -motifMappingFiles $MOUSE_JASPAR_MAPPING_ONLY $MOUSE_TRANSFAC_MAPPING $MOUSE_CHEN_MAPPING $MOUSE_UNIPROBE_MAPPING $MOUSE_WEI2010_MAPPING -compositeName $COMPOSITE_NAME/Mouse/$E5/All -repositoryDir $REPOSITORY_DIR

#Only jaspar
#E6
#java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -config $CONFIG -motifSetName Jaspar -experimentMappingFile $HUMAN_EXPERIMENT_MAPPING -taxId 9606   -tfbsFiles $HUMAN_JASPAR_SCAN  -tfbsRegion $HUMAN_REGIONS -pValueCutOf $E6 -motifMappingFiles $HUMAN_JASPAR_MAPPING -compositeName $COMPOSITE_NAME/HumanJaspar-Human-$E6 -repositoryDir $REPOSITORY_DIR
#java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -config $CONFIG -motifSetName Jaspar -experimentMappingFile $MOUSE_EXPERIMENT_MAPPING -taxId 10090  -tfbsFiles $MOUSE_JASPAR_SCAN  -tfbsRegion $MOUSE_REGIONS -pValueCutOf $E6 -motifMappingFiles $MOUSE_JASPAR_MAPPING -compositeName $COMPOSITE_NAME/Jaspar-Mouse-$E6 -repositoryDir $REPOSITORY_DIR
#E5
#java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -config $CONFIG -motifSetName Jaspar -experimentMappingFile $HUMAN_EXPERIMENT_MAPPING -taxId 9606   -tfbsFiles $HUMAN_JASPAR_SCAN  -tfbsRegion $HUMAN_REGIONS -pValueCutOf $E5 -motifMappingFiles $HUMAN_JASPAR_MAPPING -compositeName $COMPOSITE_NAME/Jaspar-Human-$E5 -repositoryDir $REPOSITORY_DIR
#java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter -config $CONFIG -motifSetName Jaspar -experimentMappingFile $MOUSE_EXPERIMENT_MAPPING -taxId 10090  -tfbsFiles $MOUSE_JASPAR_SCAN  -tfbsRegion $MOUSE_REGIONS -pValueCutOf $E5 -motifMappingFiles $MOUSE_JASPAR_MAPPING -compositeName $COMPOSITE_NAME/Jaspar-Mouse-$E5 -repositoryDir $REPOSITORY_DIR

