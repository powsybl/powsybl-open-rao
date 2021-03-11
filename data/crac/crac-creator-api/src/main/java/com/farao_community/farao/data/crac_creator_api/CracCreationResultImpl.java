/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.native_crac_api.NativeCrac;

import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationResultImpl<T extends NativeCrac, S extends CracCreationContext<T>> implements CracCreationResult<T, S> {

    private Crac crac;
    private boolean isCreationSuccessful;
    private S cracCreationContext;
    private List<String> creationReport;

    public CracCreationResultImpl(Crac crac, boolean isCreationSuccessful, S cracCreationContext, List<String> creationReport) {
        this.crac = crac;
        this.isCreationSuccessful = isCreationSuccessful;
        this.cracCreationContext = cracCreationContext;
    }

    public boolean isCreationSuccessful() {
        return isCreationSuccessful;
    }

    public Crac getCrac() {
        return crac;
    }

    public S getCracCreationContext() {
        return cracCreationContext;
    }

    public List<String> getCreationReport() {
        return creationReport;
    }

}
