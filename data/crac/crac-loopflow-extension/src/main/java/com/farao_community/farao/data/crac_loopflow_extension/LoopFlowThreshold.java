/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.commons.extensions.Extension;

/**
 * A loopFlowThreshold limits the loop-flow on a given FlowCnec
 *
 * Contrary to the BranchThreshold, the LoopFlowThresholds operates on both direction of a
 * CNEC. That is to say, the loop-flow on the CNEC should remains in the interval:
 * [-getThreshold() ; getThreshold()]
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface LoopFlowThreshold extends Extension<FlowCnec> {

    @Override
    default String getName() {
        return "LoopFlowThreshold";
    }

    /**
     * Get the native value of the Threshold, given in getUnit()
     */
    double getValue();

    /**
     * Get the native unit of the Threshold
     */
    Unit getUnit();

    /**
     * Get the loopFlow threshold in a given flow unit
     */
    double getThreshold(Unit requestedUnit);

    /**
     * Get the loopFlow threshold in a given flow unit, taking into account the reliability margin
     * of the CNEC
     */
    double getThresholdWithReliabilityMargin(Unit requestedUnit);

}
