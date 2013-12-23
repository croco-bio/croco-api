#!/bin/bash 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
CP="$DIR/target/classes:$DIR/target/dependency/*"

HUMAN_REGIONS="/home/proj/pesch/TFBS/Human/promoter/Homo_sapiens.GRCh37.73.regions"

HUMAN_JASPAR_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_jaspar"
HUMAN_TRANSFAC_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_transfac"
HUMAN_WEI_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_wei2010"
HUMAN_WANG_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_wang2012"
HUMAN_KHERADPOUR_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_Kheradpour"
HUMAN_UNIPROBE_SCAN="/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_uniprobe"

HUMAN_JASPAR_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/JASPAR_CORE_2009_vertebrates.mapping.human"
HUMAN_TRANSFAC_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/matrix_human.mapping"
HUMAN_WEI_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/wei2010_human_mws.mapping"
HUMAN_WANG_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/Wang.mapping"
HUMAN_KHERADPOUR_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/Kheradpour_human.mapping"
HUMAN_UNIPROBE_MAPPING="/home/proj/pesch/TFBS/data/Motif/mapping/uniprobe_human.mapping"

HUMAN_GTF_FILE="$DIR/data/GeneAnnotation/Homo_sapiens.GRCh37.73.gtf"
REPOSITORY_DIR="/home/extproj/pesch/CroCo"
COMPOSITE_NAME="TFBS"
E5=0.00001
E6=0.000001

java -Xmx4G -Xms4G  -cp $CP de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler \
	 -taxId 9606 \
	 -compositeName $COMPOSITE_NAME/Human \
	 -repositoryDir $REPOSITORY_DIR \
   -upstream 5000 -downstream 5000 \
	 -gtf $HUMAN_GTF_FILE \
   -pValueCutOf $E6 \
   -tfbsRegion $HUMAN_REGIONS \
   -tfbsFiles $HUMAN_JASPAR_SCAN \
   -motifMappingFiles $HUMAN_JASPAR_MAPPING -motifSetName Jaspar
