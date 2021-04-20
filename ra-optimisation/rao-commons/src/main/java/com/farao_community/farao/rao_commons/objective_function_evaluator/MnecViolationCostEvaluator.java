/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MnecParameters;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MnecViolationCostEvaluator.class);

    private final Set<BranchCnec> cnecs;
    private final Map<BranchCnec, Double> initialFlows;
    private final Unit unit;
    private final double mnecAcceptableMarginDiminution;
    private final double mnecViolationCost;

    public MnecViolationCostEvaluator(Set<BranchCnec> cnecs, Map<BranchCnec, Double> initialFlows, Unit unit, MnecParameters mnecParameters) {
        this.cnecs = cnecs;
        this.initialFlows = initialFlows;
        this.unit = unit;
        mnecAcceptableMarginDiminution = mnecParameters.getMnecAcceptableMarginDiminution();
        mnecViolationCost = mnecParameters.getMnecViolationCost();
        if (unit != MEGAWATT && unit != AMPERE) {
            throw new NotImplementedException("MNEC violation cost is only implemented in MW and AMPERE units");
        }
    }

    @Override
    public double computeCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        if (Math.abs(mnecViolationCost) < 1e-10) {
            return 0;
        }
        double totalMnecMarginViolation = 0;
        boolean mnecsSkipped = false;
        for (BranchCnec cnec : cnecs) {
            if (cnec.isMonitored()) {
                double initialFlow = initialFlows.get(cnec);
                if (Double.isNaN(initialFlow)) {
                    // Sensitivity results are not available, skip cnec
                    // (happens on search tree rao rootleaf evaluation)
                    mnecsSkipped = true;
                    continue;
                }
                double initialMargin = cnec.computeMargin(initialFlow, Side.LEFT, unit);
                double newFlow = (unit == MEGAWATT) ? sensitivityAndLoopflowResults.getSystematicSensitivityResult().getReferenceFlow(cnec) :
                        sensitivityAndLoopflowResults.getSystematicSensitivityResult().getReferenceIntensity(cnec);
                double newMargin = cnec.computeMargin(newFlow, Side.LEFT, unit);
                // The acceptable margin diminution parameter is defined in MW, so if the minimum margin is defined in ampere,
                // appropriate conversion coefficient should be used.
                double convertedAcceptableMarginDiminution = mnecAcceptableMarginDiminution * RaoUtil.getBranchFlowUnitMultiplier(cnec, Side.LEFT, MEGAWATT, unit);
                totalMnecMarginViolation += Math.max(0, Math.min(0, initialMargin - convertedAcceptableMarginDiminution) - newMargin);
            }
        }
        if (mnecsSkipped) {
            LOGGER.warn("Some MNECs were skipped during violation cost evaluation, because their initial flow results were not available.");
        }
        return mnecViolationCost * totalMnecMarginViolation;
    }

    @Override
    public Unit getUnit() {
        return this.unit;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, int numberOfElements) {
        throw new NotImplementedException("getMostLimitingElements() not implemented yet for mnec evaluators");
    }
}
