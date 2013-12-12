library("biomaRt")

linkouts <- function(mart,field1,field2,mappingFile){
  data <- getBM(attributes=c(field1,field2),mart=ensembl)
  write.table(data,file=mappingFile, quote =F, sep ="\t",row.names=F)

}

#Human

#uses most recent ensembl mart (currently 73)
mart <- useMart("ensembl")

#HUMAN
print("Processing HUMAN")
ensembl = useDataset(mart=mart,dataset = "hsapiens_gene_ensembl")
#linkouts(ensembl,"ensembl_gene_id","description","human.ensembl.description.txt")
#linkouts(ensembl,"ensembl_gene_id","external_gene_id","human.ensembl.geneId.txt")
#linkouts(ensembl,"ensembl_gene_id","entrezgene","human.ensembl.entrezgene.txt")
#linkouts(ensembl,"ensembl_gene_id","hgnc_symbol","human.ensembl.hgnc_symbol.txt")
#linkouts(ensembl,"ensembl_gene_id","uniprot_swissprot","human.ensembl.uniprot_swissprot.txt")
#linkouts(ensembl,"ensembl_gene_id","go_id","human.ensembl.go_id.txt")

#Worm
print("Processing WORM")
ensembl = useDataset(mart=mart,dataset = "celegans_gene_ensembl")
linkouts(ensembl,"ensembl_gene_id","description","worm.ensembl.description.txt")
linkouts(ensembl,"ensembl_gene_id","external_gene_id","worm.ensembl.geneId.txt")
linkouts(ensembl,"ensembl_gene_id","entrezgene","worm.ensembl.entrezgene.txt")
linkouts(ensembl,"ensembl_gene_id","uniprot_swissprot","worm.ensembl.uniprot_swissprot.txt")
linkouts(ensembl,"ensembl_gene_id","wormbase_gene","worm.ensembl.wormbase.txt")
linkouts(ensembl,"ensembl_gene_id","go_id","worm.ensembl.go_id.txt")

#Mouse
print("Processing MOUSE")
#ensembl = useDataset(mart=mart,dataset = "mmusculus_gene_ensembl")
#linkouts(ensembl,"ensembl_gene_id","description","mouse.ensembl.description.txt")
#linkouts(ensembl,"ensembl_gene_id","external_gene_id","mouse.ensembl.geneId.txt")
#linkouts(ensembl,"ensembl_gene_id","entrezgene","mouse.ensembl.entrezgene.txt")
#linkouts(ensembl,"ensembl_gene_id","mgi_symbol","mouse.ensembl.mgi_symbol.txt")
#linkouts(ensembl,"ensembl_gene_id","uniprot_swissprot","mouse.ensembl.uniprot_swissprot.txt")
#linkouts(ensembl,"ensembl_gene_id","go_id","mouse.ensembl.go_id.txt")


#Fly
print("Processing Fly")
ensembl = useDataset(mart=mart, dataset = "dmelanogaster_gene_ensembl")

#linkouts(ensembl,"ensembl_gene_id","description","fly.ensembl.description.txt")
#linkouts(ensembl,"ensembl_gene_id","external_gene_id","fly.ensembl.geneId.txt")
#linkouts(ensembl,"ensembl_gene_id","entrezgene","fly.ensembl.entrezgene.txt")
#linkouts(ensembl,"ensembl_gene_id","uniprot_swissprot","fly.ensembl.uniprot_swissprot.txt")
#linkouts(ensembl,"ensembl_gene_id","go_id","fly.ensembl.go_id.txt")


