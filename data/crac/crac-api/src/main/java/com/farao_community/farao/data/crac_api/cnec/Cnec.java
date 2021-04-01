/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.data.crac_api.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Optional;
import java.util.Set;

/**
 * Interface for Critical Network Element &amp; Contingencies
 * It represents a {@link NetworkElement} that can be either monitored, optimized or both in a RAO. It is defined
 * for a specific {@link State} of the optimization.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface Cnec<I extends Cnec<I>> extends Identifiable<I>, Synchronizable {

    /**
     * Getter of the {@link State} on which the {@code Cnec} is defined.
     *
     * @return The related {@link State} of the {@code Cnec}
     */
    State getState();

    /**
     * Getter of the {@link NetworkElement} on which the {@code Cnec} is defined.
     *
     * @return The related {@link NetworkElement} of the {@code Cnec}
     */
    NetworkElement getNetworkElement();

    /**
     * Getter of the reliability margin. This value enables to take a margin compared to what is defined in
     * the {@code thresholds} of the {@code Cnec}. This margin would be common to all {@code thresholds}.
     * It should be 0 by default to ensure that there is no inconsistencies with {@code thresholds}.
     *
     * @return The reliability margin defined on this {@code Cnec}.
     */
    double getReliabilityMargin();

    /**
     * Setter of the reliability margin.
     *
     * @param reliabilityMargin : Value of the margin. If negative it would be more permissive than what would be
     *                          defined in the {@code thresholds}.
     */
    void setReliabilityMargin(double reliabilityMargin);

    /**
     * Enables to do an entire deep copy of a {@code Cnec}.
     *
     * @return An object of the specific type {@link I} with copied inner objects.
     */
    I copy();

    /**
     * Enables to do a deep copy of a {@code Cnec} by replacing {@code networkElement} and {@code state} with already
     * created objects. The main use is to ensure objects consistency within the {@link Crac}.
     *
     * @return An object of the specific type {@link I} with copied inner objects.
     */
    I copy(NetworkElement networkElement, State state);

    /**
     * Getter of the {@link PhysicalParameter} representing the {@code Cnec}.
     * It defines the physical value that will be monitored/optimized for this {@code Cnec}.
     *
     * @return The physical parameter that will be studied for this {@code Cnec}.
     */
    PhysicalParameter getPhysicalParameter();

    /**
     * Returns if the margin of the branch should be "maximized" (optimized), in case it is the most limiting one.
     *
     * @return True if the branch is optimized.
     */
    boolean isOptimized();

    void setOptimized(boolean optimized);

    /**
     * Returns if the margin of the branch should stay positive (or above its initial value).
     *
     * @return True if the branch is monitored.
     */
    boolean isMonitored();

    void setMonitored(boolean monitored);

    /**
     * Returns the operator of the CNEC
     * @return the name of the operator (string)
     */
    String getOperator();

    /**
     * Returns the location of the cnec, as a set of optional countries
     * @param network: the network object used to look for the network element of the cnec
     * @return a set of optional countries containing the cnec
     */
    default Set<Optional<Country>> getLocation(Network network) {
        return getNetworkElement().getLocation(network);
    }
}
