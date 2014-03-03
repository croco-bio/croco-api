#!/bin/bash
###
### Import of gene annotations into the croco repository
###

#Assume: the data is available in the folder data/GeneAnnotation!
#data/GeneAnnotation/whatIDid.sh
source setEnvironment.sh

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 7227\
  -gtfFile $FLY_GTF_FILE  \
  -geneLinkOutFiles $FLY_GENE_LINKOUT_FILES \
  -descriptionFile $FLY_GENE_DESCRIPTION_FILE \
  -geneNameFile $FLY_GENE_NAME_FILE -clean

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 10090 \
  -gtfFile $MOUSE_GTF_FILE  \
  -geneLinkOutFiles $MOUSE_GENE_LINKOUT_FILES \
  -descriptionFile $MOUSE_GENE_DESCRIPTION_FILE \
  -geneNameFile $MOUSE_GENE_NAME_FILE 
#  -chromsoms $MOUSE_CHROMOSOMS

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 6239 \
  -gtfFile $WORM_GTF_FILE  \
  -geneLinkOutFiles $WORM_GENE_LINKOUT_FILES \
  -descriptionFile $WORM_GENE_DESCRIPTION_FILE \
  -geneNameFile $WORM_GENE_NAME_FILE 
#  -chromsoms $WORM_CHROSOMS


#  -chromsoms $FLY_CHROMOSOMS

java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.Annotation.GeneAnnotation \
  -taxId 9606 \
  -gtfFile $HUMAN_GTF_FILE  \
  -geneLinkOutFiles $HUMAN_GENE_LINKOUT_FILES \
  -descriptionFile $HUMAN_GENE_DESCRIPTION_FILE \
  -geneNameFile $HUMAN_GENE_NAME_FILE   
 # -chromsoms $HUMAN_CHROMOSOMS 

