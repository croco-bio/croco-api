#!/bin/bash

#Download gene annotations from ensembl (relase 73)
wget ftp://ftp.ensembl.org/pub/release-74/gtf/homo_sapiens/Homo_sapiens.GRCh37.73.gtf.gz
wget ftp://ftp.ensembl.org/pub/release-74/gtf/mus_musculus/Mus_musculus.GRCm38.73.gtf.gz
wget ftp://ftp.ensembl.org/pub/release-74/gtf/caenorhabditis_elegans/Caenorhabditis_elegans.WBcel235.73.gtf.gz
wget ftp://ftp.ensembl.org/pub/release-74/gtf/drosophila_melanogaster/Drosophila_melanogaster.BDGP5.73.gtf.gz
#Fetchs additional annotations using biomart (for the most current ENSEMBL version (currently also 73))
RScript geneLists.R

