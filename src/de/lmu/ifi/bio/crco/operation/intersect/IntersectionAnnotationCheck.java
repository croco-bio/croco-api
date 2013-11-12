package de.lmu.ifi.bio.crco.operation.intersect;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;

public interface IntersectionAnnotationCheck {
	public boolean check(TIntObjectHashMap<List<Object>>  anno1, TIntObjectHashMap<List<Object>>  anno2);
}
