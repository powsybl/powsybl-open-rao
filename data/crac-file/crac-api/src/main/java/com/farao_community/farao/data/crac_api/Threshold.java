/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * The threshold describes the monitored physical parameter of a Cnec (flow, voltage
 * or angle), as well as the domain within which the Cnec can be operated securely. This
 * domain is defined by a min and/or a max. If the two are defined, the secure operating
 * range of the Cnec is [min, max], otherwise it is [-infinity, max] or [min, +infinity].
 *
 * Some Threshold need to be synchronized with a Network to be fully defined (for instance,
 * a Threshold defined as a percentage of a maximum Network limit). If a Threshold is not
 * synchronised, most of its method can throw SynchronizationExceptions.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public interface Threshold {

    /**
     * A Threshold is based on a given Unit (MEGAWATT, AMPERE, DEGREE or
     * KILOVOLT). This Unit can be retrieved by the getUnit() method.
     */
    Unit getUnit();

    /**
     * If it is defined, this function returns the minimum limit of the Threshold,
     * below which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded below.
     * The returned value is given with the unit of measure of the Threshold,
     * which can be obtained with getUnit().
     */
    @JsonIgnore
    Optional<Double> getMinThreshold() throws SynchronizationException;

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * above which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     * The returned value is given with the Unit given in argument of the function.
     */
    @JsonIgnore
    Optional<Double> getMinThreshold(Unit unit) throws SynchronizationException;

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * above which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     * The returned value is given with the unit of measure of the Threshold,
     * which can be obtained with getUnit().
     */
    @JsonIgnore
    Optional<Double> getMaxThreshold() throws SynchronizationException;

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * above which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     * The returned value is given with the unit given in argument of the function.
     */
    @JsonIgnore
    Optional<Double> getMaxThreshold(Unit unit) throws SynchronizationException;

    /**
     * This function returns, for a given Network situation, a boolean which specify
     * whether or not the minimum limit of the Threshold has been overcome.
     */
    boolean isMinThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException;

    /**
     * This function returns, for a given Network situation, a boolean which specify
     * whether or not the maximum limit of the Threshold has been overcome.
     */
    boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException;

    /**
     * This function returns, for a given Network situation, the margin of the Threshold.
     * The margin is the distance, given with the unit of measure of the Threshold, from
     * the actual monitored physical parameter of a Cnec to the Threshold limits. If it is
     * positive, it means that the limits of the Threshold are respected. If it is negative,
     * it means that that a limit of the Threshold has been overcome.
     *
     * margin = min(maxThreshold - actualValue, actualValue - minThreshold)
     */
    double computeMargin(Network network, Cnec cnec) throws SynchronizationException;

    void synchronize(Network network, Cnec cnec);

    void desynchronize();
}
