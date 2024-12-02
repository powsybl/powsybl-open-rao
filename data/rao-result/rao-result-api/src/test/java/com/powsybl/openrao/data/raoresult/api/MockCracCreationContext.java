/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.CracCreationReport;

import java.time.OffsetDateTime;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MockCracCreationContext implements CracCreationContext {
    @Override
    public boolean isCreationSuccessful() {
        return false;
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
        return null;
    }

    @Override
    public CracCreationReport getCreationReport() {
        return null;
    }
}
