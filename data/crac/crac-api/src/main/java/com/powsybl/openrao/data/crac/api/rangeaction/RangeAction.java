/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * Remedial action interface specifying an action of type range.
 * <p>
 * When applying a Range Action, a setpoint (double value) must be set. This setpoint
 * must be included within a range, delimited by minimum and maximum values.
 * <p>
 * The apply method therefore involves a {@link Network} and a setpoint (double value).
 * The presence of this double in the apply() method explains why this interface
 * has been designed besides the {@link NetworkAction} interface
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RangeAction<T extends RangeAction<T>> extends RemedialAction<T> {

    /**
     * Apply the action on a given network, with a given setpoint
     */
    void apply(Network network, double setpoint);

    /**
     * Get the lower bound of the range within which the setpoint must remain
     */
    double getMinAdmissibleSetpoint(double previousInstantSetpoint);

    /**
     * Get the upper bound of the range within which the setpoint must remain
     */
    double getMaxAdmissibleSetpoint(double previousInstantSetpoint);

    /**
     * Get the value of the setpoint of the Range Action for a given Network
     */
    double getCurrentSetpoint(Network network);

    /**
     * Get the groupId of the Range Action. All Range Action which share the
     * same groupId should have the same setpoint.
     */
    Optional<String> getGroupId();

    /**
     * Get the variation cost to increase or decrease the setpoint by one unit.
     */
    Optional<Double> getVariationCost(VariationDirection variationDirection);

    /**
     * Get the total cost to spend to increase or decrease the setpoint by a given amount.
     */
    default double getTotalCostForVariation(Double variation) {
        if (Math.abs(variation) < 1e-6) {
            return 0.;
        }
        double activationCost = getActivationCost().orElse(0.);
        double variationCost = getVariationCost(variation > 0 ? VariationDirection.UP : VariationDirection.DOWN).orElse(0.) * Math.abs(variation);
        return activationCost + variationCost;
    }
}
