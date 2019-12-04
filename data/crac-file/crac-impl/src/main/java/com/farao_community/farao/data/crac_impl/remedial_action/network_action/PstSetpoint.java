/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

/**
 * PST setpoint remedial action: set a PST's tap at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public final class PstSetpoint extends AbstractNetworkElementAction {

    private double setpoint;

    @JsonCreator
    public PstSetpoint(@JsonProperty("networkElement") NetworkElement networkElement, @JsonProperty("setpoint") double setpoint) {
        super(networkElement);
        this.setpoint = setpoint;
    }

    public double getSetpoint() {
        return setpoint;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    @Override
    public void apply(Network network) {
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(getNetworkElement().getId()).getPhaseTapChanger();
        if (phaseTapChanger.getHighTapPosition() >= setpoint && phaseTapChanger.getLowTapPosition() <= setpoint) {
            phaseTapChanger.setTapPosition((int) setpoint);
        } else {
            throw new FaraoException("PST cannot be set because setpoint is out of PST boundaries");
        }
    }
}
