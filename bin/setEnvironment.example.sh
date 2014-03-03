#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
CP="$DIR/target/classes:$DIR/target/dependency/*"


##Human##

#Annotation
HUMAN_GTF_FILE="$DIR/data/GeneAnnotation/Homo_sapiens.GRCh37.73.gtf"
HUMAN_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/human.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/human.ensembl.entrezgene.txt  HGNC=$DIR/data/GeneAnnotation/human.ensembl.hgnc_symbol.txt SWISSPROT=$DIR/data/GeneAnnotation/human.ensembl.uniprot_swissprot.txt"
HUMAN_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/human.ensembl.description.txt
HUMAN_GENE_NAME_FILE=$DIR/data/GeneAnnotation/human.ensembl.geneId.txt
HUMAN_GENOME_DIR="/home/proj/biosoft/GENOMIC/HUMAN/HUMAN_GENOME_FASTA"
HUMAN_CHROMOSOMS="1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y"


#TBFS predictions
HUMAN_TFBS_SCAN_DIR="/Volumes/MyPassport/TFBS/Human"
HUMAN_REGIONS="$HUMAN_TFBS_SCAN_DIR/promoter/humanRegions"
HUMAN_FIMO_OUTPUT="$HUMAN_TFBS_SCAN_DIR/promoter/Homo_sapiens.GRCh37.73.promoter"
HUMAN_REGIONS="$HUMAN_TFBS_SCAN_DIR/promoter/Homo_sapiens.GRCh37.73.regions"
HUMAN_JASPAR_SCAN="$HUMAN_TFBS_SCAN_DIR/scan/human_10K_promoter_jaspar"
HUMAN_TRANSFAC_SCAN="$HUMAN_TFBS_SCAN_DIR/scan/human_10K_promoter_transfac"
HUMAN_WEI_SCAN="$HUMAN_TFBS_SCAN_DIR/scan/human_10K_promoter_wei2010"
HUMAN_WANG_SCAN="$HUMAN_TFBS_SCAN_DIR/scan/human_10K_promoter_wang2012"
HUMAN_KHERADPOUR_SCAN="$HUMAN_TFBS_SCAN_DIR/scan/human_10K_promoter_Kheradpour"
HUMAN_UNIPROBE_SCAN="$HUMAN_TFBS_SCAN_DIR/scan/human_10K_promoter_uniprobe"

#TFBS id mappings

HUMAN_TFBS_MAPPING_DIR="/Volumes/MyPassport/TFBS/mapping"
HUMAN_JASPAR_MAPPING="$HUMAN_TFBS_MAPPING_DIR/JASPAR_CORE_2009_vertebrates.mapping.human"
HUMAN_TRANSFAC_MAPPING="$HUMAN_TFBS_MAPPING_DIR/matrix_human.mapping"
HUMAN_WEI_MAPPING="$HUMAN_TFBS_MAPPING_DIR/wei2010_human_mws.mapping"
HUMAN_WANG_MAPPING="$HUMAN_TFBS_MAPPING_DIR/Wang.mapping"
HUMAN_KHERADPOUR_MAPPING="$HUMAN_TFBS_MAPPING_DIR/Kheradpour_human.mapping"
HUMAN_UNIPROBE_MAPPING="$HUMAN_TFBS_MAPPING_DIR/uniprobe_human.mapping"

HUMAN_EXPERIMENTAL_FILES=`find /home/users/pesch/Databases/ENCODE/CHIPSEQ-PEAKS/Human -name data.unmodified`
HUMAN_EXPERIMENT_MAPPING="/home/users/pesch/Databases/ENCODE/DNASE-PEAKS/Human/wgEncodeUwDnas/data.unmodified"

MOUSE_GTF_FILE="$DIR/data/GeneAnnotation/Mus_musculus.GRCm38.73.gtf"
MOUSE_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/mouse.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/mouse.ensembl.entrezgene.txt  MGI=$DIR/data/GeneAnnotation/mouse.ensembl.mgi_symbol.txt SWISSPROT=$DIR/data/GeneAnnotation/mouse.ensembl.uniprot_swissprot.txt"
MOUSE_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/mouse.ensembl.description.txt
MOUSE_GENE_NAME_FILE=$DIR/data/GeneAnnotation/mouse.ensembl.geneId.txt
MOUSE_CHROMOSOMS="1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 X Y"
MOUSE_EXPERIMENTAL_FILES=`find /home/users/pesch/Databases/ENCODE/CHIPSEQ-PEAKS/Mouse -name data.unmodified`
MOUSE_REGIONS="/home/proj/pesch/TFBS/Mouse/promoter/mouseRegions"
MOUSE_EXPERIMENT_MAPPING="/home/users/pesch/Databases/ENCODE/DNASE-PEAKS/Mouse/wgEncodeUwDnas/data.unmodified"

WORM_GTF_FILE="$DIR/data/GeneAnnotation/Caenorhabditis_elegans.WBcel235.73.gtf"
WORM_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/worm.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/worm.ensembl.entrezgene.txt  SWISSPROT=$DIR/data/GeneAnnotation/worm.ensembl.uniprot_swissprot.txt WORMBASE=$DIR/data/GeneAnnotation/worm.ensembl.wormbase.txt"
WORM_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/worm.ensembl.description.txt
WORM_GENE_NAME_FILE=$DIR/data/GeneAnnotation/worm.ensembl.geneId.txt
WORM_CHROSOMS="I II III IV V X"
WORM_EXPERIMENTAL_MAPPING_FILE="/mnt/raid1/proj/pesch/modEncode/Worm/data.unmodified"

FLY_GTF_FILE="$DIR/data/GeneAnnotation/Drosophila_melanogaster.BDGP5.20.gtf"
FLY_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/fly.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/fly.ensembl.entrezgene.txt  SWISSPROT=$DIR/data/GeneAnnotation/fly.ensembl.uniprot_swissprot.txt"
FLY_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/fly.ensembl.description.txt
FLY_GENE_NAME_FILE=$DIR/data/GeneAnnotation/fly.ensembl.geneId.txt
FLY_CHROMOSOMS="2L 2LHet 2R 2RHet 3L 3LHet 3R 3RHet 4 U Uextra X XHet YHet"
FLY_EXPERIMENTAL_MAPPING_FILE="/home/users/pesch/Databases/modEncode/Fly/data.unmodified"

TYPE="protein_coding"

E5=0.00001
E6=0.000001

REPOSITORY_HOME=""




