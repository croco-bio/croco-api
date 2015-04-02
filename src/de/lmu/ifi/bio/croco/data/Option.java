package de.lmu.ifi.bio.croco.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Option {

	 project("Project"),
	 lab("Laboratory where the experiment was conducted"),
	 composite("Composite"),
	 cellLine("Cell line/ Tissue","cell"),
	 treatment("Treatment"),
	 strain("Strain"),
	 age("Age"),
	 replicate("Replicate"),
	 TaxId("Tax ID"),
	 NetworkName("Network name"),
	 NetworkType("Network type"),
	 EdgeType("Edge type"),
	 developmentStage("Developmental-Stage","developmental-stage"),
	 reference("Reference"),
	 MotifSet("PWM motif collection name"),
	 
	 AntibodyTargetMapped("Antibody target (mapped to ensembl)","targetMapped"),
	 AntibodyTarget("Antibody target","antibody"),
	 
	 Upstream("TSS upstream"),
	 Downstream("TSS downstream"),
	 DatabaseName("Database name"),
	 /**
	  * Use ConfidenceThreshold instead
	  */
	 @Deprecated
	 OpenChromMotifPVal("OpenChrom motif match p-value"),
	 /**
	  * Use MotifSet instead
	  */
	 @Deprecated
	 OpenChromMotifSet("OpenChrom motif set"),
	 OpenChromType("Open chromatin type"),
	 ConfidenceThreshold("Confidence threshold"),

	 networkFile("Underlying network file"),
	 @Deprecated
	 FactorList("List of transcription factors"),

	 @Deprecated
	 networkOverlap("Fraction of overlapping interactions"),
	 @Deprecated
	 explainability("Fraction of explainable interactions"),
	 @Deprecated
	 networkDegreeOverlap("Network degree overlap"),

	 numberOfInteractions("Number of interactions"), 
	 numberOfNodes("Number of nodes"), 
     
	 
	 ENCODEName("Encode file id"),
	 numberOfPeak("Number of called peaks"),
	 
	 TextMiningSpeciesContext("Text-Mining species context"),
	 Compendium("Compendium");
	 
	 public String description;
	 public List<String> alias;
	 

	 public static Option getOption(String name){
		 for(Option o : Option.values()){
			 if ( o.name().equals(name)) return o;
		 }
		 return null;
	 }
	 Option(String description,String...alias){
		 this.description = description;
		 this.alias = new ArrayList<String>();
		 this.alias.add(this.name());
		 if ( alias.length > 0) this.alias.addAll(Arrays.asList(alias));
	 }
	 Option(){}
	
	 
	 public static void main(String[] args)
	 {
	     for(Option option: Option.values())
	     {
	         System.out.println(option.name() + " " + option.ordinal());
	     }
	 }
}
