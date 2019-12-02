/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.farao_community.farao.data.crac_api.Unit;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

/**
 * Generic threshold (flow, voltage, etc.) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = VoltageThreshold.class, name = "voltageThreshold")
    })
public abstract class AbstractThreshold {
    protected Unit unit;

    public AbstractThreshold(Unit unit) {
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }

    public abstract boolean isMinThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException;

    public abstract boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException;

    public void synchronize(Network network, Cnec cnec) {

    }

    public void desynchronize() {

    }
}
