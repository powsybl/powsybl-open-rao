package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.raw_crac_api.RawCrac;

import java.util.List;

public class CracCreationResult<T extends RawCrac, S extends CracCreationContext<T>> {

    private Crac crac;
    private boolean isCreationSuccessful;
    private S cracCreationContext;
    private List<String> creationReport;

    public CracCreationResult(Crac crac, boolean isCreationSuccessful, S cracCreationContext, List<String> creationReport) {
        this.crac = crac;
        this.isCreationSuccessful = isCreationSuccessful;
        this.cracCreationContext = cracCreationContext;
    }

    public boolean isCreationSuccessful() {
        return isCreationSuccessful;
    }

    public S getCracCreationContext() {
        return cracCreationContext;
    }

    public Crac getCrac() {
        return crac;
    }
}
