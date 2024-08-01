/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.CracCreationReport;

import java.time.OffsetDateTime;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class JsonCracCreationContext implements CracCreationContext {
    private final boolean isCreationSuccessful;
    private final Crac crac;
    private final String networkName;
    private final CracCreationReport cracCreationReport;

    public JsonCracCreationContext(boolean isCreationSuccessful, Crac crac, String networkName) {
        this.isCreationSuccessful = isCreationSuccessful;
        this.crac = crac;
        this.networkName = networkName;
        this.cracCreationReport = new CracCreationReport();
    }

    @Override
    public boolean isCreationSuccessful() {
        return isCreationSuccessful;
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
        return networkName;
    }

    @Override
    public CracCreationReport getCreationReport() {
        return cracCreationReport;
    }
}
