#!/bin/bash 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
CP="$DIR/target/classes:$DIR/target/dependency/*"

HUMAN_GTF_FILE="$DIR/data/GeneAnnotation/Homo_sapiens.GRCh37.73.gtf"
MOUSE_GTF_FILE="$DIR/data/GeneAnnotation/Mus_musculus.GRCm38.73.gtf"
WORM_GTF_FILE="$DIR/data/GeneAnnotation/Caenorhabditis_elegans.WS220.66.gtf"
FLY_GTF_FILE="$DIR/data/GeneAnnotation/Drosophila_melanogaster.BDGP5.20.gtf"

FLY_EXPERIMENTAL_MAPPING_FILE="/home/users/pesch/Databases/modEncode/Fly/data.unmodified"
WORM_EXPERIMENTAL_MAPPING_FILE="/mnt/raid1/proj/pesch/modEncode/Worm/data.unmodified"

HUMAN_EXPERIMENTAL_FILES=`find /home/users/pesch/Databases/ENCODE/CHIPSEQ-PEAKS/Human -name data.unmodified`
REPOSITORY_DIR="/home/extproj/pesch/CroCo"
COMPOSITE_NAME="ChIP"

MOUSE_EXPERIMENTAL_FILES=`find /home/users/pesch/Databases/ENCODE/CHIPSEQ-PEAKS/Mouse -name data.unmodified`

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










