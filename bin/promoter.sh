DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
CP="$DIR/target/classes:$DIR/target/dependency/*"

TYPE="protein_coding"
HUMAN_GTF_FILE="/home/proj/biosoft/GENOMIC/HUMAN/Homo_sapiens.GRCh37.73.gtf"
HUMAN_GENOME_DIR="/home/proj/biosoft/GENOMIC/HUMAN/HUMAN_GENOME_FASTA"
HUMAN_FIMO_OUTPUT="/home/proj/pesch/TFBS/Human/promoter/Homo_sapiens.GRCh37.73.promoter"
HUMAN_CHROMOSOMS="1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y"
 
java -Xmx4G -Xms4G -cp $CP -Xmx2G -Xms2G de.lmu.ifi.bio.crco.processor.TFBS.FIMOInputGenerator \
  -gtf $HUMAN_GTF_FILE  \
  -genome $HUMAN_GENOME_DIR  \
  -type $TYPE\
  -downstream 5000 \
  -upstream 5000 \
  -chromosoms $HUMAN_CHROMOSOMS \
  -output $HUMAN_FIMO_OUTPUT
