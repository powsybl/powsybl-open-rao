/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record PstRegulationInput(PstRangeAction pstRangeAction, TwoSides limitingSide, double limitingThreshold) {
    public static PstRegulationInput of(PstRangeAction pstRangeAction, String monitoredNetworkElement, Crac crac) {
        Instant lastInstant = crac.getLastInstant();
        Set<FlowCnec> curativeFlowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> monitoredNetworkElement.equals(flowCnec.getNetworkElement().getId()))
            .filter(flowCnec -> lastInstant.equals(flowCnec.getState().getInstant()))
            .collect(Collectors.toSet());
        if (curativeFlowCnecs.isEmpty()) {
            return null;
        }
        double thresholdOne = getMostLimitingThreshold(curativeFlowCnecs, TwoSides.ONE);
        double thresholdTwo = getMostLimitingThreshold(curativeFlowCnecs, TwoSides.TWO);
        return thresholdOne <= thresholdTwo ? new PstRegulationInput(pstRangeAction, TwoSides.ONE, thresholdOne) : new PstRegulationInput(pstRangeAction, TwoSides.TWO, thresholdTwo);
    }

    private static double getMostLimitingThreshold(Set<FlowCnec> curativeFlowCnecs, TwoSides twoSides) {
        return Math.min(
            Math.abs(curativeFlowCnecs.stream().map(flowCnec -> flowCnec.getUpperBound(twoSides, Unit.AMPERE).orElse(Double.MAX_VALUE)).min(Double::compareTo).orElse(Double.MAX_VALUE)),
            Math.abs(curativeFlowCnecs.stream().map(flowCnec -> flowCnec.getLowerBound(twoSides, Unit.AMPERE).orElse(-Double.MAX_VALUE)).max(Double::compareTo).orElse(Double.MAX_VALUE))
        );
    }
}
