/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.threshold.TerminalThreshold;

import java.util.Optional;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface TerminalCnec extends Cnec<TerminalCnec> {

    /**
     * Getter of the {@link TerminalThreshold} that limit the physical parameter of this {@code TerminalCnec}.
     *
     * @return The set of {@link TerminalThreshold} defined on the {@code TerminalCnec}.
     */
    Set<TerminalThreshold> getThresholds();

    /**
     * Enables to add a {@link TerminalThreshold} to the {@code TerminalCnec}.
     *
     * @param terminalThreshold: The {@link TerminalThreshold} to be added to the {@code TerminalCnec}
     */
    void addThreshold(TerminalThreshold terminalThreshold);

    /**
     * Getter of the nominal voltage of the {@code TerminalCnec}. This value is related to network information so
     * it might be unavailable before synchronization.
     *
     * @return The value of nominal voltage. It could be {@code null} if the {@code TerminalCnec} has not been synchronized.
     */
    Double getNominalVoltage();

    /**
     * A margin can be computed on a {@code TerminalCnec}. It is the worst (minimal including negative) difference
     * between the {@code actualValue} and the {@code thresholds}. The {@link Unit} is the one of the
     * {@code actualValue} and will be the one of the returned margin. This margin will take the
     * {@code reliabilityMargin} into account.
     * If the margin is positive, it means that the limits of the {@code thresholds} are respected. If it is negative,
     * it means that that a limit of the {@code thresholds} has been overcome.
     *
     * margin = min(maxThreshold - actualValue, actualValue - minThreshold)
     *
     * @param actualValue: Value of the {@link PhysicalParameter} on the {@code side} of the {@code TerminalCnec}
     *                   on which to make the difference to compute the margin.
     * @param unit: Unit of the {@code actualValue}. It will also be the one of the returned value.
     * @return The margin of the {@code TerminalCnec} with the given {@code unit} taking {@code reliabilityMargin}
     * into account.
     */
    double computeMargin(double actualValue, Unit unit);

    /**
     * Getter that returns the lower acceptable value of the {@link PhysicalParameter} with the given {@link Unit}.
     * The {@code unit} must match the {@link PhysicalParameter} of the {@code TerminalCnec}. It returns an optional
     * because the {@code TerminalCnec} is not necessarily bounded by a lower value. This value would take
     * {@code reliabilityMargin} into account.
     *
     * @param unit: The unit in which the bound would be returned. It could require conversions if the thresholds are
     *            defined in a different unit that the one requested.
     * @return The lower bound of the {@link PhysicalParameter} on this {@code TerminalCnec}.
     */
    Optional<Double> getLowerBound(Unit unit);

    /**
     * Getter that returns the upper acceptable value of the {@link PhysicalParameter} with the given {@link Unit}.
     * The {@code unit} must match the {@link PhysicalParameter} of the {@code TerminalCnec}. It returns an optional
     * because the {@code TerminalCnec} is not necessarily bounded by an upper value. This value would take
     * {@code reliabilityMargin} into account.
     *
     * @param unit: The unit in which the upper bound would be returned. It could require conversions if the
     *            thresholds are defined in a different unit that the one requested.
     * @return The upper bound of the {@link PhysicalParameter} on this {@code TerminalCnec}.
     */
    Optional<Double> getUpperBound(Unit unit);
}
