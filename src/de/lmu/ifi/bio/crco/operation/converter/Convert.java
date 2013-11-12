package de.lmu.ifi.bio.crco.operation.converter;

import de.lmu.ifi.bio.crco.network.Network;

public interface Convert<E> {
	public E convert(Network network) ;
	
	
}
