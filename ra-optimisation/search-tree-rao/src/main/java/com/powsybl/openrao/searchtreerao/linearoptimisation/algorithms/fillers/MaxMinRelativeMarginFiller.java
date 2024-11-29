/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MaxMinRelativeMarginFiller extends MaxMinMarginFiller {
    private final FlowResult preOptimFlowResult;
    private final PtdfApproximation ptdfApproximationLevel;
    private final Unit unit;
    private final double ptdfSumLowerBound;
    private final double highestThreshold;
    private final double maxPositiveRelativeRam;
    private final double maxNegativeRelativeRam;

    public MaxMinRelativeMarginFiller(Set<FlowCnec> optimizedCnecs,
                                      FlowResult preOptimFlowResult,
                                      Unit unit,
                                      RelativeMarginsParameters maxMinRelativeMarginParameters) {
        super(optimizedCnecs, unit);
        this.preOptimFlowResult = preOptimFlowResult;
        this.ptdfApproximationLevel = maxMinRelativeMarginParameters.getPtdfApproximation();
        this.unit = unit;
        this.ptdfSumLowerBound = maxMinRelativeMarginParameters.getPtdfSumLowerBound();
        this.highestThreshold = RaoUtil.getLargestCnecThreshold(optimizedCnecs, MEGAWATT);
        this.maxPositiveRelativeRam = highestThreshold / ptdfSumLowerBound;
        this.maxNegativeRelativeRam = 5 * maxPositiveRelativeRam;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        super.fill(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
        buildMinimumRelativeMarginSignBinaryVariable(linearProblem);
        updateMinimumNegativeMarginDefinition(linearProblem);
        Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(optimizedCnecs, sensitivityResult);
        buildMinimumRelativeMarginVariable(linearProblem, validFlowCnecs);
        FlowResult flowResultToUse = ptdfApproximationLevel.shouldUpdatePtdfWithPstChange() ? flowResult : preOptimFlowResult;
        buildMinimumRelativeMarginConstraints(linearProblem, validFlowCnecs, flowResultToUse);
        fillObjectiveWithMinRelMargin(linearProblem);
    }

    private void updateMinimumNegativeMarginDefinition(LinearProblem linearProblem) {
        OpenRaoMPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        OpenRaoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();
        double maxNegativeRam = 5 * highestThreshold;

        // Minimum Margin is negative or zero
        minimumMarginVariable.setUb(.0);
        // Forcing miminumRelativeMarginSignBinaryVariable to 0 when minimumMarginVariable is negative
        OpenRaoMPConstraint minimumRelMarginSignDefinition = linearProblem.addMinimumRelMarginSignDefinitionConstraint(-linearProblem.infinity(), maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(minRelMarginSignBinaryVariable, maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(minimumMarginVariable, -1);
    }

    /**
     * Add a new minimum relative margin variable. Unfortunately, we cannot force it to be positive since it
     * should be able to be negative in unsecured cases (see constraints)
     */
    private void buildMinimumRelativeMarginVariable(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        if (!validFlowCnecs.isEmpty()) {
            linearProblem.addMinimumRelativeMarginVariable(-linearProblem.infinity(), linearProblem.infinity());
        } else {
            // if there is no Cnecs, the minRelativeMarginVariable is forced to zero.
            // otherwise it would be unbounded in the LP
            linearProblem.addMinimumRelativeMarginVariable(0.0, 0.0);
        }
    }

    /**
     * Build the  miminum relative margin sign binary variable, P.
     * P represents the sign of the minimum margin.
     */
    private void buildMinimumRelativeMarginSignBinaryVariable(LinearProblem linearProblem) {
        linearProblem.addMinimumRelativeMarginSignBinaryVariable();
    }

    /**
     * Define the minimum relative margin (like absolute margin but by dividing by sum of PTDFs)
     */
    private void buildMinimumRelativeMarginConstraints(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs, FlowResult flowResult) {
        OpenRaoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        OpenRaoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();

        // Minimum Relative Margin is positive or null
        minRelMarginVariable.setLb(.0);
        // Forcing minRelMarginVariable to 0 when minimumMarginVariable is negative
        OpenRaoMPConstraint minimumRelativeMarginSetToZero = linearProblem.addMinimumRelMarginSetToZeroConstraint(-linearProblem.infinity(), 0);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginSignBinaryVariable, -maxPositiveRelativeRam);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginVariable, 1);

        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side ->
            setOrUpdateRelativeMarginCoefficients(linearProblem, flowResult, cnec, side)
        ));
    }

    private void setOrUpdateRelativeMarginCoefficients(LinearProblem linearProblem, FlowResult flowResult, FlowCnec cnec, TwoSides side) {
        OpenRaoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        OpenRaoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);

        double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(cnec, side, unit, MEGAWATT);
        // If PTDF computation failed for some reason, instead of ignoring the CNEC completely, set its PTDF to the lowest value
        double relMarginCoef = Double.isNaN(flowResult.getPtdfZonalSum(cnec, side)) ?
            ptdfSumLowerBound : Math.max(flowResult.getPtdfZonalSum(cnec, side), ptdfSumLowerBound);

        Optional<Double> minFlow = cnec.getLowerBound(side, MEGAWATT);
        Optional<Double> maxFlow = cnec.getUpperBound(side, MEGAWATT);

        if (minFlow.isPresent()) {
            OpenRaoMPConstraint minimumMarginNegative;
            try {
                minimumMarginNegative = linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
            } catch (OpenRaoException ignored) {
                minimumMarginNegative = linearProblem.addMinimumRelativeMarginConstraint(-linearProblem.infinity(), linearProblem.infinity(), cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
            }
            minimumMarginNegative.setUb(-minFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginNegative.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
            minimumMarginNegative.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginNegative.setCoefficient(flowVariable, -1);
        }
        if (maxFlow.isPresent()) {
            OpenRaoMPConstraint minimumMarginPositive;
            try {
                minimumMarginPositive = linearProblem.getMinimumRelativeMarginConstraint(cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
            } catch (OpenRaoException ignored) {
                minimumMarginPositive = linearProblem.addMinimumRelativeMarginConstraint(-linearProblem.infinity(), linearProblem.infinity(), cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
            }
            minimumMarginPositive.setUb(maxFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginPositive.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
            minimumMarginPositive.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
            minimumMarginPositive.setCoefficient(flowVariable, 1);
        }
    }

    private void fillObjectiveWithMinRelMargin(LinearProblem linearProblem) {
        OpenRaoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        linearProblem.getObjective().setCoefficient(minRelMarginVariable, -1);
    }
}
