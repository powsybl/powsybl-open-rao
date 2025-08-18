/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.cnec;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for Critical Network Element &amp; Contingency
 *
 * A Cnec represents a set of {@link NetworkElement} that are considered critical in the network.
 * The {@link PhysicalParameter} of the Cnec is therefore monitored and/or optimized in
 * RAOs relying on the Crac containing this Cnec.
 *
 * A Cnec is defined for a specific {@link State}. That is to say, either for the preventive
 * instant, or after a contingency in a post-outage instant.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface Cnec<I extends Cnec<I>> extends Identifiable<I> {

    /**
     * Getter of the set of {@link NetworkElement}s on which the {@code Cnec} is defined.
     */
    Set<NetworkElement> getNetworkElements();

    /**
     * Getter of the {@link State} on which the {@code Cnec} is defined.
     */
    State getState();

    /**
     * Getter of the reliability margin. This value enables to take a margin compared to what is defined in
     * the {@code thresholds} of the {@code Cnec}. This margin would be common to all {@code thresholds}.
     * It should be 0 by default to ensure that there is no inconsistencies with {@code thresholds}.
     */
    double getReliabilityMargin();

    /**
     * Getter of the {@link PhysicalParameter} representing the {@code Cnec}.
     * It defines the physical value that will be monitored/optimized for this {@code Cnec}.
     */
    PhysicalParameter getPhysicalParameter();

    /**
     * Returns a tag indicating whether or not the {@link PhysicalParameter} of the Cnec is optimized.
     *
     * For instance, in the search-tree-rao, the margin of such a Cnec will be "maximized" (optimized), in case
     * it is the most limiting one.
     */
    boolean isOptimized();

    /**
     * Returns a tag indicating whether or not the {@link PhysicalParameter} of the Cnec is monitored.
     *
     * For instance, in the search-tree-rao, the margin of such a Cnec should remain positive.
     */
    boolean isMonitored();

    /**
     * Getter of the operator of the Cnec
     */
    String getOperator();

    /**
     * Getter of the border of the Cnec
     */
    String getBorder();

    /**
     * Returns the location of the cnec, as a set of optional countries
     *
     * @param network: the network object used to look for the location of the network element of the Cnec
     * @return a set of optional countries containing the cnec location(s). Note that a Cnec on a interconnection can
     * belong to two countries.
     */
    default Set<Optional<Country>> getLocation(Network network) {
        Set<Optional<Country>> locations = new HashSet<>();
        getNetworkElements().forEach(networkElement -> locations.addAll(networkElement.getLocation(network)));
        return locations;
    }
}
