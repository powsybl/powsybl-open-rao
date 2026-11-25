/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering;

import com.powsybl.openrao.raoimpl.result.api.FlowResult;

import java.util.OptionalDouble;

/**
 * How the linear problem aggregates the margins of the optimized flow CNECs.
 *
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
public sealed interface MarginCriterion {

    /**
     * Minimum absolute margin.
     *
     * @param secureThreshold empty when the minimum margin is maximized (SECURE_FLOW, MAX_MIN_MARGIN),
     *                        the shifted-violation threshold when only its violation is penalized (MIN_COST)
     */
    record Absolute(OptionalDouble secureThreshold) implements MarginCriterion {
    }

    /**
     * Minimum relative margin (MAX_MIN_RELATIVE_MARGIN).
     *
     * @param ptdfSource        flow result providing the PTDF zonal sums used by the linear problem,
     *                          {@code null} to use the flow result of the linearization point
     * @param ptdfSumLowerBound lower bound applied to the PTDF zonal sums
     */
    record Relative(FlowResult ptdfSource, double ptdfSumLowerBound) implements MarginCriterion {
    }
}
