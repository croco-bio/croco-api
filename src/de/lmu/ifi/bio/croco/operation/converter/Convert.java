package de.lmu.ifi.bio.croco.operation.converter;

import de.lmu.ifi.bio.croco.network.Network;

public interface Convert<E> {
	public E convert(Network network) ;
	
	
}
