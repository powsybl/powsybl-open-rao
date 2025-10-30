/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.cnec;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

import java.util.Optional;
import java.util.Set;

/**
 * Specific type of {@link Cnec} that monitors phase angle shift between two ends of a branch.
 *
 * The angle direction is defined by having an exporting element and an importing element.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface AngleCnec extends Cnec<AngleCnec> {

    /**
     * Getter of the {@link NetworkElement} of the exporting end of the branch.
     */
    NetworkElement getExportingNetworkElement();

    /**
     * Getter of the {@link NetworkElement} of the importing end of the branch.
     */
    NetworkElement getImportingNetworkElement();

    /**
     * Getter of the {@link Threshold}s that the {@link PhysicalParameter} of this {@code AngleCnec}
     * should ideally meet.
     */
    Set<Threshold> getThresholds();

    /**
     * Getter that returns the lower acceptable value of the {@link PhysicalParameter} with the given {@link Unit}.
     * The {@code unit} must match the {@link PhysicalParameter} of the {@code AngleCnec}. It returns an optional
     * because the {@code AngleCnec} is not necessarily bounded by a lower value. This value would take
     * {@code reliabilityMargin} into account.
     *
     * @param unit The unit in which the lower bound would be returned. The only accepted unit for now is DEGREE.
     * @return The lower bound of the {@link PhysicalParameter} on this {@code AngleCnec}.
     */
    Optional<Double> getLowerBound(Unit unit);

    /**
     * Getter that returns the upper acceptable value of the {@link PhysicalParameter} with the given {@link Unit}.
     * The {@code unit} must match the {@link PhysicalParameter} of the {@code AngleCnec}. It returns an optional
     * because the {@code AngleCnec} is not necessarily bounded by an upper value. This value would take
     * {@code reliabilityMargin} into account.
     *
     * @param unit The unit in which the upper bound would be returned. The only accepted unit for now is DEGREE.
     * @return The upper bound of the {@link PhysicalParameter} on this {@code AngleCnec}.
     */
    Optional<Double> getUpperBound(Unit unit);

    /**
     * A margin can be computed on an {@code AngleCnec}. It is the worst (minimal including negative) difference
     * between the {@code actualValue} and the {@code thresholds}. The {@link Unit} is the one of the
     * {@code actualValue} and will be the one of the returned margin. This margin will take the
     * {@code reliabilityMargin} into account.
     * If the margin is positive, it means that the limits of the {@code thresholds} are respected. If it is negative,
     * it means that that a limit of the {@code thresholds} has been overcome.
     *
     * margin = min(maxThreshold - actualValue, actualValue - minThreshold)
     *
     * @param actualValue Value of the {@link PhysicalParameter} on the {@code side} of the {@code AngleCnec}
     *                   on which to make the difference to compute the margin.
     * @param unit Unit of the {@code actualValue}. It will also be the one of the returned value. The only accepted
     *            unit for now is DEGREE.
     * @return The margin of the {@code AngleCnec} on the given {@code side} with the given {@code unit} taking
     * {@code reliabilityMargin} into account.
     */
}
