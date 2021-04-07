/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ElementaryAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Injection setpoint remedial action: set a load or generator at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("injection-setpoint")
public final class InjectionSetpointImpl implements ElementaryAction {

    private NetworkElement networkElement;
    private double setpoint;

    public InjectionSetpointImpl(NetworkElement networkElement, double setpoint) {
        this.networkElement = networkElement;
        this.setpoint = setpoint;
    }

    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public void apply(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
        if (identifiable instanceof Generator) {
            Generator generator = (Generator) identifiable;
            generator.setTargetP(setpoint);
        } else if (identifiable instanceof Load) {
            Load load = (Load) identifiable;
            load.setP0(setpoint);
        } else if (identifiable instanceof DanglingLine) {
            DanglingLine danglingLine = (DanglingLine) identifiable;
            danglingLine.setP0(setpoint);
        } else {
            throw new NotImplementedException("Injection setpoint only handled for generators, loads or dangling lines");
        }
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }
}
