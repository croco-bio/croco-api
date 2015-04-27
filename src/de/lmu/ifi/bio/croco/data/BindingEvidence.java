package de.lmu.ifi.bio.croco.data;

import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;

public class BindingEvidence {
    public NetworkMetaInformation networkMetaInformation;
    public Peak pean;
    
    public BindingEvidence(NetworkMetaInformation networkMetaInformation,
            Peak pean) {
        super();
        this.networkMetaInformation = networkMetaInformation;
        this.pean = pean;
    }
}
