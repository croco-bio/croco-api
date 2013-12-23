#!/bin/bash
###
### Import of gene annotations into the croco repository
###

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
CP="$DIR/target/classes:$DIR/target/dependency/*"

#Assume: the data is available in the folder data/GeneAnnotation!
#data/GeneAnnotation/whatIDid.sh

HUMAN_GTF_FILE="$DIR/data/GeneAnnotation/Homo_sapiens.GRCh37.73.gtf"
HUMAN_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/human.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/human.ensembl.entrezgene.txt  HGNC=$DIR/data/GeneAnnotation/human.ensembl.hgnc_symbol.txt SWISSPROT=$DIR/data/GeneAnnotation/human.ensembl.uniprot_swissprot.txt"
HUMAN_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/human.ensembl.description.txt
HUMAN_GENE_NAME_FILE=$DIR/data/GeneAnnotation/human.ensembl.geneId.txt
#HUMAN_CHROMOSOMS="1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y"

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 9606 \
  -gtfFile $HUMAN_GTF_FILE  \
  -geneLinkOutFiles $HUMAN_GENE_LINKOUT_FILES \
  -descriptionFile $HUMAN_GENE_DESCRIPTION_FILE \
  -geneNameFile $HUMAN_GENE_NAME_FILE  -clean
 # -chromsoms $HUMAN_CHROMOSOMS -clean

MOUSE_GTF_FILE="$DIR/data/GeneAnnotation/Mus_musculus.GRCm38.73.gtf"
MOUSE_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/mouse.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/mouse.ensembl.entrezgene.txt  MGI=$DIR/data/GeneAnnotation/mouse.ensembl.mgi_symbol.txt SWISSPROT=$DIR/data/GeneAnnotation/mouse.ensembl.uniprot_swissprot.txt"
MOUSE_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/mouse.ensembl.description.txt
MOUSE_GENE_NAME_FILE=$DIR/data/GeneAnnotation/mouse.ensembl.geneId.txt
#MOUSE_CHROMOSOMS="1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 X Y"

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 10090 \
  -gtfFile $MOUSE_GTF_FILE  \
  -geneLinkOutFiles $MOUSE_GENE_LINKOUT_FILES \
  -descriptionFile $MOUSE_GENE_DESCRIPTION_FILE \
  -geneNameFile $MOUSE_GENE_NAME_FILE 
#  -chromsoms $MOUSE_CHROMOSOMS


WORM_GTF_FILE="$DIR/data/GeneAnnotation/Caenorhabditis_elegans.WBcel235.73.gtf"
WORM_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/worm.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/worm.ensembl.entrezgene.txt  SWISSPROT=$DIR/data/GeneAnnotation/worm.ensembl.uniprot_swissprot.txt WORMBASE=$DIR/data/GeneAnnotation/worm.ensembl.wormbase.txt"
WORM_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/worm.ensembl.description.txt
WORM_GENE_NAME_FILE=$DIR/data/GeneAnnotation/worm.ensembl.geneId.txt
#WORM_CHROSOMS="I II III IV V X"

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 6239 \
  -gtfFile $WORM_GTF_FILE  \
  -geneLinkOutFiles $WORM_GENE_LINKOUT_FILES \
  -descriptionFile $WORM_GENE_DESCRIPTION_FILE \
  -geneNameFile $WORM_GENE_NAME_FILE 
#  -chromsoms $WORM_CHROSOMS

FLY_GTF_FILE="$DIR/data/GeneAnnotation/Drosophila_melanogaster.BDGP5.20.gtf"
FLY_GENE_LINKOUT_FILES="GO=$DIR/data/GeneAnnotation/fly.ensembl.go_id.txt ENTREZ=$DIR/data/GeneAnnotation/fly.ensembl.entrezgene.txt  SWISSPROT=$DIR/data/GeneAnnotation/fly.ensembl.uniprot_swissprot.txt"
FLY_GENE_DESCRIPTION_FILE=$DIR/data/GeneAnnotation/fly.ensembl.description.txt
FLY_GENE_NAME_FILE=$DIR/data/GeneAnnotation/fly.ensembl.geneId.txt
#FLY_CHROMOSOMS="2L 2LHet 2R 2RHet 3L 3LHet 3R 3RHet 4 U Uextra X XHet YHet"

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 7227\
  -gtfFile $FLY_GTF_FILE  \
  -geneLinkOutFiles $FLY_GENE_LINKOUT_FILES \
  -descriptionFile $FLY_GENE_DESCRIPTION_FILE \
  -geneNameFile $FLY_GENE_NAME_FILE 
#  -chromsoms $FLY_CHROMOSOMS
