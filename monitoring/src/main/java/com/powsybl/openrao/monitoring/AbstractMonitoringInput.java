/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractMonitoringInput<I extends Cnec<?>> implements MonitoringInput<I> {
    protected final Crac crac;
    protected final Network network;
    protected final RaoResult raoResult;
    protected final ZonalData<Scalable> scalableZonalData;

    protected AbstractMonitoringInput(Crac crac, Network network, RaoResult raoResult, ZonalData<Scalable> scalableZonalData) {
        this.crac = crac;
        this.network = network;
        this.raoResult = raoResult;
        this.scalableZonalData = scalableZonalData;
    }

    @Override
    public Crac getCrac() {
        return crac;
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public RaoResult getRaoResult() {
        return raoResult;
    }

    @Override
    public ZonalData<Scalable> getScalableZonalData() {
        return scalableZonalData;
    }
}
