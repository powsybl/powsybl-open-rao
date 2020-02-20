/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

/**
 * Interface for Critical Network Element & Contingencies
 * State object represents the contingency. This type of elements can be violated
 * by maximum value or minimum value. So they have thresholds.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface Cnec extends Identifiable<Cnec>, Synchronizable {

    State getState();

    NetworkElement getNetworkElement();


    /**
     * This function returns, for a given Network situation, the margin of the Threshold.
     * The margin is the distance, given with the unit of measure of the Threshold, from
     * the actual monitored physical parameter of a Cnec to the Threshold limits. If it is
     * positive, it means that the limits of the Threshold are respected. If it is negative,
     * it means that that a limit of the Threshold has been overcome.
     *
     * margin = min(maxThreshold - actualValue, actualValue - minThreshold)
     */
    double computeMargin(Network network) throws SynchronizationException;

    Threshold getThreshold();

    /**
     * Get the flow (in A) transmitted by Cnec in a given Network. Note that an I
     * value exists in the Network only if an AC load-flow has been previously run.
     * If no value is present in the network, throws a FaraoException.
     */
    double getI(Network network);

    /**
     * Get the flow (in MW) transmitted by Cnec in a given Network. Note that an P
     * value exists in the Network only if an load-flow (AC or DC) has been previously
     * run. If no value is present in the network, throws a FaraoException.
     */
    double getP(Network network);
}
