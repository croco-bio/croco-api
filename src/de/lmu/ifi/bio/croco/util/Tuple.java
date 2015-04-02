package de.lmu.ifi.bio.croco.util;

import java.io.Serializable;


public interface Tuple<FIRST,SECOND> extends Serializable { 
	public FIRST getFirst();
	public SECOND getSecond();

}
