package de.lmu.ifi.bio.crco.stat.generator;

import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.network.Network;

/**
 * 
 * @author Robert Pesch
 *
 */
public interface PairwiseStatGenerator {
	enum FeatureType{
		SYMMETRIC, ASYMMETRIC;
	}
	
	
	/**
	 * Computes a similarity value between two networks.
	 * @param network1 the source network
	 * @param network2 the target network
	 * @return similarity value
	 * @throws Exception when the taxId differs
	 */
	public float compute( Network network1, Network network2 ) throws Exception;
	public Option getOption();
	public FeatureType getFeatureType();

}
