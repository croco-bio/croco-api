package de.lmu.ifi.bio.croco.data;

import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;

public class BindingEvidence {
    public NetworkMetaInformation networkMetaInformation;
    public Peak peak;
    
    public BindingEvidence(NetworkMetaInformation networkMetaInformation, Peak peak) {
        super();
        this.networkMetaInformation = networkMetaInformation;
        this.peak = peak;
    }
    
    public String getName()
    {
        return networkMetaInformation.getName();
    }
}
