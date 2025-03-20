/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.Arrays;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractFlowRaoResult implements RaoResult {
    @Override
    public boolean isSecure(Instant optimizedInstant, PhysicalParameter... u) {
        if (ComputationStatus.FAILURE.equals(getComputationStatus())) {
            return false;
        }
        if (Arrays.stream(u).noneMatch(PhysicalParameter.FLOW::equals)) {
            throw new OpenRaoException("This is a flow RaoResult, isSecure is available for FLOW physical parameter");
        }
        if (anyOverload(optimizedInstant)) {
            return false;
        }
        if (Arrays.stream(u).anyMatch(physicalParameter -> !PhysicalParameter.FLOW.equals(physicalParameter))) {
            throw new OpenRaoException("This is a flow RaoResult, flows are secure but other physical parameters' security status is unknown");
        }
        return true;
    }

    private boolean anyOverload(Instant optimizedInstant) {
        if (getVirtualCostNames().contains("min-margin-violation-evaluator")) {
            return getVirtualCost(optimizedInstant, "min-margin-violation-evaluator") > 0;
        }
        return getFunctionalCost(optimizedInstant) >= 0;
    }
}
