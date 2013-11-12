package de.lmu.ifi.bio.crco.operation.intersect;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;


public class BindingSiteOverlapCheck implements IntersectionAnnotationCheck {

	@Override
	public boolean check(TIntObjectHashMap<List<Object>> anno1, TIntObjectHashMap<List<Object>> anno2) {
		
		if ( !anno1.contains(EdgeOption.BindingSite.ordinal())  ||!anno1.contains(EdgeOption.BindingSite.ordinal())  ) return false;
		
		List<TFBSPeak>bindingsSites1 = (List) anno1.get(EdgeOption.BindingSite.ordinal());
		List<TFBSPeak>bindingsSites2 = (List) anno2.get(EdgeOption.BindingSite.ordinal());
		
		
		List<TFBSPeak> intersectedBindings = Peak.intersections(bindingsSites1,bindingsSites2);

		if ( intersectedBindings.size() > 0){
			return true;
		}
		return false;
		
	}

}
