/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

/**
 * Interface for Critical Network Element &amp; Contingencies
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
     *
     * @param actualValue: Actual value on the network element.
     * @param unit: Actual value unit.
     * @return Delta between actual value and threshold value with the proper unit.
     */
    double computeMargin(double actualValue, Unit unit);

    /**
     * A Threshold consists in monitoring a given physical value (FLOW, VOLTAGE
     * or ANGLE). This physical value can be retrieved by the getPhysicalParameter()
     * method.
     *
     * @return Network element physical parameter.
     */
    @JsonIgnore
    PhysicalParameter getPhysicalParameter();

    /**
     * If it is defined, this function returns the minimum limit of the Threshold,
     * below which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded below.
     *
     * @param unit: Determines unit of the returned value.
     * @return Minimum operating value on the network element.
     */
    @JsonIgnore
    Optional<Double> getMinThreshold(Unit unit);

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * above which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     *
     * @param unit: Determines unit of the returned value.
     * @return Maximum operating value on the network element.
     */
    @JsonIgnore
    Optional<Double> getMaxThreshold(Unit unit);

    /**
     * Returns if the margin of the branch should be "maximized" (optimized), in case it is the most limiting one.
     *
     * @return True if the branch is optimized.
     */
    boolean isOptimized();

    /**
     * Returns if the margin of the branch should stay positive (or above its initial value).
     *
     * @return True if the branch is monitored.
     */
    boolean isMonitored();

    /**
     * get FRM value in MW
     *
     * @return FRM value
     */
    double getFrm();
}
