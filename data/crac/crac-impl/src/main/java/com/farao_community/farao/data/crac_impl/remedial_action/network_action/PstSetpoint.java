/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.ElementaryAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

import java.util.List;

import static com.farao_community.farao.data.crac_api.RangeDefinition.CENTERED_ON_ZERO;
import static com.farao_community.farao.data.crac_api.RangeDefinition.STARTS_AT_ONE;

/**
 * PST setpoint remedial action: set a PST's tap at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("pst-setpoint")
public final class PstSetpoint extends AbstractIdentifiable implements ElementaryAction {

    private NetworkElement networkElement;
    private double setpoint;
    private RangeDefinition rangeDefinition;

    public PstSetpoint(String id, String name, NetworkElement networkElement, double setPoint, RangeDefinition rangeDefinition) {
        super(id, name);
        this.networkElement = networkElement;
        this.setpoint = setPoint;
        this.rangeDefinition = rangeDefinition;
    }

    public RangeDefinition getRangeDefinition() {
        return this.rangeDefinition;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    /**
     * Change tap position of the PST pointed by the network element at the tap given at object instantiation.
     *
     * @param network network to modify
     */
    @Override
    public void apply(Network network) {
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger();

        int normalizedSetPoint = 0;

        if (rangeDefinition == CENTERED_ON_ZERO) {
            normalizedSetPoint = ((phaseTapChanger.getLowTapPosition() + phaseTapChanger.getHighTapPosition()) / 2) + (int) setpoint;
        } else if (rangeDefinition == STARTS_AT_ONE) {
            normalizedSetPoint = phaseTapChanger.getLowTapPosition() + (int) setpoint - 1;
        }

        if (normalizedSetPoint >= phaseTapChanger.getLowTapPosition() && normalizedSetPoint <= phaseTapChanger.getHighTapPosition()) {
            phaseTapChanger.setTapPosition(normalizedSetPoint);
        } else {
            throw new FaraoException(String.format(
                    "Tap value %d not in the range of high and low tap positions [%d,%d] of the phase tap changer %s steps",
                    normalizedSetPoint,
                    phaseTapChanger.getLowTapPosition(),
                    phaseTapChanger.getHighTapPosition(),
                    networkElement.getId()));
        }
    }
}
