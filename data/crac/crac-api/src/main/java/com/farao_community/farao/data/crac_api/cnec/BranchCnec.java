/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;

import java.util.Optional;
import java.util.Set;

/**
 * Specific type of {@link Cnec} that is defined on a branch of the network.
 *
 * It presents the singularity of having two sides that could have different voltage levels
 * and independent {@link BranchThreshold}. These thresholds will limit the {@link PhysicalParameter}
 * defined for this {@code BranchCnec}.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface BranchCnec extends Cnec<BranchCnec> {

    /**
     * Getter of the {@link BranchThreshold} below which the {@link PhysicalParameter} of this {@code BranchCnec}
     * should ideally remain.
     */
    Set<BranchThreshold> getThresholds();

    /**
     * Getter of the nominal voltage on each {@link Side} of the {@code BranchCnec}. These values are related to
     * network information so it might be unavailable before synchronization.
     *
     * @param side: The {@link Side} on which the nominal voltage is queried.
     * @return The value of nominal voltage. It could be {@code null} if the {@code BranchCnec} has not been synchronized.
     */
    Double getNominalVoltage(Side side);

    /**
     * Getter that returns the lower acceptable value of the {@link PhysicalParameter} with the given {@link Unit}.
     * The {@code unit} must match the {@link PhysicalParameter} of the {@code BranchCnec}. It returns an optional
     * because the {@code BranchCnec} is not necessarily bounded by a lower value. This value would take
     * {@code reliabilityMargin} into account.
     *
     * @param side: The side on which the lower bound is queried. It could require conversions if the thresholds are
     *            defined on a different side that the one requested.
     * @param unit: The unit in which the bound would be returned. It could require conversions if the thresholds are
     *            defined in a different unit that the one requested.
     * @return The lower bound of the {@link PhysicalParameter} on this {@code BranchCnec}.
     */
    Optional<Double> getLowerBound(Side side, Unit unit);

    /**
     * Getter that returns the upper acceptable value of the {@link PhysicalParameter} with the given {@link Unit}.
     * The {@code unit} must match the {@link PhysicalParameter} of the {@code BranchCnec}. It returns an optional
     * because the {@code BranchCnec} is not necessarily bounded by an upper value. This value would take
     * {@code reliabilityMargin} into account.
     *
     * @param side: The side on which the upper bound is queried. It could require conversions if the thresholds are
     *            defined on a different side that the one requested.
     * @param unit: The unit in which the upper bound would be returned. It could require conversions if the thresholds are
     *            defined in a different unit that the one requested.
     * @return The upper bound of the {@link PhysicalParameter} on this {@code BranchCnec}.
     */
    Optional<Double> getUpperBound(Side side, Unit unit);

    /**
     * A margin can be computed on a {@code BranchCnec}. It is the worst (minimal including negative) difference
     * between the {@code actualValue} and the {@code thresholds}. The {@link Unit} is the one of the
     * {@code actualValue} and will be the one of the returned margin. This margin will take the
     * {@code reliabilityMargin} into account.
     * If the margin is positive, it means that the limits of the {@code thresholds} are respected. If it is negative,
     * it means that that a limit of the {@code thresholds} has been overcome.
     *
     * margin = min(maxThreshold - actualValue, actualValue - minThreshold)
     *
     * @param actualValue: Value of the {@link PhysicalParameter} on the {@code side} of the {@code BranchCnec}
     *                   on which to make the difference to compute the margin.
     * @param side: The side on which the {@code actualValue} is taken and on which the margin will be computed.
     * @param unit: Unit of the {@code actualValue}. It will also be the one of the returned value.
     * @return The margin of the {@code BranchCnec} on the given {@code side} with the given {@code unit} taking
     * {@code reliabilityMargin} into account.
     */
    double computeMargin(double actualValue, Side side, Unit unit);

    // deprecated methods
    /**
     * Enables to add a {@link BranchThreshold} to the {@code BranchCnec}.
     *
     * @param branchThreshold: The {@link BranchThreshold} to be added to the {@code BranchCnec}
     * @deprecated You will not be able to add thresholds to created CNECs anymore. Please use the {@link FlowCnecAdder} with the newThreshold() method accordingly.
     */
    @Deprecated
    void addThreshold(BranchThreshold branchThreshold);
}
