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
public abstract class CnecResult<I extends Cnec<?>> {
    protected final I cnec;
    protected final Unit unit;
    protected double margin;

    protected CnecResult(I cnec, Unit unit, Network network) {
        this.cnec = cnec;
        this.unit = unit;
        computeValue(network);
        computeMargin();
    }

    protected CnecResult(I cnec, Unit unit) {
        this.cnec = cnec;
        this.unit = unit;
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

    public String getId() {
        return cnec.getId();
    }

    public abstract String print();
    
    protected abstract void computeValue(Network network);

    protected abstract void computeMargin();

    public abstract Cnec.SecurityStatus getCnecSecurityStatus();


}
