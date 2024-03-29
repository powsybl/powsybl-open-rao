/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.api.mock;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationReport;

import java.time.OffsetDateTime;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationContextMock implements CracCreationContext {

    private boolean success = true;

    public CracCreationContextMock(boolean success) {
        this.success = success;
    }

    @Override
    public boolean isCreationSuccessful() {
        return success;
    }

    @Override
    public Crac getCrac() {
        return null;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return null;
    }

    @Override
    public String getNetworkName() {
        return "networkName";
    }

    @Override
    public CracCreationReport getCreationReport() {
        return null;
    }
}
