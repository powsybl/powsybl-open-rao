package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.raw_crac_api.RawCrac;

public class CracCreationResult<T extends RawCrac> {

    private Crac crac;
    private CracCreationContext<T> cracCreationContext;
    //CreationReport<T> creationReport;

    public CracCreationResult(Crac crac, CracCreationContext<T> cracCreationContext) {
        this.crac = crac;
        this.cracCreationContext = cracCreationContext;
    }

    public CracCreationContext<T> getCracCreationContext() {
        return cracCreationContext;
    }

    public Crac getCrac() {
        return crac;
    }
}
