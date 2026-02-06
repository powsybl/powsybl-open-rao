/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;


import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.CracCreationReport;

import java.time.OffsetDateTime;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreationContext implements CracCreationContext {
    private Crac crac;
    private boolean isCreationSuccessful;
    private CracCreationReport creationReport;
    private final String networkName;

    public NetworkCracCreationContext(Crac crac, String networkName) {
        this.crac = crac;
        this.networkName = networkName;
        this.creationReport = new CracCreationReport();
    }

    @Override
    public boolean isCreationSuccessful() {
        return false;
    }

    @Override
    public Crac getCrac() {
        return crac;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return null;
    }

    @Override
    public String getNetworkName() {
        return "";
    }

    @Override
    public CracCreationReport getCreationReport() {
        return creationReport;
    }
}
