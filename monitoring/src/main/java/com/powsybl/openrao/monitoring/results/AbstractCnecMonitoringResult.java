/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

public abstract class AbstractCnecMonitoringResult<I extends Cnec<?>> implements CnecMonitoringResult<I> {
    protected final I cnec;
    protected final Unit unit;
    protected double margin;

    protected AbstractCnecMonitoringResult(I cnec, Unit unit, Network network) {
        this.cnec = cnec;
        this.unit = unit;
        checkUnit();
        computeValue(network);
        computeMargin();
    }

    protected AbstractCnecMonitoringResult(I cnec, Unit unit) {
        this.cnec = cnec;
        this.unit = unit;
        checkUnit();
    }

    public I getCnec() {
        return cnec;
    }

    public Unit getUnit() {
        return unit;
    }

    public double getMargin() {
        return this.margin;
    }

    protected abstract void computeValue(Network network);

    protected abstract void computeMargin();

    protected abstract void checkUnit();

}
