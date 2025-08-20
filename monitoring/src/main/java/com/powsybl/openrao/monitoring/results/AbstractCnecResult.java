/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.monitoring.SecurityStatus;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public abstract class AbstractCnecResult<I extends Cnec<?>> implements CnecResult<I> {
    protected final I cnec;
    protected final double margin;
    protected final SecurityStatus securityStatus;

    protected AbstractCnecResult(I cnec, double margin, SecurityStatus securityStatus) {
        this.cnec = cnec;
        this.margin = margin;
        this.securityStatus = securityStatus;
    }

    @Override
    public I getCnec() {
        return cnec;
    }

    @Override
    public SecurityStatus getCnecSecurityStatus() {
        return securityStatus;
    }

    @Override
    public double getMargin() {
        return margin;
    }
}


