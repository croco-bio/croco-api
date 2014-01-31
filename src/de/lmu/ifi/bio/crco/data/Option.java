package de.lmu.ifi.bio.crco.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Option {

	 project(OptionType.Experiment,"Project"),
	 lab(OptionType.Experiment,"Laboratory where the experiment was conducted"),
	 composite(OptionType.Experiment,"Composite"),
	 cellLine(OptionType.Experiment,"Cell line/ Tissue","cell"),
	 treatment(OptionType.Experiment,"Treatment"),
	 strain(OptionType.Experiment,"Strain"),
	 age(OptionType.Experiment,"Age"),
	 replicate(OptionType.Experiment,"Replicate"),
	 TaxId(OptionType.Experiment,"Tax ID"),
	 NetworkName(OptionType.Experiment,"Network name"),
	 NetworkType(OptionType.Experiment,"Network type"),
	 EdgeType(OptionType.Experiment,"Edge type"),
	 developmentStage(OptionType.Experiment,"Developmental-Stage","developmental-stage"),
	 reference(OptionType.Experiment,"Reference"),
	 MotifSet(OptionType.Experiment,"PWM motif collection name"),
	 
	 
	 AntibodyTargetMapped(OptionType.Experiment,"Antibody target (mapped to ensembl)","targetMapped"),
	 AntibodyTarget(OptionType.Experiment,"Antibody target","antibody"),
	 
	 Upstream(OptionType.Experiment,"TSS upstream"),
	 Downstream(OptionType.Experiment,"TSS downstream"),
	 DatabaseName(OptionType.Experiment,"Database name"),
	 OpenChromMotifPVal(OptionType.Experiment,"OpenChrom motif match p-value"),
	 OpenChromMotifSet(OptionType.Experiment,"OpenChrom motif set"),
	 OpenChromType(OptionType.Experiment,"Open chromatin type"),
	 ConfidenceThreshold(OptionType.Experiment,"Confidence threshold"),

	 networkFile(OptionType.Experiment,"Underlying network file"),
	 
	 FactorList(OptionType.Experiment,"List of transcription factors"),
	 
	 networkOverlap(OptionType.NetworkSimilarity,"Network Overlap"),
	 explainability(OptionType.NetworkSimilarity,"Fraction of explainable source interactions"),
	 networkDegreeOverlap(OptionType.NetworkSimilarity,"Network degree overlap"),
	 
	 numberOfInteractions(OptionType.Experiment,"Number of interactions");
	 
	 public OptionType optionType;
	 public String description;
	 public List<String> alias;
	 
	 public enum OptionType{
		 Experiment,NetworkSimilarity;
	 }
	 
	 Option(OptionType optionType,String description,String...alias){
		 this.optionType = optionType;
		 this.description = description;
		 this.alias = new ArrayList<String>();
		 this.alias.add(this.name());
		 if ( alias.length > 0) this.alias.addAll(Arrays.asList(alias));
	 }
	 Option(){}
	 Option(OptionType optionType){
		 this.optionType = optionType;
	 }
}
