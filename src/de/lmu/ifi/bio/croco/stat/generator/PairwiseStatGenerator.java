package de.lmu.ifi.bio.croco.stat.generator;

import de.lmu.ifi.bio.croco.data.Option;
import de.lmu.ifi.bio.croco.network.Network;

/**
 * 
 * @author Robert Pesch
 *
 */
public interface PairwiseStatGenerator {
	enum FeatureType{
		SYMMETRIC, ASYMMETRIC;
	}
	class Result{
		public Result(Integer numerator, Integer  denominator ){
			this.numerator = numerator;
			this.denominator = denominator;
		}
		public Integer numerator;
		public Integer denominator;
		public float getFrac(){
			return (float)numerator/(float)denominator;
		}
	}
	
	/**
	 * Computes a similarity value between two networks.
	 * @param network1 the source network
	 * @param network2 the target network
	 * @return similarity value
	 * @throws Exception when the taxId differs
	 */
	public Result compute( Network network1, Network network2 ) throws Exception;
	public Option getOption();
	public FeatureType getFeatureType();

}
