/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ParametersProvider {
    private static final CoreParameters CORE_PARAMETERS = new CoreParameters();
    private static final MnecParameters MNEC_PARAMETERS = new MnecParameters();
    private static final MaxMinMarginParameters MAX_MIN_MARGIN_PARAMETERS = new MaxMinMarginParameters();
    private static final MaxMinRelativeMarginParameters MAX_MIN_RELATIVE_MARGIN_PARAMETERS = new MaxMinRelativeMarginParameters();
    private static final LoopFlowParameters LOOP_FLOW_PARAMETERS = new LoopFlowParameters();
    private static final UnoptimizedCnecParameters UNOPTIMIZED_CNEC_PARAMETERS = new UnoptimizedCnecParameters();
    private static final IteratingLinearOptimizerParameters ITERATING_LINEAR_OPTIMIZER_PARAMETERS = new IteratingLinearOptimizerParameters();

    private ParametersProvider() {
        // Should not be instantiated
    }

    public static RaoParameters.ObjectiveFunction getObjectiveFuntion() {
        return getCoreParameters().getObjectiveFunction();
    }

    public static Unit getUnit() {
        return getObjectiveFuntion().getUnit();
    }

    public static boolean hasRelativeMargins() {
        return getObjectiveFuntion() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE
            || getObjectiveFuntion() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
    }

    public static boolean isRaoWithLoopFlowLimitation() {
        return getCoreParameters().isRaoWithLoopFlowLimitation();
    }

    public static CoreParameters getCoreParameters() {
        return CORE_PARAMETERS;
    }

    public static MnecParameters getMnecParameters() {
        return MNEC_PARAMETERS;
    }

    public static MaxMinMarginParameters getMaxMinMarginParameters() {
        return MAX_MIN_MARGIN_PARAMETERS;
    }

    public static MaxMinRelativeMarginParameters getMaxMinRelativeMarginParameters() {
        return MAX_MIN_RELATIVE_MARGIN_PARAMETERS;
    }

    public static LoopFlowParameters getLoopFlowParameters() {
        return LOOP_FLOW_PARAMETERS;
    }

    public static UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return UNOPTIMIZED_CNEC_PARAMETERS;
    }

    public static IteratingLinearOptimizerParameters getIteratingLinearOptimizerParameters() {
        return ITERATING_LINEAR_OPTIMIZER_PARAMETERS;
    }
}
