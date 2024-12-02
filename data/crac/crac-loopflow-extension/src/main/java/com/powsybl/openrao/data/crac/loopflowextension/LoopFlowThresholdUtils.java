/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class LoopFlowThresholdUtils {
    private LoopFlowThresholdUtils() {
        // should not be used
    }

    static void checkAttributes(Double thresholdValue, Unit thresholdUnit) {
        if (thresholdValue == null || Double.isNaN(thresholdValue)) {
            throw new OpenRaoException("Cannot add LoopFlowThreshold without a threshold value. Please use withValue() with a non null value");
        }
        if (thresholdUnit == null) {
            throw new OpenRaoException("Cannot add LoopFlowThreshold without a threshold unit. Please use withUnit() with a non null value");
        }
        if (thresholdValue < 0) {
            throw new OpenRaoException("LoopFlowThresholds must have a positive threshold.");
        }
        if (thresholdUnit.equals(Unit.PERCENT_IMAX) && thresholdValue > 1) {
            throw new OpenRaoException("LoopFlowThresholds in Unit.PERCENT_IMAX must be defined between 0 and 1, where 1 = 100%.");
        }
        if (thresholdUnit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new OpenRaoException("LoopFlowThresholds can only be defined in AMPERE, MEGAWATT or PERCENT_IMAX");
        }
    }
}
