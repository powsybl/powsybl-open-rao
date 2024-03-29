/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

import java.util.Collections;
import java.util.Set;

/**
 * PST setpoint remedial action: set a PST's tap at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class PstSetpointImpl implements PstSetpoint {

    private final NetworkElement networkElement;
    private final int setpoint;

    PstSetpointImpl(NetworkElement networkElement, int setpoint) {
        this.networkElement = networkElement;
        this.setpoint = setpoint;
    }

    @Override
    public int getSetpoint() {
        return this.setpoint;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    private int getNormalizedSetpoint(PhaseTapChanger phaseTapChanger) {
        return ((phaseTapChanger.getLowTapPosition() + phaseTapChanger.getHighTapPosition()) / 2) + setpoint;
    }

    /**
     * Change tap position of the PST pointed by the network element at the tap given at object instantiation.
     *
     * @param network network to modify
     */
    @Override
    public void apply(Network network) {
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger();
        int normalizedSetPoint = getNormalizedSetpoint(phaseTapChanger);

        if (normalizedSetPoint >= phaseTapChanger.getLowTapPosition() && normalizedSetPoint <= phaseTapChanger.getHighTapPosition()) {
            phaseTapChanger.setTapPosition(normalizedSetPoint);
        } else {
            throw new OpenRaoException(String.format(
                    "Tap value %d not in the range of high and low tap positions [%d,%d] of the phase tap changer %s steps",
                    normalizedSetPoint,
                    phaseTapChanger.getLowTapPosition(),
                    phaseTapChanger.getHighTapPosition(),
                    networkElement.getId()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PstSetpointImpl oPstSetPoint = (PstSetpointImpl) o;
        return oPstSetPoint.getNetworkElement().equals(this.networkElement)
            && oPstSetPoint.getSetpoint() == this.setpoint;
    }

    @Override
    public boolean hasImpactOnNetwork(Network network) {
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(networkElement.getId()).getPhaseTapChanger();
        return getNormalizedSetpoint(phaseTapChanger) != phaseTapChanger.getTapPosition();
    }

    @Override
    public boolean canBeApplied(Network network) {
        // TODO : setpoint out of range ?
        return true;
    }

    @Override
    public int hashCode() {
        return networkElement.hashCode() + 7 * Double.valueOf(setpoint).hashCode();
    }
}
