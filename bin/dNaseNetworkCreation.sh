#!/bin/bash 
CP="/home/users/pesch/workspace/croco-api/target/classes/:/home/users/pesch/workspace/croco-api/target/dependency/*:/home/users/pesch/workspace/RegulatoryDatabase/class/:/home/users/pesch/workspace/RegulatoryDatabase/lib/*:/home/users/pesch/workspace/SynonymGraph/class/:/home/users/pesch/workspace/NetworkTransfer/class/:/home/users/pesch/workspace/SynonymGraph/lib/*:/home/users/pesch/workspace/NetworkTransfer/lib/*"

HUMAN_REGIONS="/home/proj/pesch/TFBS/Human/promoter/humanRegions"
MOUSE_REGIONS="/home/proj/pesch/TFBS/Mouse/promoter/mouseRegions"

HUMAN_JASPAR_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_jaspar"
HUMAN_TRANSFAC_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_transfac"
HUMAN_WEI_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_wei2010"
HUMAN_WANG_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_wang2012"
HUMAN_KHERADPOUR_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_Kheradpour"
HUMAN_UNIPROBE_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_uniprobe"

HUMAN_JASPAR_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/JASPAR_CORE_2009_vertebrates.mapping.human"
#HUMAN_JASPAR_MAPPING_ONLY="/home/proj/pesch/TFBS/data/Motif/mapping/JASPAR_CORE_2009_vertebrates.mapping.only_human"
HUMAN_TRANSFAC_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/matrix_human.mapping"
HUMAN_WEI_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/wei2010_human_mws.mapping"
HUMAN_WANG_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/Wang.mapping"
HUMAN_KHERADPOUR_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/Kheradpour_human.mapping"
HUMAN_UNIPROBE_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/uniprobe_human.mapping"

E5=0.00001
E6=0.000001

MOUSE_JASPAR_SCAN="/home/proj/pesch/TFBS/Mouse/scan/mouse_10K_promoter_jaspar"

MOUSE_TRANSFAC_SCAN="/home/proj/pesch/TFBS/Mouse/scan/mouse_10K_promoter_transfac"
MOUSE_CHEN_SCAN="/home/proj/pesch/TFBS/Mouse/scan/mouse_10K_promoter_chen2008"
MOUSE_UNIPROBE_SCAN="/home/proj/pesch/TFBS/Mouse/scan/mouse_10K_promoter_uniprobe"
MOUSE_WEI2010_SCAN="/home/proj/pesch/TFBS/Mouse/scan/mouse_10K_promoter_wei2010"

MOUSE_JASPAR_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/JASPAR_CORE_2009_vertebrates.mapping.mouse"
#MOUSE_JASPAR_MAPPING_ONLY="/home/proj/pesch/TFBS/data/Motif/mapping/JASPAR_CORE_2009_vertebrates.mapping.only_mouse"
MOUSE_TRANSFAC_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/matrix_mouse.mapping"
MOUSE_CHEN_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/chen2008.mapping"
MOUSE_UNIPROBE_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/uniprobe_mouse.mapping"
MOUSE_WEI2010_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/wei2010_mouse_mws.mapping"

HUMAN_EXPERIMENT_MAPPING="/home/users/pesch/Databases/ENCODE/DNASE-PEAKS/Human/wgEncodeUwDnas/data.unmodified"
MOUSE_EXPERIMENT_MAPPING="/home/users/pesch/Databases/ENCODE/DNASE-PEAKS/Mouse/wgEncodeUwDnas/data.unmodified"

REPOSITORY_DIR="/home/extproj/pesch/CroCo/"
COMPOSITE_NAME="OpenChromTFBS"

for e in $E5 $E6; do 
	for i in $MOUSE_JASPAR_SCAN,$MOUSE_JASPAR_MAPPING,"JASPAR" $MOUSE_TRANSFAC_SCAN,$MOUSE_TRANSFAC_MAPPING,"TRANSFAC" $MOUSE_WEI2010_SCAN,$MOUSE_WEI2010_MAPPING,"WEI" $MOUSE_CHEN_SCAN,$MOUSE_CHEN_MAPPING,"CHEN" $MOUSE_UNIPROBE_SCAN,$MOUSE_UNIPROBE_MAPPING,"UNIPROBE"; do 
		IFS=",";
		set $i;
		SCAN=$1
		MAPPING=$2;
		NAME=$3
                
		java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter \
		 -motifSetName $NAME \
	 	 -experimentMappingFile $MOUSE_EXPERIMENT_MAPPING \
		 -taxId 10090 \
		 -tfbsFiles $SCAN   \
	         -tfbsRegion $MOUSE_REGIONS \
		 -pValueCutOf $e \
		 -motifMappingFiles $MAPPING \
		 -compositeName $COMPOSITE_NAME/Mouse/$e/$NAME \
		 -repositoryDir $REPOSITORY_DIR
	done
done


for e in $E5 $E6; do 
	for i in $HUMAN_JASPAR_SCAN,$HUMAN_JASPAR_MAPPING,"JASPAR" $HUMAN_TRANSFAC_SCAN,$HUMAN_TRANSFAC_MAPPING,"TRANSFAC" $HUMAN_WEI_SCAN,$HUMAN_WEI_MAPPING,"WEI" $HUMAN_WANG_SCAN,$HUMAN_WANG_MAPPING,"WANG" $HUMAN_UNIPROBE_SCAN,$HUMAN_UNIPROBE_MAPPING,"UNIPROBE"; do 
		IFS=",";
		set $i;
		SCAN=$1
		MAPPING=$2;
		NAME=$3
                
		java -Xmx4G -Xms4G  -cp $CP network.imp.DNaseTFBSExtWriter \
		 -motifSetName $NAME \
	 	 -experimentMappingFile $HUMAN_EXPERIMENT_MAPPING \
		 -taxId 9606 \
		 -tfbsFiles $SCAN   \
	         -tfbsRegion $HUMAN_REGIONS \
		 -pValueCutOf $e \
		 -motifMappingFiles $MAPPING \
		 -compositeName $COMPOSITE_NAME/Human/$e/$NAME/ \
		 -repositoryDir $REPOSITORY_DIR
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

